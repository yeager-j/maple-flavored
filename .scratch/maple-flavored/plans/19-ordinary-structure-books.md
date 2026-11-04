# Plan: 19 Ordinary structure books

## Context / end goal

Ticket 18 built the structure book and proved it with Mineshafts. This ticket gives every "ordinary" structure, one that doesn't wake when its book is taken, its book: Desert Pyramid Blast Protection, Jungle Temple Looting, Igloo Frost Walker, Shipwreck Depth Strider, Abandoned Camp Lunge, End City Feather Falling, Ocean Ruins one of Channeling, Impaling, Loyalty or Riptide, and Pillager Outpost one of Multishot, Piercing, Projectile Protection or Quick Charge.

The manifesto's promise is "a fixed place, not a chest roll", with the book "in the structure's most important spot", and for these structures "the challenge is finding them". Closing the ticket means a fresh world's copies of all eight structures each hold exactly one level I book from their list, floating where a player who explores the structure properly will find it: usually by the loot that makes the structure worth visiting, never in a random corner.

Size: L, one ticket. The framework change below is needed by every rule here and is small. Each spot rule is a few lines. No split.

## What the evidence changed

The ticket-18 framework fixes a book's absolute position when pieces are planned. Most structures here **move after planning**, when their first chunk is built:

- Shipwrecks and Ocean Ruins set `templatePosition` from the heightmap in `postProcess`.
- Igloos shift `templatePosition` by the surface height for each `postProcess` call, then **restore it**.
- Jungle Temples and Desert Pyramids move their bounding box to the ground (`updateAverageGroundHeight`, `updateHeightPositionToLowestGroundHeight`).

So a book planned at an absolute spot would float above or be buried under these structures. Every move is a translation of one piece, so the book is planned **relative to the piece it sits in** (its anchor) and resolved when placed.

Also found:

- `Beardifier` turns every non-jigsaw piece of a terrain-adapting structure into a rigid box. Abandoned Camps and Pillager Outposts use `BEARD_THIN`, so the 1x1x1 book piece would raise a mound under the book. Book pieces are skipped there.
- Shipwrecks and Ocean Ruins put the book in water. A non-waterlogged block there renders as an air bubble, so the block gets a `waterlogged` state.
- The GameTest world is `FLAT_ALL_DIMENSIONS`, so End Cities can't find a generation point there (they need ground at Y 60 or higher). Their test plans the city's pieces directly with `EndCityPieces.startHouseTower` and runs them through `StructureBookRules.withBook`. The mixin path is covered by the other seven structures.

## Spots (Jackson's "most important spot", my picks)

Template coordinates are template-local and were read from the vanilla 26.3 `.nbt` files.

| Structure | Keys | Spot |
| --- | --- | --- |
| Desert Pyramid | `desert_pyramid` | Hidden treasure room, one block above the TNT pressure plate, in the shaft (piece-local 10, −10, 10). |
| Jungle Temple | `jungle_pyramid` | Above the hidden chest behind the lever puzzle (piece-local 9, −2, 10). Seen once the puzzle is solved. |
| Igloo | `igloo` | Basement lab centre at head height (`igloo/bottom` 3, 2, 5) when there's a basement (half of igloos), else the igloo's centre (`igloo/top` 3, 2, 4). |
| Shipwreck | `shipwreck`, `shipwreck_beached` | Just above the treasure chest (its data marker), else the map chest, else the supply chest. Fronthalf wrecks have only the supply chest. A beached wreck's chests can be buried, and so can its book, as vanilla's are. |
| Ocean Ruins | `ocean_ruin_cold`, `ocean_ruin_warm` | Just above the chest of the first piece with one (the big ruin in a cluster). |
| Pillager Outpost | `pillager_outpost` | Centre of the watchtower's top deck at head height (`watchtower` or `watchtower_overgrown` 7, 15, 7), the captain's floor beside the chest. |
| Abandoned Camp | all 18 `abandoned_camp_*` | Floating just above the campsite's campfire (every campsite template has one). Fallback when the campsite piece didn't generate: above the tent's entrance jigsaw. |
| End City | `end_city` | At the top of a tower. The highest fat tower's loot room (`fat_tower_top` 8, 2, 8). A city without a fat tower (about a third, from 500 planned cities): the highest thin tower's cap room, at head height over its spiral stairs under the guarding Shulker (`tower_top` 4, 2, 4). Never the ship, per the manifesto. (The first version used the first loot room and fell back to the start house's ground floor. Jackson wanted somewhere more challenging, towards the top.) |

Multi-choice structures pick with equal weight through the existing `StructureBookRule.chooseEnchantment`.

## Decisions

1. **Books follow an anchor piece.** A spot rule returns `BookSpot(StructurePiece anchor, BlockPos pos)`, with `pos` the world spot as planned. `StructureBookRules.withBook` stores the anchor's index in the pieces and the spot's offset from the anchor's origin. The piece saves both (`Anchor`, `Offset`). Its bounding box stays the planned spot: every move is vertical, so XZ and chunk membership never change, and vanilla only intersects boxes on XZ for placement.
2. **Anchor origin**, one rule in one place (`StructureBookPiece.originOf`): the anchor's bounding box's minimum corner. Code-built pieces move their box when they settle. `TemplateStructurePiece.postProcess` recomputes its box from the position it's about to place at, so even the Igloo, which restores its shifted `templatePosition` afterwards, leaves its box where it was built. (The first draft captured the origin with a mixin. Reading `TemplateStructurePiece` showed it isn't needed.)
3. **Ordering holds without extra state.** The book's spot is inside its anchor's footprint, so whenever the book's chunk is built the anchor intersects that chunk too and is placed first (the book is the last piece). The captured origin is therefore always the one used for this chunk.
4. **Linking.** A mixin at the end of `PiecesContainer`'s constructor hands each `StructureBookPiece` its anchor from the list by index. It runs for planned containers and for loaded ones (`PiecesContainer.load` builds one). A book with no anchor saved (ticket-18 worlds) or an index out of range uses its bounding box as the spot, as before.
5. **Clearance per rule.** `StructureBookRule` gains `Clearance { POCKET, NONE }`, saved in the piece. Mineshafts keep `POCKET` (carve 3x3x3, seal liquid). Every rule here is `NONE`: the spot is open in the template or the structure carves it, and an underwater spot holds water that the book takes as its waterlogged state.
6. **Waterlogged block.** `StructureBookBlock` gets `BlockStateProperties.WATERLOGGED` (default false) and returns still water from `getFluidState` when set. It doesn't implement `SimpleWaterloggedBlock`, so buckets still can't fill or drain it (ticket-18 tests stay as they are). Placement sets it from the water at the spot. `take` already uses `removeBlock`, which leaves the fluid's own block, so taking an underwater book leaves water. The blockstate JSON gets both variants.
7. **Beardifier skip.** A mixin on `Beardifier.forStructuresInChunk` treats a `StructureBookPiece` as not close to the chunk, so it adds no rigid box.
8. **Planning context.** `SpotRule.spot(List<StructurePiece>, Planning)` where `Planning(TerrainPreview terrain, StructureTemplateManager templates, RandomSource random)`. The mixin already has the template manager. Jigsaw pieces need it to read their template. `MineshaftBookSpots.choose` keeps its parameters and now returns `Optional<BookSpot>` (the corridor and its middle support); the Mineshaft tests map it to `.pos()`. Its selection is unchanged.
9. **Spot helpers** in a new `StructureBookSpots` (package-private rules plus small helpers): template-local to world for template pieces (`calculateRelativePosition` with the piece's settings) and pool elements (`StructureTemplate.transform` with the piece's rotation, no mirror or pivot, as `SinglePoolElement` places), piece-local to world for code-built pieces (an invoker for `StructurePiece.getWorldX/Y/Z`), and data markers or blocks found with `StructureTemplate.filterBlocks`.

## API sketch

```java
// mapleflavored.structurebooks
record BookSpot(StructurePiece anchor, BlockPos pos) {}
record StructureBookRule(List<ResourceKey<Enchantment>> enchantments, SpotRule spot, Clearance clearance) {
	interface SpotRule { Optional<BookSpot> spot(List<StructurePiece> pieces, Planning planning); }
	record Planning(TerrainPreview terrain, StructureTemplateManager templates, RandomSource random) {}
	enum Clearance { POCKET, NONE }
}
public class StructureBookPiece extends StructurePiece {
	StructureBookPiece(BlockPos spot, ResourceKey<Enchantment> e, int anchorIndex, BlockPos offset, Clearance clearance);
	public static void linkAnchors(List<StructurePiece> pieces); // from PiecesContainerMixin
	public BlockPos spot();                                      // resolved spot, for tests and placement
}
public static PiecesContainer withBook(Holder<Structure>, PiecesContainer, long seed, ChunkPos, TerrainPreview, StructureTemplateManager);
```

## Files

- `structurebooks/`: `StructureBookRule` (Planning, Clearance), `StructureBookRules` (rules map: 8 new structure groups, 18 camp keys, `enchantmentsFor` for tests), new `BookSpot`, new `StructureBookSpots`, `StructureBookPiece` (anchor, offset, clearance, waterlogging), `StructureBookBlock` (waterlogged state), `MineshaftBookSpots` (returns `BookSpot`).
- `mixin/`: `StructureMixin` (passes the template manager), new `PiecesContainerMixin` (links anchors), new `BeardifierMixin`, new `StructurePieceInvoker` (`getWorldX/Y/Z`), new `SinglePoolElementAccessor` (a jigsaw element's template), registered in `maple_flavored.mixins.json`.
- `blockstates/structure_book.json`: `waterlogged=false|true`.
- GameTests: new `OrdinaryStructureBookGameTests`, registered in the gametest `fabric.mod.json`.
- Docs: ticket boxes, this plan. Nothing reused, so `CREDITS.md` is unchanged.

## Acceptance matrix (`OrdinaryStructureBookGameTests`)

| Case | Expected |
| --- | --- |
| Place each of Desert Pyramid, Jungle Temple, Igloo (seeds with and without a basement), Shipwreck, Shipwreck beached, Ocean Ruin cold and warm, Pillager Outpost, Abandoned Camp (several biome variants) | exactly 1 book in the start's chunks, with its block entity holding an enchantment from that structure's list; the book is at its rule's spot as built (by its anchor's final origin) |
| Same structures | the book's spot is open: not inside a solid block of the structure (air, or water for Shipwreck and Ocean Ruins except beached wrecks) |
| Igloo with a basement | the book is inside the basement piece's box; without one, inside the top piece's box |
| Height moves | for the Shipwreck, Ocean Ruin, Igloo and Jungle Temple, the built book Y differs from the planned Y by exactly its anchor's height move |
| Ocean Ruin and Shipwreck in water (fill the area with water first) | book is waterlogged; taking it leaves water |
| Multi-choice: plan Ocean Ruins and Outposts at 200 chunks each | nothing outside the list; each choice's count within 4 binomial standard deviations of a quarter |
| Chunk order: each settling structure built with the book's chunk first, and with it last after saving and reloading the half-built structure | the book sits at an independent landmark: the chest under it, or for the Igloo the floor two below |
| End City: `startHouseTower` with seeds giving a city whose highest loot room isn't its first, and one with no fat tower, through `withBook`, built | 1 Feather Falling book in the highest loot room's box, or in the highest thin tower top's box |
| Save and load a planned Shipwreck, then build | 1 book at the right height (anchor link survives loading) |
| Outpost and Camp beardifier | no book piece adds a rigid box (`Beardifier` over the book's chunk equals one built from the pieces without the book) |
| Existing ticket-18 tests | still pass (Mineshaft `POCKET`, bucket and flow tests) |

## Invariants

- At most one book piece per structure start, none on an invalid start.
- The book's spot is decided once at planning, as an anchor and offset. Placement only resolves it.
- An ordinary book's placement never carves or seals terrain.
- No later structure replaces a book (unchanged from 18).

## Verification

`gradlew build`, then `gradlew runGameTest` with `JAVA_HOME` set, checking `build/gametest/report.xml`. LARP review, PR, Codex review.

## Hand checks (dev client)

For each structure, `/locate structure` then visit: the book floats at the spot in the table, underwater books show no air bubble, and camps and outposts have no mound under the book.

## Exclusions

Waking structures (Ancient City, Bastion, Stronghold, Mansion), Trail Ruins, Buried Treasure, Dungeons, the Fortress lock.
