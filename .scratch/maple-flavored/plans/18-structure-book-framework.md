# Plan: 18 Structure book framework, with Mineshafts

## Context / end goal

The enchanting manifesto's core promise is that enchantments are found in the world: every structure holds one guaranteed level I book in a fixed, visible spot, one per structure per world, and taking some of them wakes the structure. Tickets 19, 20, 22, 23, 28, 44 and 45 each add books to more structures. This ticket builds what they all stand on and proves it end to end with the Mineshaft's Efficiency book:

- a **structure book** block that floats a book, holds its enchantment and survives everything but being taken;
- one server action that takes it, refuses cleanly on a full inventory, and fires a hook the wake tickets plug into;
- a placement path where a per-structure rule picks the spot when pieces are planned, the spot and enchantment are saved with the structure, and the book appears when that chunk is built.

Closing it means a fresh world's Mineshafts each hold exactly one Efficiency I book in the central room, and the later tickets only need to add a rule (spot + enchantments) per structure, plus a `TAKEN` listener for those that wake.

Size: M–L, one ticket. No split: the block, the take action and placement are only testable together.

## Decisions

1. **The book is saved as an extra structure piece.** `StructureBookPiece` (a registered `StructurePieceType`, contextless) has a 1×1×1 bounding box at the spot and stores the enchantment key in its tag. Vanilla already saves every piece with the `StructureStart` and calls `postProcess` on each piece intersecting the chunk being built, so "saved with the structure" and "placed when that chunk is built" need no extra state and no second hook. Its `postProcess` places the block only when `chunkBB` holds the spot. Because the spot *is* the bounding box, any later `move()` keeps it right.
2. **One mixin, on `Structure.generate`.** `@ModifyExpressionValue` on `StructurePiecesBuilder.build()`, capturing `selected`, `seed` and `sourceChunkPos` with `@Local(argsOnly = true)`. It returns `StructureBookRules.withBook(selected, pieces, seed, chunkPos)`: the same container, or a new one with the book piece appended last. It covers natural worldgen and `/place structure` alike. Invariant: an empty container (an invalid start) is never touched, so a book never makes a failed structure valid. Appending last keeps index 0 (which some vanilla code reads as the start piece) unchanged, and in each chunk the book is placed after the structure's own blocks.
3. **Rules are keyed by structure key**, not type, since jigsaw structures share a type. `StructureBookRule(List<ResourceKey<Enchantment>> enchantments, Function<List<StructurePiece>, Optional<BlockPos>> spot)`. The enchantment is picked when pieces are planned, from a `WorldgenRandom` seeded with `setLargeFeatureWithSalt(seed, chunkX, chunkZ, salt)`, so it's deterministic and consumes none of vanilla's randomness. With one choice it's always that one. Equal weight is ready for ticket 19.
4. **The Mineshaft rule:** `minecraft:mineshaft` and `minecraft:mineshaft_mesa` → Efficiency. The spot is the central room (the first `MineshaftPieces.MineShaftRoom`, the hub every corridor branches from): its x/z centre, at `minY + 1`, the first air layer the room carves. Vanilla skips carving a room where it touches liquid or a `mineshaft_blocking` biome, which would bury the book. Jackson's call after the LARP review: the central room is always carved, and every liquid in its box and the layer around it turns to stone (deepslate below Y 0) first, so the room is open and dry. Each chunk seals its own part, so build order doesn't matter. Corridors keep vanilla's rule.
5. **The block** (`StructureBookBlock`, `maple_flavored:structure_book`, no item):
   - `strength(-1, 3_600_000)`, `noLootTable`, `noCollision`, `pushReaction(IMMOVEABLE)` (26.3 has no `BLOCK`), `isValidSpawn(never)`, `lightLevel(7)` (Jackson's call: glows like an enchanting table), `RenderShape.INVISIBLE`, selection shape a centred 8×8×8 box.
   - `canBeReplaced(state, Fluid)` → false. Without it a water bucket destroys it, since vanilla lets fluids replace any non-solid block. Fluid flow already can't (26.3 washes away only the `washed_away_by_fluids` tag).
   - Tags: `minecraft:wither_immune`, `minecraft:dragon_immune`, `minecraft:features_cannot_replace` (so dungeons, geodes and fossils placed later can't overwrite it).
   - Client `animateTick`: occasional `ENCHANT` particles.
   - Creative players can still break it, like bedrock.
6. **The block entity** (`StructureBookBlockEntity`) holds `Holder<Enchantment>`, saved with `Enchantment.CODEC` under `enchantment`. That also lets `/setblock maple_flavored:structure_book{enchantment:"minecraft:efficiency"}` make one by hand. No client sync: the renderer doesn't need the enchantment.
7. **Taking is one server action:** `StructureBooks.take(ServerPlayer, BlockPos) → boolean`. In order: the block entity must hold an enchantment; if `inventory.getFreeSlot() == -1`, it's refused (action-bar "No room for the book", nothing else changes, no event); otherwise add `EnchantmentHelper.createBook(new EnchantmentInstance(e, 1))`, remove the block, play the page-turn sound, then fire `StructureBooks.TAKEN`. The block's `useWithoutItem` calls it on the server and returns `SUCCESS` on the client. Holding an item still takes the book (vanilla falls through to the empty-hand use), and sneaking with an item places beside it.
8. **The wake hook:** `Event<Taken> TAKEN`, where `Taken.onTaken(ServerPlayer player, ServerLevel level, BlockPos pos, Holder<Enchantment> enchantment)` fires after the book is given and the block is gone. Listeners find their structure with `level.structureManager().getStructureWithPieceAt(pos, key)`. The Ancient City's pre-take Warden check (ticket 28) will add its own refusal step then. Adding it now would be guessing at its shape.
9. **Client renderer** (`StructureBookRenderer` + `StructureBookRenderState`): `EnchantTableRenderer`'s `BookModel` and `BOOK_TEXTURE`, open, slowly spinning and bobbing, with time taken from the level's game time plus partial ticks. Registered with `BlockEntityRenderers.register` in `MapleFlavoredClient`. Blockstate JSON points to a model with only a particle texture.
10. **Later structures can't overwrite a book** (added after the LARP review). Structures place in order within a chunk, so an overlapping Mineshaft, Stronghold or template structure such as Trial Chambers placed after the book would erase it. Two narrow mixins skip writes onto a structure book: `StructurePiece.placeBlock` wraps its `canBeReplaced` call, which also covers pieces that override it, as Mineshaft pieces do, and `StructureTemplate.placeInWorld` filters the processed block list. Features are covered by the `features_cannot_replace` tag.
11. **Glossary:** add **Structure book** under Enchanting, via `/domain-modeling`: "The one level I enchanted book a structure holds, floating in a fixed, visible spot. Taking it can wake the structure." _Avoid_: floating book, book pedestal.

## API sketch

```java
// mapleflavored.structurebooks
public final class StructureBooks {
	public static final Block BLOCK; public static final BlockEntityType<StructureBookBlockEntity> BLOCK_ENTITY;
	public static final Event<Taken> TAKEN;
	public static void register();                       // block, BE type, piece type
	public static boolean take(ServerPlayer player, BlockPos pos);
	@FunctionalInterface public interface Taken { void onTaken(ServerPlayer p, ServerLevel l, BlockPos pos, Holder<Enchantment> e); }
}
public final class StructureBookRules {
	public static PiecesContainer withBook(Holder<Structure> structure, PiecesContainer pieces, long seed, ChunkPos chunk);
}
record StructureBookRule(List<ResourceKey<Enchantment>> enchantments, Function<List<StructurePiece>, Optional<BlockPos>> spot) {}
public class StructureBookPiece extends StructurePiece { StructureBookPiece(BlockPos spot, ResourceKey<Enchantment>); StructureBookPiece(CompoundTag); }
public class StructureBookBlockEntity extends BlockEntity { Optional<Holder<Enchantment>> enchantment(); void setEnchantment(Holder<Enchantment>); }
```

## Files

- New `src/main/java/mapleflavored/structurebooks/`: `StructureBooks`, `StructureBookBlock`, `StructureBookBlockEntity`, `StructureBookPiece`, `StructureBookRules`.
- New `mixin/StructureMixin.java`, registered in `maple_flavored.mixins.json`. `MapleFlavored.onInitialize` calls `StructureBooks.register()`.
- Client: `client/structurebooks/StructureBookRenderer.java` and its render state, registered in `MapleFlavoredClient`.
- Resources: `assets/maple_flavored/blockstates/structure_book.json`, `models/block/structure_book.json`, lang (`block.maple_flavored.structure_book` "Structure Book", `…structure_book.no_room` "No room for the book"). Tags under `data/minecraft/tags/block/`: `wither_immune`, `dragon_immune`, `features_cannot_replace` (append, `replace: false`).
- GameTests: `StructureBookGameTests.java` and a `Structures.java` fixture that mirrors `PlaceCommand.placeStructure`: generate the start, force-load its chunks, call `placeInChunk` per chunk, and return the start. It also counts structure books in a start's chunks through their block entities. Each test uses its own far-off chunk, about 2000 chunks out, so a Mineshaft never overlaps other tests. Registered in the gametest `fabric.mod.json`.
- Docs: `GLOSSARY.md`, ticket boxes, this plan copied to `.scratch/maple-flavored/plans/18-structure-book-framework.md`. Nothing reused from Matcha, so `CREDITS.md` is unchanged.

## Acceptance matrix (`StructureBookGameTests`)

| Case | Expected |
| --- | --- |
| Place `mineshaft` at 3 chunk positions and `mineshaft_mesa` at 1, then count books in the start's chunks | exactly 1 each |
| Survival player uses that book (`gameMode.useItemOn`, empty hand) | inventory gains 1 enchanted book with exactly Efficiency I; block is air; `TAKEN` fired once for that pos with Efficiency |
| Same while holding dirt | book taken; dirt kept |
| 36 main slots full of dirt, then use | refused: every slot unchanged, block still there, no `TAKEN` for that pos; free one slot and use again → Efficiency I taken, `TAKEN` once |
| Place chunk by chunk | book count goes 0→1 in exactly one chunk step, the chunk that holds the book |
| `createTag` → `loadStaticStart` → place the loaded start | exactly 1 book; taking it gives Efficiency I |
| Hand-set book (`/setblock … {enchantment:"minecraft:efficiency"}`): survival start/stop destroy | still there, nothing dropped |
| TNT-power `level.explode` beside it | still there |
| Piston facing it, powered | book unmoved |
| `BucketItem.emptyContents` with water aimed at it | book still there, water lands beside it, not in it |
| Water source placed next to it, ticks pass | book still there |
| Sneaking survival player places stone on it | book still there, stone beside it |
| Tags | in `wither_immune`, `dragon_immune` and `features_cannot_replace` |
| The same Mineshaft without its book piece, built over the placed one | book survives; taking it gives Efficiency I |
| A one-block stone template placed on the book, and on air | book survives; stone placed on air |
| Central room's box (+2) filled with water before building | book has air above and on all four sides; no liquid in the room box +1; taking gives Efficiency I |

The fixture calls `Structure.generate` exactly as `/place structure` does, so a separate command test was dropped as redundant.

`TAKEN` assertions use a listener the test class registers once, recording `(pos, enchantment)` in a concurrent map, since Fabric events can't be unregistered.

## Invariants

- At most one book piece per structure start, and none on an invalid start.
- The spot and enchantment live only in the piece, then only in the block entity. Nothing else records them.
- A refused take changes nothing and fires nothing.
- No later structure placement replaces a structure book.
- `TAKEN` fires only after the book is in the inventory and the block is gone.

## Verification

With `JAVA_HOME` set per `gradle.properties`: `gradlew build`, then `gradlew runGameTest`, checking `build/gametest/report.xml` lists the new cases. Then the LARP review (`larp review --message "Review the diff"`), PR, and Codex review per the skill.

## Hand checks (dev client, fresh world)

- `/locate structure minecraft:mineshaft`, go to the central room: the book floats, spins and bobs at the room's centre, glows, and gives off glyph particles. Its outline is the small box, and it can be walked through.
- Survival: can't mine it; right-click takes Efficiency I with a page sound; with a full inventory, the action-bar message shows and nothing changes.
- Leave and reload before visiting a new Mineshaft: its book is still placed (the saved piece).

## Notes for later tickets

- Structures with terrain adaptation (villages, Ancient City, Trail Ruins) treat a non-jigsaw piece as a rigid box in `Beardifier`, so the 1×1×1 book piece would add a tiny beard there. Check when ticket 19 or 20 adds them.
- Ticket 28 adds a pre-take refusal (the Warden spot) to `take`.
- Dungeons (ticket 20) aren't structures: they'd call a shared "place a structure book" helper from the feature, not this piece.

## Exclusions

Every structure other than Mineshafts, wake behaviour, the Fortress lock, and any item form of the block.
