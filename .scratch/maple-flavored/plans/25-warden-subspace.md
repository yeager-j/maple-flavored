# Plan: 25 Warden Subspace

## Context / end goal

The boss manifesto's first case: a Warden that can't reach a flying player is pointless, so taking the Ancient City's book (ticket 28) is trivial once a player has an Elytra. A Subspace closes that escape. It's a fixed 100-block sphere where the Warden spawns, with Darkness and Grounded inside and no barrier. Its Warden can always find you inside, while anger still comes only from vanilla sniffing and vibrations. Ticket 24 delivered Grounded, and ticket 09 delivered Remnants.

This ticket builds the **shared Subspace module** that tickets 26 (sculk and shriekers), 27 (summoning), 28 (Ancient City book), 29 (Wither Subspace) and 30 (Survive the Warden) build on, plus the Warden kind's player rules and awareness. Closing it means:

- Every Warden opens its Subspace the moment it appears.
- Survival players inside get Darkness and Grounded, and lose them shortly after leaving.
- Overlapping Subspaces never stack Grounded, and each ends on its own.
- The Subspace survives the Warden unloading and world reloads, ending only when the Warden dies or is removed for good, Peaceful included.
- The Warden hunts the nearest player anywhere in its Subspace, without that awareness keeping it from burrowing.
- Clients receive the Subspaces in their dimension.

Jackson's call (2026-11-05, reconfirmed after checking the wiki): awareness covers every entity in the Subspace, and sniff anger keeps vanilla's 6×20 cylinder. **Only targets inside vanilla's awareness box reset the Warden's 60-second burrow timer.** That box is the wiki's 49×51×49 (the hitbox inflated by `FOLLOW_RANGE` 24). This is vanilla's own rule: a finished sniff investigates its target anywhere in that box, and that resets the timer. Targets it knows about only through its Subspace draw it toward them but don't hold it there. So hiding more than about 24 blocks away for a minute still makes it burrow.

## What the evidence says (26.3 sources, Fabric API 0.165.0)

- **Removal reasons.**
  - Dig-down ends in `Digging.stop` with `remove(DISCARDED)` after a 100-tick animation, and never calls `die`. Death fires `AFTER_DEATH` and then `KILLED` 20 ticks later. Unloading (`UNLOADED_TO_CHUNK`) and changing dimension never `shouldDestroy()`.
  - `ServerEntityEvents.ENTITY_UNLOAD` can fire with a null reason. `BruteKeepers.countIfGone` already handles this.
  - Wardens never despawn by distance. A loaded Warden in Peaceful is discarded.
- **Peaceful never loads a saved Warden.** `EntityType.canSpawn` refuses types that aren't allowed in Peaceful, so neither end hook fires and the record would be orphaned. `BookLocks.openBruteLocksWhilePeaceful` handles the same gap for keeper Brutes.
- **Sensing.** `WardenEntitySensor.doTick` calls `NearestLivingEntitySensor.doTick`, which fills `NEAREST_LIVING_ENTITIES` with entities whose bounding boxes intersect the Warden's box inflated by `FOLLOW_RANGE` (24). It then picks `NEAREST_ATTACKABLE`: the closest targetable player, or else the closest other targetable entity. `canTargetEntity` excludes creative and spectator players, Wardens and armor stands.
- **Sniffing and burrowing.**
  - `Sniffing.stop`, in its single lambda `entity -> {...}`, raises anger only within `closerThan(6, 20)`.
  - When there's no disturbance yet, it calls `WardenAi.setDisturbanceLocation(body, entity.blockPosition())`. That calls `setDigCooldown`, which resets `DIG_COOLDOWN` to 1200 while it's still present, and it makes INVESTIGATE walk there.
  - Far targets get a partial path of about 24 blocks, so the Warden approaches in stages. `Brain.getTimeUntilExpiry` is public.
- **GameTest spawning.**
  - `GameTestEntityBuilder` defaults `requirePersistence` to true. Persistent Wardens reset their dig cooldown every tick in `Warden.tick` and never burrow.
  - The builder calls `finalizeSpawn` when given a spawn reason, which sets `DIG_COOLDOWN` 1200. Only `TRIGGERED` adds the 134-tick emerge.
- **Darkness.** The Warden itself applies Darkness 260 within 20 blocks, to survival-mode players. That stays.
- **Level attachments.**
  - Fabric persists them through `AttachmentSavedData`, loaded with the level, and replacing the value triggers sync.
  - `AttachmentSync` also resends a level's attachments on `ServerPlayerEvents.JOIN` and on `AFTER_PLAYER_CHANGE_LEVEL`. Fabric's `PlayerListMixin` fires the latter for a respawn into another dimension too.
- **Remnants.** `Remnants.placementFor` already gives the specified placement inside a Subspace, so it needs tests only.
- **GameTest isolation.**
  - Batches are keyed by environment holder and run one after another in a shared level.
  - `GameTestInfo.succeed` and `clearSpaceForStructure` never remove players. Earlier batches leave mock players behind, some of them survival (`playerReadingChat` builds a plain `ServerPlayer`), and can leave mobs too.
  - `makeMockServerPlayerInLevel` forces CREATIVE, which Wardens and the effect rule ignore.

## Decisions

1. **Record.** `Subspace(UUID boss, Kind kind, Vec3 centre, double radius)` is a fixed sphere: `contains(pos)` is true when `centre.distanceToSqr(pos) <= radius²`. `Kind` is a closed enum, just `WARDEN` now; ticket 29 adds `WITHER`. The level holding the record is its dimension.
2. **Storage and sync: one level attachment**, `Subspaces.ATTACHMENT` (`maple_flavored:subspaces`).
   - It holds a `List<Subspace>`, declared with `persistent(Codec)` and `syncWith(StreamCodec, AttachmentSyncPredicate.all())`.
   - It is the only authority. The stored list is immutable and every change replaces it, which triggers sync.
   - Both sides read it through `Subspaces.containing`. It's public only so a GameTest can round-trip its persistence codec, as `BookLocks.TYPE` does.
3. **Opening a Warden's Subspace.** On `ServerEntityEvents.ENTITY_LOAD` for a living Warden whose UUID owns no Subspace in any level, open one at `warden.position()` with radius 100.
   - This covers every way a Warden appears without hooks on spawn paths or a duplicate per-Warden flag.
   - "None in any level" stops reloads, chunk re-tracking and portal trips from opening a second one.
   - `isAlive()` skips a dying Warden that was saved mid-animation.
   - A dimension change keeps the UUID, so the old Subspace stays until that Warden dies or burrows.
4. **Ending (generic, every kind)**, idempotent. Each ending removes the record, re-syncs it and fires `ENDED`. Unloading, logging out and changing dimension never end one. Three things end Subspaces:
   - `AFTER_DEATH` of the boss.
   - `ENTITY_UNLOAD` with `reason != null && reason.shouldDestroy()`.
   - An `END_SERVER_TICK` reconcile that ends every Subspace in a level whose difficulty is Peaceful. Every boss kind is a monster that Peaceful neither keeps nor loads, as for `BookLocks`' keeper locks.
5. **Player rules (Warden kind)** run on `END_LEVEL_TICK` from the record, so they keep working while the Warden is unloaded.
   - **Who and what.** Each living player who is neither creative nor spectator, as vanilla's Warden Darkness skips those, and is inside at least one Warden Subspace gets Darkness and Grounded. Both are amplifier 0, last 60 ticks, have no particles, and are topped up when missing or under 40 ticks left.
   - **No stacking.** It's one application however many overlap, so Grounded never stacks.
   - **Leaving.** The Subspace's contribution wears off within 3 seconds of leaving. A longer vanilla Darkness is never cut short, and nothing blocks the edge.
   - **Tuning.** The constants live in one place, and the hand check tunes how Darkness looks.
6. **Awareness (it pauses while the Warden is unloaded, since it runs in its sensor tick).**
   - **What it adds.** `WardenEntitySensorMixin` injects right after `super.doTick` and calls `WardenSubspaces.extendAwareness(level, warden)`. That merges every living entity inside the Warden's own Subspace into `NEAREST_LIVING_ENTITIES`, deduplicated and sorted by distance.
   - **What stays vanilla.** Vanilla then picks `NEAREST_ATTACKABLE`, players first and then the closest, so vanilla sniffing walks the Warden to the nearest player anywhere in the Subspace. Anger and `NEAREST_VISIBLE_LIVING_ENTITIES` are unchanged.
   - **Where it searches.** The search box is the sphere's AABB, filtered by `contains`, and only while the Warden is in its Subspace's level.
7. **Only the vanilla box holds the Warden.** `SniffingMixin` adds a `@WrapOperation` on the `WardenAi.setDisturbanceLocation` call in `Sniffing.stop`'s lambda (`lambda$stop$0`, taking the target with `@Local(argsOnly = true)`). It delegates to `WardenSubspaces.disturbFromSniff(warden, target, original)`.
   - **Test.** The target counts as in range when its bounding box intersects `warden.getBoundingBox().inflate(FOLLOW_RANGE)`, the same intersection vanilla's sensor uses.
   - **In range:** the original call runs, so vanilla is unchanged.
   - **Out of range:** it records `DIG_COOLDOWN`'s remaining `getTimeUntilExpiry` when present, runs the original, and restores that expiry. Every other `setDisturbanceLocation` caller (vibrations, pushes) is untouched.
8. **Events.** `Subspaces.STARTED` and `ENDED`, each `(ServerLevel, Subspace)`, are array-backed like `Spawners.CLEARED`. They carry no cause: `ENDED` means only that the Subspace is gone, whether by death, burrowing, Peaceful or any other removal. Ticket 30's Survive the Warden must tell death and burrowing apart from other removals itself, and that ticket gets a note saying so.
9. **Module shape.** Package `mapleflavored.subspaces`.
   - `Subspaces` (public) is the generic core: the attachment, `containing`, `ownedBy`/`ownsAny`, opening, ending, the Peaceful reconcile and the events.
   - `WardenSubspaces` (public class, because the mixins live in `mapleflavored.mixin`) exposes only the two mixin entry points as public. Radius 100, the effect constants, the opener and the effects stay package-private.
   - `open` stays package-private until ticket 29 needs it elsewhere. `Subspaces.register()` is called from `MapleFlavored.onInitialize` and registers `WardenSubspaces`.
10. **Death and respawn: no code.** Vanilla respawning and the existing Remnant placement already give the specified results. Tests pin them.

## API sketch

```java
package mapleflavored.subspaces;

/** A boss's fixed sphere. The level holding it is its dimension. */
public record Subspace(UUID boss, Kind kind, Vec3 centre, double radius) {
	public boolean contains(Vec3 position);
	static final Codec<Subspace> CODEC; static final StreamCodec<ByteBuf, Subspace> STREAM_CODEC;
	public enum Kind { WARDEN }
}

public final class Subspaces {
	public static final AttachmentType<List<Subspace>> ATTACHMENT;        // read through containing; public for the persistence test
	public static final Event<Started> STARTED;  // (ServerLevel level, Subspace subspace)
	public static final Event<Ended> ENDED;      // (ServerLevel level, Subspace subspace); any cause
	public static void register();                                        // from MapleFlavored.onInitialize
	public static List<Subspace> containing(Level level, Vec3 position);  // server and client
	static Optional<Subspace> ownedBy(Level level, UUID boss);
	static boolean ownsAny(MinecraftServer server, UUID boss);
	static void open(ServerLevel level, Entity boss, Kind kind, double radius);
}

public final class WardenSubspaces {
	static final double RADIUS = 100.0;
	static void register();                                                     // opener, effects
	public static void extendAwareness(ServerLevel level, Warden warden);      // WardenEntitySensorMixin
	public static void disturbFromSniff(Warden warden, LivingEntity target, Runnable original); // SniffingMixin
}
```

Consumers:
- Hearts, for ticket 29, calls `containing(level, pos)` filtered to `WITHER`.
- Ticket 26 uses `STARTED`/`ENDED` and `containing` on both sides.
- Ticket 30 builds its own death and burrow credit on top (Decision 8).

## Files

- New: `src/main/java/mapleflavored/subspaces/Subspace.java`, `Subspaces.java`, `WardenSubspaces.java`.
- New mixins: `mapleflavored/mixin/WardenEntitySensorMixin.java` and `SniffingMixin.java`, listed in `maple_flavored.mixins.json`.
- `MapleFlavored.java`: call `Subspaces.register()`.
- Tests:
  - `src/gametest/java/mapleflavored/gametest/SubspaceGameTests.java`, added to the gametest `fabric.mod.json` entrypoints.
  - `MockPlayers.survivalPlayer(helper)`: a loaded, survival, plain `ServerPlayer`.
  - `src/gametest/java/mapleflavored/gametest/mixin/GameTestHelperAccessor.java`, listed in `maple_flavored_gametest.mixins.json`.
  - One environment per Subspace test, `test_environment/subspace_*.json`, each `all_of [normal]`. The Peaceful test reuses `peaceful`, so it runs in that batch. That's safe, since Peaceful ends every Subspace within a tick.
- After implementation:
  - Copy this plan to `.scratch/maple-flavored/plans/25-warden-subspace.md`.
  - Tick the ticket's boxes and keep a hand-check line.
  - Add a note to ticket 30 that `ENDED` carries no cause.

## Ordered steps

1. `Subspace` record and codecs. `Subspaces` with the attachment, queries, open/end, death and unload hooks, the Peaceful reconcile and the events.
2. `WardenSubspaces` opener and player effects, registered from mod init.
3. Awareness and sniff mixins delegating to `WardenSubspaces`.
4. GameTests and environments, then the full suite. Check that the dig test fails with the range gate disabled, and that the awareness tests fail with the merge limited to players.

## Tests (SubspaceGameTests)

Shared setup, through a small `SubspaceArena` helper in the test class:
- **Clearing at the start.** Each test first logs out every player left from earlier batches and discards every non-player living entity within 110 blocks of its Warden spot. It also clears any Subspace records a failed earlier test left, by setting `Subspaces.ATTACHMENT` to an empty list. Its own batch guarantees nothing else is running.
- **Teardown on pass and fail.** A new gametest accessor mixin, `GameTestHelperAccessor` (`@Accessor("testInfo")`), lets the arena register a `GameTestListener` on the test's `GameTestInfo`. On `testPassed` or `testFailed` it discards the test's Wardens, logs out its players including respawned replacements, and empties the level's Subspace records, even when a boss is unloaded. Without that, a failure could leave a Subspace that affects later batches.
- **Ticking players.** A mock player's connection never ticks it, so its effects never count down (`BlightGameTests`' note; `GroundedGameTests` calls `doTick()`). A static `END_SERVER_TICK` listener in the test class calls `doTick()` on every player the active arena tracks, so expiry and refresh behave as on a real client. The continued-effect checks in Tests 2 and 3 must fail with the top-up disabled.
- **Survival players.** These are plain `ServerPlayer`s built like `MockPlayers.playerReadingChat`, then `setGameMode(SURVIVAL)`. `makeMockServerPlayerInLevel` overrides `gameMode()` to CREATIVE, and Wardens can't target creative players.
- **Spawning Wardens.** Every Warden goes through `helper.spawnEntity(WARDEN, pos).requirePersistence(false).spawnReason(MOB_SUMMONED).spawn()`. Mobs used as targets get `setNoAi(true)` so they stay put.

1. **Entering and leaving.** `containing` at the Warden's feet holds one `WARDEN` Subspace with radius 100.
   - A survival player 99 blocks out gets Darkness and Grounded within one tick. A creative player beside them gets neither. A survival player 101 blocks out gets neither.
   - The first player moves to 101 blocks, and after 61 ticks has neither effect.
2. **Two overlapping Subspaces.** For a player in both, Grounded is amplifier 0.
   - Killing Warden A ends only A's Subspace, and the player is still Grounded after 61 ticks.
   - Killing B ends B's, and the effects wear off.
   - A static listener filtered by UUID sees `STARTED` and `ENDED` exactly once per Warden.
3. **Unloading and reloading.**
   - The Warden is moved 10 blocks from its spawn, then saved and removed with `UNLOADED_TO_CHUNK` (RemnantsGameTests' `unload`). The Subspace remains at the original centre, and a player inside is still Grounded after 61 ticks.
   - `Subspaces.ATTACHMENT`'s persistence codec round-trips the level's list, and the result is set back, as BastionLockGameTests does for `BookLocks`. Exactly one Subspace remains, with its original centre and radius.
   - Reloading the Warden from its tag doesn't open a second one.
   - Removing it with `DISCARDED`, as burrowing does, ends it.
4. **Peaceful (environment `peaceful`).** A Warden is constructed and added. Within the same tick, before Peaceful's despawn, it's saved and removed with `UNLOADED_TO_CHUNK`.
   - `containing` is empty and a survival player inside isn't Grounded a few ticks later.
   - The test fails without the reconcile, because Peaceful never reloads the Warden.
5. **Remnants inside.** Death on the floor inside leaves the Remnant at the death spot. A player who walked on solid ground and then died in lava inside has the Remnant moved to that ground.
6. **Respawn inside.** A player whose forced respawn point is inside the Subspace respawns there.
7. **Awareness: nearest player first.**
   - Setup: survival player A 35 blocks east, player B 60 blocks west, and a no-AI pig 30 blocks west, all outside the vanilla box on a built floor.
   - Within the time limit, the Warden ends up at least 10 blocks closer to A and no closer to B.
   - Its active anger at A stays 0 while they're more than 6 blocks apart.
8. **Awareness: non-players count.** With only a no-AI pig 40 blocks away, the Warden ends up at least 10 blocks closer to it.
9. **Burrow timer.** In setup, each Warden's `DIG_COOLDOWN` expiry is shortened to about 200 ticks.
   - Far case: the only target is a survival player 40 blocks away inside the Subspace. The Warden burrows, allowing for the 100-tick animation and any walk in progress, so it's removed and its Subspace ends. This must fail with the range gate disabled.
   - Control, in its own environment: a player 10 blocks away. The Warden is still present well past 200 ticks.
   - Boundary, in its own environment: a target whose bounding box just intersects the inflated box at a fractional coordinate still holds the Warden.

`maxTicks` and distances are tuned while running. Assertions are about outcomes a player sees (effects, positions, entities present, anger) or the module's public query.

## Verification

- `./gradlew build runGameTest` with `JAVA_HOME` set to the JDK in `gradle.properties`. The whole suite must pass, including every existing batch, which also shows no Subspace leaks into other tests.
- **Hand check in a dev client** (kept as the ticket's unchecked line):
  - `/summon minecraft:warden`: Darkness and Grounded inside, and an Elytra won't glide.
  - Past 100 blocks the effects wear off.
  - Unload the area and return, and the effects resume.
  - Save, quit and reopen the world, and the Subspace is still there.
  - With a temporary, uncommitted log of `Subspaces.containing(clientLevel, pos)`, the client holds the Subspace after joining, after changing dimension, and after dying in the Nether and respawning into the Overworld.
  - `/kill` the Warden, and the Subspace ends on the client.
- `larp review --message "Review the diff"` before opening the PR.

## Exclusions

- Sculk spread, the sculk sky and shriekers are ticket 26.
- Warden Territory summoning is ticket 27.
- The Ancient City book's Warden is ticket 28.
- The Wither kind is ticket 29.
- Survive the Warden and its death-versus-burrow credit are ticket 30.
- Nothing changes vanilla's own Warden Darkness, anger, sniff range or vibrations.

## Implementation notes

Things the GameTest harness needed that the plan didn't foresee:

- **Relative spawn positions.** `GameTestHelper.spawnEntity` and `spawn` take positions relative to the test, so the arena spawns at test-relative offsets.
- **Players fall.** Ticking a plain mock `ServerPlayer` applies gravity, so every arena player stands on a block.
- **Force-loaded arena chunks.** Only a test's own chunks are kept loaded, and entities elsewhere neither tick nor show up to any sensor or entity search. The arena force-loads a strip 80 blocks each way along its x axis.
- **Never releasing those chunks.** The runner releases every forced chunk as a batch ends, right before forcing the next batch's. A cleanup that released them too unloaded the next test's chunks, and that test then waited forever: GameTest doesn't count ticks until its chunks are ticking.
- **Checking top-ups.** A check after 61 ticks can't tell topping up from re-adding an expired effect. The overlap and unloading tests check every tick that the effects never get within Darkness's 22-tick fade of ending.
