package mapleflavored.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;
import mapleflavored.structurebooks.BookSpot;
import mapleflavored.structurebooks.MineshaftBookSpots;
import mapleflavored.structurebooks.StructureBookPiece;
import mapleflavored.structurebooks.StructureBooks;
import mapleflavored.structurebooks.TerrainPreview;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Every Mineshaft holds exactly one Efficiency I structure book in a dry hallway, placed with the chunk that holds it,
 * saved with the structure and kept when another structure overlaps it. Taking it is one action, refused on a full
 * inventory, and the book can't be mined, blown up, pushed or replaced.
 */
public class StructureBookGameTests {
	// Far from every test's own area, and far enough apart that no two Mineshafts overlap.
	private static final int FAR_CHUNK = 2000;
	private static final int CHUNKS_APART = 40;
	private static final BlockPos BOOK_POS = new BlockPos(3, 2, 3);
	private static final int MAIN_INVENTORY_SLOTS = Inventory.INVENTORY_SIZE;

	// Fabric events can't be unregistered, so one listener records every take for the tests, by absolute position.
	private static final Map<BlockPos, List<Holder<Enchantment>>> TAKES = new ConcurrentHashMap<>();

	static {
		StructureBooks.TAKEN.register((player, level, pos, enchantment) ->
			TAKES.computeIfAbsent(pos.immutable(), unused -> new CopyOnWriteArrayList<>()).add(enchantment)
		);
	}

	@GameTest
	public void everyMineshaftHoldsExactlyOneBookInAHallway(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<ResourceKey<Structure>> mineshafts = List.of(
			BuiltinStructures.MINESHAFT, BuiltinStructures.MINESHAFT, BuiltinStructures.MINESHAFT, BuiltinStructures.MINESHAFT_MESA
		);

		for (int i = 0; i < mineshafts.size(); i++) {
			String name = mineshafts.get(i).identifier() + " " + i;
			StructureStart start = Structures.place(level, mineshafts.get(i), farChunk(i));
			BlockPos book = onlyBook(helper, start);
			long openSides = Direction.Plane.HORIZONTAL.stream().filter(side -> level.getBlockState(book.relative(side)).isAir()).count();

			helper.assertTrue(
				corridors(start.getPieces()).stream().anyMatch(corridor -> MineshaftBookSpots.middleSupport(corridor).equals(book)),
				"the book in " + name + " floats at a corridor's middle support"
			);
			helper.assertTrue(openSides >= 2, "the book in " + name + " is open along its hallway");
		}

		helper.succeed();
	}

	@GameTest
	public void theTerrainPreviewSteersTheBookAwayFromAWetHallway(GameTestHelper helper) {
		List<StructurePiece> pieces = Structures.plan(helper.getLevel(), BuiltinStructures.MINESHAFT, farChunk(9)).getPieces();
		List<BlockPos> corridorSpots = corridors(pieces).stream().map(MineshaftBookSpots::middleSupport).toList();
		BlockPos dryChoice = chooseSpot(pieces, previewWithLiquid((x, z) -> false), "an all-dry preview");
		BoundingBox firstHallway = corridors(pieces).stream()
			.filter(corridor -> MineshaftBookSpots.middleSupport(corridor).equals(dryChoice))
			.findFirst()
			.orElseThrow()
			.getBoundingBox()
			.inflatedBy(1);
		// Wet around the first choice's hallway but not at the spot itself, so the whole hallway must be checked.
		TerrainPreview wetAroundFirstChoice = previewWithLiquid(
			(x, z) -> firstHallway.isInside(new BlockPos(x, firstHallway.minY(), z)) && !(x == dryChoice.getX() && z == dryChoice.getZ())
		);
		BlockPos wetChoice = chooseSpot(pieces, wetAroundFirstChoice, "a preview wet around the first choice");
		BlockPos allWetChoice = chooseSpot(pieces, previewWithLiquid((x, z) -> true), "an all-wet preview");

		helper.assertTrue(corridorSpots.contains(dryChoice), "the dry choice is a corridor's middle support");
		helper.assertTrue(corridorSpots.contains(wetChoice), "the next choice is a corridor's middle support");
		helper.assertTrue(!wetChoice.equals(dryChoice), "a wet hallway is passed over");
		helper.assertValueEqual(allWetChoice, dryChoice, "with no dry hallway, the first one tried");
		helper.succeed();
	}

	@GameTest
	public void aBookWhoseHallwayIsntBuiltGetsADryPocket(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = Structures.plan(level, BuiltinStructures.MINESHAFT, farChunk(8));
		BlockPos spot = start.getPieces().stream()
			.filter(StructureBookPiece.class::isInstance)
			.findFirst()
			.orElseThrow()
			.getBoundingBox()
			.getCenter();

		// Water the preview can't see, like a flooded cave: vanilla then skips building the hallway around the spot.
		BlockPos.betweenClosedStream(new BoundingBox(spot).inflatedBy(3))
			.forEach(pos -> level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS));
		Structures.chunks(start).forEach(chunk -> Structures.build(level, start, chunk));

		BlockPos book = onlyBook(helper, start);
		ChunkPos chunk = ChunkPos.containing(book);
		BoundingBox bookChunk = new BoundingBox(
			chunk.getMinBlockX(), level.getMinY(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), level.getMaxY(), chunk.getMaxBlockZ()
		);

		helper.assertValueEqual(book, spot, "the book's spot");

		for (Direction side : Direction.values()) {
			if (bookChunk.isInside(book.relative(side))) {
				helper.assertTrue(level.getBlockState(book.relative(side)).isAir(), "open space " + side + " of the book");
			}
		}

		BlockPos.betweenClosedStream(new BoundingBox(book).inflatedBy(2))
			.filter(bookChunk::isInside)
			.forEach(pos -> helper.assertTrue(level.getFluidState(pos).isEmpty(), "liquid left at " + pos));

		ServerPlayer player = survivalPlayer(helper);

		use(player, book);

		helper.assertValueEqual(booksIn(player), List.of(Map.of(Enchantments.EFFICIENCY, 1)), "books in the inventory");
		helper.succeed();
	}

	@GameTest
	public void takingAMineshaftBookGivesEfficiencyOne(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos book = onlyBook(helper, Structures.place(level, BuiltinStructures.MINESHAFT, farChunk(4)));
		ServerPlayer player = survivalPlayer(helper);

		use(player, book);

		helper.assertValueEqual(booksIn(player), List.of(Map.of(Enchantments.EFFICIENCY, 1)), "books in the inventory");
		helper.assertTrue(level.getBlockState(book).isAir(), "the book's block is gone");
		helper.assertValueEqual(takenEnchantments(book), List.of(Enchantments.EFFICIENCY), "taken events");
		helper.succeed();
	}

	@GameTest
	public void aFullInventoryRefusesTheBook(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos book = onlyBook(helper, Structures.place(level, BuiltinStructures.MINESHAFT, farChunk(5)));
		ServerPlayer player = survivalPlayer(helper);
		Inventory inventory = player.getInventory();

		for (int slot = 0; slot < MAIN_INVENTORY_SLOTS; slot++) {
			inventory.setItem(slot, new ItemStack(Items.DIRT));
		}

		use(player, book);

		for (int slot = 0; slot < MAIN_INVENTORY_SLOTS; slot++) {
			helper.assertTrue(ItemStack.matches(inventory.getItem(slot), new ItemStack(Items.DIRT)), "slot " + slot + " changed");
		}

		helper.assertTrue(level.getBlockState(book).is(StructureBooks.BLOCK), "the refused book is still there");
		helper.assertValueEqual(takenEnchantments(book), List.of(), "taken events after a refusal");

		// The held dirt stays in hand: holding an item still takes the book.
		inventory.setItem(MAIN_INVENTORY_SLOTS - 1, ItemStack.EMPTY);
		use(player, book);

		helper.assertValueEqual(booksIn(player), List.of(Map.of(Enchantments.EFFICIENCY, 1)), "books in the inventory");
		helper.assertTrue(ItemStack.matches(player.getMainHandItem(), new ItemStack(Items.DIRT)), "the held dirt is kept");
		helper.assertTrue(level.getBlockState(book).isAir(), "the book's block is gone");
		helper.assertValueEqual(takenEnchantments(book), List.of(Enchantments.EFFICIENCY), "taken events");
		helper.succeed();
	}

	@GameTest
	public void aSavedMineshaftPlacesItsBookWithTheChunkThatHoldsIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart saved = Structures.saveAndLoad(level, Structures.plan(level, BuiltinStructures.MINESHAFT, farChunk(6)));
		List<ChunkPos> chunksThatAddedABook = new ArrayList<>();
		int books = Structures.books(level, saved).size();

		helper.assertValueEqual(books, 0, "books before any chunk is built");

		for (ChunkPos chunk : Structures.chunks(saved)) {
			Structures.build(level, saved, chunk);

			int booksNow = Structures.books(level, saved).size();

			if (booksNow != books) chunksThatAddedABook.add(chunk);

			books = booksNow;
		}

		BlockPos book = onlyBook(helper, saved);

		helper.assertValueEqual(chunksThatAddedABook, List.of(ChunkPos.containing(book)), "chunks whose building added a book");

		ServerPlayer player = survivalPlayer(helper);

		use(player, book);

		helper.assertValueEqual(booksIn(player), List.of(Map.of(Enchantments.EFFICIENCY, 1)), "books in the inventory");
		helper.succeed();
	}

	@GameTest
	public void anOverlappingStructureCantEraseTheBook(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = Structures.place(level, BuiltinStructures.MINESHAFT, farChunk(7));
		BlockPos book = onlyBook(helper, start);
		// The same Mineshaft without its book, so its central room carves through the book's spot.
		List<StructurePiece> piecesWithoutBook = start.getPieces().stream()
			.filter(piece -> !(piece instanceof StructureBookPiece))
			.toList();
		StructureStart overlapping = new StructureStart(start.getStructure(), start.getChunkPos(), 0, new PiecesContainer(piecesWithoutBook));

		Structures.chunks(overlapping).forEach(chunk -> Structures.build(level, overlapping, chunk));

		helper.assertTrue(level.getBlockState(book).is(StructureBooks.BLOCK), "the book survives the overlapping structure");

		ServerPlayer player = survivalPlayer(helper);

		use(player, book);

		helper.assertValueEqual(booksIn(player), List.of(Map.of(Enchantments.EFFICIENCY, 1)), "books in the inventory");
		helper.succeed();
	}

	@GameTest
	public void aTemplateCantReplaceTheBook(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos book = setBook(helper);
		BlockPos stone = BOOK_POS.east(2);
		BlockPos empty = BOOK_POS.west(2);
		StructureTemplate template = new StructureTemplate();

		helper.setBlock(stone, Blocks.STONE);
		template.fillFromWorld(level, helper.absolutePos(stone), new Vec3i(1, 1, 1), false, List.of());

		template.placeInWorld(level, helper.absolutePos(empty), helper.absolutePos(empty), new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);
		template.placeInWorld(level, book, book, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);

		helper.assertBlockPresent(Blocks.STONE, empty);
		helper.assertBlockPresent(StructureBooks.BLOCK, BOOK_POS);
		helper.succeed();
	}

	@GameTest
	public void theBookCantBeMined(GameTestHelper helper) {
		BlockPos book = setBook(helper);
		ServerPlayer player = survivalPlayer(helper);

		player.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(BOOK_POS.north(2))));
		helper.assertTrue(player.isWithinBlockInteractionRange(book, 1.0), "the player can reach the book");

		mine(player, book, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
		mine(player, book, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK);

		helper.assertBlockPresent(StructureBooks.BLOCK, BOOK_POS);
		helper.assertEntityNotPresent(EntityTypes.ITEM);

		// The book set by command holds its enchantment.
		use(player, book);

		helper.assertValueEqual(booksIn(player), List.of(Map.of(Enchantments.EFFICIENCY, 1)), "books in the inventory");
		helper.succeed();
	}

	@GameTest
	public void theBookSurvivesAnExplosion(GameTestHelper helper) {
		BlockPos book = setBook(helper);

		helper.getLevel().explode(null, book.getX() + 1.5, book.getY() + 0.5, book.getZ() + 0.5, 4.0F, Level.ExplosionInteraction.TNT);

		helper.assertBlockPresent(StructureBooks.BLOCK, BOOK_POS);
		helper.succeed();
	}

	@GameTest
	public void aPistonCantPushTheBook(GameTestHelper helper) {
		setBook(helper);

		BlockPos piston = BOOK_POS.west();

		helper.setBlock(piston, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
		helper.setBlock(piston.west(), Blocks.REDSTONE_BLOCK);

		helper.runAfterDelay(4, () -> {
			helper.assertBlockPresent(StructureBooks.BLOCK, BOOK_POS);
			helper.assertBlockProperty(piston, PistonBaseBlock.EXTENDED, false);
			helper.succeed();
		});
	}

	@GameTest
	public void aBucketCantReplaceTheBook(GameTestHelper helper) {
		BlockPos book = setBook(helper);
		ServerPlayer player = survivalPlayer(helper);
		BucketItem bucket = (BucketItem) Items.WATER_BUCKET;

		bucket.emptyContents(player, helper.getLevel(), book, hitTop(book));

		helper.assertBlockPresent(StructureBooks.BLOCK, BOOK_POS);
		helper.assertTrue(helper.getLevel().getFluidState(book.above()).is(Fluids.WATER), "the water lands on top of the book");
		helper.succeed();
	}

	@GameTest
	public void flowingWaterCantReplaceTheBook(GameTestHelper helper) {
		setBook(helper);
		helper.setBlock(BOOK_POS.east(), Blocks.WATER);

		helper.runAfterDelay(20, () -> {
			helper.assertBlockPresent(StructureBooks.BLOCK, BOOK_POS);
			helper.succeed();
		});
	}

	@GameTest
	public void placingABlockOnTheBookPlacesItBeside(GameTestHelper helper) {
		BlockPos book = setBook(helper);
		ServerPlayer player = survivalPlayer(helper);

		player.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(BOOK_POS.north(2))));
		player.setShiftKeyDown(true);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
		use(player, book);

		helper.assertBlockPresent(StructureBooks.BLOCK, BOOK_POS);
		helper.assertBlockPresent(Blocks.STONE, BOOK_POS.above());
		helper.assertValueEqual(takenEnchantments(book), List.of(), "taken events");
		helper.succeed();
	}

	@GameTest
	public void witherAndDragonCantBreakTheBookAndFeaturesCantReplaceIt(GameTestHelper helper) {
		setBook(helper);

		var state = helper.getBlockState(BOOK_POS);

		helper.assertTrue(state.is(BlockTags.WITHER_IMMUNE), "wither immune");
		helper.assertTrue(state.is(BlockTags.DRAGON_IMMUNE), "dragon immune");
		helper.assertTrue(state.is(BlockTags.FEATURES_CANNOT_REPLACE), "features can't replace it");
		helper.succeed();
	}

	private static List<StructurePiece> corridors(List<StructurePiece> pieces) {
		return pieces.stream().filter(MineshaftPieces.MineShaftCorridor.class::isInstance).toList();
	}

	// Seeded the same way each call, so every choice tries the corridors in the same order.
	private static BlockPos chooseSpot(List<StructurePiece> pieces, TerrainPreview terrain, String preview) {
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));

		return MineshaftBookSpots.choose(pieces, terrain, random)
			.map(BookSpot::pos)
			.orElseThrow(() -> new IllegalStateException("no spot chosen with " + preview));
	}

	// A preview with no Mineshaft-blocking biome and liquid in the columns that wet picks.
	private static TerrainPreview previewWithLiquid(BiPredicate<Integer, Integer> wet) {
		return new TerrainPreview() {
			@Override
			public boolean hasLiquid(int x, int z, int minY, int maxY) {
				return wet.test(x, z);
			}

			@Override
			public boolean biomeIs(BlockPos pos, TagKey<Biome> tag) {
				return false;
			}
		};
	}

	@GameTest
	public void aBookSavedBeforePlacementsKeepsItsPocketRule(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructurePieceSerializationContext context = StructurePieceSerializationContext.fromLevel(level);
		StructureBookPiece planned = Structures.plan(level, BuiltinStructures.MINESHAFT, farChunk(10)).getPieces().stream()
			.filter(StructureBookPiece.class::isInstance)
			.map(StructureBookPiece.class::cast)
			.findFirst()
			.orElseThrow();
		CompoundTag current = planned.createTag(context);
		// The old format's Pocket flag: true, false, and a book saved before it existed, which carved a pocket.
		List<Optional<Boolean>> pockets = List.of(Optional.of(true), Optional.of(false), Optional.empty());

		for (int i = 0; i < pockets.size(); i++) {
			Optional<Boolean> pocket = pockets.get(i);
			boolean carves = pocket.orElse(true);
			BlockPos spot = helper.absolutePos(new BlockPos(2 + 4 * i, 2, 2));
			CompoundTag legacy = current.copy();

			legacy.remove("Placement");
			legacy.remove("Anchor");
			pocket.ifPresent(value -> legacy.putBoolean("Pocket", value));
			legacy.putIntArray("BB", new int[] {spot.getX(), spot.getY(), spot.getZ(), spot.getX(), spot.getY(), spot.getZ()});
			BlockPos.betweenClosed(spot.offset(-1, -1, -1), spot.offset(1, 1, 1)).forEach(pos -> level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState()));

			StructureBookPiece loaded = new StructureBookPiece(legacy);
			BoundingBox chunkBox = new BoundingBox(spot).inflatedBy(2);

			loaded.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), chunkBox, ChunkPos.containing(spot), spot);

			String saved = loaded.createTag(context).getStringOr("Placement", "");

			helper.assertTrue(level.getBlockState(spot).is(StructureBooks.BLOCK), "Pocket " + pocket + ": the book is placed");
			helper.assertValueEqual(level.getBlockState(spot.east()).isAir(), carves, "Pocket " + pocket + ": carved a pocket");
			helper.assertValueEqual(saved, carves ? "POCKET" : "OPEN", "Pocket " + pocket + ": saved again as");
		}

		helper.succeed();
	}

	private static ChunkPos farChunk(int index) {
		return new ChunkPos(FAR_CHUNK, FAR_CHUNK + index * CHUNKS_APART);
	}

	private static BlockPos onlyBook(GameTestHelper helper, StructureStart start) {
		List<BlockPos> books = Structures.books(helper.getLevel(), start);

		helper.assertValueEqual(books.size(), 1, "structure books at " + books + " in " + start.getChunkPos());

		return books.getFirst();
	}

	// Set by command, so the block entity loads its enchantment from saved data.
	private static BlockPos setBook(GameTestHelper helper) {
		BlockPos book = helper.absolutePos(BOOK_POS);
		String command = "setblock %d %d %d maple_flavored:structure_book{enchantment:\"minecraft:efficiency\"}"
			.formatted(book.getX(), book.getY(), book.getZ());

		MockPlayers.runCommand(helper, survivalPlayer(helper), command);
		helper.assertBlockPresent(StructureBooks.BLOCK, BOOK_POS);

		return book;
	}

	private static ServerPlayer survivalPlayer(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		player.setGameMode(GameType.SURVIVAL);

		return player;
	}

	private static void use(ServerPlayer player, BlockPos pos) {
		player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hitTop(pos));
	}

	private static void mine(ServerPlayer player, BlockPos pos, ServerboundPlayerActionPacket.Action action) {
		player.gameMode.handleBlockBreakAction(pos, action, Direction.UP, player.level().getMaxY(), 0);
	}

	private static BlockHitResult hitTop(BlockPos pos) {
		return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
	}

	// Each enchanted book in the main inventory, as its stored enchantments and levels.
	private static List<Map<ResourceKey<Enchantment>, Integer>> booksIn(ServerPlayer player) {
		List<Map<ResourceKey<Enchantment>, Integer>> books = new ArrayList<>();

		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (!stack.is(Items.ENCHANTED_BOOK)) continue;

			ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
			Map<ResourceKey<Enchantment>, Integer> levels = new HashMap<>();

			stored.entrySet().forEach(entry -> levels.put(entry.getKey().unwrapKey().orElseThrow(), entry.getIntValue()));
			books.add(levels);
		}

		return books;
	}

	private static List<ResourceKey<Enchantment>> takenEnchantments(BlockPos pos) {
		return TAKES.getOrDefault(pos, List.of()).stream().map(enchantment -> enchantment.unwrapKey().orElseThrow()).toList();
	}
}
