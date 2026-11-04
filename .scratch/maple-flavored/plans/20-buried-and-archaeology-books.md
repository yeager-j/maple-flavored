# Plan: 20 Buried and archaeology books

## Context / end goal

Tickets 18 and 19 gave every structure with an open room a floating book. Buried Treasure and Trail Ruins have no open room: one is a chest under the sand and the other is a buried ruin you dig and brush. A floating book would either give away the treasure or sit buried where nobody could see it. Closing this ticket means both structures hold exactly one level I book in a place the structure's own activity leads to, so finding it is the challenge. Buried Treasure gives Aqua Affinity or Respiration, with equal weight, and Trail Ruins give Silk Touch.

Jackson's calls (in chat, 2026-11-04):

- **Buried Treasure:** the book goes in the treasure chest, as an item. The treasure map leads straight to it.
- **Trail Ruins:** exactly one suspicious gravel holds the book. Trail Ruins' archaeology is all suspicious gravel, so this ticket uses gravel only.
- **Dungeons are deferred to ticket 21 (Spawner Clearing)**, so the dungeon book is designed alongside Clearing. Ticket 20 drops that criterion and ticket 21 gains it.

Size: M. One ticket.

## What the evidence says

- `BuriedTreasurePiece` is planned as a 1x1x1 box at chunk-local (9, 90, 9). Its `postProcess` searches down from the ocean floor for stone, sandstone, andesite, granite or diorite, places the chest there, and moves its own box to the chest. If it finds none, there's no chest. The book's anchor is that piece with a zero offset, so `StructureBookPiece.spot()` resolves to the chest once the treasure is built. The book piece comes last, so it's always placed after the chest in that chunk. Their XZ is the same, so they share a chunk.
- `LootTable.fill` only fills empty slots and never clears the container. A book put in a slot before the loot is rolled survives the roll. `RandomizableContainerBlockEntity.setItem` unpacks the loot table first, and does so in a GameTest's `ServerLevel`, which would roll vanilla's loot early. So the book is written straight into the container's item list through an invoker on `BaseContainerBlockEntity.getItems()`, and the loot table stays unrolled. But `ChestBlockEntity` saves and loads only the loot table while it's unrolled, never its items, so a chest book would be lost on the first chunk save. Found while building.
- Trail Ruins are a rigid jigsaw structure, buried 15 blocks under `WORLD_SURFACE_WG` (`TerrainAdjustment.BURY`). The pieces don't move after planning. Their archaeology processors turn up to 6 or 3 gravel blocks per piece into suspicious gravel with a loot table, chosen at placement. So which blocks end up suspicious can't be known at planning time. The start piece, a `trail_ruins/tower/tower_1..5`, is always present and holds 114 to 218 gravel in its template, read from the 26.3 jar.
- `BrushableBlockEntity` keeps a private `item` and `lootTable`. Brushing drops `item` when there's no loot table. The block entity saves `item` only when there's no loot table. The book is set through an accessor (`item` set to the book, `lootTable` cleared).
- The GameTest world is flat, with its surface near Y -60, so a Trail Ruin plans mostly below the world's floor. The test lifts the planned pieces with `StructurePiece.move` before building. The book follows its anchor and moves with it, so this also checks that the book is tied to its piece.

## Decisions

1. **`StructureBookRule.Clearance` becomes `Placement`**, which says how the book is placed: `POCKET` (floating, carving a pocket: Mineshafts), `OPEN` (floating, taking the spot as it is: was `NONE`), `IN_CHEST` (an item in the container at the spot), `IN_SUSPICIOUS_GRAVEL` (the block at the spot becomes suspicious gravel holding the book). `StructureBookPiece.postProcess` switches on it once. It's saved as `Placement` by name. A piece saved before this change reads the old `Pocket` boolean.
2. **Buried Treasure spot:** the `BuriedTreasurePiece`, anchored at its own planned position (zero offset). Placement: if the block entity at the spot is a `RandomizableContainerBlockEntity`, put the book in its middle slot (13, or the last slot for a smaller container) without unpacking the loot. With no container (the treasure found no stone), there's no book, as there's no treasure.
3. **Trail Ruins spot:** a gravel block of the start tower's template, chosen with the planning random from the template's gravel (`StructureTemplate.filterBlocks` with the piece's rotation). Placement overwrites whatever the processors left there (gravel, dirt, coarse dirt or suspicious gravel) with suspicious gravel holding the book and no loot table. One of the tower's vanilla suspicious blocks may be replaced. That's accepted, since the book is the better find.
4. **The level I book item** is made in one place, `StructureBooks.levelOneBook(Holder<Enchantment>)`, which `take` and both new placements use.
5. **Mixins:** `BaseContainerBlockEntityInvoker` (`getItems`), `BrushableBlockEntityAccessor` (setters for `item` and `lootTable`), and `ChestBlockEntityMixin`. That last one also saves and loads a chest's non-empty slots while its loot is unrolled, so the book survives saves until the chest is opened. Every other chest has empty slots then, so nothing changes for them. The loot still rolls on first open, with the opener's luck. Rolling the loot at generation instead was rejected: loot functions would run on worldgen threads, and the opener's luck would be lost.

## API sketch

```java
record StructureBookRule(List<ResourceKey<Enchantment>> enchantments, SpotRule spot, Placement placement) {
	enum Placement { POCKET, OPEN, IN_CHEST, IN_SUSPICIOUS_GRAVEL }
}
// StructureBookSpots
static Optional<BookSpot> buriedTreasure(List<StructurePiece> pieces, Planning planning);
static Optional<BookSpot> trailRuins(List<StructurePiece> pieces, Planning planning);
// StructureBooks
static ItemStack levelOneBook(Holder<Enchantment> enchantment);
```

Rules added in `StructureBookRules`: `BUILTIN BURIED_TREASURE` gets `[AQUA_AFFINITY, RESPIRATION]` with `IN_CHEST`, and `TRAIL_RUINS` gets `[SILK_TOUCH]` with `IN_SUSPICIOUS_GRAVEL`.

## Files

- `structurebooks/StructureBookRule.java`, `StructureBookRules.java`, `StructureBookSpots.java`, `StructureBookPiece.java`, `StructureBooks.java`
- `mixin/BaseContainerBlockEntityInvoker.java`, `mixin/BrushableBlockEntityAccessor.java`, `mixin/ChestBlockEntityMixin.java`, `maple_flavored.mixins.json`
- GameTests: new `BuriedStructureBookGameTests`, registered in the gametest `fabric.mod.json`. The legacy-format test goes in `StructureBookGameTests`. The test world's floor is sandstone, which Buried Treasure rests on, and its built chunks' heightmaps read low. So the tests find the surface by scanning blocks and lay their own floor.
- Docs: `GLOSSARY.md` (a structure book can be a chest item or a suspicious block, not only floating), ticket 20 (boxes, Dungeons moved), ticket 21 (gains the Dungeon book criterion), this plan.

## Acceptance matrix (`BuriedStructureBookGameTests`)

| Case | Expected |
| --- | --- |
| Buried Treasure placed on a stone floor | the chest holds exactly one enchanted book, level I, Aqua Affinity or Respiration. Its loot table is still unrolled. No structure book block. |
| Same chest, loot then rolled | the book is still there, alongside vanilla loot |
| Buried Treasure with no stone under it | no chest, no book, no error |
| Buried Treasure saved and loaded before building | the book is still in the chest |
| The built chest's block entity saved and loaded | loot still unrolled, book still in the slot, and rolling then keeps it |
| The built gravel's block entity saved and loaded | still holds the book |
| Multi-choice: 200 planned Buried Treasures | only Aqua Affinity or Respiration, each within 4 binomial SD of half |
| Trail Ruins placed (lifted) | exactly one brushable block holds a book: suspicious gravel, Silk Touch level I, no loot table, inside the start tower's box. No structure book block. |
| Trail Ruins saved and loaded before building | same |
| Brushing the book's block to completion | drops the Silk Touch book |
| A book piece saved before `Placement`, with `Pocket` true, false or missing | carves a pocket for true and missing, not for false, and saves back as `POCKET` or `OPEN` (in `StructureBookGameTests`) |
| Existing structure book tests | still pass (Mineshaft pocket, floating books) |

## Invariants

- At most one book per structure start.
- A chest book never rolls the chest's loot early, and the roll never removes it.
- The book's spot is fixed at planning and only resolved at placement.

## Verification

`gradlew build`, then `gradlew runGameTest` with `JAVA_HOME` set, checking `build/gametest/report.xml`. LARP review, PR, Codex review.

## Hand checks

Follow a buried treasure map and dig up the chest: the book is inside. Brush a Trail Ruin's tower: one suspicious gravel gives Silk Touch.

## Exclusions

Dungeons (ticket 21), waking structures, the Fortress lock.
