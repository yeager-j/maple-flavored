# Plan: 52 Deep variants and the Deep Zombie

## End goal

Caving below Y 0 should be visibly and mechanically more dangerous, so a day-one player in copper thinks twice
(stories 165a–165c, 165f, 165g). This ticket lands the Deep variant framework plus its first mob. Closing it means: a
natural zombie spawn in deep caves is a Deep Zombie (own look, 30 health, 6 attack, shield-disabling hits, zombie
loot and double XP), everything else that makes zombies stays untouched, and ticket 53 can add the other three
variants by adding an entity class, a registration line, a texture and their advantage.

## Facts confirmed on the 26.3 jar

- An axe disables a shield through `LivingEntity.getSecondsToDisableBlocking()` (axe `Weapon` component: 5 s).
  `Player.blockUsingItem` passes it to `BlocksAttacks.disable`, which puts `seconds * disableCooldownScale * 20`
  ticks of cooldown on the shield (shield scale 1 → 100 ticks) and stops its use. The Warden overrides the same
  method to return 5. The Deep Zombie does the same.
- `Zombie.hurtServer` spawns reinforcements with `this.getType()`, so a `DeepZombie extends Zombie` already calls
  Deep Zombies. Reinforcements also call `SpawnPlacements.checkSpawnRules(type, …)`, so the Deep Zombie needs the
  zombie's spawn placement registered (Fabric `FabricEntityType.Builder.createMob(...).spawnPlacement(...)`).
- `NaturalSpawner.spawnCategoryForPosition` picks the type (`getRandomSpawnMobAt`), then calls
  `getMobForSpawn(level, type)`, then `finalizeSpawn(..., groupData)`. A zombie's baby roll happens in
  `Zombie.finalizeSpawn` when `groupData == null` (`Zombie.getSpawnAsBabyOdds(random)` → `ZombieGroupData`), and the
  returned group data is reused by the rest of the group. Hostile mobs don't spawn at chunk generation.
- `Mob.getLootTable()` is final and `EntityType.Builder` has no loot-table setter, so the default loot table
  `maple_flavored:entities/deep_zombie` is shipped as data that nests `minecraft:entities/zombie` (a
  `minecraft:loot_table` entry). The zombie table and any Maple edits to it stay the single authority.
- Vanilla entity tags listing `minecraft:zombie` directly: `zombies`, `burn_in_daylight`,
  `no_anger_from_wind_charge`. `undead`, `sensitive_to_smite` and others follow through `#zombies`.
- `Monster` sets `xpReward = 5`.

## Design

### New module `mapleflavored.deepvariants`

```java
/** Deep variants: stronger forms of common mobs that take their place in natural spawns below Y 0. */
public final class DeepVariants {
	public static final EntityType<DeepZombie> DEEP_ZOMBIE = ...; // createMob, zombie's size, spawn placement, attributes
	public static final Item DEEP_ZOMBIE_SPAWN_EGG = ...;

	public static void register();                                  // creative tab entries; called from MapleFlavored

	/** The base type a Deep variant stands in for, or empty for any other type. */
	public static Optional<EntityType<?>> baseOf(EntityType<?> type);

	/** What natural spawning makes when it picks {@code picked} at {@code pos}, with the group data to pass on. */
	public static NaturalSpawn naturalSpawn(EntityType<?> picked, ServerLevel level, BlockPos pos, @Nullable SpawnGroupData groupData);
	public record NaturalSpawn(EntityType<?> type, @Nullable SpawnGroupData groupData) {}

	/** A Deep variant's health and damage from the game rules, as base values to set at spawn. */
	public static Map<Holder<Attribute>, Double> spawnStats(Mob mob, EntityType<?> base);   // used by CommonMobs
	public static final int XP_MULTIPLIER = 2;
}
```

- One `Map<EntityType<?>, EntityType<?>> BASE_TO_DEEP` (zombie → deep zombie) is the authority for which types have
  variants; `baseOf` is its inverse. Ticket 53 adds three entries.
- `naturalSpawn`: replaces only in the Overworld, at `pos.getY() < 0`, when `picked` has a variant. For zombies it
  first rolls the age the way `Zombie.finalizeSpawn` would (same random, same odds, only when `groupData` is null),
  hands that `ZombieGroupData` back, and keeps a plain zombie if the roll is baby. Everything else returns
  `(picked, groupData)` unchanged.
- `DeepZombie extends Zombie`: overrides `getSecondsToDisableBlocking()` to `Math.max(super, 5)` (the axe's seconds)
  and doubles `xpReward` in its constructor.

### Hooks

- `NaturalSpawnerMixin`: `@WrapOperation` on the `getMobForSpawn` call inside `spawnCategoryForPosition`, with
  `@Local MutableBlockPos pos` and `@Local LocalRef<SpawnGroupData> groupData`. Spawn weights, caps, placement and
  position checks keep running on the picked zombie, so they stay vanilla. Spawners, eggs, commands, structures,
  conversions, Restores and reinforcements never pass through here, so they're unaffected by construction.
- `CommonMobs.applyRow` (already called for every fresh entity from `ServerLevelMixin`/`WorldGenRegionMixin`): a
  Deep variant looks up its base type's row, drops `MAX_HEALTH` and `ATTACK_DAMAGE` from it, and merges
  `DeepVariants.spawnStats` (health = base's vanilla default × health percent, damage = base's vanilla default ×
  damage percent, from `DefaultAttributes`). The existing "set base values, then full health" code applies them.
  So a Deep Zombie gets zombie speed, step-up and Hard's jump, and its 30/6 at any height and difficulty (vanilla
  scales damage by difficulty against players). The one stat-application point stays in `CommonMobs`.
- `MapleGameRules`: `DEEP_VARIANT_HEALTH_PERCENT` (150, range 1–1000) and `DEEP_VARIANT_DAMAGE_PERCENT` (200, range
  0–1000), category MOBS. Apply to Deep variants that spawn after a change.

### Data and assets

- Tags (`replace: false`): `minecraft:zombies`, `burn_in_daylight`, `no_anger_from_wind_charge` add
  `maple_flavored:deep_zombie`.
- Loot table `data/maple_flavored/loot_table/entities/deep_zombie.json` nesting the zombie's.
- Client: `DeepZombieRenderer extends ZombieRenderer`, overriding the texture (adult and baby, since a spawn egg used
  on a Deep Zombie makes a baby). Registered from `MapleFlavoredClient`.
- Textures made with Pillow from the zombie's: skin and clothes kept recognisable, deepslate-grey crust patches over
  shoulders/head/arms, desaturated skin. Spawn egg texture recoloured from the zombie egg. Credited in `CREDITS.md`.
- Lang: `entity.maple_flavored.deep_zombie`, `item.maple_flavored.deep_zombie_spawn_egg`. Item asset + model.
- `docs/project-overview.md`: a short Deep variants paragraph in "Early equipment and mobs".

## Invariants

- Natural replacement only: Overworld, Y < 0, picked type has a variant, zombie age roll adult.
- Baby rate unchanged below Y 0; babies are plain baby zombies.
- Deep stats are set once at spawn from the game rules then; loaded mobs keep saved values.
- Deep Zombie is in every tag a zombie is in (tested over every loaded entity type tag, Fabric's included).

## Out of scope

Deep Skeleton/Creeper/Spider and spider jockeys (53). Villager flee distances and parrot imitation hardcoded on
`EntityTypes.ZOMBIE` aren't tags; left vanilla.

## Tests (`DeepVariantGameTests`, Normal unless noted)

1. Natural spawning below Y 0: sealed dark room at a far forced site below Y 0, a survival mock player 30 blocks
   away, call `NaturalSpawner.spawnCategoryForPosition(MONSTER, …)` repeatedly until ≥ 6 zombies of either kind
   spawn; assert every adult is a Deep Zombie, no Deep Zombie is a baby, and at least one Deep Zombie spawned.
2. Same above Y 0 (Y 10): zombies spawn, no Deep Zombies.
2a. `naturalSpawn` decisions, deterministic: adult and baby `ZombieGroupData` at Y −1 (Deep Zombie / zombie, group
    data passed through), Y 0 (zombie), Nether at Y −1 (zombie), a skeleton at Y −1 (unchanged), and a fresh group
    (null data) gets `ZombieGroupData` back whose age matches the returned type.
3. A zombie spawner below Y 0 spawns zombies, not Deep Zombies.
4. Stats: Deep Zombie spawned on Normal and Hard → base max health 30, attack 6, full health, zombie speed and
   step-up (and Hard's 0.6 jump); with game rules 200/300 via a test environment → 40 and 9.
4a. Rules apply to later spawns only: spawn and damage a Deep Zombie, change both rules, spawn another; the first
    keeps 30/6 and its damaged health, the second gets the new values.
5. Shield: survival mock player blocking with a shield (use ticks advanced past the block delay through a gametest
   accessor), Deep Zombie `doHurtTarget` → shield on cooldown and use stopped; still on cooldown after 99 cooldown
   ticks, off after 100. A plain zombie's hit leaves it usable.
6. Smite I adds 2.5 damage against a Deep Zombie (`EnchantmentHelper.modifyDamage`); every entity type tag holding
   the zombie holds the Deep Zombie.
7. Drops and XP: the Deep Zombie's loot table, rolled with the same seeds and params as the zombie's, gives the same
   items each time; a player kill drops loot. `getExperienceReward` of a gearless adult is twice a plain zombie's
   (gear's per-item XP bonus stays vanilla's).
8. Reinforcements: on Hard with reinforcement chance forced to 1, a hurt Deep Zombie calls a Deep Zombie.

Then the full `gradlew build` (runs server GameTests).

Hand check (Jackson): texture in a dark cave; raised shield goes on cooldown when hit.
