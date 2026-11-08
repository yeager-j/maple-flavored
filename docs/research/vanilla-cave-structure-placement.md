# Placing a structure in large open caves

Research notes for Maple Flavored, written before any ticket. The idea is a tiny structure, a core block and maybe a little decoration, at the heart of large open deepslate caves below Y 0. Within about 32 to 48 blocks of it, placed light fizzles and Deep mob variants spawn regardless of light, until the core is Cleared and broken. Big open caves come from terrain noise and aren't objects the game knows about. These notes check whether a custom structure type can find them when structures are planned, and how to build it in Minecraft 26.3.

## Source and confidence

Code comes from the Mojang-named sources jar that Loom generated, `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-common-1b0498e74c/26.3/minecraft-common-1b0498e74c-26.3-sources.jar`. Paths below are relative to `net/minecraft/` in that jar, and line numbers are from it. Vanilla worldgen JSON comes from `minecraft-common-1b0498e74c-26.3.jar` in the same folder, under `data/minecraft/`. 26.3 ships structures, structure sets and biome tags as JSON there, and also builds them in `data/worldgen/Structures.java` and `StructureSets.java`. I checked the Fabric API 0.165 jars in `~/.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/` by listing their classes. Mod paths start with `src/`.

- **Fact**: stated directly by the cited code. Statements without a label are facts.
- **Inference**: my reading of how the cited code works at run time.
- **Unconfirmed**: needs a test world.

Nothing was run in game, and no code was compiled.

## Summary

1. **Noise caves can be seen at planning time.** `ChunkGenerator.getBaseColumn` samples the Overworld's full final density, and that includes cheese, spaghetti and noodle caves. It also runs the aquifer, so water and lava show up, including the lava below Y −54. Carver caves and canyons don't show up, and nor does any structure's terrain adaptation. Vanilla's Nether Fossil already uses `getBaseColumn` to find an open floor.
2. **A custom `Structure` subclass can decline.** `findGenerationPoint` returns an `Optional`, and empty means no structure in that chunk. The biome check runs afterwards, at the stub's own X, Y and Z, so a stub at Y −30 is checked against the cave biome there. A jigsaw structure can't look at cave shape (`JigsawStructure` is `final` and always places at its `start_height`), so the core needs its own structure type.
3. **Recognising a big cave is cheap enough.** One column whose X and Z are multiples of 4 costs 49 evaluations of the density graph plus the aquifer. Building a chunk's terrain costs 1,225. A scan of up to 13 columns, done once per placement chunk, costs about half of one chunk's terrain density. **Inference.**
4. **Carvers can't delete the core.** Carvers run during the `terrain` stage, and structures are built later, during `features`. Features in later decoration steps, and features from neighbouring chunks, can still write over it.
5. **Use a block entity for runtime lookups, not the structure.** `StructureManager.getStructureAt` is cheap but only covers the start's bounding box, and `/place structure` never records starts at all. The core needs a block entity anyway, to hold its Cleared state. Keeping an index of loaded cores answers "is this position within 40 blocks of a live core" without loading chunks.
6. **Fabric API has no structure helpers.** Registration is two vanilla `Registry.register` calls (structure type and piece type) plus a block, a block entity, and three JSON files.
7. **Measured in three test worlds (section 6):** big caves are common, with about 10 per km² of 50,000 blocks or more. They hold more loot per cave, but most exposed diamond isn't in them. The finder as first proposed almost never fires and would give about 0.4 cores per km². A variant that scans a whole 8 × 8 chunk cell gives about 20 cores per km² and reaches about 70% of the big caves.

## 1. What the planning-time terrain preview contains

### How `getBaseColumn` builds a column

`NoiseBasedChunkGenerator.getBaseColumn` (`world/level/levelgen/NoiseBasedChunkGenerator.java:140`) and `getBaseHeight` (line 135) both call `iterateNoiseColumn` (line 164):

- It builds a 1 × 384 × 1 `DensityVolume` at the column (line 177).
- It makes a fresh `NoiseChunk` with **no Beardifier** and `Blender.empty()` (line 186).
- It samples the router's `finalDensity` for the whole column in one call (lines 189-191).
- Then it walks from the top down. Each block goes through `aquifer.computeSubstance(x, y, z, density)`, and `null` becomes the default block, stone (lines 192-196). `getBaseHeight` stops at the first block that matches the heightmap's test (line 201). `getBaseColumn` keeps them all.

That is the same density and aquifer that `doFill` uses to build real terrain (lines 391-429). The differences are that real terrain has the Beardifier and the Blender, and that surface rules and carvers run after it.

### Caves are in the final density

The Overworld's final density is built in `NoiseRouterData.registerTerrainNoises` (`world/level/levelgen/NoiseRouterData.java:321-327`):

- `caves` picks between the surface with cave entrances and `underground(...)` (line 322).
- `underground` (line 450) is the cheese caves (line 457), cave layers, spaghetti entrances, 2D spaghetti and pillars.
- The final density is `min(postProcess(slide(caves)), noodle) + beardifier` (line 326).

So cheese, spaghetti and noodle caves are all in the sample. **Fact.**

### Structures don't affect the sample

`NoiseChunk` puts the Beardifier into the sampler context under `Beardifier.CONTEXT_KEY` (`world/level/levelgen/NoiseChunk.java:30`). With `null`, the `beardifier` density function falls back to a constant 0 (`world/level/levelgen/densityfunction/generator/SimpleDensityFunction.java:29`, `ContextBoundSampler.java:8`). So the sample ignores every structure's terrain adaptation. **Fact.**

**Inference:** the real terrain can differ from the sample near another structure with terrain adaptation, such as a Trial Chamber (`encapsulate`), an Ancient City (`beard_box`) or a Stronghold (the mod overrides it to `encapsulate`). Their Beardifier can fill in part of the cave after the core was planned.

### Water and lava are in the sample

The aquifer runs in `getBaseColumn` (line 187), and the Overworld has one (`world/level/levelgen/NoiseGeneratorSettings.java:148`).

- Open space below Y −54 is always lava. The global fluid picker returns lava below `min(-54, seaLevel)` (`NoiseBasedChunkGenerator.java:79, 87`), and `computeSubstance` returns lava there at once (`world/level/levelgen/Aquifer.java:218`).
- Above that, the aquifer can flood a cave with water, or with lava if its fluid level is at or below Y −10 and the lava noise is strong (`Aquifer.java:550-562`).
- The aquifer's exclusion is the Deep Dark region (`NoiseRouterData.java:503`). Inside it, floodedness is forced off (`Aquifer.java:510-512`), so Deep Dark caves are always dry.

So the sample distinguishes air, water, lava and stone. It never shows deepslate, because deepslate comes from the surface rules, which `getBaseColumn` doesn't run.

### Carvers are not in the sample

Carvers (cave and canyon carvers) run inside `buildTerrain`, after `doFill` and `buildSurface` (`NoiseBasedChunkGenerator.java:369, 379, 383`). `getBaseColumn` never calls them. **Inference:** carver caves only add air, so missing them only causes missed caves, never false positives. They can widen a cave the sample saw, and `applyCarvingMask` fills new space using the aquifer, which can add water or lava (line 308).

### Heights don't help underground

`getBaseHeight`, `getFirstFreeHeight` and `getFirstOccupiedHeight` all scan from the top and return the first block that matches the heightmap (`world/level/chunk/ChunkGenerator.java:662-674`). `WORLD_SURFACE_WG` matches anything that isn't air, and `OCEAN_FLOOR_WG` matches blocks that block motion (`world/level/levelgen/Heightmap.java:146-148`). Both find the surface. There is no underground heightmap, so cave floors must be found by scanning `getBaseColumn` yourself.

### Vanilla precedent

- **Nether Fossil** picks a random X and Z, takes `getBaseColumn`, and scans down for air over a sturdy block (`world/level/levelgen/structure/structures/NetherFossilStructure.java:37-48`). It returns empty if it finds none. This is the closest vanilla pattern to finding a cave floor.
- **Ruined Portal** reads `getBaseColumn` at the four corners of its box to settle it onto solid ground (`RuinedPortalStructure.java:197-215`).
- The mod's `TerrainPreview` (`src/main/java/mapleflavored/structurebooks/TerrainPreview.java`) already caches `getBaseColumn` per column for structure books. Its comment says the preview doesn't know "caves". By this reading it does know noise caves. Only carver caves and lakes come later.

## 2. The placement hook

### How a structure start is made

At the `structure_starts` stage, `ChunkGenerator.createStructures` (`ChunkGenerator.java:483`) goes through every structure set that could have a biome in this world:

1. If the chunk already has a valid start for any structure in the set, the set is skipped (lines 501-506).
2. Otherwise it asks `placement.isStructureChunk(state, x, z)` (line 508).
3. If yes, it calls `tryGenerateStructure`, which calls `Structure.generate` with a biome test built from the structure's `biomes` (lines 584-602).
4. `generate` makes a `GenerationContext` and calls `findValidGenerationPoint` (`world/level/levelgen/structure/Structure.java:100-103`). If that returns a stub, it builds the pieces. The start is valid only if it has at least one piece (`StructureStart.java:118`).
5. A valid start is saved to the chunk (`ChunkGenerator.java:603-605`).

### What a custom structure overrides

`Structure.findGenerationPoint(GenerationContext)` is abstract and returns `Optional<GenerationStub>` (`Structure.java:195`). Returning `Optional.empty()` declines, and the chunk gets no start. `findValidGenerationPoint` then filters the stub by biome (line 198).

`GenerationContext` (`Structure.java:203-216`) holds the chunk generator, biome source, climate sampler, a `biomeResolver`, `randomState`, the template manager, a `random` seeded from the world seed and chunk (lines 245-249), the seed, `chunkPos`, `heightAccessor` and the biome test. It doesn't hold any other structure's starts.

A `GenerationStub` is a position plus either a piece-building callback or a ready `StructurePiecesBuilder` (lines 282-294). Pieces can sit anywhere near the start chunk, not just inside it.

### The biome check uses the stub's real Y

`isValidBiome` looks up the biome at the stub's X, Y and Z, each converted to quarter coordinates (`Structure.java:251-255`). So for a stub at the core's position, the check sees the cave biome at that depth: `dripstone_caves`, `lush_caves`, `sulfur_caves`, `deep_dark`, or the surface biome continued down. **Fact.** That makes "no core in the Deep Dark" a biome-tag job. Tags can't subtract, so the core's tag must list every Overworld biome except `deep_dark`. Vanilla's `#minecraft:is_overworld` includes `deep_dark`.

### Placement: random spread

In 26.3, `RandomSpreadStructurePlacement` extends a new `AbstractSpreadingStructurePlacement`. The JSON fields are:

| Field | Required | Meaning | Source |
| --- | --- | --- | --- |
| `spacing` | yes | Grid cell size in chunks, 0-4096 | `RandomSpreadStructurePlacement.java:19` |
| `separation` | yes | Minimum gap; must be less than `spacing` | lines 20, 32 |
| `spread_type` | no, `linear` | `linear` or `triangular` offset in the cell | line 21 |
| `salt` | yes | Seeds the offsets | `AbstractSpreadingStructurePlacement.java:38` |
| `frequency` | no, 1.0 | Chance a placement chunk is kept | line 37 |
| `frequency_reduction_method` | no, `default` | Which random roll `frequency` uses | lines 34-36, 148-152 |
| `exclusion_zone` | no | `{ "other_set", "chunk_count" }`, count 1-16 | lines 39, 127-141 |
| `locate_offset` | no, 0 0 0 | Offset of the `/locate` result from the chunk corner | line 33 |

Each `spacing` × `spacing` cell of chunks gets one placement chunk, offset by up to `spacing - separation` (lines 67-76). `isStructureChunk` checks the placement chunk, then `frequency`, then the exclusion zone (`AbstractSpreadingStructurePlacement.java:81-86`).

### The exclusion zone is too blunt for Ancient Cities

`ExclusionZone` is marked `@Deprecated` (line 126). It forbids a chunk if the other set's placement test passes for any chunk within `chunk_count` (`ChunkGeneratorStructureState.java:202-214`). That test only looks at the placement grid. It doesn't ask whether the other structure's biome is there. Ancient Cities use spacing 24 and separation 8 (`data/minecraft/worldgen/structure_set/ancient_cities.json`), and only generate in `deep_dark`. **Inference:** a zone large enough to clear a city's 116-block reach (`ancient_city.json`, `max_distance_from_center`) would forbid most of the world, since a 33-chunk square around every 24-chunk grid point covers almost everything.

A better guard, done inside `findGenerationPoint`:

1. Look up the `ancient_cities` set through `context.registryAccess()` (key `BuiltinStructureSets.ANCIENT_CITIES`, `world/level/levelgen/structure/BuiltinStructureSets.java:24`).
2. For the 3 × 3 grid cells around the candidate, get each potential city chunk with the public `RandomSpreadStructurePlacement.getPotentialStructureChunk(seed, x, z)`.
3. For each one within about 11 chunks, check `context.biomeResolver()` at that chunk's centre and Y −27, the city's fixed start height (`ancient_city.json`, `start_height`). If it's `deep_dark`, decline.

That costs up to 9 placement rolls and 9 biome lookups. **Inference:** it matches the city's own biome test closely, though not exactly. The city checks the biome at its centre piece, which sits near the chunk corner, not the chunk centre.

Add a cheap biome ring as well. Check the biome at the core's Y at four points 40 blocks out, and decline if any is `deep_dark`. `BiomeResolver.getBiomesWithin`, which the Ocean Monument uses (`OceanMonumentStructure.java:34`), samples a full 3D box of quarter positions. At a 40-block radius that's 21³, over 9,000 biome lookups, so avoid it here.

The same potential-site check works for Trial Chambers (spacing 34, separation 12, `StructureSets.java:161-164`), if cores should also stay away from them.

## 3. Recognising a large open cave cheaply

### What one column costs

Inside `finalDensity`, the cave and terrain part is wrapped in `interpolated` with cells 4 blocks wide and 8 tall (`NoiseRouterData.java:471-474`), and noodle caves the same way. When the volume isn't aligned to cells, `InterpolatedFunction` evaluates its input at the cell corners and interpolates between them (`world/level/levelgen/densityfunction/op/InterpolatedFunction.java:96-146`).

- A column whose X and Z are both multiples of 4 needs 1 × 49 × 1 corners. 384 blocks is 48 cells, plus one more corner.
- Any other column needs 2 × 49 × 2 = 196 corners.
- A chunk's own terrain fill needs 5 × 49 × 5 = 1,225 corners and 98,304 aquifer calls.

Each corner runs the whole graph: terrain splines, 3D base noise, cheese, cave layers, spaghetti and pillars. Caches only live inside one `NoiseChunk` (`NoiseChunk.java:38-40`), and every `getBaseColumn` call makes a new one, so columns share nothing.

The aquifer adds a few dozen 2D surface-level samples when it's set up (`Aquifer.java:144-163`) and 384 `computeSubstance` calls. Solid blocks return at once (lines 207-210). Open blocks underground do a nearest-cell search over 12 aquifer cells, each cached for the call.

**Inference:** a 4-aligned column costs about 4% of one chunk's terrain density work and well under 1% of its aquifer work. Always sample at multiples of 4.

### Optional: a coarse volume screen

When a `DensityVolume` has the same step as the cells and lines up with them, `InterpolatedFunction` samples its input directly, without interpolating (`InterpolatedFunction.java:55-61`). Add and min operators pass the volume down unchanged (`op/BinaryFunction.java:151-160`). **Inference:** the core could cast the generator to `NoiseBasedChunkGenerator`, take `generatorSettings().value().noiseRouter().finalDensity()`, and sample a stepped volume through `randomState.samplersWithContext(...)`. That gives a cell-resolution density map of a whole region, for example 64 × 56 × 64 blocks in 2,312 evaluations. It skips the aquifer, so it can't tell air from water or lava, and it only works with the noise generator. Use it only if profiling shows the column scan is too slow.

### How vanilla structures read terrain

| Structure | What it checks | Source |
| --- | --- | --- |
| Ancient City | Nothing. Fixed Y −27 and a `deep_dark` biome tag. | `data/worldgen/Structures.java:307-327` |
| Trial Chambers | Nothing. Random Y −40 to −20, `encapsulate` carves its own space. | `Structures.java:344-363` |
| Mineshaft | Nothing for normal shafts. The mesa type reads the surface height. | `MineshaftStructure.java:36-60` |
| Stronghold | Nothing. Ring placement, moved below sea level. | `StrongholdStructure.java` |
| Buried Treasure | Chunk centre on `OCEAN_FLOOR_WG`. | `BuriedTreasureStructure.java:19-21` |
| Ocean Monument | Every biome within 29 blocks at sea level must be ocean. | `OceanMonumentStructure.java:30-41` |
| Woodland Mansion | Lowest of four corner heights, at least Y 60. | `WoodlandMansionStructure.java:30-43`, `Structure.java:150-193` |
| Desert Pyramid and others | `getMeanFirstOccupiedHeight` or `getLowestY` over corners. | `Structure.java:162-177` |
| Jigsaw with `project_start_to_heightmap` | Surface height at the start piece's centre. | `pools/JigsawPlacement.java:110-118` |
| Nether Fossil | Scans `getBaseColumn` down for air over a sturdy block. | `NetherFossilStructure.java:37-48` |

No vanilla structure looks for an Overworld cave. The Nether Fossil is the only one that reads the shape of open space.

### Proposed sampling algorithm

All numbers are starting guesses.

1. **Candidates.** Take 9 columns in a 3 × 3 grid, 16 blocks apart, centred on the start chunk's centre, snapped to multiples of 4. Shuffle them with `context.random()` so the result stays the same for each seed and chunk.
2. **Screen each candidate.** Read its `getBaseColumn`. Find runs of air between Y −52 and Y −4. Water and lava break a run. The block under the run must be solid, not fluid. Keep the candidate with the longest run, if that run is at least 10 blocks tall.
3. **Confirm the winner.** Read four more columns at ±12 blocks on X and Z. Each must have air at the centre's floor + 2 to floor + 6, and its own floor within 6 blocks of the centre's. That rules out a tall narrow shaft. Add the four diagonals for a stricter test.
4. **Guard.** Run the Ancient City potential-site check and the Deep Dark biome ring from section 2.
5. **Return** a stub at the floor + 1 of the centre column. `findValidGenerationPoint` then checks the core's own biome.

Worst case is 13 aligned columns, about 640 corner evaluations. That's about half of one chunk's terrain density, spent once per placement chunk. **Inference.**

**Unconfirmed:** how often this succeeds, and whether 10 tall and ±12 wide matches what players call a big cave. To settle it, add a debug command that runs the finder over a few hundred placement chunks in a test world and logs hit rate and sizes, then fly to a sample of hits.

### Decoration step and terrain adaptation

- **Terrain adaptation: `none`.** That's the default (`Structure.java:299-301, 309`). The Beardifier skips structures with `none` (`world/level/levelgen/Beardifier.java:47`), so the core never fills its own cave. Any other value would add density around the core and close the cave around it.
- **Step: late, such as `top_layer_modification`.** In each chunk, each decoration step places that step's structures first and then that step's features (`ChunkGenerator.java:364-419`). A late step means the core is placed after almost all of its own chunk's features, and overwrites any moss or dripstone at its spot. The default step is `surface_structures` (`Structure.java:300`). The Ancient City uses `underground_decoration` (`Structures.java:314`).
- **Carvers don't matter.** Carvers run during `terrain`, before any structure is built at `features` (`world/level/chunk/status/ChunkPyramid.java:16-27`, `NoiseBasedChunkGenerator.java:383`). The carver filter hook in `vanilla-stronghold-generation.md` stops carvers digging where they shouldn't. It doesn't apply here, because the core is placed after carving anyway.
- **Neighbouring features can still write.** A feature can write into the 3 × 3 chunks around its own (`ChunkPyramid.java:27`, `blockStateWriteRadius(1)`), and neighbours decorate in any order. **Inference:** ores only replace stone tags, and lichen, dripstone and vines only grow into air, so a solid core block is rarely hit. Lichen and dripstone may attach to it.
- **Check again when building.** At `features` the real blocks exist, carvers included. The core piece's `postProcess` should check that its spot is air over a solid floor. If not, it should search a few blocks up and down that column before falling back to the planned spot. This covers a cave a neighbouring structure's Beardifier filled in, or a floor a carver moved.

## 4. Asking at runtime whether a position is near a core

### What the structure manager offers

| Method | What it does | Chunks it touches | Source |
| --- | --- | --- | --- |
| `getStructureAt(pos, structure)` | Start whose **bounding box** holds `pos` | `pos` chunk at `structure_references`, each referenced start chunk at `structure_starts` | `world/level/StructureManager.java:97-108` |
| `getStructureWithPieceAt(pos, ...)` | Start with a **piece** box holding `pos` | Same | lines 131-142 |
| `startsForStructure(x, z, structure)` | Every start referenced from one chunk | Same | lines 64-69 |
| `fillStartsForStructure` | Resolves references to starts | Loads each start chunk at `structure_starts` | lines 71-79 |
| `getAllStructuresAt(pos)` | The chunk's reference map | `pos` chunk only | lines 180-184 |
| `ServerLevel.findNearestMapStructure` | `/locate` search | May plan and load chunks | `ChunkGenerator.java:160-235` |

`level.getChunk(x, z, status)` loads or generates if needed (`world/level/LevelReader.java:131-133`). **Inference:** on the server thread this is a synchronous load. For a position the player is standing in, the chunk is already loaded. A referenced start chunk is at most 8 chunks away and was generated to `structure_starts` long ago, so the worst case is a disk read, never new generation.

### How far references reach

`createReferences` scans starts in chunks within 8 chunks (`ChunkGenerator.java:616-646`, `range = 8`). It adds a reference only to chunks that the start's **bounding box** overlaps (line 631). The `structure_references` stage needs `structure_starts` 8 chunks out (`ChunkPyramid.java:14`).

So a 1-block core is only referenced from its own chunk. A player 40 blocks away in another chunk can't find it through `getStructureAt`. There are two ways round this:

- **Make the box bigger.** Add an invisible zone piece whose bounding box spans the radius, for example 81 × 49 × 81 blocks. Its `postProcess` places nothing. Every chunk the zone overlaps then gets a reference, and `getStructureAt` works from anywhere in the zone, followed by your own distance check. A radius up to 128 blocks stays within the 8-chunk reference range.
- **Look in nearby chunks yourself.** Call `startsForStructure` on every chunk within 3 of the position, 49 chunks, and measure distance to each core. That's fine for a rare event such as placing a torch, and too slow for every spawn attempt.

### `/place structure` and GameTests

`/place structure` calls `Structure.generate` with a biome test that always passes, then builds each chunk. It never saves the start to a chunk (`server/commands/PlaceCommand.java:262-303`). **Fact.** So a core placed that way is invisible to `getStructureAt`. It also runs `findGenerationPoint` against the generator's noise, not the world on screen, so in a superflat or void world the cave check always declines. The mod's GameTest helper `Structures.plan` (`src/gametest/java/mapleflavored/gametest/Structures.java:31-52`) copies `/place structure` and would fail the same way.

### Mob spawning

`NaturalSpawner` asks `ChunkGenerator.getMobsAt` for the mob list (`world/level/NaturalSpawner.java:307-312`). That reads the chunk's references and returns a structure's `spawn_overrides` list if `pos` is inside its full box or a piece box (`ChunkGenerator.java:456-481`). Overrides only change **which** mobs can spawn. The light check is separate, in each type's spawn rule, such as `Monster.checkMonsterSpawnRules` (`world/entity/monster/Monster.java:102`). So "regardless of light" needs a change to the spawn rules either way. The mod already wraps `SpawnPlacements.checkSpawnRules` (`src/main/java/mapleflavored/mixin/SpawnPlacementsMixin.java`). With a `full` override, the area is a box, not a sphere.

### A block entity index

The core needs state: whether it's Cleared, and maybe who has been near it. The mod keeps a spawner's Clearing on its block entity (`src/main/java/mapleflavored/spawners/SpawnerClearing.java`), and keeps level-wide indexes in `SavedData`, such as `RestoreSpots` (`src/main/java/mapleflavored/bloodmoons/RestoreSpots.java:26`).

So the core block entity can add itself to an in-memory index of loaded cores, keyed by chunk, when it loads, and remove itself when it unloads or breaks. Fabric's `ServerBlockEntityEvents.BLOCK_ENTITY_LOAD` and `BLOCK_ENTITY_UNLOAD` exist in `fabric-lifecycle-events-v1` 4.1.9. A query checks the cores in the chunks within 3 of the position, with no chunk loads. This works the same for world generation, `/place structure`, GameTests and `/setblock`, and a broken core just disappears from the index.

**Inference:** only loaded cores are known. Lights, spawns and players only matter in loaded chunks, and a core that matters is within 48 blocks of them. It can miss a core in an unloaded chunk at the very edge of the simulation distance. **Unconfirmed:** exactly when `BLOCK_ENTITY_LOAD` fires for chunks promoted from world generation.

## 5. Fabric registration

### Code

- **Structure type.** `StructureType` has one method, `codec()` (`world/level/levelgen/structure/StructureType.java:41`, registered the same way vanilla does at lines 43-45), so registration is `Registry.register(BuiltInRegistries.STRUCTURE_TYPE, MapleFlavored.id("cave_core"), () -> CaveCoreStructure.CODEC)`. Build the codec from `Structure.settingsCodec(i)` plus the core's own fields, as `JigsawStructure` does (`structures/JigsawStructure.java:36-52`).
- **Piece type.** A code-built piece needs a `StructurePieceType` so it saves and loads with the chunk: `Registry.register(BuiltInRegistries.STRUCTURE_PIECE, id("cave_core"), (context, tag) -> new CaveCorePiece(tag))`. This is the same as `StructureBooks.PIECE` (`src/main/java/mapleflavored/structurebooks/StructureBooks.java:90-94`). The piece extends `StructurePiece` with the protected constructor `(type, genDepth, boundingBox)` (`StructurePiece.java:60`). It implements `addAdditionalSaveData` and `postProcess` (lines 95, 100-108), and places blocks with `placeBlock`, which clips to the chunk (line 168).
- **Block and block entity** for the core, registered like `StructureBooks.BLOCK` and `BLOCK_ENTITY`.
- Register all of these from mod init, before any world loads. Structures and structure sets are datapack registries (`resources/RegistryDataLoader.java:105-106`), decoded when a world loads, so the type must already exist then. The structure registry isn't one of the registries synced to clients. The mod is required on both sides anyway.

Code-built is simpler than a template for "a block and a little decoration". A template needs a `TemplateStructurePiece` subclass with its own template-loading piece type (`StructurePieceType.StructureTemplateType`, `pieces/StructurePieceType.java:107-114`). Alternatively the custom type can call `JigsawPlacement.addPieces(context, pool, ..., maxDepth 0, ...)` (`pools/JigsawPlacement.java:51`) and reuse vanilla's jigsaw piece type with a `template_pool` and an NBT file. That choice can wait for the decoration design.

### Data

`data/maple_flavored/worldgen/structure/cave_core.json`. The first four fields are `Structure.StructureSettings`, written at the top level (`Structure.java:302-312`). `biomes`, `spawn_overrides` and `step` are required, and `spawn_overrides` can be `{}`. The rest are proposed fields for the custom codec.

```json
{
  "type": "maple_flavored:cave_core",
  "biomes": "#maple_flavored:has_structure/cave_core",
  "spawn_overrides": {},
  "step": "top_layer_modification",
  "terrain_adaptation": "none",
  "min_floor_y": -52,
  "max_floor_y": -4,
  "min_open_height": 10,
  "probe_distance": 12
}
```

`data/maple_flavored/worldgen/structure_set/cave_cores.json`. Field names come from `StructureSet.java:15-16, 37-38`. Pick a salt no vanilla set uses.

```json
{
  "placement": {
    "type": "minecraft:random_spread",
    "spacing": 8,
    "separation": 3,
    "salt": 1702406281,
    "locate_offset": [8, 0, 8]
  },
  "structures": [
    { "structure": "maple_flavored:cave_core", "weight": 1 }
  ]
}
```

`data/maple_flavored/tags/worldgen/biome/has_structure/cave_core.json`: every entry of `minecraft:is_overworld` except `minecraft:deep_dark`, including `dripstone_caves`, `lush_caves` and 26.3's `sulfur_caves`.

### Gotchas

- **New chunks only.** Starts are made at `structure_starts`. Existing chunks never get cores. The design is for new worlds only, so this is fine.
- **Tuning.** The zone radius, the light fizzle and the spawn rules run on the server thread, so they can be `MapleGameRules` playtest rules. The placement spacing and the cave thresholds are read during world generation, on worldgen threads, with no access to the level's game rules. They belong in the JSON above, which a datapack can change without a rebuild, but only for chunks generated afterwards.
- **`/locate structure maple_flavored:cave_core` works.** For chunks not generated yet, `StructureCheck.canCreateStructure` runs `findValidGenerationPoint` itself (`world/level/levelgen/structure/StructureCheck.java:118-133`), so each candidate costs one scan. A hit then loads that chunk to `structure_starts` (`ChunkGenerator.java:318`). The result is the start chunk's corner plus `locate_offset` at Y 0 (`placement/StructurePlacement.java:20-22`), not the core itself.
- **GameTests.** Keep the cave finder a pure function over columns, so a test can feed it fake columns. Give the structure a way to build its pieces at a given position without the check, so tests can place a core in the flat test world.
- **Thread safety.** `findGenerationPoint` runs on worldgen threads, and also from `/locate` and `/place`. It must use only `context.random()` and keep no shared mutable state. `getBaseColumn` makes its own `NoiseChunk` each call, and vanilla already calls it from Nether Fossil planning.
- **Structure books.** `StructureMixin` adds books by rule (`src/main/java/mapleflavored/mixin/StructureMixin.java`). A new structure gets none unless a rule names it.
- **Fabric API 0.165 has nothing for structures.** No class names "structure" in `fabric-biome-api-v1`, `fabric-data-generation-api-v1` or `fabric-registry-sync-v0`. `fabric-gametest-api-v1` only has its empty test template.

## 6. Measurements in three test worlds

Added after the sections above, to choose how often cores appear. Jackson's constraint: if cores are rare, caving goes back to being easy, but they shouldn't blanket every small cave either.

### How it was measured

- A throwaway Fabric mod ran on a dedicated dev server with Maple Flavored loaded. It used the default Overworld with seeds 1001, 2026 and 77731. The code lived in a scratch folder, never in the repo.
- For each seed it generated 64 × 64 chunks to full status. That's 1,024 × 1,024 blocks around 0, 0, and 3.15 km² across all three. It dumped every block from Y −64 to −1.
- From the same worlds it also dumped `getBaseColumn` at every X and Z that's a multiple of 4, so the finder ran on exactly what it would see during planning.
- **Measured** means counted directly in those three worlds.
- **Simulated** means measured finder hits replayed through a placement grid 40 times per seed, with random grid offsets. The offsets come from my own random numbers, not vanilla's seeded placement. **Inference:** the two have the same distribution.
- **Estimated** means arithmetic on the measured numbers.

Definitions:

- **Open** means air, or a block that isn't solid, such as glow lichen, vines or rails. Water and lava are not open.
- **Exposed ore** is ore with a face against an open block.
- **A cave can't be found by flood fill.** Connected open space runs together. The largest connected region holds 72% of all open space below Y 0 across the three seeds, and 88% in seed 1001.
- **A room is used instead:** open space a 7 × 7 × 7 clear cube can reach, which is a morphological opening. Rooms split wherever a passage narrows below 7 blocks. Even so, the largest rooms are linked cheese networks, up to 960,000 blocks.

### Cave sizes (Measured)

Below Y 0, 8.9% of the volume is open, 0.29% is water and 0.25% is lava.

| Room volume at least | Rooms per km² | Per seed (1001 / 2026 / 77731) | Share of open space | Median footprint (columns) | Median height | 90th percentile footprint |
| --- | --- | --- | --- | --- | --- | --- |
| 1,000 | 161.8 | | 51% | 283 | 16 | 1,833 |
| 5,000 | 55.9 | | 47% | 1,030 | 27 | 5,528 |
| 10,000 | 33.4 | 40.1 / 28.6 / 31.5 | 44% | 1,755 | 35 | 9,027 |
| 20,000 | 21.0 | | 41% | 3,051 | 42 | 11,950 |
| 50,000 | 9.9 | 13.4 / 5.7 / 10.5 | 36% | 6,469 | 48 | 19,826 |
| 100,000 | 6.4 | 8.6 / 3.8 / 6.7 | 31% | 9,331 | 52 | 33,068 |

- A room of 50,000 blocks or more is typically about 80 × 80 blocks across and 48 tall. These are the caves Jackson describes. Below they're called **big caves**.
- 32% of big caves touch the edge of the survey area, so their sizes are cut short. Treat the counts as approximate.
- Of the open blocks in the real world, 74% are also open in the `getBaseColumn` sample. The other 26% come from carvers and structures, which the finder can't see.
- 2.3% of the open blocks in the sample are filled in the real world. That's the Beardifier, features and fluids.

### Where the exposed loot is (Measured)

- About 1,060 exposed diamond ore blocks per km², 1.16% of all diamond ore below Y 0. The rest is buried. Vanilla throws away much of the diamond ore that would face air.
- They're spread evenly from Y −56 to −9, at 10 to 17% per 8-block band. There are none below Y −57, because open space there is lava.

| Rooms at least | Exposed diamond touching them | Exposed diamond within 16 blocks of them | Exposed iron touching | Exposed gold touching |
| --- | --- | --- | --- | --- |
| 1,000 | 15.0% | 54.5% | 14.1% | 14.4% |
| 10,000 | 12.5% | 39.8% | 11.2% | 12.2% |
| 50,000 | 9.6% | 25.5% | 7.9% | 9.3% |
| 100,000 | 7.9% | 20.5% | 7.1% | 7.8% |

"Within 16 blocks" uses a box and ignores walls, so it is generous.

Exposed diamonds touching each room, on average:

| Room size (blocks) | Exposed diamonds per room |
| --- | --- |
| 100,000 or more | 13.2 |
| 50,000 to 100,000 | 5.1 |
| 10,000 to 50,000 | 1.2 to 1.5 |
| under 5,000 | 0.5 or fewer |

So:

- **Big caves are loot-rich per cave.** A cave of 100,000 blocks or more shows a player about 13 diamond faces at once, which matches Jackson's experience.
- **But most exposed diamond isn't in big caves.** About 85% faces tunnels and small pockets. Even with a generous 16-block reach, rooms of 10,000 blocks or more only account for 40%.
- **Inference:** cores can make big caves harder. They can't make deep diamonds in general harder. Deep variants below Y 0, or ore changes, would have to do that.

### Finder accuracy (Measured, every chunk treated as a placement attempt)

- **Hit rate** is the share of chunks where the finder returns a spot. The Deep Dark guard is applied.
- **Precision** is the share of hits inside a room of the given size.
- **Reach** is the share of those rooms that would get at least one hit if every chunk were tried.
- **Strict** is the finder from section 3: probes need air at floor + 2 to floor + 6 and a floor within 6 blocks, and all 4 probes must pass.
- **Relaxed** means probes need air at floor + 2 to floor + 4 and a floor within 10 blocks, and 3 of the 4 probes must pass.

| Finder | Hit rate | Precision, 10,000+ | Precision, 50,000+ | Reach, 10,000+ | Reach, 50,000+ |
| --- | --- | --- | --- | --- | --- |
| H8 P8 strict | 2.7% | 84% | 67% | 28% | 48% |
| H14 P8 strict | 2.2% | 90% | 78% | 23% | 48% |
| H8 to H14, P12 strict | 0.7 to 0.8% | 90 to 91% | 81 to 89% | 9 to 11% | 26% |
| H8 to H14, P16 strict | 0.4% | 100% | 100% | 8% | 26% |
| H10 P8 relaxed | 10.8% | 77% | 53% | 68% | 90% |
| H10 P12 relaxed | 5.1% | 83% | 67% | 48% | 84% |
| H14 P12 relaxed | 4.5% | 86% | 73% | 41% | 81% |
| H14 P16 relaxed | 2.3% | 88% | 76% | 26% | 55% |

H is the minimum open height and P is the probe distance.

- **The probe rule matters most.** The minimum open height (8, 10 or 14) barely changes the results.
- **Strict probes rarely pass.** **Inference:** cheese cave floors slope, so a floor 12 blocks away is rarely within 6 blocks of the same height, with 5 blocks of air above it.
- **False positives are rare.** Only 3 to 8% of hits are in no room at all.
- **Big caves collect many hits.** A room that gets hit gets 8 to 13 hits when every chunk is tried. That's why tight spacing puts two cores in one cave.

### Placement (Simulated)

**The placement from section 3 is far too rare.** The strict H10 P12 finder with `random_spread` spacing 8 and separation 3 gives 0.41 cores per km². Only 3.5% of big caves get one.

Random spread with the relaxed finder only improves by trying more often, and then small caves get cores too:

| Finder, spacing / separation | Cores per km² | Big caves with a core | Big caves with 2 or more |
| --- | --- | --- | --- |
| H14 P12 relaxed, 8 / 3 | 2.7 | 17% | 3% |
| H14 P12 relaxed, 4 / 2 | 10.9 | 43% | 17% |
| H10 P8 relaxed, 4 / 2 | 26.3 | 58% | 30% |

**A better scheme is the cell scan.** Each placement attempt covers its whole S × S chunk cell of the spacing grid. It checks one candidate column at the centre of every chunk in the cell, S² columns in all. It keeps the candidate that passes with the tallest open run. Each cell then gets at most one core, and gets one only if it holds a suitable cave.

| Cell scan setting | Cores per km² | Cells with a core | Cores in rooms of 10,000+ | Cores in big caves | Big caves with a core | Big caves with 2 or more | Big cave volume inside a zone | All exposed diamond inside a zone |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| H14 P12 relaxed, S 8 | 20.5 | 42% | 81% | 60% | 70% | 29% | 35% | 10.8% |
| H14 P12 relaxed, S 6 | 26.5 | 29% | 83% | 62% | 77% | 39% | 43% | 13.3% |
| H10 P12 relaxed, S 8 | 24.8 | 51% | 79% | 54% | 71% | 34% | 36% | 12.6% |
| H20 P12 relaxed, S 8 | 14.1 | 29% | 86% | 67% | 54% | 23% | 29% | 7.5% |
| H14 P16 relaxed, S 8 | 13.3 | 27% | 84% | 61% | 51% | 18% | 27% | 7.4% |

- A zone is a 40-block sphere around a core.
- The results vary by seed. For H14 P12 relaxed with S 8, cores per km² were 21.7, 16.6 and 23.2. Big caves with a core were 78%, 53% and 68%.
- **Inference:** most big caves with 2 or more cores are the huge networks of 100,000+ blocks. Their 90th percentile footprint is 33,000 columns, far wider than one 40-block zone. This wasn't broken out separately.

### Cost

- **Measured:** `getBaseColumn` at a column whose X and Z are multiples of 4 took 0.27 to 0.30 ms per column, with 8 threads sampling in parallel.
- **Measured:** generating each seed's 4,096 chunks took 16 to 21 seconds of wall time on this 16-thread machine.
- **Estimated:** a cell scan at S 8 reads 64 columns plus a few probes, about 18 ms per attempt. With one attempt per 64 chunks, that's about 0.3 ms per chunk on a worldgen thread, well under 1% of generation time.
- **Estimated:** `/locate` pays the 18 ms for every cell it checks.

### What the numbers recommend

- **Use the cell scan,** not section 3's per-chunk 3 × 3 finder. Start with `random_spread` spacing 8. Then:
  1. Scan the 8 × 8 chunk cell that holds the placement chunk, which is `floorDiv` of its chunk X and Z by 8.
  2. Use the relaxed probes: air at floor + 2 to floor + 4 in at least 3 of 4 probes at ±12 blocks, with a floor within 10 blocks.
  3. Require a minimum open height of 14.
  4. Keep the Deep Dark guard.

  The chosen spot is at most 7 chunks from the start chunk, which is inside the 8-chunk structure reference range.
- **What to expect:**
  - about 20 cores per km², or one per 220 × 220 blocks on average;
  - 80% of cores in rooms of 10,000 blocks or more, and 60% in big caves;
  - about 70% of big caves get a core, about 30% get two or more, and about 30% get none.
- **Turning it down:** H20 gives 14 cores per km² and reaches 54% of big caves, with 86% of cores in rooms of 10,000+.
- **Turning it up:** S 6 gives 26.5 cores per km² and reaches 77%.
- **These are world-generation values, not game rules.** They belong in the structure and structure set JSON, which a datapack can change for newly generated chunks.
- **The limit:** even at these settings, zones cover only about 11% of all exposed diamond. Cores answer "big caves are too easy". They don't answer "deep diamonds are too easy".

### Limits of this measurement

- Three seeds, 1 km² each, all around spawn. Big-cave counts differ by more than 2× between seeds.
- Rooms are a stand-in for what a player calls a cave. The biggest ones are linked networks rather than single caverns.
- Exposed means facing open space only. Ore facing water or lava isn't counted.
- The finder ran on the noise sample, so it is blind to the 26% of open space carvers make, as section 1 predicted.
- The simulated Deep Dark guard used the biome ring only, not the Ancient City site check. The Deep Dark was rare in these areas: the guard turned down 0 to 1.3% of attempts.
- Nothing was checked by flying around the test worlds.

## Recommended approach

1. **A custom structure type, `maple_flavored:cave_core`**, with a code-built `CaveCorePiece` that places the core block and its decoration. Its codec reads `StructureSettings` plus the cave thresholds.
2. **Placement:** `random_spread` with spacing 8, and one cell scan per attempt (section 6). This replaces the first guess of spacing 8 and separation 3 with a per-chunk finder, which the measurements showed would give only about 0.4 cores per km².
3. **Sampling** in `findGenerationPoint`:
   1. Scan the centre column of each of the 64 chunks in the attempt's 8 × 8 chunk cell, at X and Z multiples of 4.
   2. For each column, find the longest air run between Y −52 and −4 on a solid floor. It must be at least 14 tall.
   3. Confirm a candidate with four probes at ±12 blocks. At least 3 must have air from floor + 2 to floor + 4 and a floor within 10 blocks.
   4. Keep the candidate that passes with the tallest run.
   5. Decline near a potential Ancient City site whose biome is `deep_dark`, or if any of four points 40 blocks out is `deep_dark`.

   Section 6 gives the measured trade-offs for other settings.
4. **Biomes:** a tag listing every Overworld biome except `deep_dark`, checked by vanilla at the core's own Y.
5. **Step `top_layer_modification`, terrain adaptation `none`.** `postProcess` checks the floor again against the real world and adjusts by a few blocks.
6. **Runtime:** the core's block entity holds its Cleared state and adds itself to an in-memory index of loaded cores. Light and spawn checks query that index within the zone radius. Make the radius a `MapleGameRules` rule, starting at 40. Use a mixin for the light-independent spawns, not `spawn_overrides`.
7. **Registration:** structure type, piece type, core block, block entity, `worldgen/structure/cave_core.json`, `worldgen/structure_set/cave_cores.json`, `tags/worldgen/biome/has_structure/cave_core.json`, and a debug command that reports finder hit rates.

## Open risks

- **How many caves qualify.** Section 6 measured about 10 big caves per km², with more than 2× variation between seeds. The cell scan reaches about 70% of them. It's still unconfirmed whether the rooms it counts match what players call a big cave. Check by flying to a sample of cores in a test world.
- **Most exposed diamond is outside big caves.** About 85% faces tunnels and small pockets, so cores alone won't make deep diamonds hard to get (section 6).
- **Flooded caves.** The aquifer makes many deep caves water or lava. Treating fluid as solid avoids cores in lakes, but a cave half-flooded can still pass if the probes land on dry ground. **Unconfirmed.**
- **Lava below Y −54.** Deep cave floors are lava lakes. The Y −52 floor limit keeps cores above them, but the zone can still reach down into lava.
- **Ancient Cities and the Deep Dark.** The potential-site check is close, not exact. A city whose centre lands just inside `deep_dark` but whose pieces spread 116 blocks could still overlap a zone. Cities also block all natural spawns in their box (`ancient_city.json`, every category `"spawns": []`), which would cancel Deep spawns there.
- **Other structures' terrain adaptation.** A Trial Chamber, Ancient City or Stronghold planned near a core can fill in part of the cave. The check in `postProcess` limits the damage, but the cave may end up smaller than planned.
- **Carvers.** They can't delete the core, but they can widen the cave or add water or lava around it after planning.
- **Neighbouring features.** Rarely, a feature from a chunk decorated later writes into the core's spot. **Unconfirmed:** check in a test world.
- **World generation cost.** About half a chunk's terrain density per placement chunk, done on worldgen threads. **Inference:** under 1% of total generation at spacing 8. **Unconfirmed:** time it with a JFR recording or a timer during a pregeneration run.
- **`/locate` cost.** Each candidate chunk runs a full scan. A long locate radius runs many.
- **Lookups across chunk boundaries.** The block entity index only knows loaded cores, and a core at the edge of the simulation distance can be missed. Structure-based lookups only see chunks the box overlaps, unless a zone piece makes the box cover the radius.
- **`/place structure` and the flat GameTest world** never pass the cave check and never record starts. Tests need the bypass described above.
