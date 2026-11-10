# 02: Restore spots can't be rebuilt after a crash

**What to fix:** Whether a Restore spot's mob still exists is recorded twice, in level saved data and on the mob, and the two save at different times. A crash between them leaves a spot Occupied forever, so no Blood Moon ever Restores it. Give occupancy one authority, or a way to reconcile the two.

**Blocked by:** None

**Status:** needs-triage

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

## Open decisions

- **Which side is the authority.** These are suggestions from the merge, not from the reviewer:
  - Make the mob's tag carry everything a spot needs, so spots can be rebuilt from loaded mobs and a tag with no spot plans one.
  - Or keep the spot as the authority and, at each Blood Moon, treat an `OCCUPIED` spot as empty when its occupant can be shown not to exist.

## Checklist

- [ ] The decision above is made and recorded here.
- [ ] A spot left `OCCUPIED` by a crash after its mob died is Restored by a later Blood Moon.
- [ ] A structure mob saved with a tag whose spot was never saved gets a spot.
- [ ] A refused `addFreshEntity` leaves no spot behind.
- [ ] GameTests cover the first two cases by reloading `RestoreSpots` from an older saved state after the world has changed.
