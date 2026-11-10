# 01: Book locks can't be rebuilt after a crash

**What to fix:** A structure book's lock is saved in level saved data, but every fact in it is owned by a chunk and reaches the lock through a one-shot event or an in-memory queue. If one is missed, the book is left permanently locked or never locked, and nothing repairs it. Make a lock rebuildable from the saved structure and the world.

**Blocked by:** None

**Status:** done

**Priority:** P1 (High)

**Estimate:** L

**Source:** Architectural Review 11/9, the structure book locks and structure book placement reviewers.

Related: ticket 08 finding 1 also asks for the book piece to save the lock's kind. Do the two together.

Paths: `sb/` is `src/main/java/mapleflavored/structurebooks/`, and `mixin/` is `src/main/java/mapleflavored/mixin/`.

## Principle

One authority with explicit derivations (Hunt and Thomas). A projection must not become an accidental second authority.

## What the review found

- **Keys are persisted twice.** Keeper keys are saved with the structure as `StructureBookPiece.keepers` (`sb/StructureBookRules.java:160`) and again as `Lock.keys` (`sb/BookLocks.java:163`). The only link is the static `PLANNED` queue (`sb/BookLocks.java:52`, `:88`). The piece's copy is lossy: positions only, no kind (`sb/StructureBookPiece.java:159`). Fortress spawner keys exist only in `BookLocks`.
- **Planning has a hidden side effect.** `withBook` reads as a function from pieces to pieces-with-book, but it also pushes the lock onto the queue through `BookLocks.plan` (`sb/StructureBookRules.java:119-123`, `:152-163`; `sb/StructureBookRule.java:42-53`; `sb/BookLocks.java:49-55`, `:85-89`).
- **A lock has no rebuild path.** `put` is called only from the codec constructor, `recordPlanned` and `markDone` (`sb/BookLocks.java:64-66`, `:163`, `:202`).
- **`done` copies facts the world owns.** Each is written once, by an event: a Cleared spawner through `Spawners.CLEARED` (`sb/BookLocks.java:78`, `:190-197`), a broken shrieker through `markShriekerBroken` (`:117-124`), and a dead keeper through `markKeeperGone` (`:97-104`).
- **The two stores save on different schedules.** Chunks are written every tick (`ChunkMap.saveChunksEagerly`) and on unload. Saved data is written only from `ServerLevel.save`. The reviewer confirmed this with `javap` on the 26.3 jar. A crash or kill between the two leaves the chunk ahead of `BookLocks`:
  - Structure saved, lock not saved: the book was "never locked" (`sb/BookLocks.java:30-31`).
  - Keeper dead, shrieker broken or spawner Cleared in the chunk, `done` not saved: the key can never complete. Only Brute and Evoker locks have an escape, which is Peaceful (`sb/BookLocks.java:174-188`).
- **Worldgen changes are invisible.** `mixin/LevelChunkMixin.java:20-25` hooks `LevelChunk.setBlockState` only, so a keeper shrieker removed while its chunk is still a proto-chunk is never counted.
- **The inconsistency is already detectable.** `/keepers` reports it as `missing` (`sb/KeepersCommand.java:88`), but nothing heals it.

Not verified: a structure planned during the server's final flush may queue its lock after saved data is written. The comment at `sb/BookLocks.java:49-51` covers only worldgen that finishes before `BEFORE_SAVE`. This is inferred from the save order, not observed.

## Decisions

- **What counts as evidence that a keeper mob is gone: only its death or removal.** A keeper mob that is absent may have wandered, unloaded or changed dimension, so a lock is never healed from a missing mob. `BookLocks` stays the authority for a mob key. So that it can't fall far behind the keeper's own chunk, a mob key that becomes done schedules a save of the locks at once (`SavedDataStorage.scheduleSave`, which writes off-thread) and no longer waits for the next autosave. Peaceful is still the escape for a crash before that write lands. Claude made this call following the reviewer's suggestion; Jackson has not reviewed it.
- **A spawner key that is gone is done.** A survival player can't break a spawner that isn't Cleared, so a missing spawner was Cleared first or removed by a command or creative player. This follows the checklist.
- **Block keys heal on every take, not only a refused one.** The lock is checked against the world before the take is decided, so the take that finds every key done goes through.
- **A missing lock is planned again when the book is next read**, from the structure start in the book's chunk references. Each book with no lock is looked up once a session. The lookup can load the structure's start chunk from disk on the server thread; the GameTests cover it only with that chunk already loaded.
- **A key is only checked in a chunk that is loaded and fully generated** (`getChunkNow`). A structure's pieces are all placed by then, so a missing shrieker or spawner there is gone, not pending.
- **The final flush.** At shutdown the server drains worldgen, saves every chunk, then closes each level, which saves saved data once more (`ServerChunkCache.close`). `BookLocks` now also records its queues on `AFTER_SAVE`, so a lock or a forgiven keeper queued during the chunk save reaches that last write. A lock that still misses the file is planned again from its piece. Whether worldgen really queues during the flush was not observed.
- **Structures saved before this change are not covered.** Their pieces carry only `Keepers`, with no kind, and a Fortress's carries no keys at all. They still place their keepers, but their lock can't be planned again from them, so the crash window stays open for them. Dropping that path waits on ticket 08 finding 9.

## Checklist

- [x] The book piece saves the full lock, kind and keys, including a Fortress's spawner keys.
- [x] A saved structure whose book has no entry in `BookLocks` gets its lock planned again from the piece, so a book is never left unlocked by a crash.
- [x] When a take is refused, the remaining block keys in loaded chunks are checked against the world, and a key whose shrieker or spawner is gone is marked done.
- [x] A keeper shrieker removed during worldgen is counted, or its key is forgiven.
- [x] The decision above for keeper mobs is made and recorded here.
- [x] The final-flush case is either shown not to happen or handled.
- [x] GameTests: a lock missing from saved data is rebuilt from its structure, and a lock whose `done` is behind the world heals on a refused take.
