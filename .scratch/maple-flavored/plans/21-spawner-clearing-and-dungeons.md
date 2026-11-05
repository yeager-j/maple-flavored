# Plan: 21 Spawner Clearing and more dungeons

## Context / end goal

Spawners are the mod's first "deal with it" obstacle: right now a torch beats a dungeon and a pickaxe removes the spawner for free. Closing this ticket means every normal spawner fights back. It ignores light and can't be mined or blown up until its own mobs have died enough times (12, plus 2 per extra player who came near). Once Cleared it stays Cleared and can be kept as a farm or broken once for Pith. Dungeons get more common and become the early source of Bane of Arthropods and Smite books. The Crystal Heart advancements finally get their parent. Ticket 22 (the Fortress lock) builds on the `isCleared` query and the Cleared event this ticket adds.

Jackson's calls (2026-11-04):

- **Dungeon book:** it's held by the dungeon's spawner and drops with the Pith when the Cleared spawner is broken. It doesn't float above the spawner. It fights the dungeon's mob, chosen at generation: Smite for a zombie or skeleton dungeon, Bane of Arthropods for a spider dungeon. (It started as an equal-weight choice and was tied to the mob after review.) Every Mineshaft cave spider spawner also holds a Bane of Arthropods book, on top of the Mineshaft's Efficiency book.
- **Progress look:** no cracks and no new textures. An uncleared spawner is wrapped in the world border's forcefield. It starts half a block out and closes in on the spawner in step with the share of the requirement still to go, then vanishes once Cleared. A Cleared spawner is just a normal spawner, with no extra effect. The spec's "cracks that grow with the count" (lines 362, 448 and 518) and the ticket's wording change to match. (A first try, a particle cage of 12 edge bars, was replaced after a look in game.)
- **Spawn speed:** until Cleared, a spawner waits half the delay vanilla chooses, so it spawns twice as fast. Once Cleared it goes back to vanilla speed.
- **Tool rule:** Pith and the book follow vanilla's spawner XP rule and need a pickaxe (`requiresCorrectToolForDrops`). Breaking a spawner by hand drops nothing. The advancement fires only when the Pith drops.

## What the evidence says (26.3 sources)

- `BaseSpawner.serverTick` has two light gates. (1) `SpawnPlacements.checkSpawnRules` → `Monster.checkMonsterSpawnRules`, which skips `isDarkEnoughToSpawn` only when `EntitySpawnReason.ignoresLightRequirements(reason)` is true, and that's only for `TRIAL_SPAWNER`. (2) `mob.checkSpawnRules(level, SPAWNER)`, which for every `PathfinderMob` is `getWalkTargetValue >= 0`. That value is light-based for monsters and animals. The only non-light overrides are the Hoglin's repellent check and the Strider's lava check. Custom `SpawnData` spawn rules (explicit light ranges) skip both gates, and are left alone.
- `SpawnerBlockEntity` saves through `saveAdditional`/`loadAdditional`, and its update tag is `saveCustomOnly` minus `SpawnPotentials`. So fields saved there reach the client too, which matters because `getDestroyProgress` runs on both sides.
- `Blocks.SPAWNER` has strength 5 and `requiresCorrectToolForDrops`. Its loot table `blocks/spawner` exists but has no drops, so `LootTableEvents.MODIFY_DROPS` fires for it. Both `Block.playerDestroy` (survival, correct tool only) and `BlockBehaviour.onExplosionHit` roll it with `BLOCK_ENTITY` set.
- `MonsterRoomFeature.place` sets the spawner and calls `spawner.setEntityId(...)` with the feature's random. Placed features `monster_room` (count 10, Y 0 to top) and `monster_room_deep` (count 4, 6 above bottom to Y -1) are data JSON.
- `consume_item` (fired from `Consumable.onConsume`, which our capped Crystal Heart never reaches) and `used_totem` (fired with the protecting item) are vanilla triggers that already carry the item. So two of the Crystal Heart advancements are data only.
- Monster Hunter is `minecraft:adventure/kill_a_mob`.

## Decisions

1. **New module `mapleflavored.spawners`.** `Spawners` is the facade: `register()`, `isCleared(ServerLevel, BlockPos)`, the `CLEARED` event and the internal hooks the mixins call. `SpawnerClearing` is the per-spawner state: deaths, the set of player UUIDs, the Cleared flag and an optional held book (`ResourceKey<Enchantment>`). It has the pure rules: `requirement()` = 12 + 2 × max(0, players − 1), `recordPlayer`, `recordDeath` (returns true on the death that Clears it), `remainingShare()` ((requirement − deaths) / requirement, 0 once Cleared) and `spawnDelay(vanillaDelay)` (half until Cleared). It has a codec, so it saves and syncs under `maple_flavored:clearing`. Once Cleared it ignores further deaths and players.
2. **State lives on `SpawnerBlockEntity`** through `SpawnerBlockEntityMixin`, with a duck interface. It saves and loads in `saveAdditional`/`loadAdditional`, and it ticks in `serverTick`. Each tick, if the spawner isn't Cleared, it records alive non-spectator players within the spawner's own `requiredPlayerRange` (16 by default, read through a `BaseSpawner` accessor). That lets tests shrink the range so neighbouring tests' mock players stay out. Every death and every new player calls `sendBlockUpdated`. Both are rare, and the client needs the state to draw the forcefield and predict `getDestroyProgress`. "Since counting began" means since the spawner existed, so the set only grows and the requirement never drops.
3. **The forcefield is drawn on the client.** `SpawnerForcefieldRenderer` replaces vanilla's spawner renderer, drawing vanilla's spawner through a `SpawnerRenderer` it holds and then a box around it. The box uses the world border's `forcefield.png` with the `energySwirl` render type, which is additive and scrolls. It's tinted the world border's stationary blue, and repeats and scrolls the way the world border does. Its outset goes from 0.5 to 0.02 blocks with `remainingShare()`. `BaseSpawnerMixin` also hooks the private `delay` to halve the wait while the spawner block isn't Cleared. Spawner minecarts keep vanilla's wait.
4. **Tagging spawned mobs.** `BaseSpawnerMixin` wraps `tryAddFreshEntityWithPassengers`. When this `BaseSpawner` belongs to a `SpawnerBlockEntity` at `pos`, `Spawners.onSpawned` gives the entity and its passengers a persistent Fabric entity attachment, `maple_flavored:spawned_by` (`GlobalPos`). `ServerLivingEntityEvents.AFTER_DEATH` looks the spawner up in that dimension and records the death. Any cause counts. A lookup can load the spawner's chunk in the rare case the mob wandered far from it. A spawner that's gone or already Cleared ignores the death. When the count reaches the requirement, it's Cleared: setChanged, `sendBlockUpdated` (so the client learns it's Cleared), and `CLEARED` fires once.
5. **Light.** `EntitySpawnReasonMixin` makes `ignoresLightRequirements(SPAWNER)` true. `BaseSpawnerMixin` also wraps `Mob.checkSpawnRules` in `serverTick` to return true. This covers every `BaseSpawner` (blocks and command-made spawner minecarts), but never trial spawners, which use their own code and `TRIAL_SPAWNER`.
6. **Unbreakable and explosion-proof until Cleared.** `SpawnerBlockMixin` adds overrides to `SpawnerBlock`. `getDestroyProgress` returns 0 unless the block entity is Cleared, which works on the client from the synced state. `onExplosionHit` does nothing unless Cleared. `playerDestroy` calls super and fires `MapleTriggers.CLEARED_SPAWNER_BROKEN` when the spawner was Cleared. Vanilla only calls it with the correct tool, which is the same rule as the drops. `minecraft:spawner` is added to the `wither_immune` and `dragon_immune` tags, since those bosses use `destroyBlock` and skip the explosion path. That also makes Cleared spawners immune to them, which is accepted. Creative and commands still remove spawners.
7. **Drops.** `MODIFY_DROPS` on `Blocks.SPAWNER`'s loot table: if `BLOCK_ENTITY` is a Cleared spawner, add 1 Pith and, if it holds a book, `StructureBooks.levelOneBook(enchantment)` (made public). That covers a pickaxe break and an explosion of a Cleared spawner. Enchanted book entities already resist explosions.
8. **Spawner books.** `structurebooks.SpawnerBooks.forMob` reads the mob the dungeon chose. It gives Smite for `#sensitive_to_smite` and Bane of Arthropods for `#sensitive_to_bane_of_arthropods`, the tags the enchantments themselves use. Any other mob gets one of the two with equal weight from the feature random. `MonsterRoomFeatureMixin` and `MineShaftCorridorMixin` hook after `setEntityId`, where they have the mob, and call `Spawners.holdBook(spawnerBlockEntity, enchantment)`. A held key whose enchantment is missing at break drops no book.
9. **Dungeon frequency:** `data/minecraft/worldgen/placed_feature/monster_room.json` (count 15) and `monster_room_deep.json` (count 8), copied from the 26.3 jar with only the count changed. Credit Matcha Flavoured in `CREDITS.md` under Reused designs.
10. **Advancements** (Adventure tab, each with a title and description in `en_us.json`):
   - `adventure/break_cleared_spawner`: parent `minecraft:adventure/kill_a_mob`, task, trigger `maple_flavored:cleared_spawner_broken`, icon spawner.
   - `adventure/consume_crystal_heart`: parent break_cleared_spawner, task, `minecraft:consume_item` with item `maple_flavored:crystal_heart`.
   - `adventure/saved_by_crystal_heart`: parent consume, task, `minecraft:used_totem` with item crystal_heart. Postmortal stays Totem-only.
   - `adventure/max_hearts_cap`: parent consume, challenge, `rewards.experience: 100`, trigger `maple_flavored:max_hearts_cap_reached`. It fires from `Hearts.setMaxHearts` whenever the result is 30, so the command path counts too.

## API sketch

```java
// mapleflavored.spawners.Spawners
public static final Event<Cleared> CLEARED; // (ServerLevel level, BlockPos pos), fired once per spawner
public static boolean isCleared(ServerLevel level, BlockPos pos);
public static void holdBook(SpawnerBlockEntity spawner, ResourceKey<Enchantment> enchantment);
static void onSpawned(ServerLevel level, BlockPos spawnerPos, Entity entity);  // called from BaseSpawnerMixin; public for tests
// SpawnerClearing: requirement(), deaths(), isCleared(), heldBook(), remainingShare(), spawnDelay(int), CODEC
```

## Files

- New: `spawners/Spawners.java`, `spawners/SpawnerClearing.java`, `spawners/ClearableSpawner.java` (duck), `structurebooks/SpawnerBooks.java`, `mixin/MineShaftCorridorMixin.java`
- Mixins: `SpawnerBlockEntityMixin`, `SpawnerBlockMixin`, `BaseSpawnerMixin` (plus a `requiredPlayerRange` accessor), `EntitySpawnReasonMixin`, `MonsterRoomFeatureMixin`, registered in `maple_flavored.mixins.json`
- Changed: `MapleTriggers` (two triggers), `Hearts.setMaxHearts`, `StructureBooks.levelOneBook` (public), `MapleFlavored.onInitialize` (`Spawners.register()`)
- Data: the 4 advancement JSONs, the 2 placed features, the `wither_immune` and `dragon_immune` tags, `en_us.json`
- GameTests: new `SpawnerGameTests` and `DungeonGameTests`, registered in the gametest `fabric.mod.json`. Crystal Heart advancement checks go in `CrystalHeartGameTests`, which already has `isDone`.
- Docs: `GLOSSARY.md` (structure book: a dungeon's book is held by its spawner; Cleared stays), spec lines 353 and 357 (dungeon book drops from the broken Cleared spawner) and 362, 448 and 518 (the forcefield replaces cracks; the spawn speed), ticket 21 boxes and the wording of its dungeon-book and progress criteria, `CREDITS.md`, and this plan copied to `.scratch/maple-flavored/plans/21-spawner-clearing-and-dungeons.md`

## Acceptance matrix

| Case | Expected |
| --- | --- |
| Zombie spawner surrounded by light 15, player in range, delay 0 | a zombie spawns, tagged with the spawner's `GlobalPos` |
| Trial spawner | breakable at its vanilla speed; its mobs carry no tag |
| Uncleared spawner, survival player with a pickaxe | `getDestroyProgress` is 0. After Clearing it's > 0 |
| Uncleared spawner next to an explosion | still there. A Cleared one is destroyed and drops 1 Pith |
| 1 player in range, 11 tagged deaths | not Cleared. The 12th Clears it and `CLEARED` fires once |
| 3 players entered, then 2 leave | the requirement stays 16: 15 deaths don't Clear it, 16 do |
| A player enters mid-count | the requirement rises by 2 |
| Tagged mob dies in lava, or with `/kill` | counts |
| `remainingShare()` with 1 player, after 0, 1, 11 and 12 deaths | 1, 11/12, 1/12, 0 (Cleared). With 3 players (requirement 16) after 4 deaths: 0.75 |
| Spawners with a fixed 200-tick wait, one uncleared, one Cleared | they wait at most 100 and more than 100 ticks |
| A death on the server | the client copy of the spawner's state matches (update tag round trip) |
| A death after Cleared, or an untagged mob's death | changes nothing |
| Cleared spawner broken with a pickaxe | 1 Pith, no book (not a dungeon), and the breaker gets `break_cleared_spawner` |
| Cleared spawner broken by hand | no drops, no advancement |
| Spawner state saved and loaded (`saveWithFullMetadata` → `loadWithComponents`) | deaths, players, Cleared and the held book all survive |
| `monster_room` placed in prepared stone boxes, 8 seeds | each spawner holds Smite for zombies and skeletons, Bane of Arthropods for spiders, and both kinds appear. Clearing and breaking it drops that book at level I, plus Pith |
| `SpawnerBooks.forMob` | zombie and skeleton give Smite, spider and cave spider give Bane of Arthropods. A Blaze, in neither tag, gives either, each within 4 binomial SD of half over 200 rolls |
| 6 Mineshafts placed | at least one cave spider spawner, and each holds Bane of Arthropods |
| Placed features from the loaded registry | `monster_room` count 15, Y 0 to top. `monster_room_deep` count 8 |
| Consume a Crystal Heart | `consume_crystal_heart` done. A refused heart at 30 doesn't grant it |
| Saved by a Crystal Heart | `saved_by_crystal_heart` done. Postmortal isn't |
| Reach 30 Max Hearts | `max_hearts_cap` done. It's a challenge with a 100 XP reward, and its parent chain matches the table |
| Existing GameTests | still pass |

## Invariants

- A spawner's requirement never drops, and Cleared is never undone.
- `CLEARED` fires exactly once per spawner.
- An uncleared spawner can't be removed by survival mining, explosions, the Wither or the Dragon.
- Pith and the held book drop only from a Cleared spawner, and only once (the block is gone).

## Verification

Set `JAVA_HOME` to `C:/Users/jackson/.jdks/jdk-25.0.4.1+1`, then `gradlew build` and `gradlew runGameTest`, and check `build/gametest/report.xml`. Then `larp review --message "Review the diff"`, fix the findings, commit on branch `21-spawner-clearing-and-dungeons`, push, open a PR, and watch Codex's review with `/watch-prs`.

## Hand checks (left unchecked on the ticket)

The forcefield reads like the world border, closes in visibly with each kill, and is gone once Cleared. Tune its starting distance and colour then. Dungeons show up more often in a fresh world.

## Exclusions

The Fortress record and lock (ticket 22), Blood Moon Restores, the Orichalcum drop bonus, and blocking explosion rays behind an uncleared spawner (it survives, but blocks behind it can still break).
