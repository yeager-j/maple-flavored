# Plan: 31 Common mob changes

Ticket: `.scratch/maple-flavored/issues/31-common-mob-changes.md`. Spec: `.scratch/maple-flavored/spec.md`, "Mobs" (the table and the two bullets after it). Design answer: `.scratch/maple-flavored-design/issues/05-mob-changes.md`.

## End goal

Common Overworld fights get quicker and riskier the way Matcha Flavoured made them: zombies are fast and frail, skeletons, creepers and small pests die quickly, spiders and husks keep their own threat, while curing a zombie villager is never a chase. Surface builds stop getting cratered because creepers can't spawn naturally at Y 63 or above in the Overworld, but creeper farms below that and creeper spawners keep working. Closing it means every mob of a listed type gets its row's numbers whenever it enters the world through vanilla's spawn setup or a conversion, the numbers come from one table, and GameTests pin every cell.

## Decisions

- **Difficulty:** the level's difficulty when the stats are applied (`mob.level().getDifficulty()`), which equals `DifficultyInstance.getDifficulty()` in `finalizeSpawn`. Peaceful has no column, so it applies nothing (hostile mobs are removed there anyway).
- **Types are exact `EntityType`s**, matching Matcha's predicates: "adult zombie" is `minecraft:zombie` only (not drowned, zombified piglins or zombie villagers); "spider" is `minecraft:spider` only (cave spiders have their own row). Babies use `isBaby()`.
- **Values are base values** (`AttributeInstance.setBaseValue`), as Matcha's `attribute ... base set` does. Vanilla's random spawn modifiers (zombie knockback/follow-range bonus, leader-zombie health multiplier, spider Hard effects) still stack on top. When a row sets max health, health is set to the new max.
- **Attributes:** health `MAX_HEALTH`, speed `MOVEMENT_SPEED`, "steps up 1 block" `STEP_HEIGHT` = 1.0, damage `ATTACK_DAMAGE`, jump `JUMP_STRENGTH`, armor `ARMOR`, follow range `FOLLOW_RANGE`, knockback resistance `KNOCKBACK_RESISTANCE` (full = 1.0), "no fall damage" `FALL_DAMAGE_MULTIPLIER` = 0.
- **Spec numbers win over Matcha** where they differ (Matcha's Hard husk has 10 damage, step height and movement efficiency; Matcha's spider gets Weaving). The spec is the contract.
- **When stats apply ("on spawn"):** at the head of `ServerLevel.addFreshEntity`, for every `Mob`. Every new mob reaches the world through it, already set up and with its age decided: natural spawns, spawners, trial spawners (including configured ones that skip `finalizeSpawn`, such as baby zombie trial configs), spawn eggs and egg offspring, infested blocks and the Infested effect, conversions (`Mob.convertTo` adds the new mob through it after copying baby state), reinforcements, jockeys and `/summon`. Hooking `finalizeSpawn` instead misses cave spiders (`CaveSpider.finalizeSpawn` doesn't call super), configured trial spawns, infestation and egg offspring.
  Loading from disk (`addWorldGenChunkEntities`/`addLegacyChunkEntities`) and dimension travel (`addDuringTeleport` → `addEntity`) don't pass through it, so a mob keeps the stats it spawned with. A converted mob is a fresh mob of its new type at full health in vanilla, so it gets its new type's row.
  Worldgen: structures that place mobs as their chunk generates (ocean ruin drowned, which can roll baby) add them through `WorldGenRegion.addFreshEntity`, which saves the mob into the proto chunk with its passengers. The same call hooks there, applied to the mob and its passengers, before the save. Not covered by a GameTest: the flat test world generates no ocean ruins, and `/place` uses the `ServerLevel` path.
- **Application is idempotent** (absolute base values from type, baby flag and difficulty).
- `/summon` with explicit attribute NBT has its listed base values overwritten. Accepted: "every mob of the type".
- **Creepers:** `SpawnPlacements.checkSpawnRules` returns false for `CREEPER` when the reason is `NATURAL` or `CHUNK_GENERATION`, the level is `Level.OVERWORLD`, and `pos.getY() >= 63`. `SPAWNER`, `TRIAL_SPAWNER`, spawn eggs and commands are untouched. 63 is a literal from the spec, not `getSeaLevel()`. Applies "with no exceptions for structures or caves" (design answer).
- **No game rules.** AGENTS.md only asks to offer game rules when Jackson tweaks a value.

## API sketch

New package `mapleflavored.mobs`:

```java
/** The spec's common mob table: each listed mob type's attribute base values on spawn, by difficulty. */
public final class CommonMobs {
	/** Gives mob its row's base values for the level's difficulty, and full health if the row sets max health. Safe to repeat. */
	public static void applySpawnStats(Mob mob);
}

/** Creepers never spawn naturally at Y 63 or above in the Overworld. */
public final class CreeperSpawns {
	public static boolean allowed(EntityType<?> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos);
}
```

Consumed only by mixins:

- New `ServerLevelMixin`: `@Inject(addFreshEntity, HEAD)` → `if (entity instanceof Mob mob) CommonMobs.applySpawnStats(mob)`.
- New `SpawnPlacementsMixin`: `@ModifyReturnValue(checkSpawnRules)` → `allowed && CreeperSpawns.allowed(...)`.

The table inside `CommonMobs` is a private list of rows: a match on (type, baby) and an attribute → value map per difficulty, written to read like the spec table.

## Files

- `src/main/java/mapleflavored/mobs/CommonMobs.java`, `CreeperSpawns.java` (new).
- `src/main/java/mapleflavored/mixin/ServerLevelMixin.java`, `SpawnPlacementsMixin.java` (new); register in `maple_flavored.mixins.json`.
- `CREDITS.md`: "Reused designs" entry crediting Matcha Flavoured by Klei for the mob numbers and the Y 63 creeper limit (`dp/data/matcha/function/mechanics/spawn_mechanic/{easy,normal,hard,normal_and_easy}_modifications/` and `predicate/surface_spawn.json`), noting Maple's differences (spec's Hard husk, no Weaving, Y limit only, no sky check).
- `src/gametest/java/mapleflavored/gametest/CommonMobGameTests.java` (new).
- Ticket: tick checkboxes, set status done.

## Acceptance matrix (GameTests)

Spawning in tests: `type.create(level, NATURAL)`, `snapTo`, `finalizeSpawn(level, difficultyAt, NATURAL, groupData)` with `new Zombie.ZombieGroupData(baby, false)` for the zombie family (vanilla's own way to force a baby) and `null` otherwise, persistence required, `addFreshEntityWithPassengers`. Assertions read attribute base values, plus `getHealth() == getMaxHealth()` where health is set. Spec numbers are literal in the test; "vanilla" cells compare with `DefaultAttributes.getSupplier(type)`.

One test per difficulty environment (`easy`, `normal`, `hard`), each checking every row:

| Case | Easy | Normal | Hard |
| --- | --- | --- | --- |
| Adult zombie | vanilla health, speed, step, damage, jump | 10 / 0.34 / step 1 | 15 / 0.38 / step 1 / 5 damage / 0.6 jump |
| Baby zombie, husk, drowned, zombie villager | 4 | 4 | 7 |
| Skeleton, stray, bogged, parched | 10 | 10 | 12 |
| Creeper | 14 | 16 | 18 |
| Cave spider | 2, 0.35 | 4, 0.40 | 6, 0.42 |
| Silverfish | 2 | 2 | 4 |
| Spider | vanilla jump, fall multiplier | 0.85 jump, 0 fall | 0.85 jump, 0 fall |
| Adult husk | vanilla | 30 / 0.25 / 6 / 12 / 50 / 0.9 | 40 / 0.265 / 8 / 14 / 60 / 1.0 |
| Adult zombie villager | vanilla | vanilla | vanilla |
| Adult drowned (unlisted) | vanilla | vanilla | vanilla |

Baby zombies, husks, drowned and zombie villagers also assert `isBaby()` and that speed, step height, damage, jump, armor, follow range and knockback resistance are vanilla, so adult rows can't leak onto babies.

Paths that skip `finalizeSpawn` (Normal): a silverfish from a broken infested stone has 2 health; a baby from a zombie spawn egg used on a zombie has 4 health.

Conversion (Normal): a baby zombie drowns into a baby drowned with 4 health; an adult husk drowns into a zombie with 10 health and 0.34 speed. Driven through the drowning tracker (`setInWaterTime`, wait for `isUnderWaterConverting`, `setConversionTime(0)`) in water.

Creeper spawn checks (Normal, via `SpawnPlacements.checkSpawnRules`) at a far, sealed dark stone site, waiting until sky and block light read 0:
- Overworld `NATURAL` at Y 62: allowed. At Y 63: refused. At Y 64: refused.
- Overworld `CHUNK_GENERATION` at Y 63: refused.
- Overworld `SPAWNER` and `TRIAL_SPAWNER` at Y 63: allowed.
- Zombie `NATURAL` at Y 63: allowed (only creepers are limited).
- Nether `NATURAL` at Y 63, in a sealed dark spot: allowed (only the Overworld is limited).

## Exclusions

- Skeleton bow enchantments and husk spawn locations stay vanilla (design answer), nothing to do.
- No trial spawner integration test: the trial spawner adds mobs through `tryAddFreshEntityWithPassengers`, the same hook the infestation and egg tests prove.
- No migration for mobs already in old worlds.

## Verification

`./gradlew build` with `runGametest` (the repo's full suite) must pass. Hand check: none (all server-side).
