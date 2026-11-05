# Plan: 22 Nether Fortress book lock

## Context / end goal

The enchanting manifesto puts Fire Protection in Nether Fortresses ("common and full of fire"). The project overview makes the Fortress the one structure whose book is *earned* rather than found: it stays locked until every Blaze spawner that generated there is Cleared, so the spawners can remain as farms. A fortress with none starts unlocked, so nobody is blocked for good. Ticket 21 gave us Clearing and the `Spawners.CLEARED` event for exactly this.

Closing the ticket means a fresh world's fortresses each hold one Fire Protection I book. It shows as locked (closed, still, in the spawner forcefield) until its recorded Blaze spawners are all Cleared, wherever the player is when that happens and even if one of them was since broken. A player who tries to take a locked book is told why and shown where the remaining spawners are. Once unlocked, it's taken like any other structure book.

Jackson's calls (2026-11-04):

- **Spot:** above the lava well in the Castle Entrance courtyard. A fortress without one falls back to the centre of its start bridge crossing.
- **Locked look:** a closed, still book (no spin, no bob) wrapped in the same world-border forcefield as an uncleared spawner. Once unlocked it opens and spins like any structure book.
- **Trails (his follow-up idea, after the Creaking):** trying to take a locked book sends trail particles from the book to each recorded spawner not yet Cleared.
- **Every fortress gets a Blaze spawner.** New fortresses are re-planned until they have at least one spawner room. "A fortress with none starts unlocked" stays as the fallback for when every retry fails.

## What the evidence says (26.3 sources)

- Blaze spawners in a fortress come only from `NetherFortressPieces.MonsterThrone` (weight 5, at most 2 per fortress, so 0–2). Its `postProcess` places the spawner at piece-local `(3, 5, 5)` and sets Blaze. So every spawner position is known when the pieces are planned.
- `NetherFortressStructure.generatePieces` calls `moveInsideHeights` *before* `build()`, and nothing moves afterwards. Positions planned in `StructureMixin` are final.
- `CastleEntrance` (at most 1, not guaranteed): floor bricks at y 3–4, air from 5 to 13, a 3×3 brick rim at y 5 from 5 to 7, and lava at `(6, 5, 6)`. `StartPiece` extends `BridgeCrossing` (19×10×19): floor at y 4, air at 5–7 along both arms, centre `(9, *, 9)`.
- `/place structure` and our `Structures` fixture call `placeInChunk` without registering structure references, so `structureManager().getStructureWithPieceAt` can't find such a fortress. The lock record must not depend on structure lookup.
- `Structure.generate` runs on worldgen worker threads with the `ResourceKey<Level> dimension` as an argument, but no `ServerLevel`. `SavedDataStorage` is a plain `HashMap`, so it's main-thread only.
- `SavedDataType(Identifier, Supplier, Codec, DataFixTypes)`. `readTagFromDisk` calls `type.update(...)` directly, so a null `DataFixTypes` would fail on load unless Fabric guards it (check at implementation and pick a no-op type if not).
- The Creaking heart's trails: `new TrailParticleOption(target, color, random.nextInt(40) + 10)` sent with `sendParticles(option, true, true, x, y, z, 1, 0, 0, 0, 0)` (forced, always shown).

## Decisions

1. **Rule.** `minecraft:fortress` → Fire Protection, `Placement.OPEN`, spot `StructureBookSpots.netherFortress`: anchor the `CastleEntrance` at local `(6, 7, 6)`, two above the lava, at head height for a player on the rim. Without one, use the `StartPiece` at local `(9, 6, 9)`. Uses the existing `inCodeBuiltPiece` helper.
2. **Locks are part of a rule.** `StructureBookRule` gains a `SpawnerLock lock` component (`List<BlockPos> spawners(List<StructurePiece> pieces)`, with `SpawnerLock.NONE`). A three-argument constructor keeps every existing rule unchanged. The Fortress lock is `StructureBookSpots.fortressBlazeSpawners`: every `MonsterThrone`'s world `(3, 5, 5)` through `StructurePieceInvoker`.
3. **The record lives in world data: `BookLocks`**, a `SavedData` per level (`maple_flavored:book_locks`). It holds one `Lock(book spot, spawners, cleared)` per locked book, keyed by the book's planned spot, plus a spawner→book index rebuilt on load. The book block entity looks itself up by its own position, so neither the piece nor the block entity carries lock data. Invariant: a locked book's anchor never moves after planning (true for fortress pieces), so its planned spot is where it's placed.
4. **Made at generation, handed to the main thread.** `StructureBookRules.withBook` gains a `dimension` parameter, which `StructureMixin` passes. When a rule's lock finds at least one spawner, it calls `BookLocks.plan(dimension, spot, spawners)`, which only adds to a static `ConcurrentLinkedQueue`. The queue is drained into each level's `BookLocks` (with `putIfAbsent`, so re-planning the same fortress never resets its Cleared set) on `END_SERVER_TICK` and at the start of every `BookLocks` read or write, so no Cleared event or lock check can come before its record. `SERVER_STARTING` clears the queue, so a previous integrated server's leftovers never reach a new world. A fortress with no Blaze spawners gets no record, and a book without a record is unlocked.
5. **Clearing updates the record wherever the book is.** `BookLocks.register()` (from `StructureBooks.register()`) listens on `Spawners.CLEARED`. If the position is indexed, it's added to the Cleared set and the data is marked dirty. Nothing else changes, so breaking a Cleared spawner later never un-counts it. If that unlocks the book and `level.isLoaded(book)` is true, it calls `sendBlockUpdated` so clients re-read the lock. An unloaded book just reads the record when its chunk is sent.
6. **Taking.** `StructureBooks.take` checks the lock after "has an enchantment" and before "has room". While locked, it shows the action-bar message "Locked until every Blaze spawner here is Cleared (%s of %s)", sends the trails, and changes nothing else: no book is given and `TAKEN` doesn't fire.
7. **Trails.** For each remaining spawner, `level.sendParticles(new TrailParticleOption(spawnerCentre, FORCEFIELD_BLUE, random 10..50), true, true, bookCentre, ...)`, with a small burst per spawner as the Creaking heart does. That blue is the world border's stationary colour, the same as the forcefield. Only Cleared positions are skipped. The count and duration are tuned in the hand check.
8. **Client sync.** `StructureBookBlockEntity` gets `getUpdatePacket()` (`ClientboundBlockEntityDataPacket.create(this)`) and `getUpdateTag`, which writes only `locked`, computed on the server from `BookLocks`. `loadAdditional` reads it into a `locked` field, and `isShownLocked()` is the client's copy. The server never saves `locked`. `BookLocks` is the only authority.
9. **Guaranteed Blaze spawner.** New `NetherFortressStructureMixin` uses `@WrapMethod` on the private static `generatePieces(StructurePiecesBuilder, GenerationContext)`. It calls the original. If the builder holds no `MonsterThrone`, it calls `builder.clear()` and runs it again, up to 16 attempts, keeping the last. It's deterministic because each retry keeps drawing from the context's worldgen random. `StartPiece`'s constructor resets the piece-count limits, and `moveInsideHeights` runs inside each attempt. It runs before `StructureMixin` adds the book, so the lock sees the final pieces.
10. **Look.** A new `client/Forcefield` helper is pulled out of `SpawnerForcefieldRenderer` (box faces, UVs, scroll, `energySwirl` with the forcefield texture). Both renderers use it. While locked, `StructureBookRenderer` draws the book closed (`open = 0`) at a fixed angle with no bob, then a forcefield box around the block. Its size is tuned in the hand check.

## API sketch

```java
// mapleflavored.structurebooks
record StructureBookRule(List<ResourceKey<Enchantment>> enchantments, SpotRule spot, Placement placement, SpawnerLock lock) {
	StructureBookRule(enchantments, spot, placement) { this(..., SpawnerLock.NONE); }
	@FunctionalInterface interface SpawnerLock { List<BlockPos> spawners(List<StructurePiece> pieces); SpawnerLock NONE = p -> List.of(); }
}
public static PiecesContainer withBook(Holder<Structure>, PiecesContainer, long seed, ChunkPos, ResourceKey<Level> dimension, TerrainPreview, StructureTemplateManager);

public final class BookLocks extends SavedData {
	static void register();                                    // CLEARED listener, tick drain, SERVER_STARTING reset
	static void plan(ResourceKey<Level> dim, BlockPos book, List<BlockPos> spawners); // any thread
	public static boolean isLocked(ServerLevel level, BlockPos book);
	public static List<BlockPos> remainingSpawners(ServerLevel level, BlockPos book);    // empty when unlocked or unrecorded
	public static Optional<Lock> lockAt(ServerLevel level, BlockPos book);              // for tests
	record Lock(BlockPos book, List<BlockPos> spawners, Set<BlockPos> cleared) { CODEC }
}
// StructureBookBlockEntity: boolean isShownLocked(); getUpdateTag/getUpdatePacket
```

## Files

- `structurebooks/`: new `BookLocks`; `StructureBookRule` (lock); `StructureBookRules` (Fortress rule, `dimension` param, `plan` call); `StructureBookSpots` (`netherFortress`, `fortressBlazeSpawners`); `StructureBooks` (`take` lock check and trails, `BookLocks.register()`); `StructureBookBlockEntity` (sync).
- `mixin/StructureMixin` passes the `dimension` argument. New `mixin/NetherFortressStructureMixin` (the guarantee), registered in `maple_flavored.mixins.json`.
- Client: new `client/Forcefield.java`; `SpawnerForcefieldRenderer` uses it; `StructureBookRenderer` (locked look).
- `en_us.json`: `block.maple_flavored.structure_book.locked`.
- GameTests: new `FortressBookGameTests`, registered in the gametest `fabric.mod.json`; `OrdinaryStructureBookGameTests` End City call gets the new `withBook` argument. Reuse `SpawnerGameTests.killMarkedMobs` and `survivalPlayer`, and the `Structures` fixture.
- Docs: spec line 356 (trails on a refused take; every new fortress generates at least one Blaze spawner, and none means unlocked is the fallback), user story 58 reworded to match, the project overview's Fortress line, ticket 22 boxes and a new criterion for the guarantee; this plan copied to `.scratch/maple-flavored/plans/22-nether-fortress-book-lock.md`. Nothing reused, so `CREDITS.md` is unchanged.

## Acceptance matrix (`FortressBookGameTests`)

Fortresses are planned at far-off chunk positions, scanning a bounded number of positions for the shape each case needs (with a Castle Entrance, without one), since the GameTest world's seed isn't fixed. The no-spawner cases can't come from planning anymore, so they use a hand-built record: `BookLocks.plan` with an empty list is a no-op, and a hand-set book has no record.

| Case | Expected |
| --- | --- |
| Plan 300 fortresses | every one has at least 1 `MonsterThrone`. Log the vanilla rate of fortresses without one, using the original method through an invoker, for the PR |
| The same chunk planned twice | identical piece lists (the retry is deterministic) |
| `enchantmentsFor(FORTRESS)` | `[fire_protection]` |
| Place a fortress with a Castle Entrance | exactly 1 book, at the entrance's world `(6, 7, 6)`, air before placement, holding Fire Protection |
| Fortress without a Castle Entrance | book at the start piece's `(9, 6, 9)` |
| Planned fortress with thrones | `lockAt(book)` lists exactly each throne's world `(3, 5, 5)`, and after building, each is a Blaze spawner |
| Locked book, survival player uses it | refused: inventory unchanged, block still there, no `TAKEN`; update tag `locked = true`; `remainingSpawners` = every recorded spawner |
| **Ticket test:** build every chunk except the book's; Clear every spawner (12 marked deaths each); break one Cleared spawner with a pickaxe; round-trip `BookLocks` through its codec and `set` it back; build the book's chunk | record says all Cleared; the broken one still counts; update tag `locked = false`; the player takes Fire Protection I and `TAKEN` fires once |
| All but one Cleared | still locked; `remainingSpawners` is just that one; the refused take's message counts N−1 of N |
| Book loaded, last spawner Cleared | `isLocked` false straight away; update tag `locked = false` |
| Re-plan the same fortress after Clearing some spawners | Cleared set unchanged |
| Book with no record (the no-spawner fallback): a hand-set Fire Protection book | unlocked; taken straight away |
| A Cleared spawner outside any record | no record changes |
| Saved and loaded structure start, built | book at the same spot, still locked by the same record |
| Existing GameTests | still pass |

## Invariants

- One record per locked book. It's made at planning, its spawner list never changes, and its Cleared set only grows.
- `BookLocks` is the only authority on the lock. The block entity's `locked` is just the client's copy.
- A locked book is never taken: a refused take changes nothing and fires no `TAKEN`.
- A book with no record is unlocked, so a fortress with no Blaze spawners never blocks anyone.
- Every newly planned fortress has at least one Blaze spawner unless all 16 attempts fail. Fortresses already saved are never re-planned.

## Verification

Set `JAVA_HOME` to `C:/Users/jackson/.jdks/jdk-25.0.4.1+1`, then `gradlew build` and `gradlew runGameTest`, and check `build/gametest/report.xml`. Then `larp review --message "Review the diff"`, fix the findings, commit on branch `22-nether-fortress-book-lock`, push, open a ready PR, and watch Codex's review with `/watch-prs`.

## Hand checks (left unchecked on the ticket)

The locked book reads as locked: closed and still in the forcefield. It opens and spins once the last spawner is Cleared, including when that happens out of sight. The trails from a refused take clearly lead toward the remaining spawner rooms. Tune their count and duration, the forcefield's size around the book, and the book's height over the lava well.

## Exclusions

Lock progress on the book itself (it's locked or not), lock records for structures other than the Fortress, Bastion and other wake events, and Blood Moon Restores of Blazes.
