# Vanilla Ancient City sculk and shriekers

Research notes for Maple Flavored. They describe how vanilla Minecraft 26.3 puts sculk, sculk sensors and sculk shriekers into Ancient Cities, and when their positions can be known. They make no design recommendations.

## Source and confidence

Data files come from `minecraft-common.jar` in `~/.gradle/caches/fabric-loom/26.3/`. Paths starting `data/minecraft/` are inside that jar. In 26.3 the configured features live in `data/minecraft/worldgen/feature/`, not `configured_feature/`.

Code comes from the Mojang-named jar `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.3/minecraft-merged-deobf-26.3.jar`. I decompiled the classes named below with Vineflower 1.12.0, which was already in the Gradle cache (`~/.gradle/caches/modules-2/files-2.1/org.vineflower/vineflower/1.12.0/`), and checked a few classes with `javap -c -p`. Class and method names below are from that jar.

The 58 template files under `data/minecraft/structure/ancient_city/` were read with a small read-only NBT script. It counted every block whose id contains `sculk` and listed every jigsaw block's pool, name, target, final state and orientation. 26.3 templates store palette entries as `id` and `properties`.

Labels:

- **Fact**: directly stated by a cited data file or by the decompiled code. Unlabelled statements in tables and lists are facts.
- **Inference**: my reading of how the cited code and data combine at run time.
- **Unconfirmed**: could not be established from the jar.

Nothing was run in game, and no world was generated.

## Summary

Jackson's belief is mostly right.

1. **No Ancient City template contains a sculk shrieker, sculk block, sculk vein or sculk catalyst.** The only sculk-family blocks in any template are 10 sculk sensors: one in each of the three `city_center` templates and seven in the two `city_center/walls/bottom_*` templates.
2. **All shriekers come from features run on top of the placed templates.** Six building templates hold 12 jigsaw blocks that point at the pool `minecraft:ancient_city/sculk`. That pool picks the feature `minecraft:sculk_patch_ancient_city` with weight 6 or nothing with weight 1. The Deep Dark biome's own `sculk_patch_deep_dark` feature also runs inside the city.
3. **Every shrieker either feature makes has `can_summon: true`.** The ancient-city feature places 1 to 3 shrieker attempts, hard-coded `can_summon: true`, within 2 blocks of the patch origin on each axis. Both features can also grow shriekers from the sculk spreader: 1 in 11 growths is a shrieker, and the spreader sets `can_summon` to true during world generation. Growth shriekers land up to 12 blocks horizontally from the origin.
4. **Patch origins are known at structure-start time. Shrieker positions are not.** The jigsaw step that plans the city creates a piece for each sculk patch, with its exact origin, when the structure start is generated. The patch itself runs later, when the chunk holding the origin reaches the features step. Its random source comes from that chunk's decoration seed and from how much randomness earlier pieces in the same chunk used. It also reads and writes blocks in the surrounding chunks. So the result is seeded, but it can only be learned by running it against the world.
5. The processor lists do not touch sculk. They can remove the deepslate block under a patch origin 5% of the time.
6. Deep Dark biome patches run after the city's pieces in each chunk and can grow `can_summon: true` shriekers anywhere in the city's air spaces. These are indistinguishable by block state from the city's own shriekers.
7. A sculk patch can place no shrieker at all. If its origin is not air or a water source, or has no full-block neighbour, the sequence stops before the shrieker step. Even when it runs, every shrieker attempt can fail the air and sturdy-floor checks. A typical city's shrieker count is Unconfirmed.

## 1. What the templates contain

Template folders under `data/minecraft/structure/ancient_city/`: `city/entrance/` (6), `city_center/` (3), `city_center/walls/` (11), `structures/` (21) and `walls/` (17).

Sculk-family blocks across all 58 templates:

| Template | Sculk sensors | Shriekers | Sculk, vein or catalyst |
| --- | --- | --- | --- |
| `city_center/city_center_1.nbt` | 1, at local (7, 3, 16), waterlogged | 0 | 0 |
| `city_center/city_center_2.nbt` | 1, at local (7, 3, 16), waterlogged | 0 | 0 |
| `city_center/city_center_3.nbt` | 1, at local (7, 3, 16), waterlogged | 0 | 0 |
| `city_center/walls/bottom_1.nbt` | 4 | 0 | 0 |
| `city_center/walls/bottom_2.nbt` | 3 | 0 | 0 |
| All other 53 templates | 0 | 0 | 0 |

Every sensor is `sculk_sensor_phase: inactive`, `power: 0`. All are waterlogged except one in `bottom_2` at (10, 6, 16).

The city centre. `worldgen/structure/ancient_city.json` sets `start_pool: minecraft:ancient_city/city_center`, `start_jigsaw_name: minecraft:city_anchor`, `start_height: absolute -27`, `size: 7` and `max_distance_from_center: 116`. `worldgen/template_pool/ancient_city/city_center.json` picks `city_center_1`, `_2` or `_3`, weight 1 each, all with the processor list `ancient_city_start_degradation`. Each centre template is 18 × 31 × 41 blocks, has 55 `reinforced_deepslate` blocks (the portal frame), and has its `city_anchor` jigsaw at local (13, 24, 20). The centre templates have no sculk jigsaws, so no sculk patch is attached to the centre piece itself.

The templates store almost no air. Across all 58 there are no `minecraft:air` entries and 3 `cave_air` entries, all in `city_center_2`. **Inference:** a template only writes the blocks it lists (`StructureTemplate.placeInWorld`), so open space in the city comes from terrain carving, not from the templates. `ancient_city.json` sets `terrain_adaptation: beard_box`. In `Beardifier.sampleValueUnchecked`, `BEARD_BOX` lowers terrain density inside each piece's box above the piece's ground level. Whether that always leaves air at every patch origin is Unconfirmed.

## 2. How sculk is attached to the city

### The `sculk` pool

`worldgen/template_pool/ancient_city/sculk.json`:

| Element | Weight |
| --- | --- |
| `feature_pool_element`, feature `minecraft:sculk_patch_ancient_city`, projection `rigid` | 6 |
| `empty_pool_element` | 1 |

Fallback: `minecraft:empty`, which has no elements (`worldgen/template_pool/empty.json`).

### Jigsaws that use it

Only these templates have jigsaws with `pool: minecraft:ancient_city/sculk`. Every one has name and target `minecraft:bottom`, `final_state: minecraft:deepslate`, joint `rollable`, and an orientation whose front is `up`.

| Template | Sculk jigsaws | Local positions | Weight in `ancient_city/structures` |
| --- | --- | --- | --- |
| `structures/barracks.nbt` | 2 | (10, 3, 10), (11, 3, 2) | 4 |
| `structures/chamber_1.nbt` | 2 | (12, 0, 6), (5, 3, 1) | 4 |
| `structures/chamber_2.nbt` | 1 | (8, 1, 5) | 4 |
| `structures/chamber_3.nbt` | 1 | (4, 0, 7) | 4 |
| `structures/ice_box_1.nbt` | 2 | (12, 0, 6), (5, 3, 1) | 1, as the only member of a `list_pool_element` |
| `structures/sauna_1.nbt` | 4 | (5, 0, 26), (8, 0, 12), (18, 0, 33), (21, 0, 9) | 4 |

No other template in the city has a sculk jigsaw: not the centre, the centre walls, the entrance paths, the city walls, the camps, the ruins, the pillars or the statue.

The rest of `worldgen/template_pool/ancient_city/structures.json` is: `empty_pool_element` 7; `small_statue` 4; `tall_ruin_3` and `tall_ruin_4` 2 each; `large_ruin_1`, `tall_ruin_1`, `tall_ruin_2`, `medium_ruin_1`, `medium_ruin_2`, `small_ruin_1`, `small_ruin_2`, `large_pillar_1`, `medium_pillar_1` 1 each; and a list element of `camp_1` to `camp_3`, weight 1. Structures attach to `connect_structure` jigsaws on the city wall templates.

`ListPoolElement.getShuffledJigsawBlocks` returns the first member's jigsaws, so the ice box's two sculk jigsaws are used.

### How a feature element attaches

`FeaturePoolElement` (package `net.minecraft.world.level.levelgen.structure.pools`):

- `getSize` returns `Vec3i.ZERO`, and `getBoundingBox` returns a box from the position to the position plus zero: a single block.
- `getShuffledJigsawBlocks` returns one jigsaw at the element's own position, facing down (`FrontAndTop.fromFrontAndTop(DOWN, SOUTH)`), joint rollable, with a null name.
- `place` calls `this.feature.value().place(level, generator, random, position)`, so the feature's origin is the piece's position.

`JigsawBlock.canAttach` needs the source front to be the opposite of the target front, and treats a null target name as matching any source target. In `JigsawPlacement.Placer.tryPlacingChildren`, the child's position is the source jigsaw's position moved one block along its front, minus the child jigsaw's local position (here zero). **Fact:** the patch origin is therefore the block directly above the template's sculk jigsaw. When the template is placed, `JigsawReplacementProcessor` turns the jigsaw into its `final_state`, deepslate, so the origin sits on a deepslate block unless that block was rotted away (section 5).

### The feature

`worldgen/placed_feature/sculk_patch_ancient_city.json` has an empty placement list. `worldgen/feature/sculk_patch_ancient_city.json` is a `minecraft:sequence` of:

1. `minecraft:sculk_patch` with `charge_count` 10, `amount_per_charge` 32, `spread_attempts` 64, `growth_rounds` 0, `spread_rounds` 1. No placement modifiers.
2. `minecraft:overlay` of two placed features, run in order:
   - `simple_block` of `sculk_catalyst` (`bloom: false`), with `random_chance` 0.5, then a filter that the block below has a sturdy top face.
   - `simple_block` of `sculk_shrieker` with `can_summon: true`, `shrieking: false`, `waterlogged: false`. Placement: `count` uniform 1 to 3; `offset` x, y and z each uniform −2 to 2; then a filter that the target is in the `#minecraft:air` tag **and** the block below has a sturdy top face.

For comparison, `worldgen/feature/sculk_patch_deep_dark.json` is a `sequence` of the same `sculk_patch` config, then the same 50% catalyst. It has **no shrieker step**.

## 3. How the feature code places shriekers

### Sequence and overlay

- `SequenceFeature.place` runs its features in order and returns `false` at the first one that fails, so later ones don't run.
- `OverlayFeature.place` runs every feature and returns true if any placed.
- `SimpleBlockFeature.place` gets the state, checks `canSurvive`, then calls `level.setBlock(origin, state, 2)`. It does not check what was there before. `SculkShriekerBlock` does not override `canSurvive`. The shrieker's air filter is the only check that the spot is empty.
- `FeaturePlacer.place` runs placement modifiers in order. `RepeatingPlacement.modify` (used by `CountPlacement`) emits the origin `count` times. `OffsetPlacement.modify` samples x, y and z from their providers and offsets the position. `PlacementFilter.modify` keeps the position only if `shouldPlace` passes.

So the ancient-city sequence is: run the sculk patch; if it succeeds, maybe place a catalyst at the origin, then make 1 to 3 shrieker attempts. Each attempt picks a spot independently from the 5 × 5 × 5 cube centred on the origin, and each places only if that spot is air with a sturdy floor. Two attempts can pick the same spot, and the second then fails the air filter. A catalyst placed at the origin first blocks a shrieker there. **Fact:** an ancient-city patch makes 0 to 3 overlay shriekers, each within 2 blocks of the origin on every axis, all `can_summon: true`.

### `SculkPatchFeature`

`net.minecraft.world.level.levelgen.feature.SculkPatchFeature` is a record with exactly five fields: `chargeCount`, `amountPerCharge`, `spreadAttempts`, `growthRounds` and `spreadRounds`. It has no shrieker or catalyst field; in 26.3 those are in the sequence JSON above.

`place`:

1. Returns `false` unless `canSpreadFrom(level, origin)` passes. That needs the origin to hold a `SculkBehaviour` block, or to be air or a water source with at least one neighbour whose collision shape is a full block.
2. Makes `SculkSpreader.createWorldGenSpreader()`.
3. For each of `spreadRounds + growthRounds` rounds (1 round for both vanilla configs), it adds `chargeCount` cursors of `amountPerCharge` charge at the origin (10 cursors of 32), calls `updateCursors` `spreadAttempts` times (64) with vein spreading on, then clears the cursors.
4. Returns `true`.

### The world-generation spreader

`SculkSpreader.createWorldGenSpreader()` is `new SculkSpreader(true, BlockTags.SCULK_REPLACEABLE_WORLD_GEN, 50, 1, 5, 10)`. In order: `isWorldGeneration` true, replaceable tag, `growthSpawnCost` 50, `noGrowthRadius` 1, `chargeDecayRate` 5, `additionalDecayRate` 10. The in-world spreader used by catalysts, `createLevelSpreader()`, is `(false, SCULK_REPLACEABLE, 10, 4, 10, 5)`.

`#minecraft:sculk_replaceable_world_gen` (`data/minecraft/tags/block/`) is `#sculk_replaceable` plus deepslate bricks, deepslate tiles, cobbled deepslate, the two cracked variants and polished deepslate. So patches convert city floors.

`SculkSpreader.ChargeCursor`:

- `canMoveToPos`: during world generation a cursor may only move to a position whose horizontal squared distance from the origin is at most 144, so within 12 blocks horizontally.
- If a world-generation cursor has charge left but nowhere to move, its charge is set to 0.
- Cursors only move onto `SculkBehaviour` blocks (sculk, veins, catalysts).

`SculkVeinBlock.attemptUseCharge`: with vein spreading on, the vein converts a replaceable block it is attached to into sculk (`attemptPlaceSculk`) and loses 1 charge. It then spreads veins from the new sculk and removes vein faces that now touch sculk.

`SculkBlock.attemptUseCharge` is where growths happen:

1. Only with chance 1 in `chargeDecayRate` (1 in 5) per update.
2. If the cursor is at least `noGrowthRadius` (1) from the origin and `canPlaceGrowth` passes, it rolls `random.nextInt(growthSpawnCost) < charge`. With cost 50 and charge at most 32, that is at most a 32 in 50 chance. On success it places `getRandomGrowthState` on the block above.
3. Either way it returns `max(0, charge - 50)`, which is 0.

`canPlaceGrowth` needs air or water above the sculk, and at most 2 blocks from `#minecraft:sculk_growth_inhibitors` (sensor and shrieker) in the box from (−4, 0, −4) to (+4, +2, +4).

`getRandomGrowthState`: `random.nextInt(11) == 0` gives a `SCULK_SHRIEKER` with `CAN_SUMMON` set to `isWorldGen`, and otherwise a `SCULK_SENSOR`. `SculkSpreader.SHRIEKER_PLACEMENT_RATE` is 11. Both are waterlogged if the spot holds fluid.

**Inference:** a cursor uses up all its charge on its first growth attempt, so each cursor makes at most one growth and a patch makes at most 10 growths. Each growth is a shrieker with probability 1 in 11. So a patch's growth shriekers average well under 1 and are capped at 10. Every growth shrieker sits on sculk within 12 blocks horizontally of the origin, never at the origin. Its height is limited only by how far the cursor walks in 64 updates.

### Ancient-city patch versus Deep Dark patch

| | `sculk_patch_ancient_city` | `sculk_patch_deep_dark` |
| --- | --- | --- |
| Spreader config | charge 10 × 32, 64 attempts, 1 spread round, 0 growth rounds | Same |
| Growth shriekers | Yes, 1 in 11 growths, `can_summon: true` | Yes, 1 in 11 growths, `can_summon: true` |
| Overlay shriekers | 1 to 3 attempts, ±2 on each axis, `can_summon: true` | None |
| Catalyst | 50% at origin, needs sturdy block below | Same |
| Where it runs | One origin per sculk jigsaw that picked the feature | 256 tries per chunk, any height from the bottom to Y 256, deep_dark biome only |

**Fact:** both features produce only `can_summon: true` shriekers. `SculkShriekerBlock`'s default state and `getStateForPlacement` give `can_summon: false`, so player-placed shriekers are false. `SculkShriekerBlockEntity.canRespond` needs `CAN_SUMMON`, a difficulty other than peaceful and the `SPAWN_WARDENS` game rule. Only then does a shriek raise the warning level and `trySummonWarden` run.

## 4. When it runs, and what is known when

### Structure start: piece positions are fixed

- `Structure.generate` builds a `GenerationContext`. Its random source comes from `GenerationContext.makeRandom`, which is `new WorldgenRandom(new LegacyRandomSource(0))` followed by `setLargeFeatureSeed(seed, chunkX, chunkZ)`. So it depends only on the world seed and the start chunk.
- `JigsawStructure.findGenerationPoint` calls `JigsawPlacement.addPieces`. That picks the centre template and rotation, then expands every jigsaw breadth-first into `PoolElementStructurePiece`s, including the `FeaturePoolElement` pieces for sculk patches.
- The feature-or-empty choice happens here. `tryPlacingChildren` walks `StructureTemplatePool.getShuffledTemplates`, a shuffled list holding each element `weight` times, and stops at the first `EmptyPoolElement`. So each eligible sculk jigsaw gets a patch piece with probability 6 in 7, and whether it did is fixed in the piece list.
- The finished list goes into a `StructureStart`. `StructureStart.createTag` saves it under `Children`, and `PoolElementStructurePiece` saves its element under `pool_element`, along with its position.
- `ChunkStatusTasks.generateStructureStarts` runs this at the structure-starts chunk status, long before any blocks are placed.

**Fact:** every sculk patch origin in a city is known exactly once the structure start exists: the position of each piece whose element is `Feature[...sculk_patch_ancient_city...]`. No shrieker positions are known at that point.

Depth limit: in `tryPlacingChildren`, a piece at the maximum depth (`size`, 7) only tries the fallback pool, which here is `minecraft:empty`. **Inference:** a building that lands at depth 7 gets no sculk patch, because its sculk jigsaws only see the empty fallback.

### Features step: patches are run

- `ChunkStatusTasks.generateFeatures` calls `ChunkGenerator.applyBiomeDecoration` for one chunk.
- For each generation step in order, `applyBiomeDecoration` first places structures whose `step` is that step, then the biome features for that step. The Ancient City's `step` is `underground_decoration`. `GenerationStep.Decoration` order is raw generation, lakes, local modifications, underground structures, surface structures, strongholds, underground ores, **underground decoration**, fluid springs, vegetal decoration, top layer modification.
- Before each structure it calls `random.setFeatureSeed(decorationSeed, index, stepIndex)`. `decorationSeed` comes from `setDecorationSeed(worldSeed, chunkMinX, chunkMinZ)`, and `index` is the structure's position among structures in that step. The same random source then passes through every start of that structure touching the chunk.
- `StructureStart.placeInChunk` calls `postProcess` on every piece whose bounding box intersects the chunk's writable box, in piece-list order. `PoolElementStructurePiece.postProcess` calls `place`, which hands the shared random source to the element.
- A feature piece's box is one block, so **each sculk patch runs exactly once, while decorating the chunk that contains its origin**.
- Template pieces placed earlier in that chunk draw from the same random source: `StructureTemplate.placeInWorld` calls `random.nextLong()` for each container's `LootTableSeed`, and earlier feature pieces draw as well. Template processors do not use it. `BlockRotProcessor` uses `StructurePlaceSettings.getRandom(pos)`, which is `RandomSource.create(Mth.getSeed(pos))` when no random source is set.

The features step's chunk step needs the neighbouring chunks only at an earlier status, and may write one chunk out (`ChunkPyramid` sets `blockStateWriteRadius(1)` with requirements `STRUCTURE_STARTS` at 8 and `TERRAIN` at 1; `WorldGenRegion.isWithinWriteZone`). A patch can reach 12 blocks out, plus 2 more for overlay shriekers, so it often writes into neighbouring chunks.

**Inference on determinism:**

- The random numbers a patch gets depend on the world seed, the chunk, the piece list, and every random draw by earlier Ancient City pieces in that chunk. All of that is fixed by the seed.
- The patch also reads the world: `canSpreadFrom`, sculk spreading, `canPlaceGrowth` and the overlay filters all check blocks. Some of those blocks are in neighbouring chunks, and those chunks may or may not have run their own structure pieces and features yet. After the patch runs, a neighbouring chunk can place its share of template pieces, or its own deep-dark patches, over what this patch wrote.
- So the outcome is seeded but can depend on chunk generation order. Shrieker positions can only be learned after the relevant chunks have generated. Whether vanilla is order-independent in practice is Unconfirmed.

## 5. Processor lists

From `data/minecraft/worldgen/processor_list/`:

| List | Used by | Processors |
| --- | --- | --- |
| `ancient_city_start_degradation` | the three city centres | rule: deepslate bricks and tiles become cracked 30%; soul lantern becomes air 5%. `protected_blocks` `#features_cannot_replace` |
| `ancient_city_generic_degradation` | centre walls, entrance, buildings except the ice box | `block_rot` integrity 0.95 on `#ancient_city_replaceable`, then the same rules and `protected_blocks` |
| `ancient_city_walls_degradation` | city walls | as generic, plus deepslate tile slab becomes air 30% |

The ice box uses an empty processor list.

- None of the lists names a sculk block, and neither `#ancient_city_replaceable` nor `#features_cannot_replace` contains one. So the template sensors are never rotted or changed.
- `#features_cannot_replace` is bedrock, spawner, chest, end portal frame, reinforced deepslate, trial spawner and vault. `protected_blocks` stops a template from overwriting those blocks in the world. It does not limit the sculk feature, which runs as a separate piece.
- `SinglePoolElement.getSettings` adds `JigsawReplacementProcessor` before the pool's own list. **Inference:** the sculk jigsaw first becomes deepslate, then `block_rot` (deepslate is in `#ancient_city_replaceable`) leaves it unplaced 5% of the time in every building except the ice box. Then the block under the patch origin is whatever terrain was there. That changes the catalyst and floor checks, and possibly `canSpreadFrom`.

## 6. Deep Dark biome features inside the city

`data/minecraft/worldgen/biome/deep_dark.json` lists `sculk_vein` and `sculk_patch_deep_dark` in its `underground_decoration` step. `#minecraft:has_structure/ancient_city` is only `minecraft:deep_dark`.

- `placed_feature/sculk_patch_deep_dark.json`: `count` 256, `in_square`, `height_range` uniform from `above_bottom: 0` to `absolute: 256`, `biome`.
- `placed_feature/sculk_vein.json`: `count` uniform 204 to 250, `in_square`, the same height range, `biome`. The feature is `multiface_growth` of `sculk_vein` on stone, andesite, diorite, granite, dripstone block, calcite, tuff and deepslate. It places veins only.

`applyBiomeDecoration` places the step's structures before its features. **Fact:** in each chunk, deep-dark sculk patches run after that chunk's Ancient City pieces, and can start in the city's air spaces next to its floors and walls. Their only shriekers are growth shriekers, and those are `can_summon: true`, the same as the city's own. Nothing in the code or data tells the two sources apart once placed.

**Inference:** most of the 256 tries per chunk fail `canSpreadFrom` in solid rock. The city's carved air volume gives them many valid origins, so deep-dark growth shriekers are likely among the shriekers inside a city. How many is Unconfirmed.

## 7. Other points relevant to picking keeper shriekers at planning time

- **A patch can make no shriekers.**
  - If `canSpreadFrom` fails, `SculkPatchFeature.place` returns false and `SequenceFeature` skips the catalyst and shrieker steps. `canSpreadFrom` needs the origin to be air or a water source with a full-block neighbour. The origin is the block above a template floor, and the templates hold no air, so this depends on terrain carving (section 1).
  - Even when the patch runs, each of the 1 to 3 overlay attempts can miss: the spot may not be air (a vein, a catalyst or a template block) or may have no sturdy floor.
  - The count roll can be 1.
  - Growth shriekers are only 1 in 11 of at most 10 growths.
- **A patch can make more than one shrieker.** The cap is 3 overlay plus up to 10 growth shriekers. `canPlaceGrowth` stops growths near more than 2 sensors or shriekers, which keeps them sparse.
- **Where to look.**
  - Overlay shriekers are within ±2 blocks of a known origin on every axis.
  - Growth shriekers are within 12 blocks horizontally of a known origin.
  - Deep-dark shriekers can be anywhere in the biome.
- **Slots per city.**
  - The six sculk-hosting buildings carry 1 to 4 sculk jigsaws each, 12 in total across the six templates.
  - In the `structures` pool, the weight of those buildings, multiplied by their slot counts, gives 42 slots per 46 weight. The empty element has weight 7 of that 46.
  - **Inference:** that is roughly 0.9 sculk slots per building pick before collisions. Each slot then has a 6 in 7 chance of becoming a patch.
  - How many building picks a city makes is Unconfirmed, so a typical city's patch and shrieker counts are Unconfirmed.
- **The centre is bare.** The centre and its walls have no sculk jigsaws. Every ancient-city patch is attached to a building that hangs off the city walls, at depth 4 to 7 in the jigsaw tree.

## Not checked

- Nothing was generated or tested in game. Typical counts of patches and shriekers per city are unknown.
- Whether the beard-box carving always leaves air at patch origins.
- Whether chunk generation order changes sculk results in practice.
- `MultifaceSpreader` was decompiled but not traced in detail. The vein-spreading notes come from `SculkVeinBlock` and `SculkBehaviour` only.
- `walls/no_corners.json` lists `ancient_city/walls/intact_horizontal_wall_stairs_5`, but no such template exists in the jar. It is not a sculk-hosting template, and I did not check what the game does with the missing file.
