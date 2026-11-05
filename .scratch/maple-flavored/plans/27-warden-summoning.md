# Plan: 27 Warden summoning in Warden Territory

## Context / end goal

Vanilla's shrieker has a cheap off switch. A shriek always counts for the player who caused it, wherever they stand, and the Warden spawns next to the shrieker. So a chunk-loaded, spawn-proofed shrieker hit by a clock-fired arrow keeps failing to summon, and the 10-second cooldown it sets keeps every real shrieker in the city from warning that player. The Ancient City then has no Warden.

This ticket replaces vanilla's summoning rule so that switch does nothing:

- A shriek counts only for players in **Warden Territory**: the Deep Dark biome or an Ancient City's bounds. Nobody else gets a warning level or a cooldown.
- The Warden spawns near a player, not the shrieker. It's the player who caused the shriek if they're in Warden Territory, else the nearest player the shriek counted for who still is. Spawn-proofing a shrieker is useless.
- A failed spawn clears the cooldown, so it can't lock out the next shriek.

Ticket 28 (the Ancient City book) finds its Warden's spot "through the summoning rules", so the spawn search is a public entry point.

## What the evidence says (26.3 sources)

- `SculkShriekerBlockEntity.tryShriek(level, player)` resets `warningLevel` to 0, then shrieks only if the shrieker can't respond (`CAN_SUMMON` false, Peaceful, or `spawnWardens` off), or if `tryToWarn` succeeds. `tryToWarn` calls `WardenSpawnTracker.tryWarn(level, pos, player)`.
- `tryWarn`:
  1. It returns empty if any Warden is in a 48-block box around the shrieker.
  2. Its players are the living non-spectators within 16 blocks, plus the trigger player wherever they are.
  3. It returns empty if any of them is on cooldown.
  4. Otherwise it raises the highest tracker by one, setting a 200-tick cooldown, copies that tracker to all of them, and returns the new level.
  - `increaseWarningLevel`, `copyData` and `onCooldown` are private. `setWarningLevel` and `getWarningLevel` are public.
- 90 ticks later, or when a shrieking shrieker is removed, `tryRespond` runs. At warning level 4, `trySummonWarden` calls `SpawnUtil.trySpawnMob(WARDEN, TRIGGERED, level, shriekerPos, 20, 5, 6, ON_TOP_OF_COLLIDER, false)`. On failure, the level 1–4 reply sound plays. Darkness is applied within 40 blocks either way.
- `SpawnUtil.trySpawnMob` picks a random offset in a square, scans down from `+rangeY` to `-rangeY` for a full-topped block with an empty block above, checks the world border, then creates the mob.
  - `checkSpawnRules` is `Mob`'s default `true`. Warden spawn placements apply only to natural spawning, so **light never matters**.
  - `Warden.checkSpawnObstruction` adds `noCollision` for the Warden's whole 0.9×2.9 box. So "room" means a full-topped block with space for the Warden's box, clear of entities.
- `Structure` lookups: `StructureManager.getStructureAt(pos, structure)` tests the start's overall bounding box, from the chunk's structure references. `getStructureWithPieceAt` tests pieces only.

## Decisions

1. **Warden Territory** is `WardenTerritory.contains(ServerLevel, BlockPos)`: the biome at the position is `minecraft:deep_dark`, or the position is inside an Ancient City's overall bounding box (`getStructureAt`). "Bounds" means the whole box, not just its pieces, so the gaps between a city's buildings count. A player is in it when their `blockPosition()` is.
2. **Warning** replaces the `tryWarn` call in `tryToWarn`, with a `WrapOperation` that calls `WardenSummoning.warn(level, shrieker, cause)`. It keeps vanilla's checks and order, but every player outside Warden Territory is dropped first:
   1. If a Warden is in the 48-block box around the shrieker, it returns empty, as vanilla does.
   2. The counted players are the living non-spectators within 16 blocks, plus the cause, kept only if each is in Warden Territory.
   3. If nobody is counted, it returns empty. The shrieker doesn't shriek, as for vanilla's cooldown, and the summon is cancelled before it begins.
   4. If any counted player is on cooldown, it returns empty.
   5. Otherwise it raises and copies the highest tracker, as vanilla does, through invokers on `WardenSpawnTracker`.
   - On success it records the shriek on the shrieker: the cause's UUID and the counted players' UUIDs. This is a persistent block entity attachment, `maple_flavored:shriek`, so a world saved mid-shriek still knows whom to summon on. It's removed at the start of every warn, so a failed warn leaves nothing stale.
3. **Summoning** replaces `trySummonWarden`, with a HEAD inject on the private method. Below warning level 4 it returns false, as vanilla does. At level 4 it calls `WardenSummoning.summon(level, shrieker)`:
   - **Target.** The cause, if they're still alive, not a spectator, in this level and in Warden Territory. Otherwise the player nearest the shrieker who passes the same checks, counted or not, as the spec says "the nearest player in Warden Territory". The counted players are kept only to clear their cooldowns.
   - **Cancelled.** With no target, nothing spawns and the cooldowns stay. Vanilla's reply sound and Darkness still play, as for any summon that doesn't happen.
   - **Search.** `spawnNear(level, target)` tries spots 5 to 15 blocks from the target, then 5 to 50 blocks. If it spawns a Warden, `summon` returns true.
   - **Failed.** Every counted player still online gets their cooldown cleared, but keeps warning level 4, so the next shriek summons again. It returns false, so the reply sound plays.
   - The attachment is removed once the summon has been decided.
4. **The spawn search** is `WardenSummoning.spawnNear(ServerLevel, ServerPlayer) : Optional<Warden>`, public for ticket 28. It skips warning levels and the nearby-Warden check, which are the shrieker's.
   - **Each attempt** picks a random direction and a horizontal distance in the ring. It then calls `SpawnUtil.trySpawnMob` there with one attempt, an XZ range of 0 and `ON_TOP_OF_COLLIDER`, so vanilla's vertical scan, border check, spawn reason (`TRIGGERED`, with its emerging animation) and obstruction check all stay.
   - **Distances are horizontal**, from the target's exact position to the middle of the block the Warden would stand on. The vertical scan reaches 6 blocks either way, as vanilla's does, in the near ring and 12 in the wide ring.
   - **Attempts.** 20 in the near ring, as vanilla, and 60 in the wide ring. A spot in an unloaded chunk is skipped, so the search never loads or generates chunks.
   - These counts and the vertical reach aren't game rules: the spec fixes the distances, and they aren't playtest values.
   - **Subspaces.** The Warden opens its Subspace through ticket 25's `ENTITY_LOAD` hook.
5. **Module.** A new package `mapleflavored.wardensummoning` holds `WardenTerritory` and `WardenSummoning`. It's the spec's "Warden summoning" module. The two mixins, `SculkShriekerBlockEntityMixin` and `WardenSpawnTrackerInvoker`, stay thin.

## API sketch

```java
public final class WardenTerritory {
	public static boolean contains(ServerLevel level, BlockPos position);
}

public final class WardenSummoning {
	// Mixin-facing: replaces WardenSpawnTracker.tryWarn and SculkShriekerBlockEntity.trySummonWarden.
	public static OptionalInt warn(ServerLevel level, SculkShriekerBlockEntity shrieker, ServerPlayer cause);
	public static boolean summon(ServerLevel level, SculkShriekerBlockEntity shrieker);
	// Ticket 28's book uses this, falling back to the book's own spot.
	public static Optional<Warden> spawnNear(ServerLevel level, ServerPlayer target);
}
```

## Invariants

- A player outside Warden Territory never gains a warning level or a cooldown from a shriek.
- A summoned Warden always spawns within 50 blocks horizontally of a player in Warden Territory, never at the shrieker unless that player stands there.
- A failed search leaves no counted player on cooldown.
- Shriekers that can't respond keep vanilla's decorative shriek. Peaceful and `spawnWardens` off still stop everything.

## Tests (GameTests, `WardenSummoningGameTests`)

They run in a `SubspaceArena`, each alone in its batch: a summoned Warden opens a Subspace. A helper fills a small box of `deep_dark` biome around a position and restores the old biome in the arena's cleanup.

- **Territory.** A deep dark position counts. A position in a registered Ancient City's bounding box but in no piece counts, and so does one in a piece. A plains position doesn't.
- **Remote shriek, outside.** A player outside Warden Territory at level 3 causes a shriek on a can-summon shrieker 30 blocks away, in Deep Dark. Their level and cooldown don't change, the shrieker doesn't shriek, and no Warden appears.
- **Remote shriek, inside.** A player in Warden Territory at level 3 causes a shriek 40 blocks away, outside Warden Territory. They reach level 4, and after 90 ticks a Warden appears 5 to 15 blocks from the player.
- **A friend's shriek.** A player outside Warden Territory causes a shriek. A friend in Warden Territory, 10 blocks from the shrieker and at level 3, reaches level 4 with a cooldown. The cause stays at 0. The Warden appears 5 to 15 blocks from the friend.
- **Widening.** The player stands on a lone block, with a broad floor 20 to 50 blocks out. The Warden appears 20 to 50 blocks out.
- **A failing spot.** The player stands on a lone block with nothing within 50 blocks. No Warden appears, and the player keeps level 4 with the cooldown cleared.
- **Cancelled.** The cause leaves Warden Territory before the 90 ticks end. No Warden appears, and the cooldown stays.
- **The nearest player in Warden Territory.** The cause leaves Warden Territory mid-shriek. Of two uncounted players in Warden Territory, 20 and 30 blocks from the shrieker, the Warden comes for the nearer.
- **An outsider's cooldown.** The friend test's cause starts on cooldown, which doesn't stop the friend's warning. The failing test counts two players, and both come off cooldown keeping level 4.
- **Below level 4.** A shriek at level 0 raises it to 1 and summons nothing.
- **The search alone.** `spawnNear` summons for a player at level 0 with another Warden beside them, and the Warden owns a Subspace.
- **Saved mid-shriek.** The shrieker's block entity is saved and loaded back mid-shriek, losing its in-memory state, and the Warden still comes for the cause.

## Exclusions

- The Ancient City book's summon and fallback spot are ticket 28.
- Vanilla's warning level decay, sounds and Darkness are unchanged.
- Shriekers grown by a Subspace keep ticket 26's rules.
