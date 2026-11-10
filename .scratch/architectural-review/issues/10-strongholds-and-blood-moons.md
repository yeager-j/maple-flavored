# 10: Strongholds and Blood Moons

**What to fix:** The architecture findings for Stronghold Illagers and their routes, and for the Blood Moon cycle that Restores structure mobs. Each finding stands alone unless it says otherwise.

**Blocked by:** None

**Status:** done

**Priority:** P2 (Medium)

**Estimate:** L

**Source:** Architectural Review 11/9, the strongholds and Blood Moons reviewer, who read 26.3's vanilla sources. Finding 1 merges the same defect from the bosses and structure book locks reviewers. Nothing was built or run.

What held up: `Restoring` is a deep module, `Restorable` is a single table of what is Restorable, the mixins that feed them are one-line delegates, the timeline JSON and Java agree, and the five-way crossing realignment matches vanilla's `FiveCrossing`.

Other tickets hold findings from this slice: ticket 02 holds its P1, ticket 08 finding 1 covers the keeper room being decided twice, ticket 04 finding 3 covers the client test that asserts nothing, and ticket 04 finding 9 covers the tuning constants.

Paths: `bm/` is `src/main/java/mapleflavored/bloodmoons/`, `st/` is `src/main/java/mapleflavored/strongholds/`, `main/` is `src/main/java/mapleflavored/`, `mixin/` is `src/main/java/mapleflavored/mixin/`, `client/` is `src/client/java/mapleflavored/client/`, and `test/` is `src/gametest/java/mapleflavored/gametest/`.

## Findings

### 1. Persisted enums decode with `valueOf`, so one unknown name throws (P2)

**Principle:** Parse, don't validate (King). Evolve a saved format without breaking it.

Four enums use `Codec.STRING.xmap(X::valueOf, X::name)`. The save format is therefore the Java constant name, so renaming `WOODLAND_MANSION` or `PENDING` changes the format. `valueOf` throws where a codec should return a `DataResult` error.

For Restore spots the consequence is the whole file. Vanilla's `SavedDataStorage.readSavedData` would drop only the bad element for a proper codec error, but a thrown exception lands in its `catch (Exception)` and returns null (`net/minecraft/world/level/storage/SavedDataStorage.java:65-99`). `computeIfAbsent` then starts an empty `RestoreSpots`, and the next `setDirty` overwrites the file. One unknown name loses every spot in the level, with one log line.

- `bm/Restorable.java:57`, `:72-77`. `Restorable` also carries three identities for one thing: the saved enum name, the tag-path string and the structure key.
- `bm/RestoreSpot.java:34-42`, `:75`
- `bm/RestoreSpots.java:27-31`
- `main/subspaces/Subspace.java:51-52`, for both the codec and the stream codec
- `main/structurebooks/BookLocks.java:240`

### 2. "Gone for good" is implemented three times, and the copies have diverged (P2)

**Principle:** DRY as shared knowledge.

The rule for when a mob is gone for good is `AFTER_DEATH` plus `ENTITY_UNLOAD` with `reason.shouldDestroy()`. It is written out in `Restoring`, in `MobKeepers` and in `Subspaces`. `Restoring` has since learned a third channel, a saved mob that fails to load, through `EntityTypeMixin`. `MobKeepers` doesn't have it and compensates with a separate Peaceful sweep in `BookLocks`. The same Evoker can be gone for its Restore spot and not yet gone for its lock.

- `bm/Restoring.java:86-90`, `:131-141`, `:237-242`
- `mixin/EntityTypeMixin.java:21-32`
- `main/structurebooks/MobKeepers.java:43-46`, `:116-120`
- `main/subspaces/Subspaces.java:63-70`

### 3. The crossing realignment and the Blood Moon wiring have no test that must exercise them (P2)

**Principle:** Prove a gate can go red.

- **Realignment.** `StrongholdLayout.realignFiveCrossingDoorways` is the most intricate vanilla fix in the slice (`st/StrongholdLayout.java:63-92`; `mixin/FiveCrossingMixin.java:39-51`). Its only coverage checks one Stronghold planned from the test world's seed (`test/StrongholdGameTests.java:284-311`, `:478`). Nothing asserts that Stronghold contains a north- or west-facing crossing with side rooms, so the test can pass without touching the fix. Libraries and prison halls are built alone in every facing; `FiveCrossing` is not.
- **Wiring.** Every Restoring test raises `BloodMoons.RISEN.invoker()` itself, and `BloodMoonGameTests` checks only chat and the advancement. Deleting `RISEN.invoker().onRisen(server)` from `BloodMoons.rise` fails no test (`bm/BloodMoons.java:89-99`).
- **Isolation.** `RISEN` is server-global, so each test's Blood Moon makes other tests' empty spots pending. Isolation rests on a convention in the class Javadoc (`test/BloodMoonRestoringGameTests.java:64-65`, `:529-531`).

### 4. Per-piece-type Stronghold knowledge is spread over five tables (P3)

**Principle:** Single Choice.

`StructurePieceType` is consulted in `StrongholdLayout.PIECES`, `GARRISONS`, `KEEPER_EVOKER_AREAS`, `ALWAYS_EVOKER` and two `PORTAL_ROOM` literals. A library's Evoker is assembled from three of them. Nothing checks that `GARRISONS` covers `PIECES` minus the start.

Separately, layout and garrisons key on piece type, while routing, Restoring and the book key on `BuiltinStructures.STRONGHOLD`. A data pack structure of type `minecraft:stronghold` would get guards and doorless rooms but no spots, routes or book.

- `st/StrongholdLayout.java:30-44`, `:95-97`
- `st/StrongholdIllagers.java:73-100`, `:111`, `:139`, `:186-192`, `:375-388`
- `st/StrongholdRoutes.java:39`
- `bm/Restorable.java:50-55`, `:80-84`

### 5. `StrongholdRoutes.route` folds four outcomes into an empty list (P3)

**Principle:** Parse, don't validate. Empty and absent conflated.

An empty list means same room, not in a Stronghold, not the same Stronghold, or no open route. `StrongholdRouteGoal` is added to every non-spellcaster Illager and can't learn "not in a Stronghold", so a hunting Woodland Mansion Vindicator runs a full `createPath` to its quarry on every check before the route lookup says no. Each hunter also runs its own search with live block reads for what is a fact about the quarry's room.

- `st/StrongholdRoutes.java:33-50`, `:99-128`
- `st/StrongholdRouteGoal.java:39-47`, `:91-105`
- `test/StrongholdGameTests.java:514-526`

### 6. `RestoreSpot` can represent illegal states (P3)

**Principle:** Parse, don't validate.

`state` and `Optional<UUID> occupant` are independent fields on a public record with a public constructor. The codec accepts `OCCUPIED` with no occupant and `EMPTY` with one. The invariant lives only in three factory methods.

- `bm/RestoreSpot.java:25-64`

### 7. The fresh-entity hook is duplicated across two mixins (P3)

**Principle:** Single Choice.

`ServerLevelMixin` and `WorldGenRegionMixin` each carry the same pair of `addFreshEntity` injections, for `CommonMobs.applySpawnStats` and `Restoring.tagIfPlacing`. Each is a one-line delegate, which is right. But "what happens to a fresh entity" is listed twice, and a third subscriber must be added in both or the worldgen and live paths diverge silently.

- `mixin/ServerLevelMixin.java:12-32`
- `mixin/WorldGenRegionMixin.java:12-29`

### 8. A structure mob's setup is written twice, and the Restored copy is generic (P3)

**Principle:** DRY. Names tell the truth.

`StrongholdIllagers.spawn` and `Restoring.restoredMob` both write create, snap, `finalizeSpawn(STRUCTURE)`, persistence. They already differ in order and yaw. A spot records only `type` and `persistent`, so anything a structure later adds at placement won't survive a Restore. Not verified: Bastion template NBT, so the Bastion case is a risk and not a confirmed mismatch.

- `st/StrongholdIllagers.java:306-322`
- `bm/Restoring.java:220-235`

### 9. State sits on objects with the wrong lifetime (P3)

**Principle:** Home state according to its lifetime.

- `SkyRendererMixin` keeps the frame's Blood Moon strength in a `@Unique` field on the renderer, bypassing the `SkyRenderState` vanilla extracts for this (`client/mixin/SkyRendererMixin.java:20-38`).
- `BUILDING`, `DROPPED`, `LOADED_CHUNKS`, `PLANNED` and `StrongholdRoutes.TOUCHING` are JVM statics cleared by a `SERVER_STARTING` hook (`bm/Restoring.java:64-71`, `:77-85`, `:178-191`; `bm/RestoreSpots.java:33-36`; `st/StrongholdRoutes.java:26-28`).
- `CHUNK_LOAD` adds every loaded chunk of every level to `LOADED_CHUNKS`, even when no spot exists.

### 10. Small leaks around `RestoreSpots` (P3)

**Principle:** Information hiding.

- `Restoring` reads `level.getDataStorage().get(RestoreSpots.TYPE)` directly in two places and uses `RestoreSpots.of` elsewhere (`bm/Restoring.java:57-62`, `:144-152`, `:164`, `:268`).
- The id `restore_spot` is built twice, for the attachment and for the hand-parsed NBT key.
- `RestoreSpots.TYPE`, `RestoreSpots.in` and `Restoring.spotOf` are public only for tests (`bm/RestoreSpots.java:31`, `:82-84`).

## Checklist

- [x] 1. The four enums decode through a codec that returns an error for an unknown name, their saved names don't depend on Java constant names, and a test shows one bad Restore spot loses only that spot.
- [x] 2. One place decides a mob is gone for good, including the failed-load case, and `Restoring`, `MobKeepers` and `Subspaces` subscribe to it.
- [x] 3. A test builds `FiveCrossing` alone in each facing and fails without the realignment, a test fails when `BloodMoons.rise` stops raising `RISEN`, and Restoring tests can't make each other's spots pending.
- [x] 4. Each Stronghold piece type has one row, a check shows the rows cover every piece, and one discriminator says what a Stronghold is.
- [x] 5. `route` returns a result that tells its outcomes apart, and an Illager outside a Stronghold does no pathfinding for the route goal.
- [x] 6. `RestoreSpot` can't be built or decoded as Occupied without an occupant, or as Empty with one.
- [x] 7. One place lists what happens to a fresh entity, and both mixins call it.
- [x] 8. A Restored mob is set up by the same code that set it up at generation, and the Bastion case is checked.
- [x] 9. The sky strength travels through `SkyRenderState`, and `LOADED_CHUNKS` tracks only chunks that hold spots.
- [x] 10. `Restoring` reaches spots only through `RestoreSpots`, and the test-only members aren't public.

## Comments

Worked in one pull request. Findings 1 to 7 and 10 held as written against `main` after tickets 02, 08, 09, 11, 13 and 14. Finding 9's list of statics had already lost `PLANNED` to ticket 02. What each box means in the code:

1. `Restorable`, `RestoreSpot.State`, `BookLocks.Kind` and `Subspace.Kind` each carry a saved name of their own and decode through `StringRepresentable.fromEnum`, which returns an error for an unknown name. `Restorable`'s one name is both its saved name and its block tag's name. Saves written before this change hold the old upper-case names and are not read: a spot or lock saved under one is dropped as its level loads. No migration, following the legacy-saves decision in ticket 08.
2. `mobs/GoneForGood` owns the rule and its three channels: died, removed in a way that destroys, and failed to load. `Restoring`, `MobKeepers` and `Subspaces` listen to its event. The Peaceful sweeps in `BookLocks` and `Subspaces` stay, because Peaceful also never spawns a keeper or boss in the first place.
3. `StrongholdGameTests` builds a five-way crossing alone in each facing with each of the 16 side-room plans. `BloodMoonGameTests` raises a Blood Moon from the clock and checks an empty spot goes pending. Every other Restoring test raises a Blood Moon over its own test ground only.
4. `StrongholdRooms` has one row for each of the 13 piece types, and a GameTest checks every `STRONGHOLD_` piece type vanilla declares has one. A Stronghold is the structure `Restorable.STRONGHOLD` names: routes ask that, and the layout and the guards apply only while that structure is being built.
5. `StrongholdRoutes.route` returns `Doorways` or one of four `NoRoute` reasons, and `StrongholdRouteGoal` asks it before any path search. Not done: each hunter still runs its own route search.
6. A spot holds an `Occupancy`: `Occupied` with its occupant, or `Vacant.EMPTY` or `Vacant.PENDING` without one. The codec dispatches on `state`.
7. `FreshEntities.entering` is the list, and both mixins call it.
8. `StructureMobs.setUp` sets up both a Stronghold's guards as they generate and every Restored mob. The Bastion case was checked against vanilla's templates and was a real mismatch: they save each Piglin with a crossbow or golden sword and `CannotHunt`, and each Hoglin with `CannotBeHunted`, none of which `finalizeSpawn` gives a mob spawned for a structure. Jackson decided on 2026-11-10 that Bastions Restore only Piglin Brutes, which the templates add nothing to: a Brute never hunts and arms itself.
9. The strength is stored in `SkyRenderState` when the sky is extracted and read back from the state being drawn. A level's loaded-chunk set lives on its `RestoreSpots` and takes only chunks that hold spots.
10. `RestoreSpots.ifAny` and `RestoreSpots.of` are the only ways to a level's spots. `TYPE`, `all` and `Restoring.spotOf` are package-private, and the tests reach them through `RestoringAccess` in the tests' source set.
