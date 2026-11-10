# 13: Mobs, spawners and loot

**What to fix:** The architecture findings for common mob changes, Deep variants, Cleared spawners and loot substitution. Each finding stands alone unless it says otherwise.

**Blocked by:** None

**Status:** needs-triage

**Priority:** P2 (Medium)

**Estimate:** M

**Source:** Architectural Review 11/9, the mobs, spawners and loot reviewer. Nothing was built or run.

What held up: mixins are thin, each feature has one entry class, the shard decision has a single authority, `RaidWaves` and its JSON condition split the rule correctly, and the GameTests restate the spec instead of importing production tables.

Other tickets hold findings from this slice: ticket 04 finding 8 covers the copied loot test helpers, ticket 04 finding 9 covers the tuning constants, and ticket 10 finding 7 covers the fresh-entity hook that also serves `mobs`.

Paths: `main/` is `src/main/java/mapleflavored/`, `mixin/` is `src/main/java/mapleflavored/mixin/`, `client/` is `src/client/java/mapleflavored/client/`, `test/` is `src/gametest/java/mapleflavored/gametest/`, and `lt/` is `src/main/resources/data/minecraft/loot_table/`.

## Findings

### 1. The book-and-gear loot rule is steered by a JSON `type` field (P2)

**Principle:** Single Choice (Meyer). One authority. Rules live with the context that can enforce them.

`LootEnchantments` decides what happens to a rolled book or enchanted gear from the table's declared type: a chest book becomes a diamond, any other book drops to level I, and gear from four table types goes plain. That couldn't express "the ominous vault keeps its Mace books". So the two ominous vault tables were re-typed in JSON from `chest` to `vault`, and the one non-Mace book entry was turned into a diamond by hand. The rule is now implemented twice, in two languages, and the JSON `type` field is a hidden policy switch. Commit `4842764` describes the re-typing as intentional.

- A data pack that restores vanilla's `chest` type on `reward_ominous_rare` silently turns every Mace book into a diamond.
- Maple's own overrides still declare loot that Java later erases: `enchant_with_levels` on the fishing bow and rod, `enchant_randomly` gear and a Soul Speed book in the Bastion tables, and an enchanted golden apple in `bastion_treasure`.
- `piglin_bartering.json` expresses the same family of rule the other way, by deleting entries.
- The test already states the rule in its honest shape, as allowed books per table type (`BOOKS_BY_TABLE_TYPE`). Production has no such table.

Citations:

- `main/loot/LootEnchantments.java:27-32`, `:56-64`
- `lt/chests/trial_chambers/reward_ominous.json:2`
- `lt/chests/trial_chambers/reward_ominous_rare.json:2`, `:19`, `:38`, `:52`, `:64-66`, `:71`
- `lt/gameplay/fishing/treasure.json:26`, `:45`, `:55`
- `lt/chests/bastion_other.json:14`, `:42`, `:130-136`
- `lt/chests/bastion_treasure.json:72-167`, `:223`
- `main/loot/LootSubstitutions.java:21-22`
- `test/BookLootGameTests.java:33-41`

### 2. The client forcefield reads the spawner's save format and re-runs the Cleared requirement (P2)

**Principle:** A view type named for storage. One authority with explicit derivations.

`SpawnerClearing` is saved with one codec, and vanilla's update tag reuses the save. Every client receives the death count, the full set of player UUIDs, the Cleared flag and the held book. The renderer then calls `remainingShare()`, which recomputes `requirement()` on the client from compiled-in constants. A GameTest pins this by asserting the held book and player count reach the client.

- The requirement (12, plus 2 per additional player) is evaluated on both sides. Making either number a game rule would draw the wrong forcefield unless the sync is redesigned first.
- The client needs two values, the share remaining and Cleared. It gets the whole model, so any change to the saved shape is also a protocol change.
- The saved `cleared` flag is a second fact beside `deaths` and `players`. If the constants change, a state such as 15 deaths and not Cleared decodes to a negative share: no forcefield, yet unbreakable until one more death.

Citations:

- `main/spawners/SpawnerClearing.java:22-24`, `:27-32`, `:62-64`, `:77-81`
- `mixin/SpawnerBlockEntityMixin.java:36-44`
- `client/spawners/SpawnerForcefieldRenderer.java:54`, `:61-63`
- `mixin/SpawnerBlockMixin.java:30-34`
- `test/SpawnerGameTests.java:319-324`, `:429-434`

### 3. "The Overworld surface starts at Y 63" is declared twice (P2)

**Principle:** DRY as shared knowledge (Hunt and Thomas).

`CommonMobs` and `CreeperSpawns` sit in the same package and each declare `private static final int LOWEST_SURFACE_Y = 63` with its own `dimension == OVERWORLD && y < 63` test. Retuning one leaves creepers and mob health disagreeing about where the surface is. `DeepVariants.isDeep` is a third copy of the predicate with a different, legitimately separate threshold. Nothing names the Overworld's depth bands in one place.

- `main/mobs/CommonMobs.java:29`, `:136`
- `main/mobs/CreeperSpawns.java:15`, `:22-27`
- `main/deepvariants/DeepVariants.java:54`, `:182-184`

### 4. `spawners` imports `structurebooks` for one vanilla line, closing a cycle (P2)

**Principle:** Information hiding (Parnas).

The outward direction is well done: `Spawners.CLEARED` is an event, so spawners know nothing of `BookLocks`. The cycle comes from the other half. `SpawnerClearing` stores a `Holder<Enchantment>` called `heldBook`, and `Spawners.addClearedDrops` calls `StructureBooks.levelOneBook`, a one-line wrapper over `EnchantmentHelper.createBook`. So the spawner module knows the structure book concept, persists it and syncs it, while `structurebooks` calls back into `Spawners.holdBook` and `Spawners.CLEARED`. If the spawner held an opaque extra drop, all book knowledge would sit in `structurebooks`.

- `main/spawners/Spawners.java:9`, `:87-91`, `:189`
- `main/spawners/SpawnerClearing.java:31`, `:37`, `:72-74`, `:104-106`
- `main/structurebooks/SpawnerBooks.java:4`, `:43`
- `main/structurebooks/BookLocks.java:14`, `:78`
- `main/structurebooks/StructureBooks.java:233-235`

### 5. Three Bastion tables and the ominous vault have two authors (P3)

**Principle:** One authority.

Maple ships full JSON replacements of `bastion_treasure`, `bastion_other`, `bastion_hoglin_stable` and `reward_ominous`. `StructureChestRolls` then injects Pith and Crystal Heart pools into those same tables at data load. Pith for Evokers, Brutes and Elder Guardians is in JSON; Pith for chests is in Java; Pith for spawners is a post-roll Java drop. Knowing what a Bastion treasure chest gives means reading the JSON and three Java classes.

- `abandoned_mineshaft` is "safe, early" in `LootDiamonds` and "dangerous" in `StructureChestRolls`.
- `LootDiamonds` names tables by hand-typed path strings, while `StructureChestRolls` uses `BuiltInLootTables` keys.

Citations:

- `main/loot/StructureChestRolls.java:22-23`, `:45-66`, `:73-76`
- `lt/chests/bastion_treasure.json:30`, `:57`; `lt/chests/bastion_other.json:64`; `lt/entities/evoker.json:52`
- `main/spawners/Spawners.java:181-190`
- `main/loot/LootDiamonds.java:18-33`

### 6. Deep variant spawn stats are assembled by a protocol split across two classes (P3)

**Principle:** Information hiding. Empty and absent conflated.

`DeepVariants.spawnStats` owns the values, but `CommonMobs.deepVariantStats` owns the composition: it takes the base mob's row, strips health and damage, then overlays the Deep stats. `DeepVariants.damageMultiplier` later recovers the damage rule by dividing the mob's attack base value by vanilla's, which is right only because `CommonMobs` wrote exactly that value at spawn. Neither class states the contract the other relies on.

The seam already leaks once. `spawnStats` returns `Map.of()` for the Deep Creeper, documented as "keeps its base mob's". But `CommonMobs` has already removed the row's health, so empty means vanilla's 20 and not the creeper row's 14 to 18. This shows only for an egg-spawned or summoned Deep Creeper at Y 63 or above. The glossary says "a creeper's health".

- `main/mobs/CommonMobs.java:137-140`, `:150-159`
- `main/deepvariants/DeepVariants.java:104-105`, `:143-161`, `:167-175`
- `main/deepvariants/DeepSkeleton.java:45-47`
- `test/DeepVariantGameTests.java:385`

### 7. "Normal spawners ignore light" lives only in two mixins and reaches further than the other spawner rules (P3)

**Principle:** Rules live with the module that owns them.

The halved delay and the death marking both go through `Spawners`, which excludes spawner minecarts. The light rule is a constant `return true` in `BaseSpawnerMixin` plus a global change to `EntitySpawnReason.ignoresLightRequirements` for every `SPAWNER` caller. Neither consults `Spawners`, so minecart spawners ignore light but aren't hurried or counted.

- `mixin/BaseSpawnerMixin.java:37-46`
- `mixin/EntitySpawnReasonMixin.java:11-14`
- `main/spawners/Spawners.java:45-49`, `:97-111`, `:201-205`

### 8. The Wither Skeleton change is policy inside a mixin (P3)

**Principle:** Thin mixins. One mechanism per kind of rule.

`WitherSkeletonMixin` holds the Netherite sword, a drop chance of 0 and a base attack of 3.0. `CommonMobs.TABLE` exists to set attribute base values per mob type at spawn, and the Wither Skeleton isn't in it. The `ModifyArg` also targets every `setBaseValue` call in `finalizeSpawn`, which is correct only while vanilla has exactly one.

- `mixin/WitherSkeletonMixin.java:21-38`
- `main/mobs/CommonMobs.java:31-116`

### 9. One loot test bypasses the shared roller, and one expectation is tautological (P3)

**Principle:** Has a negative control shown the test detects the failure?

- `LootRolls` exists to roll tables the way the game does, yet `StructureChestGameTests.rollMany` builds its own params (`test/LootRolls.java:23-42`; `test/StructureChestGameTests.java:95-110`).
- `eachDeepVariantDropsItsBaseMobsLootAndTwiceItsXp` expects `DeepVariants.XP_MULTIPLIER * MONSTER_XP`. It would stay green if the multiplier became 1; only the skeleton-with-bow test pins the literal 2 (`test/DeepVariantGameTests.java:348`, `:366-367`).

## Checklist

- [ ] 1. One production table says which books and gear each loot table type keeps. The ominous vault tables don't depend on their `type` field for it, and Maple's overrides no longer declare loot that Java erases.
- [ ] 2. The client receives a spawner view with the share remaining and Cleared, not the save format, and doesn't compute the requirement.
- [ ] 3. One place names the Overworld's depth bands, and `CommonMobs`, `CreeperSpawns` and `DeepVariants` read it.
- [ ] 4. A spawner holds an opaque extra drop, and `spawners` no longer imports `structurebooks`.
- [ ] 5. Each replaced table gets its Pith and Crystal Heart rolls from one author, `abandoned_mineshaft` has one danger class, and `LootDiamonds` names tables by key.
- [ ] 6. One class assembles a Deep variant's stats, and a summoned Deep Creeper above Y 63 has a creeper's health.
- [ ] 7. The light rule goes through `Spawners` and applies to the same spawners as the other rules.
- [ ] 8. The Wither Skeleton's attack is a row in `CommonMobs`, and the mixin only gives the sword.
- [ ] 9. `StructureChestGameTests` rolls through `LootRolls`, and the Deep variant XP test expects a literal.
