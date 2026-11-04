package mapleflavored.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import mapleflavored.structurebooks.StructureBookPiece;
import mapleflavored.structurebooks.StructureBooks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Every Mineshaft holds exactly one Efficiency I structure book, placed with the chunk that holds it, saved with the
 * structure and kept when another structure overlaps it. Taking it is one action, refused on a full inventory, and the
 * book can't be mined, blown up, pushed or replaced.
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
	public void everyMineshaftHoldsExactlyOneBook(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<ResourceKey<Structure>> mineshafts = List.of(
			BuiltinStructures.MINESHAFT, BuiltinStructures.MINESHAFT, BuiltinStructures.MINESHAFT, BuiltinStructures.MINESHAFT_MESA
		);

		for (int i = 0; i < mineshafts.size(); i++) {
			StructureStart start = Structures.place(level, mineshafts.get(i), farChunk(i));

			helper.assertValueEqual(Structures.books(level, start).size(), 1, "books in " + mineshafts.get(i).identifier() + " " + i);
		}

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

	private static ChunkPos farChunk(int index) {
		return new ChunkPos(FAR_CHUNK, FAR_CHUNK + index * CHUNKS_APART);
	}

	private static BlockPos onlyBook(GameTestHelper helper, StructureStart start) {
		List<BlockPos> books = Structures.books(helper.getLevel(), start);

		helper.assertValueEqual(books.size(), 1, "structure books");

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
