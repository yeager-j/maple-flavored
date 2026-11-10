# 02: Restore spots can't be rebuilt after a crash

**What to fix:** Whether a Restore spot's mob still exists is recorded twice, in level saved data and on the mob, and the two save at different times. A crash between them leaves a spot Occupied forever, so no Blood Moon ever Restores it. Give occupancy one authority, or a way to reconcile the two.

**Blocked by:** None

**Status:** done

**Priority:** P1 (High)

**Estimate:** M

**Source:** Architectural Review 11/9, the strongholds and Blood Moons reviewer, who read 26.3's vanilla sources to check the save timing.

Related: ticket 01 is the same flaw in book locks. Ticket 10 finding 2 asks for one shared "gone for good" rule, which this fix would use.

Paths: `bm/` is `src/main/java/mapleflavored/bloodmoons/`, and `mixin/` is `src/main/java/mapleflavored/mixin/`.

## Principle

One authority with explicit derivations (Hunt and Thomas). Home state according to its lifetime.

## What the review found

The spot holds `state` and `occupant` in level saved data (`bm/RestoreSpot.java:25-33`, `:54-64`; `bm/RestoreSpots.java:33-36`, `:60-72`). The mob holds a `SpotTag` in its entity chunk (`bm/Restoring.java:120-129`). Three event channels keep them in step: death, a destroying unload, and a failed load (`bm/Restoring.java:59-62`, `:87-90`, `:135-141`, `:244-261`, `:286-291`). Nothing ever rebuilds one from the other.

Vanilla stores a chunk's entities when the chunk unloads (`PersistentEntitySectionManager.processChunkUnload`), but saves level saved data only on the save cycle, which autosave runs every 6,000 ticks. So:

- **Crash after a kill.** Kill a Bastion's Brutes, walk away, and crash before the next autosave. The disk has no Brutes and spots still `OCCUPIED`. Those spots never become `EMPTY`, so they never Restore.
- **Crash after generation.** A freshly generated structure's mobs can be saved with tags whose spots were never saved. Those mobs never Restore either.
- **Pruned entity regions.** Deleting entity region files leaves the same orphaned `OCCUPIED` spots.
- **Refused add.** `tagIfPlacing` runs at the head of `ServerLevel.addFreshEntity` and plans the spot even when the add then returns false (`mixin/ServerLevelMixin.java:28-31`).

The failure is silent and permanent. A mob that wandered off legitimately keeps its spot, so "the occupant isn't in its home chunk" can't be used as evidence that it is gone.

## Decisions

Claude made these calls; Jackson has not reviewed them.

- **The mob is the authority for whether its spot is held.** `RestoreSpots` keeps the list of spots and the Empty and Pending cycle. Neither of the merge's two suggestions was enough alone, so the fix uses both: the tag carries the whole spot, and an Occupied spot is emptied when its occupant can be shown not to exist.
- **A living mob that loads holds the spot it carries.** If the level has no such spot, it is recorded from the tag. If the spot is Empty or Pending, or names a mob no level holds, the mob takes it. If a loaded mob holds it, the mob drops its tag. A saved mob that fails to load, as in Peaceful, has a missing spot recorded Empty.
- **Generation only tags the mob.** The spot is recorded when the mob first loads into the level, by the same rule that repairs a crash. The static queue of planned spots is gone, and a refused `addFreshEntity` leaves no spot because the mob never loads.
- **Absence is evidence only in the chunk the occupant was saved in.** A spot records that chunk. While that chunk's entities are loaded and no level holds the occupant, loaded or hidden, the spot is emptied and waits for the next Blood Moon. This is checked on the retry, every 100 ticks, since a chunk that comes back before it finished unloading raises no load event. A mob that wandered off was saved in another chunk, so the spot's own chunk loading without it proves nothing.
- **The saved chunk is recorded at both moments vanilla writes entities.** When a mob unloads somewhere new, the chunk is recorded and the spots are saved at the end of that tick (`SavedDataStorage.scheduleSave`, as ticket 01 does). Before every autosave, each loaded occupant's chunk is refreshed; `ServerLevel.save` writes saved data before entities.
- **The chunk is unknown, and the spot is never emptied for absence,** while the occupant is in another dimension or is saved with a player it rides with.

What is not covered:

- **A crash between a chunk unload's entity write and the spots' write**, when the mob had changed chunk since the spots were last saved. The old chunk loads without the mob and the spot is emptied wrongly. If the mob loads before a Blood Moon refills the spot, it takes the spot back. Otherwise the structure gets one extra mob and the wanderer loses its tag.
- **Spots and mobs saved before this change.** Their spots have no saved chunk until the occupant next loads, and their tags can't record a missing spot. A spot already orphaned by an earlier crash stays Occupied.
- **A spot a crash left Occupied is noticed when its chunk next loads**, not at the Blood Moon itself. It is Restored by the first Blood Moon after that.

## Checklist

- [x] The decision above is made and recorded here.
- [x] A spot left `OCCUPIED` by a crash after its mob died is Restored by a later Blood Moon.
- [x] A structure mob saved with a tag whose spot was never saved gets a spot.
- [x] A refused `addFreshEntity` leaves no spot behind.
- [x] GameTests cover the first two cases by reloading `RestoreSpots` from an older saved state after the world has changed.
