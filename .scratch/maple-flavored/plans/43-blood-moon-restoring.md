# Plan: ticket 43, Blood Moon Restoring

Ticket: `.scratch/maple-flavored/issues/43-blood-moon-restoring.md`. Spec: `.scratch/maple-flavored/spec.md`, Blood Moons module (stories 194-199, the Blood Moons paragraph and its three bullets, the "Blood Moon spot that can't fill yet" edge case). Builds on ticket 42 (`mapleflavored.bloodmoons`). Ticket 45 later adds Stronghold Illagers as another Restorable structure.

## End goal

Structures stop being one-shot: after each Blood Moon, Bastions, Ocean Monuments, End Cities and Woodland Mansions get back exactly the mobs they generated with, at the same spots, so Ancient Embers, Pith and Illager books renew slowly. The player stays in control: a mob returns only where its spot is clear, its structure's block is still there and no player is within 32 blocks; blocks and loot never return; and nothing loads chunks or piles up from missed nights.

## Model

A **spot** is one mob a structure placed as it generated:

```java
record RestoreSpot(UUID id, Vec3 position, Restorable structure, EntityType<?> type, boolean persistent, State state, Optional<UUID> occupant)
enum State { OCCUPIED, EMPTY, PENDING }
```

- `position`: where the mob's feet were (not a block position: a mob on a slab stands inside the slab's block). `type`: the mob that spot rolled. `persistent`: the original's `isPersistenceRequired()`.
- `OCCUPIED` with `occupant` = the live mob's UUID. `EMPTY`: its mob is gone for good, waiting for a Blood Moon. `PENDING`: a Blood Moon rose while it was empty; it fills as soon as it can.
- At most one mob per spot by construction: only `PENDING` fills, and filling makes it `OCCUPIED`.

`Restorable` (enum, `mapleflavored.bloodmoons`): one constant per structure, holding the structure key, the mob types it Restores, and the block condition:

| Constant | Structure | Mobs | Condition |
| --- | --- | --- | --- |
| `BASTION` | `minecraft:bastion_remnant` | piglin, piglin_brute, hoglin | block below in `#maple_flavored:bastion_restore_blocks` (blackstone family, basalt family) |
| `OCEAN_MONUMENT` | `minecraft:monument` | elder_guardian | spot block is water; first non-water block below is in `#maple_flavored:ocean_monument_restore_blocks` (prismarine family) |
| `END_CITY` | `minecraft:end_city` | shulker | the block the Shulker is attached to (below: Sentry Shulkers attach down) in `#maple_flavored:end_city_restore_blocks` (purpur family, end stone bricks) |
| `WOODLAND_MANSION` | `minecraft:mansion` | vindicator, evoker | block below in `#maple_flavored:woodland_mansion_restore_blocks` (birch planks, cobblestone, smooth stone slab) |

The block families are block tags so they're data, visible and easy to adjust.

## Seams and flow

1. **Tagging at generation.** `StructureStartMixin` wraps `placeInChunk` (mixinextras `@WrapMethod`) and sets a thread-local `Restorable` for the structure being built (looked up by the structure's registry key). `RestoreSpots.tagIfPlacing(Entity, ResourceKey<Level>)` is called from both `WorldGenRegionMixin.addFreshEntity` (real worldgen) and `ServerLevelMixin.addFreshEntity` (`/place` and the GameTest structure helper build straight into the level). When a Restorable is set and the entity's type is one of its mobs: make a new spot (`OCCUPIED` by the entity), attach `SpotTag(dimension, spotId)` to the entity (persistent attachment), and queue the spot. Allays, chests, blocks and spawners are never tagged.
2. **Recording.** Worldgen threads can't touch saved data, so planned spots wait in a `ConcurrentLinkedQueue` until the server thread records them into the level's `RestoreSpots` saved data: each tick, before every save, and before any read (the `BookLocks` pattern). Cleared on server start.
3. **Emptying.** `AFTER_DEATH` and `ENTITY_UNLOAD` with a destroying removal reason (killed, discarded: conversion, despawn, Peaceful) empty the entity's spot, only if the spot's occupant is this entity. Unloading or changing dimension holds the spot.
4. **Blood Moon rises.** `BloodMoons.RISEN` (new Fabric event, fired from `rise`) turns every `EMPTY` spot in every level `PENDING`. Nothing loads. A spot that's `PENDING` stays so (no stacking); an `OCCUPIED` spot is untouched.
5. **Filling.** A pending spot is tried when its chunk loads (the chunk is queued on `ServerChunkEvents.CHUNK_LOAD`, tried next tick) and every 100 ticks while its chunk's entities are loaded. It fills when: its entities are loaded (`areEntitiesLoaded`), the mob's bounding box at the spot has no collision, the block condition holds, no player in the level is within 32 blocks, and the level isn't Peaceful for a monster. Fill: create the type with `EntitySpawnReason.STRUCTURE`, place it at the spot's bottom centre, `finalizeSpawn`, `setPersistenceRequired` if the original was persistent, attach the same `SpotTag`, add it with passengers, mark the spot `OCCUPIED` by it. Pending spots ignore dawn.
6. **API for ticket 45.** `Restorable` gains a `STRONGHOLD` constant; nothing else changes.

## Public API

- `BloodMoons.RISEN`: `Event<Risen>`; `interface Risen { void onRisen(MinecraftServer server); }`. Consumed by `RestoreSpots`.
- `RestoreSpots` (package `mapleflavored.bloodmoons`):
  - `static void build(Optional<Restorable> structure, Runnable buildChunk)`: runs a structure's chunk build with its Restorable set. Called by `StructureStartMixin`.
  - `static void tagIfPlacing(Entity, ResourceKey<Level>)`: called by both `addFreshEntity` mixins.
  - `static List<Spot> spotsIn(ServerLevel)`, `static Optional<Spot> spotOf(Entity)` and `TYPE`: read by tests.
- `Restorable.of(ResourceKey<Structure>)`: the structure's Restorable, if it has one.

## Invariants

- A spot's occupant, when set, is the only mob tagged with that spot.
- Only the server thread touches `RestoreSpots` saved data.
- No code path loads a chunk.
- Restoring never places or replaces a block.

## Exclusions

Stronghold Illagers (45). Existing worlds' structures generated before this change have no spots. Mobs that change dimension: Fabric's persistent attachments are copied, so they keep the spot; if not, they just hold it.

## Tests (`BloodMoonRestoringGameTests`)

Generation tagging is tested with real structures placed by `Structures.place`. The Restoring behaviour is tested on small hand-built spots: the test sets a Restorable around spawning one mob on the right floor (`RestoreSpots.build(Optional.of(kind), () -> level.addFreshEntity(mob))`), exactly the path generation takes, so no test-only hook is needed.

Each test that raises a Blood Moon uses its own environment and its own Blood Moon (as in ticket 42), calling `BloodMoons` through the clock.

- Generation: placing a Bastion tags its Piglins, Brutes and Hoglins with spots of the right type; a Woodland Mansion tags Vindicators and Evokers, not Allays; an Ocean Monument tags its 3 Elder Guardians; an End City tags its Shulkers. Each tagged mob's spot is `OCCUPIED` by it.
- Killed mob returns: kill the mob → `EMPTY`, no mob; Blood Moon → refills with the same type, tagged, persistent like the original.
- Killed after the Blood Moon: waits; nothing until the next Blood Moon.
- Moved mob holds its spot: teleport it 20 blocks away; Blood Moon → no new mob.
- Converted: a Piglin turning Zombified empties its spot.
- Missing block: replace the block beneath → stays pending; put it back → fills.
- Nearby player who then leaves: a player within 32 blocks → stays pending through the periodic retries; move them 40 away → fills within the retry interval.
- Fill after dawn: pending during the night, time set to the next day, then the player leaves → fills.
- Blocked spot: a block in the mob's space → pending.
- No stacking: two Blood Moons over an empty spot → one mob.
- Reloads: save/reload `RestoreSpots` (codec round trip installed in place) keeps spot states; a pending spot still fills after reload.
- Chunk load: a pending spot in an unloaded chunk fills when the chunk is loaded (force-load in the test).

Full suite: `./gradlew build` (JAVA_HOME set).

## Changes after review and testing

- **Peaceful.** A hostile mob's save that Peaceful refuses to load is dropped for good, without dying or unloading. `EntityTypeMixin` sees every failed load of a saved entity; if it held a spot, the spot empties on the next tick (occupant-checked). Filling relies on the game's own spawn check, so a Piglin or Shulker fills in Peaceful and a Brute's spot waits.
- **Chunk edges.** A spot fills only when every chunk the mob's body would overlap is loaded, since collisions skip unloaded chunks.
- **Footing.** Structures don't place their mobs against their block: Bastion templates put mobs a block over the floor, Monument Elders swim a few blocks up, and End City Sentries float in hollow towers until the Shulker finds a wall. So each footing looks for its block: `STANDING` (Bastion, Mansion) takes the first non-air block within 4 below the feet (a slab or stair underfoot counts); `SWIMMING` (Monument) needs water at the spot and the first non-water block within 16 below; `ATTACHED` (End City) takes the first non-air block within 24 in any of the six directions. Every scan stops at an unloaded chunk, so checking a spot never loads one. Generation tests assert that every new spot's block holds.
- **Clear.** A spot is clear of blocks and of mobs (`isUnobstructed`), not only of collidable entities.
- **`BloodMoons.RISEN`** stays an event, as `Spawners.CLEARED` and `Subspaces.STARTED` are, rather than a direct call; the reviewer's P3 was declined.
- **Tests.** The test world is superflat in every dimension, so Mansions, Monuments and End Cities can't plan there: their tests build vanilla's own pieces (`generateMansion`, `MonumentBuilding`, `startHouseTower`) into a real `StructureStart`, placed chunk by chunk through `StructureStartMixin`. The `WorldGenRegion` path can't run in a flat world, so it's a hand check. Restoring tests use far walled sites in force-loaded chunks and wait for the chunk's entities to load (disk reads lag behind an idle test server's ticks), and check states in the same tick as the change, since any test's Blood Moon makes every empty spot pending.
