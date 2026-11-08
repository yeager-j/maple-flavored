# Plan: 53 Deep Skeleton, Deep Creeper and Deep Spider

## End goal

Finish the Deep variant roster so deep caving below Y 0 carries four distinct threats, not one (stories 165a, 165b,
165d–165g). Each variant has its own look and one threat: the Deep Skeleton's Slowness arrows make retreating costly,
the Deep Creeper blows craters like TNT, and the Deep Spider's bite blinds. Closing it means the swap that ticket 52
built for zombies also turns natural skeletons, creepers, spiders and spider jockeys below Y 0 into Deep variants, with
their stats driven by the existing Deep variant game rules, while spawners and everything else stay vanilla.

## Facts confirmed on the 26.3 jar

- `Stray.getArrow` calls `super.getArrow` and, if the arrow is an `Arrow`, adds `new MobEffectInstance(SLOWNESS, 600)`
  (amplifier 0). A plain arrow's potion duration scale is 1 (the eighth belongs to tipped arrow items), so a target
  gets the full 600 ticks.
- `AbstractSkeleton.getArrow` → `ProjectileUtil.getMobArrow` → `arrow.setBaseDamageFromMob(power)`, which sets
  `baseDamage = power * 2 + triangle(difficultyId * 0.11, 0.57425)`. `baseDamage` is private with a setter only.
  A skeleton's bow goal attacks within 15 blocks; follow range defaults to 16.
- Skeleton, creeper and spider attributes come from `Monster.createMonsterAttributes()` (attack 2, follow range 16):
  skeleton 20 health, creeper 20, spider 16. `Monster` sets `xpReward = 5`.
- `Creeper.explosionRadius` is a private int, 3 by default, saved as `ExplosionRadius` and read back with a default of
  3, which `/summon` without data hits. `explodeCreeper` explodes with `explosionRadius * (isPowered() ? 2 : 1)`.
- `Mob.getBaseExperienceReward` starts from `xpReward` and adds 1–3 per piece of droppable gear, so doubling the field
  alone wouldn't double an armed skeleton's XP.
- `Spider.finalizeSpawn` makes a jockey 1 in 100 times with `EntityTypes.SKELETON.create(level, JOCKEY)`, finalizes it
  and starts it riding the spider. Natural spawning adds the spider with its passengers, so `CommonMobs` sees both.
- `CaveSpider.doHurtTarget` adds its poison after `super.doHurtTarget` succeeds; the Deep Spider's Blindness uses the
  same shape.
- Vanilla tags listing the base mobs directly: skeleton in `skeletons`, `burn_in_daylight`,
  `no_anger_from_wind_charge`; spider in `arthropod`, `dismounts_underwater`, `no_anger_from_wind_charge`; creeper in
  none. Smite and Bane of Arthropods follow `#skeletons` (through `#undead`) and `#arthropod`.
- `SpiderRenderer` adds `SpiderEyesLayer` (red `spider_eyes.png`); Jackson's spider texture has teal eyes on exactly the
  same pixels, so the Deep Spider needs its own eyes texture or it would glow red.

## Design

### `DeepVariants` (extended, still the one authority)

```java
public static final EntityType<DeepSkeleton> DEEP_SKELETON;  // skeleton's size, placement; skeleton attributes + follow range 32
public static final EntityType<DeepCreeper> DEEP_CREEPER;    // creeper's size, placement, attributes
public static final EntityType<DeepSpider> DEEP_SPIDER;      // spider's size, placement, attributes
public static final Item DEEP_SKELETON_SPAWN_EGG, DEEP_CREEPER_SPAWN_EGG, DEEP_SPIDER_SPAWN_EGG;

/** How many times its base mob's vanilla damage a Deep variant deals, read from its attack base value at spawn. */
public static double damageMultiplier(Mob deep);              // used by DeepSkeleton for arrows

/** The skeleton type a spider's jockey is: a Deep Skeleton for a Deep Spider, the vanilla skeleton otherwise. */
public static EntityType<? extends Skeleton> jockeyRider(Spider spider);   // used by SpiderMixin
```

- `DEEP_BY_BASE` gains skeleton, creeper and spider. `naturalSpawn` needs no change: for non-zombies it already
  swaps type and passes group data through (spider effect group data included).
- Entity types register through one private helper taking the factory, attributes and the builder's sizing, so all
  four share `createMob(..., spawnPlacement(ON_GROUND, MOTION_BLOCKING_NO_LEAVES, checkMonsterSpawnRules))`. The
  placement matters for ticket 59's deep dungeon spawners.
- `spawnStats(base, rules)`: a creeper base returns no stats, so a Deep Creeper keeps its vanilla 20 health and its
  explosion power is the fixed 4 (the spec says no rule scales either). Others as before: health × health percent,
  attack × damage percent.
- `damageMultiplier` = the mob's attack *base* value ÷ its base mob's vanilla attack. The base value is saved with the
  mob, so a Deep Skeleton's arrows keep the multiplier it spawned with, as the rules require, and potions don't scale it.
- Spawn eggs go in the creative tab after their base mob's egg.

### Entity classes (each doubles `getBaseExperienceReward`, gear bonus included; the Deep Zombie moves to this too)

- `DeepSkeleton extends Skeleton`: `getArrow` = super's arrow with `baseDamage × damageMultiplier(this)` (via a main
  `AbstractArrowAccessor`), plus a stray's Slowness (600 ticks, amplifier 0) on an `Arrow`. Attributes:
  `AbstractSkeleton.createAttributes().add(FOLLOW_RANGE, 32)`.
- `DeepCreeper extends Creeper`: constructor sets explosion radius 4 through a main `CreeperAccessor`, and
  `readAdditionalSaveData` reads a missing `ExplosionRadius` as 4. Vanilla doubles it when charged and saves it.
- `DeepSpider extends Spider`: `doHurtTarget` adds Blindness for 60 ticks to a living target after a successful hit.

### Hooks

- New `SpiderMixin`: `@ModifyExpressionValue` on the `EntityTypes.SKELETON` read in `Spider.finalizeSpawn`, returning
  `DeepVariants.jockeyRider(this)`. Natural jockeys below Y 0 are Deep Spiders (by the existing swap), so their riders
  become Deep Skeletons; a Deep Spider from an egg rides the same way, and a plain spider's jockey stays vanilla.
- `CommonMobs` unchanged: a Deep variant already takes its base row minus health and attack, plus `spawnStats`. So the
  Deep Spider gets the spider row's jump 0.85 and no fall damage; the skeleton and creeper rows only set health.

### Data and assets

- Tags (`replace: false`): add `deep_skeleton` to `skeletons`, `burn_in_daylight`, `no_anger_from_wind_charge`; add
  `deep_spider` to `arthropod`, `dismounts_underwater`, `no_anger_from_wind_charge`. Tested against every loaded tag.
- Loot tables `deep_{skeleton,creeper,spider}.json` nesting the base mob's table.
- Textures: Jackson's `deep_skeleton.png`, `deep_creeper.png`, `deep_spider.png` as given; `deep_spider_eyes.png` is
  vanilla's eyes mask filled with his teal eye colours. Spawn eggs recoloured from vanilla's to deepslate greys (spider
  egg's eyes teal). Credited in `CREDITS.md`.
- Client: `DeepSkeletonRenderer extends SkeletonRenderer`, `DeepCreeperRenderer extends CreeperRenderer` (texture only,
  charged layer kept), `DeepSpiderRenderer extends SpiderRenderer<DeepSpider>` swapping the eyes layer for a teal one.
  Registered from `MapleFlavoredClient`.
- Lang and item assets/models for the three types and eggs. `docs/project-overview.md` Deep variants paragraph updated.

## Invariants

- Natural replacement only (Overworld, Y < 0); spawners, eggs, commands, structures, conversions, Restores unchanged.
- Deep stats set once at spawn from the rules; arrows use the saved attack base value.
- Each Deep variant is in every tag its base mob is in.

## Out of scope

- A charged creeper's kill drops no head for Deep variants: vanilla's `charged_creeper/root` table matches exact entity
  types (same as the Deep Zombie today). Flag to Jackson.
- A Deep Skeleton frozen in powder snow becomes a plain stray (vanilla conversion). Bow attack radius stays 15.
- Deep dungeon spawners (59).

## Tests (`DeepVariantGameTests`)

1. Natural spawning in the deep and shallow dark rooms now counts all four base kinds: until ≥ 3 of each kind seen, every
   adult zombie/skeleton/creeper/spider below Y 0 is Deep, none above.
2. `naturalSpawn` decisions: skeleton, creeper and spider at Y −1 → Deep, Y 0 and Nether → base, group data passed on.
3. Jockeys: finalize Deep Spiders until one has a rider → Deep Skeleton; finalize spiders until one has a rider →
   skeleton. Added with passengers, the Deep Skeleton rider has Deep stats.
4. Stats, Normal: Deep Skeleton 30 health 4 attack follow range 32; Deep Spider 24/4, jump 0.85, fall multiplier 0;
   Deep Creeper 20 health. Under `deep_variant_rules` (200/300): 40/6, 32/6, creeper still 20.
5. Arrows: a Deep Skeleton's `performRangedAttack` arrow has base damage in 2× (Normal) / 3× (rules) vanilla's range; the
   arrow hits a target 4 blocks away and gives it Slowness (amplifier 0).
6. Deep Creeper: power 4 when spawned, summoned without data and reloaded (gametest accessor); in two far dirt cubes, a
   Deep Creeper's explosion clears more blocks than a creeper's.
6a. A Deep Skeleton's arrow multiplier stays at its spawn value after the damage rule changes and after reloading; a
   later one gets the new value. Its arrow's Slowness matches a real stray's in amplifier and duration.
7. Deep Spider bite gives Blindness 60 ticks to a survival player; a spider's doesn't. Bane of Arthropods I adds 2.5
   against it, Smite I adds 2.5 against the Deep Skeleton; tag inclusion for all three.
8. Drops and XP for each: same loot as the base table over 200 seeds; gearless XP 10 vs 5; a bow-holding Deep
   Skeleton's XP is always even and within twice an armed skeleton's 6–8.

Then the full `gradlew build`.

Hand check (Jackson): textures in a dark cave, a Deep Skeleton shooting from beyond torchlight, Deep Creeper crater
beside a creeper's. Story 165b's balance target (full iron beats one of each and survives two or three; copper loses to
two) can only be playtested: note difficulty and the two Deep variant rule values.
