package mapleflavored.gametest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.AABB;

/**
 * Buried Treasure's chest gives its Aqua Affinity or Respiration book with its loot. Trail Ruins keep their Silk Touch
 * book in one suspicious gravel of the ruin's tower, given by brushing. Neither has a floating book.
 */
public class BuriedStructureBookGameTests {
	private static final List<ResourceKey<Enchantment>> BURIED_TREASURE_BOOKS = List.of(Enchantments.AQUA_AFFINITY, Enchantments.RESPIRATION);
	private static final int CHOICE_ROLLS = 400;
	private static final int SEEDS = 20;
	// The GameTest world is flat, with its surface near the bottom, under which a Trail Ruin would be planned.
	private static final int TRAIL_RUIN_LIFT_ABOVE_SURFACE = 10;
	private static final int BRUSH_STROKES = 10;
	private static final int BRUSH_COOLDOWN = 10;

	@GameTest(maxTicks = 400)
	public void buriedTreasureGivesOneBookWithItsChestsLoot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = Structures.plan(level, BuiltinStructures.BURIED_TREASURE, TestSites.of(helper).chunk(0));
		BlockPos treasure = plannedTreasure(start);

		layStoneUnder(level, treasure);
		Structures.chunks(start).forEach(chunk -> Structures.build(level, start, chunk));

		ChestBlockEntity chest = onlyChest(helper, level, start);

		helper.assertValueEqual(Books.pieces(start), List.of(), "the book pieces Buried Treasure plans");
		helper.assertTrue(Structures.books(level, start).isEmpty(), "Buried Treasure has no floating book");
		helper.assertValueEqual(chest.getLootTable(), BuiltInLootTables.BURIED_TREASURE, "the chest's unrolled loot");
		helper.assertTrue(chest.getLootTableSeed() != 0L, "the chest's loot was seeded when the treasure was built");

		chest.unpackLootTable(null);

		List<ItemStack> rolled = IntStream.range(0, chest.getContainerSize()).mapToObj(chest::getItem).filter(stack -> !stack.isEmpty()).toList();

		helper.assertTrue(rolled.stream().anyMatch(stack -> stack.is(Items.HEART_OF_THE_SEA)), "vanilla's loot is there too");
		assertOneBook(helper, rolled, BURIED_TREASURE_BOOKS, "the chest after its loot roll");
		helper.succeed();
	}

	// The seed a treasure's chest is given at generation decides its book, so the same world gives the same one.
	@GameTest
	public void theSameLootSeedGivesTheSameBuriedTreasureBook(GameTestHelper helper) {
		for (long seed = 1; seed <= SEEDS; seed++) {
			List<ItemStack> first = LootRolls.roll(helper, BuiltInLootTables.BURIED_TREASURE, RandomSource.create(seed));
			List<ItemStack> again = LootRolls.roll(helper, BuiltInLootTables.BURIED_TREASURE, RandomSource.create(seed));

			assertOneBook(helper, first, BURIED_TREASURE_BOOKS, "the roll seeded " + seed);
			helper.assertValueEqual(onlyBookEnchantment(again), onlyBookEnchantment(first), "the book of a second roll seeded " + seed);
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void buriedTreasureWithNoStoneToRestOnHasNoChestAndNoBook(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = Structures.plan(level, BuiltinStructures.BURIED_TREASURE, TestSites.of(helper).chunk(0));
		BlockPos treasure = plannedTreasure(start);

		fillColumnWithDirt(level, treasure);
		Structures.chunks(start).forEach(chunk -> Structures.build(level, start, chunk));

		helper.assertTrue(chests(level, start).isEmpty(), "no chest without stone");
		helper.assertTrue(Structures.books(level, start).isEmpty(), "no floating book either");
		helper.succeed();
	}

	@GameTest
	public void buriedTreasurePicksAquaAffinityOrRespirationWithEqualWeight(GameTestHelper helper) {
		RandomSource random = RandomSource.create(BuiltInLootTables.BURIED_TREASURE.identifier().hashCode());
		Map<ResourceKey<Enchantment>, Integer> counts = new HashMap<>();

		for (int i = 0; i < CHOICE_ROLLS; i++) {
			List<ItemStack> rolled = LootRolls.roll(helper, BuiltInLootTables.BURIED_TREASURE, random);

			assertOneBook(helper, rolled, BURIED_TREASURE_BOOKS, "roll " + i);
			counts.merge(onlyBookEnchantment(rolled), 1, Integer::sum);
		}

		Books.assertCount(helper, "Aqua Affinity", counts.getOrDefault(Enchantments.AQUA_AFFINITY, 0), CHOICE_ROLLS, 0.5);
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void aTrailRuinHidesOneSilkTouchBookInItsTowersGravel(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = Structures.plan(level, BuiltinStructures.TRAIL_RUINS, TestSites.of(helper).chunk(0));

		liftAboveSurface(level, start);
		Structures.chunks(start).forEach(chunk -> Structures.build(level, start, chunk));

		List<BrushableBlockEntity> withBooks = brushables(level, start).stream().filter(gravel -> gravel.getItem().is(Items.ENCHANTED_BOOK)).toList();

		helper.assertValueEqual(withBooks.size(), 1, "suspicious blocks holding a book");

		BrushableBlockEntity gravel = withBooks.getFirst();
		BlockPos pos = gravel.getBlockPos();
		StructurePiece tower = start.getPieces().getFirst();

		helper.assertTrue(level.getBlockState(pos).is(Blocks.SUSPICIOUS_GRAVEL), "the book is in suspicious gravel");
		helper.assertTrue(tower.getBoundingBox().isInside(pos), "the gravel is in the ruin's tower");
		helper.assertValueEqual(pos, Books.piece(start).spot(), "the gravel is at the book's planned spot");
		assertOneBook(helper, List.of(gravel.getItem()), List.of(Enchantments.SILK_TOUCH), "the gravel");
		helper.assertTrue(Structures.books(level, start).isEmpty(), "Trail Ruins have no floating book");

		// The block entity saves its item only when it has no loot table left to roll.
		BrushableBlockEntity reloaded = (BrushableBlockEntity) saveAndLoad(level, gravel);

		assertOneBook(helper, List.of(reloaded.getItem()), List.of(Enchantments.SILK_TOUCH), "the reloaded gravel");

		brushToCompletion(helper, level, gravel);

		List<ItemStack> dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream().map(ItemEntity::getItem).toList();

		helper.assertTrue(level.getBlockState(pos).is(Blocks.GRAVEL), "brushing turned it to plain gravel");
		assertOneBook(helper, dropped, List.of(Enchantments.SILK_TOUCH), "what brushing dropped");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void aTrailRuinsBookIsPlacedFromASavedStructure(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart loaded = Structures.saveAndLoad(level, Structures.plan(level, BuiltinStructures.TRAIL_RUINS, TestSites.of(helper).chunk(0)));

		liftAboveSurface(level, loaded);
		Structures.chunks(loaded).forEach(chunk -> Structures.build(level, loaded, chunk));

		List<BrushableBlockEntity> withBooks = brushables(level, loaded).stream().filter(gravel -> gravel.getItem().is(Items.ENCHANTED_BOOK)).toList();

		helper.assertValueEqual(withBooks.size(), 1, "suspicious blocks holding a book after a reload");
		helper.assertValueEqual(withBooks.getFirst().getBlockPos(), Books.piece(loaded).spot(), "the gravel is at the book's planned spot");
		helper.succeed();
	}

	// Planned at Y 90; the treasure comes to rest when built.
	private static BlockPos plannedTreasure(StructureStart start) {
		BoundingBox box = start.getPieces().getFirst().getBoundingBox();

		return new BlockPos(box.minX(), box.minY(), box.minZ());
	}

	// The treasure rests on the first stone or sandstone it finds going down from the surface. The test world's floor
	// may or may not be either, so each test lays its own.
	private static void layStoneUnder(ServerLevel level, BlockPos treasure) {
		int surface = surface(level, treasure);

		level.setBlockAndUpdate(new BlockPos(treasure.getX(), surface - 1, treasure.getZ()), Blocks.STONE.defaultBlockState());
	}

	private static void fillColumnWithDirt(ServerLevel level, BlockPos treasure) {
		int surface = surface(level, treasure);

		for (int y = level.getMinY(); y < surface; y++) {
			level.setBlockAndUpdate(new BlockPos(treasure.getX(), y, treasure.getZ()), Blocks.DIRT.defaultBlockState());
		}
	}

	// The lowest open block over the column's ground, read from the blocks, since a built chunk's heightmaps can lag
	// behind its blocks here.
	private static int surface(ServerLevel level, BlockPos column) {
		for (int y = level.getMaxY(); y > level.getMinY(); y--) {
			if (!level.getBlockState(new BlockPos(column.getX(), y - 1, column.getZ())).isAir()) return y;
		}

		return level.getMinY();
	}

	// Moves every piece up, the book with them since it follows its piece.
	private static void liftAboveSurface(ServerLevel level, StructureStart start) {
		int surface = surface(level, start.getPieces().getFirst().getBoundingBox().getCenter());
		int lowest = start.getPieces().stream().mapToInt(piece -> piece.getBoundingBox().minY()).min().orElseThrow();
		int lift = surface + TRAIL_RUIN_LIFT_ABOVE_SURFACE - lowest;

		start.getPieces().forEach(piece -> piece.move(0, lift, 0));
	}

	private static ChestBlockEntity onlyChest(GameTestHelper helper, ServerLevel level, StructureStart start) {
		List<ChestBlockEntity> chests = chests(level, start);

		helper.assertValueEqual(chests.size(), 1, "chests");

		return chests.getFirst();
	}

	private static List<ChestBlockEntity> chests(ServerLevel level, StructureStart start) {
		return blockEntities(level, start, ChestBlockEntity.class);
	}

	private static List<BrushableBlockEntity> brushables(ServerLevel level, StructureStart start) {
		return blockEntities(level, start, BrushableBlockEntity.class);
	}

	private static <T extends BlockEntity> List<T> blockEntities(ServerLevel level, StructureStart start, Class<T> type) {
		return Structures.chunks(start).stream()
			.flatMap(chunk -> level.getChunk(chunk.x(), chunk.z()).getBlockEntities().values().stream())
			.filter(type::isInstance)
			.map(type::cast)
			.toList();
	}

	private static BlockEntity saveAndLoad(ServerLevel level, BlockEntity blockEntity) {
		CompoundTag tag = blockEntity.saveWithFullMetadata(level.registryAccess());
		BlockEntity loaded = BlockEntity.loadStatic(blockEntity.getBlockPos(), blockEntity.getBlockState(), tag, level.registryAccess());

		if (loaded == null) throw new IllegalStateException("the saved block entity didn't load");

		return loaded;
	}

	private static void brushToCompletion(GameTestHelper helper, ServerLevel level, BrushableBlockEntity gravel) {
		long time = level.getGameTime();
		ItemStack brush = new ItemStack(Items.BRUSH);

		for (int stroke = 0; stroke < BRUSH_STROKES; stroke++) {
			gravel.brush(time + (long) stroke * BRUSH_COOLDOWN, level, helper.makeMockServerPlayerInLevel(), Direction.UP, brush);
		}
	}

	private static ResourceKey<Enchantment> onlyBookEnchantment(List<ItemStack> stacks) {
		ItemStack book = stacks.stream().filter(stack -> stack.is(Items.ENCHANTED_BOOK)).findFirst().orElseThrow();

		return book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).keySet().stream()
			.findFirst()
			.flatMap(Holder::unwrapKey)
			.orElseThrow();
	}

	// Exactly one enchanted book among the stacks, holding one enchantment from the list at level I.
	private static void assertOneBook(GameTestHelper helper, List<ItemStack> stacks, List<ResourceKey<Enchantment>> allowed, String where) {
		List<ItemStack> books = stacks.stream().filter(stack -> stack.is(Items.ENCHANTED_BOOK)).toList();

		helper.assertValueEqual(books.size(), 1, "enchanted books in " + where);

		ItemEnchantments enchantments = books.getFirst().getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		Optional<Holder<Enchantment>> only = enchantments.keySet().stream().findFirst();

		helper.assertValueEqual(enchantments.size(), 1, "enchantments on the book in " + where);
		helper.assertTrue(only.flatMap(Holder::unwrapKey).filter(allowed::contains).isPresent(), "the book in " + where + " holds " + enchantments);
		helper.assertValueEqual(enchantments.getLevel(only.get()), 1, "the book's level in " + where);
	}

}
