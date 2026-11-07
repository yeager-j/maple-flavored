# Vanilla Stronghold generation, and making it intact with an outpost above

Research notes for Maple Flavored, written for ticket 45 (Stronghold Illagers, book and Restoring). They explain how Minecraft 26.3 generates Strongholds with the old piece system, why caves cut through them, and how practical two changes are: Strongholds that generate whole, and an Illager outpost above each one with a way down.

## Source and confidence

Code comes from the Mojang-named sources jar that Loom generated, `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-common-1b0498e74c/26.3/minecraft-common-1b0498e74c-26.3-sources.jar`. Paths below are relative to `net/minecraft/` in that jar. Line numbers are from that jar.

- **Fact**: stated directly by the cited code. Statements without a label are facts.
- **Inference**: my reading of how the cited code works at run time.
- **Unconfirmed**: needs a test world.

Nothing was run in game.

## Summary

1. **Old-style structures are Java code, not templates.** A Stronghold is a tree of `StructurePiece` subclasses in `StrongholdPieces`. Each piece's `postProcess` places its blocks one at a time with helpers such as `generateBox` and `placeBlock`. There are no NBT files.
2. **Caves don't cut Strongholds. Strongholds go around caves.** Caves are dug at the `terrain` chunk stage. The Stronghold is built later, at the `features` stage. Every Stronghold piece except the portal room builds its shell with `skipAir = true`, which skips any position that is already air. That leaves a hole wherever a cave, Mineshaft tunnel or dungeon was dug first.
3. **Making Strongholds whole is very feasible.** Change `skipAir` to `false` for Stronghold pieces with one small mixin, and every piece rebuilds its full box over whatever was there before. Two optional extras put solid rock around the pieces, so that corridors don't hang through caves like tubes. The first is a data-only change to the Stronghold's terrain adaptation. The second is a mixin that stops cave carvers digging inside the Stronghold. 26.3's carver code already has a filter hook for this.
4. **An outpost above is feasible, with some design questions.** The best approach adds the outpost's pieces and a shaft piece to the Stronghold's own structure start, the same seam the structure book code already uses. The outpost can be planned by vanilla's jigsaw assembler from a modified Pillager Outpost pool. The shaft should be a code-built piece, because its height varies with each Stronghold. The open questions are about design, not engineering: what to do over oceans, how deep the shaft goes, outpost Illagers on wooden floors that a Blood Moon can't Restore, and outposts no longer spawning Pillagers forever.

## How old-style structures work

### Planning

A structure is generated in two phases. This is the same for old and jigsaw structures.

1. **Planning**, at the `structure_starts` chunk stage. `Structure.findGenerationPoint` returns a stub that fills a `StructurePiecesBuilder` with pieces. Each piece is a bounding box plus whatever fields it needs, such as a direction or door type. Pieces are saved to the chunk as NBT, and that's why every piece class has a `CompoundTag` constructor and `addAdditionalSaveData`. No blocks are placed yet.
2. **Building**, at the `features` stage, separately for each chunk. `StructureStart.placeInChunk` calls `postProcess` on every piece that overlaps the chunk, clipped to that chunk's box (`chunkBB`).

The chunk stages in 26.3 are `empty`, `structure_starts`, `structure_references`, `biomes`, `terrain`, `features`, and then the lighting stages (`world/level/chunk/status/ChunkStatus.java:21-30`). Noise, surface and carvers used to be separate stages, but 26.3 merges them into `terrain` (`NoiseBasedChunkGenerator.buildTerrain`, line 345).

A jigsaw structure plans with `JigsawPlacement.addPieces`, which produces `PoolElementStructurePiece`s that each place a template. An old structure plans with hand-written code. The Stronghold's is `StrongholdStructure.generatePieces` (`world/level/levelgen/structure/structures/StrongholdStructure.java:23`):

- It puts a `StartPiece`, the starting staircase, at block (2, 2) of the start chunk, facing a random direction.
- It grows children from doorways. Each piece's `addChildren` calls `generateSmallDoorChildForward/Left/Right`, which picks a weighted piece class from `STRONGHOLD_PIECE_WEIGHTS` and tries to fit its box without hitting an existing piece (`findCollisionPiece`).
- Pending children are expanded in random order until none are left. Depth is capped at 50, and no piece may be more than 112 blocks from the start (`StrongholdPieces.java:200-204`).
- The Library needs depth over 4 and the Portal Room depth over 5. If the plan has no portal room, it starts again with `seed + tries` (lines 26-43).
- Finally `moveBelowSeaLevel(seaLevel, minY, random, 10)` shifts the whole plan so its top sits at a random Y between `ySpan + minY + 1` and `seaLevel - 10`, which is 53 in the Overworld (`StructurePiecesBuilder.java:29`).

The piece weights and the placement logic are `private static` fields and methods with static mutable state. That's awkward to change, but changing the layout isn't needed here.

### Building

`StructurePiece` (`world/level/levelgen/structure/StructurePiece.java`) provides the helpers. Pieces work in local coordinates, and `getWorldPos` turns them into world coordinates using the piece's box and orientation.

- `placeBlock` (line 166) sets one block if it is inside `chunkBB`. It doesn't check what was there before.
- `generateBox(..., skipAir, random, selector)` (line 253) fills a box. Edge positions get the selector's wall block and inner positions its fill block. Strongholds use `SmoothStoneSelector`, which gives random stone, mossy or cracked bricks on the edges and air inside. **If `skipAir` is true, any position that is already air is left alone, wall and interior alike** (line 269).
- `generateMaybeBox` adds random blocks such as cobwebs. `maybeGenerateBlock` does the same for torches.

### Why caves cut through Strongholds

Each Stronghold piece's first call builds its shell. All of them pass `skipAir = true` except the portal room:

| Piece | Shell call | `skipAir` |
| --- | --- | --- |
| Chest corridor | `StrongholdPieces.java:268` | true |
| Five-way crossing | 451 | true |
| Left and right turn | 543, 988 | true |
| Library | 614 | true |
| **Portal room** | **780** | **false** |
| Prison hall | 909 | true |
| Room crossing | 1053 | true |
| Stairs down and starting staircase | 1227 | true |
| Straight | 1332 | true |
| Straight stairs down | 1394 | true |

The order is what matters:

1. At `terrain`, the noise router makes noise caves (cheese, spaghetti and noodle caves), and then the carvers dig carver caves and canyons (`NoiseBasedChunkGenerator.generateCarvers`, line 225).
2. At `features`, the decoration steps run in order. Mineshafts, Trial Chambers, Buried Treasure, dungeons and fossils are `underground_structures`. **The Stronghold runs at `surface_structures`.** It sets no step of its own, so it takes the default (`data/worldgen/Structures.java:124-127`, `Structure.java:300`). The `strongholds` step is never used by anything.
3. When a Stronghold piece is built, every position that is already air is skipped. Cave air stays air, walls go missing, and torches, doors and bars placed with plain `placeBlock` float in mid-air.

So the "carving" is really the Stronghold declining to build over air. Liquids are not air, so walls do get built through water and lava. The portal room is always whole because it passes `false`. **Fact.**

### What already keeps some caves out: terrain adaptation

The Stronghold has `terrain_adaptation: bury` (`Structures.java:126`). Before the terrain is filled in, `Beardifier.forStructuresInChunk` (`world/level/levelgen/Beardifier.java:45`) collects the boxes of every non-jigsaw piece within 12 blocks of the chunk. Their contribution is added to the Overworld's final density (`NoiseRouterData.java:326`), and a higher density means more solid terrain.

- `bury` measures the vertical distance from the box's **floor**, halved, and adds `1 - distance / 6`, which falls to nothing 6 blocks out horizontally or 12 blocks above or below the floor (lines 148-158 and 174).
- `encapsulate`, which Trial Chambers use (`Structures.java:348`), measures from the whole box on every axis, halved, and multiplies by 0.8. It reaches 12 blocks from every face.

**Inference:** `bury` already stops most noise caves near a Stronghold's floors. It does little for the upper half of tall pieces such as the Library and the staircases, and it doesn't affect carvers at all, because carvers run after the density is computed and never look at structures. `grep` finds no reference to structures anywhere under `world/level/levelgen/carver/`. Carver caves are the main cause of holes.

### What still runs after the Stronghold

These run after `surface_structures` and could still change a whole Stronghold:

- **Ancient Cities** (`underground_decoration`, `Structures.java:314`) place templates over anything. They are rare to overlap a Stronghold, but possible.
- **Features in neighbouring chunks.** A feature can write into the 3×3 chunks around its own. A dungeon, fossil or geode from a neighbour that is decorated later could cut into a Stronghold that was already built. **Inference:** this is rare.
- **Cave decorations** that attach to any solid face: glow lichen, sculk veins and pointed dripstone. These only show up where a cave meets the Stronghold's outer walls or open doorways.
- **Fluid springs** need their particular stone around them, so they shouldn't start inside stone bricks. Liquid that reaches an open doorway can still flow in.
- Ores and disks only replace the stone tags, so they never touch stone bricks.

## Making Strongholds whole

### Option A: build every piece over air (the core change)

Wrap `skipAir` so it's `false` when the piece is a Stronghold piece. The base class `StrongholdPieces.StrongholdPiece` is private, so key the check on `StructurePiece.getType()` against the `StructurePieceType.STRONGHOLD_*` constants. One `@ModifyVariable` or `@ModifyExpressionValue` in a `StructurePiece` mixin is enough, without touching all 11 piece classes. The mod already has a `StructurePieceMixin` on `placeBlock`.

Effect: each piece rebuilds its full box, walls and air, over caves, Mineshaft tunnels, dungeons, geodes and aquifer liquid. Everything that runs before `surface_structures` is overwritten inside piece boxes. The portal room already works this way.

Side effects:

- **Tubes in caves.** A corridor that crosses a large cave or ravine becomes a stone brick tube hanging in the air. That is "intact", but it may not be the look you want. Options B and C fix this.
- **Mineshafts.** Mineshafts are built before Strongholds, so their tunnels would now end at Stronghold walls instead of opening into them.
- **Trial Chambers** are also built before Strongholds. Where the two overlap, the Stronghold would now cut into the Trial Chamber.
- **Changed randomness.** `SmoothStoneSelector` draws from the random source only when it places a block, so the brick variants, chest loot seeds and later random rolls in that chunk will differ from vanilla. This doesn't matter for a new world.
- **Existing worlds.** Only newly built chunks change. A Stronghold that crosses into chunks generated before the update would show a seam.
- **The Restore spot rule gets simpler.** The design skips spots "where caves or Mineshafts cut through". That rule would almost never come up.

Effort: small. One mixin and a GameTest that places a Stronghold through a cave.

### Option B: solid rock around the pieces (data only)

Override `data/minecraft/worldgen/structure/stronghold.json` and change `terrain_adaptation` from `bury` to `encapsulate`, the setting Trial Chambers use for the same purpose. The mod already overrides `minecraft` worldgen files, such as `structure_set/villages.json`.

Effect: noise caves are pushed back from every face of every piece, including the tops of tall rooms. It also raises the density under oceans, so a Stronghold that rises into the sea floor gets stone around it instead of open water. **Inference:** how far the rock reaches depends on how strongly the cave noise pulls the other way at each spot. The contribution is 0.8 at the wall and falls to 0 at 12 blocks. **Unconfirmed:** check in a test world.

This does nothing to carver caves.

### Option C: stop carvers inside the Stronghold

In 26.3, `generateCarvers` builds a `CarvingMask` and then `applyCarvingMask` applies it. That method already takes a `CarvingMask.Filter` (from `blender.getCarvingFilter()`, `NoiseBasedChunkGenerator.java:273`) and skips any position the filter rejects (line 301). A mixin can combine that filter with "not inside a Stronghold piece's box, plus a margin of a few blocks".

The catch is that `generateCarvers` isn't given the `StructureManager`. `buildTerrain` is, and it calls `generateCarvers` inside a lambda, so the mixin needs to pass the Stronghold boxes in from `buildTerrain`. That means targeting the lambda, or using a thread-local set around the call. Both are standard but a little fiddly. The structure starts for the chunk are already known at `terrain`, since the Beardifier reads them in the same method.

### Recommendation

Use **A + B**, and check for holes in a test world before adding **C**. A alone makes every Stronghold whole. B turns tubes in caves into corridors in rock, and costs one JSON file. C is the most code, and its only job is to fill the carver caves that run alongside the walls.

## The Illager outpost above

### Approach 1 (recommended): add the outpost to the Stronghold's own start

After vanilla plans the Stronghold, add more pieces to the same builder. The mod already does this:

- `NetherFortressStructureMixin` wraps the private static `generatePieces` with `@WrapMethod` and inspects the builder after vanilla is done.
- `StructureMixin` wraps `builder.build()` in `Structure.generate` to add a structure book piece.

The steps:

1. **Find the starting staircase.** It's the `StartPiece`, a `StairsDown` whose entry door is at local (1, 7, 0) (`StrongholdPieces.java:1228`). In vanilla that doorway opens into solid stone. It's where the shaft would join.
2. **Find the surface** at that column. `Structure.GenerationContext` carries the chunk generator, the random state and the height accessor. That's what jigsaw structures use with `WORLD_SURFACE_WG` to predict the surface before terrain exists.
3. **Plan the outpost** with `JigsawPlacement.addPieces(context, pool, ..., position, ..., Optional.of(WORLD_SURFACE_WG), ...)` (`world/level/levelgen/structure/pools/JigsawPlacement.java:51`). Use a copy of the vanilla Pillager Outpost pool with a modified watchtower template that has a hatch or stairwell in its floor. Build the template with structure blocks as usual, and take the resulting pieces from the stub and add them to the Stronghold's builder.
4. **Add a shaft piece** from the outpost floor down to the staircase door. This should be our own code-built `StructurePiece` subclass, registered as a `StructurePieceType` so it saves, as `StructureBookPiece` already is. Its height changes with every Stronghold, which a jigsaw pool can't hit exactly.
5. **Leave the vanilla plan untouched.** Do all of this after vanilla returns, so the Stronghold layout for a given seed stays the same.

What you get:

- **One structure.** `/locate structure minecraft:stronghold` and `/place structure minecraft:stronghold` cover the outpost too, so GameTests place everything at once.
- **Eyes of Ender lead to the outpost.** **Inference:** for ring-placed structures, the Eye of Ender's target is the start chunk's corner (`EnderEyeItem.java:87`, `StructurePlacement.getLocatePos`, line 20), and the starting staircase is at (2, 2) in that chunk. The outpost would sit where the Eyes go down.
- **Shared Restorable and structure book code.** Anything keyed to the Stronghold structure, such as Restoring and book alerts, covers the outpost too. This could be good or bad, depending on the design.

Things to handle:

- **Terrain adaptation is per structure, not per piece.** The Beardifier applies the Stronghold's adaptation to every piece in its start (`Beardifier.java:59-84`), so without a fix the outpost would be buried or encapsulated in a lump of terrain. The fix is a `Beardifier` mixin that picks the adaptation per piece: `beard_thin` (the Pillager Outpost's) for outpost pieces, and the Stronghold's for everything else. `Beardifier.Rigid` already stores an adaptation per box, and the existing `BeardifierMixin` already filters per piece.
- **Structure references reach about 8 chunks.** The outpost sits above the start chunk, and Stronghold pieces already reach 112 blocks, so this is fine. **Inference.**
- **Outposts would stop spawning Pillagers forever.** A vanilla outpost keeps spawning Pillagers in its box through `spawn_overrides` (`Structures.java:54-65`). The Stronghold has none, so the new outpost would only have the Illagers placed when it generates, plus Blood Moons. That fits the design's fixed counts. It also means no outpost Captains appear naturally.

### Approach 2: a separate structure in a matching structure set

A separate `pillager_outpost`-style structure could share the Stronghold's `concentric_rings` settings (`StructureSets.java:154`: distance 32, spread 3, count 128, biased to `stronghold_biased_to`). The X and Z of the starting staircase are fixed at (2, 2) of the ring chunk, but its Y comes from `moveBelowSeaLevel`'s random roll. The outpost would have to replay the whole Stronghold plan to know how deep to dig. It would also be a second structure for `/locate`, books and Restoring, and nothing would keep the two lined up if either changes. Not recommended.

### Approach 3: rewrite the Stronghold as a jigsaw structure

This isn't needed for either goal, and it would lose vanilla's layout logic. Not recommended.

### Design questions this raises

These aren't engineering blockers, but ticket 45 or a design grill would need answers:

1. **Oceans, rivers and steep slopes.** Strongholds are only biased toward land biomes, so a ring can still land in an ocean. Should the outpost be skipped there, stand on a platform, or should the shaft come up through the sea floor?
2. **Shaft depth.** The Stronghold's top can be anywhere from deep in the deepslate up to Y 53. With mountain surfaces, the shaft could be over 100 blocks. Ladders, a spiral staircase, a water drop?
3. **Restoring.** The design puts Restore spots only on the stone bricks family. Outpost floors are dark oak and birch. Do outpost Illagers get Restored, which would mean stone brick floors in the modified outpost, or not?
4. **Counts.** Ticket 02 puts 12 to 20 Illagers in the Stronghold, and the starting staircase is empty so players can step inside. Does the outpost add Illagers on top of that, and does taking the book alert them too?
5. **Outpost book.** Pillager Outposts have their own structure book. This outpost would be part of the Stronghold, so it wouldn't get one unless that's decided.

## Feasibility at a glance

| Idea | Feasible? | Work | Main risk |
| --- | --- | --- | --- |
| Pieces rebuild over air (A) | Yes | One small mixin | Tubes hanging in caves without B or C |
| Rock around pieces (B) | Yes | One JSON override | Shell thickness unconfirmed |
| No carvers in Strongholds (C) | Yes | Medium mixin into terrain generation | Passing structure boxes into carving |
| Outpost and shaft in the Stronghold's start | Yes | New templates, a shaft piece, a planning mixin, a per-piece Beardifier change | The design questions above |
