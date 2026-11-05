package mapleflavored.gametest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mapleflavored.mixin.SinglePoolElementAccessor;
import mapleflavored.structurebooks.StructureBookBlockEntity;
import mapleflavored.structurebooks.StructureBookPiece;
import mapleflavored.structurebooks.StructureBookRules;
import mapleflavored.structurebooks.StructureBooks;
import mapleflavored.structurebooks.TerrainPreview;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Each Bastion layout holds its own level I book above that layout's chest: Fire Aspect in the treasure room, Soul
 * Speed on the bridge, Flame in the housing units and Thorns in the hoglin stables. Taking it angers every Piglin and
 * Piglin Brute in the Bastion at the player, however far away and whatever the player wears.
 */
public class BastionBookGameTests {
	// Far from every other test's area. Each test plans along its own row, so no two share a Bastion.
	private static final int FAR_CHUNK = 6000;
	private static final int CHUNKS_APART = 40;
	private static final int SCAN_ATTEMPTS = 60;
	private static final int PLANS = 80;
	// Beyond a Piglin's 16-block targeting range, which vanilla's anger is limited to.
	private static final double FAR_FROM_THE_BOOK = 16.0;
	private static final int OUTSIDE_THE_BASTION = 4;
	private static final BlockPos HAND_SET_BOOK = new BlockPos(3, 2, 3);
	// Long enough for a Piglin's sensors and fight behaviour to run, short of its zombifying in the Overworld.
	private static final int AI_TICKS = 40;
	// The only templates with a hoglin stable chest; both are optional.
	private static final List<Identifier> STABLES_WITH_A_CHEST = List.of(
		Identifier.withDefaultNamespace("bastion/hoglin_stable/large_stables/inner_3"),
		Identifier.withDefaultNamespace("bastion/hoglin_stable/small_stables/inner_2")
	);
	private static final Identifier STABLES_BASE_WALL = Identifier.withDefaultNamespace("bastion/hoglin_stable/walls/wall_base");

	private static final Layout TREASURE_ROOM = new Layout("treasure/big_air_full", Enchantments.FIRE_ASPECT);
	private static final Layout BRIDGE = new Layout("bridge/starting_pieces/entrance_base", Enchantments.SOUL_SPEED);
	private static final Layout HOUSING_UNITS = new Layout("units/air_base", Enchantments.FLAME);
	private static final Layout HOGLIN_STABLES = new Layout("hoglin_stable/air_base", Enchantments.THORNS);
	private static final List<Layout> LAYOUTS = List.of(TREASURE_ROOM, BRIDGE, HOUSING_UNITS, HOGLIN_STABLES);

	@GameTest
	public void theBastionsBooksAreItsFourLayouts(GameTestHelper helper) {
		helper.assertValueEqual(
			StructureBookRules.enchantmentsFor(BuiltinStructures.BASTION_REMNANT),
			LAYOUTS.stream().map(Layout::enchantment).toList(),
			"the Bastion's books"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void everyBastionIsPlannedWithItsLayoutsBook(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		for (int i = 0; i < PLANS; i++) {
			StructureStart start = Structures.plan(level, BuiltinStructures.BASTION_REMNANT, farChunk(0, i));
			Layout layout = layoutOf(start).orElseThrow(() -> new IllegalStateException("an unknown Bastion layout at " + start.getChunkPos()));
			List<StructureBookPiece> books = bookPieces(start);

			helper.assertValueEqual(books.size(), 1, "books planned in the Bastion at " + start.getChunkPos());
			helper.assertValueEqual(books.getFirst().enchantment(), layout.enchantment(), "the book planned at " + start.getChunkPos());
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void takingTheTreasureRoomsBookAngersTheBastion(GameTestHelper helper) {
		placeTakeAndCheckAnger(helper, TREASURE_ROOM, 1);
	}

	@GameTest(maxTicks = 400)
	public void takingTheBridgesBookAngersTheBastion(GameTestHelper helper) {
		placeTakeAndCheckAnger(helper, BRIDGE, 2);
	}

	@GameTest(maxTicks = 400)
	public void takingTheHousingUnitsBookAngersTheBastion(GameTestHelper helper) {
		placeTakeAndCheckAnger(helper, HOUSING_UNITS, 3);
	}

	@GameTest(maxTicks = 400)
	public void takingTheHoglinStablesBookAngersTheBastion(GameTestHelper helper) {
		placeTakeAndCheckAnger(helper, HOGLIN_STABLES, 4);
	}

	@GameTest(maxTicks = 400)
	public void anAngeredPiglinAttacksAPlayerWearingGold(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = placeRegistered(level, planLayout(level, HOUSING_UNITS, 6));
		BlockPos book = onlyBook(helper, start);
		AbstractPiglin piglin = spawn(level, EntityTypes.PIGLIN, book.east());
		ServerPlayer player = SpawnerGameTests.survivalPlayer(helper, Items.STONE);

		piglin.setBaby(false);
		player.setPos(Vec3.atBottomCenterOf(book.west()));
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
		tick(piglin);
		helper.assertValueEqual(attackTarget(piglin), Optional.empty(), "the Piglin's target before the book is taken");

		helper.assertTrue(StructureBooks.take(player, book), "the book is taken");
		tick(piglin);
		helper.assertValueEqual(attackTarget(piglin), Optional.of(player), "the Piglin's target once the book is taken");
		helper.succeed();
	}

	// The optional stables are the only ones with a stable chest, so the rule is given a planned layout without them.
	@GameTest(maxTicks = 400)
	public void hoglinStablesWithoutAStableChestHoldTheBookAboveTheBaseWallsChest(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart planned = planLayout(level, HOGLIN_STABLES, 7);
		List<StructurePiece> withoutStableChests = planned.getPieces().stream()
			.filter(piece -> !(piece instanceof StructureBookPiece))
			.filter(piece -> templateOf(piece).filter(STABLES_WITH_A_CHEST::contains).isEmpty())
			.toList();
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		RandomState randomState = level.getChunkSource().randomState();
		TerrainPreview terrain = TerrainPreview.of(
			generator, generator.getBiomeSource(), randomState.createClimateSampler(SamplerContext.EMPTY_UNCACHED), randomState, level
		);
		Holder.Reference<Structure> bastion = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.BASTION_REMNANT);
		PiecesContainer withBook = StructureBookRules.withBook(
			bastion,
			new PiecesContainer(withoutStableChests),
			level.getSeed(),
			planned.getChunkPos(),
			level.dimension(),
			terrain,
			level.getStructureTemplateManager()
		);
		List<StructureBookPiece> books = bookPieces(new StructureStart(bastion.value(), planned.getChunkPos(), 0, withBook));

		helper.assertValueEqual(books.size(), 1, "books planned without a stable chest");

		BlockPos spot = books.getFirst().spot();
		boolean aboveTheBaseWall = withoutStableChests.stream()
			.filter(piece -> templateOf(piece).equals(Optional.of(STABLES_BASE_WALL)))
			.anyMatch(wall -> wall.getBoundingBox().isInside(spot.below()));

		helper.assertValueEqual(books.getFirst().enchantment(), Enchantments.THORNS, "the book's enchantment");
		helper.assertTrue(aboveTheBaseWall, "the book at " + spot + " is above a chest in the stables' base wall");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void aRefusedBookAngersNoPiglin(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = placeRegistered(level, planLayout(level, TREASURE_ROOM, 5));
		BlockPos book = onlyBook(helper, start);
		AbstractPiglin piglin = spawn(level, EntityTypes.PIGLIN, book);
		ServerPlayer player = SpawnerGameTests.survivalPlayer(helper, Items.DIRT);
		Inventory inventory = player.getInventory();

		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			inventory.setItem(slot, new ItemStack(Items.DIRT));
		}

		helper.assertTrue(!StructureBooks.take(player, book), "a full inventory refuses the book");
		helper.assertValueEqual(angryAt(piglin), Optional.empty(), "the Piglin beside the refused book is angry at");
		helper.succeed();
	}

	@GameTest
	public void aBookOutsideABastionAngersNoPiglin(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		helper.setBlock(HAND_SET_BOOK, StructureBooks.BLOCK);

		BlockPos book = helper.absolutePos(HAND_SET_BOOK);
		Holder<Enchantment> fireAspect = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FIRE_ASPECT);

		((StructureBookBlockEntity) level.getBlockEntity(book)).setEnchantment(fireAspect);

		AbstractPiglin piglin = spawn(level, EntityTypes.PIGLIN, book.east());
		ServerPlayer player = SpawnerGameTests.survivalPlayer(helper, Items.STONE);

		helper.assertTrue(StructureBooks.take(player, book), "the book is taken");
		helper.assertValueEqual(angryAt(piglin), Optional.empty(), "the Piglin beside a book outside any Bastion is angry at");
		helper.succeed();
	}

	// Places a Bastion of the layout, adds a far Piglin and a Brute inside it and a Piglin outside it, then takes the book
	// as a player wearing gold.
	private static void placeTakeAndCheckAnger(GameTestHelper helper, Layout layout, int row) {
		ServerLevel level = helper.getLevel();
		StructureStart start = placeRegistered(level, planLayout(level, layout, row));
		BlockPos book = onlyBook(helper, start);

		helper.assertValueEqual(enchantmentAt(level, book), layout.enchantment(), "the book's enchantment");
		helper.assertTrue(level.getBlockState(book.below()).is(Blocks.CHEST), "the layout's chest is under the book");

		List<AbstractPiglin> bastionsOwn = piglinsIn(level, start);
		BlockPos farSpot = farthestPieceCentre(start, book);

		helper.assertTrue(!bastionsOwn.isEmpty(), "the Bastion generated with Piglins");
		helper.assertTrue(Math.sqrt(farSpot.distSqr(book)) > FAR_FROM_THE_BOOK, "the far Piglin is out of vanilla's anger range");

		AbstractPiglin farPiglin = spawn(level, EntityTypes.PIGLIN, farSpot);
		AbstractPiglin brute = spawn(level, EntityTypes.PIGLIN_BRUTE, book);
		BoundingBox box = start.getBoundingBox();
		AbstractPiglin outsider = spawn(level, EntityTypes.PIGLIN, new BlockPos(box.maxX() + OUTSIDE_THE_BASTION, book.getY(), box.maxZ() + OUTSIDE_THE_BASTION));
		AbstractPiglin betweenPieces = spawn(level, EntityTypes.PIGLIN, outsideEveryPiece(start));
		ServerPlayer player = SpawnerGameTests.survivalPlayer(helper, Items.STONE);

		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));

		helper.assertTrue(StructureBooks.take(player, book), "the book is taken");

		Optional<UUID> taker = Optional.of(player.getUUID());

		for (AbstractPiglin piglin : piglinsIn(level, start)) {
			helper.assertValueEqual(angryAt(piglin), taker, "the " + piglin.getType().toShortString() + " at " + piglin.blockPosition() + " is angry at");
		}

		helper.assertValueEqual(angryAt(farPiglin), taker, "the far Piglin is angry at");
		helper.assertValueEqual(angryAt(brute), taker, "the Brute is angry at");
		helper.assertValueEqual(angryAt(outsider), Optional.empty(), "the Piglin outside the Bastion is angry at");
		helper.assertValueEqual(angryAt(betweenPieces), Optional.empty(), "the Piglin in the Bastion's bounds but none of its pieces is angry at");
		helper.succeed();
	}

	private static StructureStart planLayout(ServerLevel level, Layout layout, int row) {
		for (int i = 0; i < SCAN_ATTEMPTS; i++) {
			StructureStart start = Structures.plan(level, BuiltinStructures.BASTION_REMNANT, farChunk(row, i));

			if (layoutOf(start).equals(Optional.of(layout))) return start;
		}

		throw new IllegalStateException("no " + layout.start() + " Bastion in " + SCAN_ATTEMPTS + " plans");
	}

	private static StructureStart placeRegistered(ServerLevel level, StructureStart start) {
		Structures.chunks(start).forEach(chunk -> Structures.build(level, start, chunk));
		Structures.register(level, start);

		return start;
	}

	private static Optional<Layout> layoutOf(StructureStart start) {
		Optional<Identifier> template = templateOf(start.getPieces().getFirst());

		return LAYOUTS.stream().filter(layout -> template.equals(Optional.of(layout.start()))).findFirst();
	}

	private static Optional<Identifier> templateOf(StructurePiece piece) {
		if (!(piece instanceof PoolElementStructurePiece poolPiece)) return Optional.empty();
		if (!(poolPiece.getElement() instanceof SinglePoolElement single)) return Optional.empty();

		return ((SinglePoolElementAccessor) single).template().left();
	}

	// A spot inside the Bastion's bounding box that none of its pieces covers, as between its outer walls.
	private static BlockPos outsideEveryPiece(StructureStart start) {
		return BlockPos.betweenClosedStream(start.getBoundingBox())
			.filter(pos -> start.getPieces().stream().noneMatch(piece -> piece.getBoundingBox().isInside(pos)))
			.findFirst()
			.map(BlockPos::immutable)
			.orElseThrow(() -> new IllegalStateException("the Bastion's pieces fill its bounding box"));
	}

	// Runs the Piglin's AI by hand, since nothing keeps the far chunk it's in ticking.
	private static void tick(AbstractPiglin piglin) {
		for (int i = 0; i < AI_TICKS; i++) {
			piglin.tick();
		}
	}

	private static Optional<LivingEntity> attackTarget(AbstractPiglin piglin) {
		return piglin.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
	}

	private static List<StructureBookPiece> bookPieces(StructureStart start) {
		return start.getPieces().stream().filter(StructureBookPiece.class::isInstance).map(StructureBookPiece.class::cast).toList();
	}

	private static BlockPos onlyBook(GameTestHelper helper, StructureStart start) {
		List<BlockPos> books = Structures.books(helper.getLevel(), start);

		helper.assertValueEqual(books.size(), 1, "the Bastion's books");

		return books.getFirst();
	}

	private static ResourceKey<Enchantment> enchantmentAt(ServerLevel level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof StructureBookBlockEntity book)) throw new IllegalStateException("no book at " + pos);

		return book.enchantment().flatMap(Holder::unwrapKey).orElseThrow();
	}

	private static List<AbstractPiglin> piglinsIn(ServerLevel level, StructureStart start) {
		return level.getEntitiesOfClass(
			AbstractPiglin.class,
			AABB.of(start.getBoundingBox()),
			piglin -> level.structureManager().structureHasPieceAt(piglin.blockPosition(), start)
		);
	}

	private static BlockPos farthestPieceCentre(StructureStart start, BlockPos from) {
		return start.getPieces().stream()
			.map(StructurePiece::getBoundingBox)
			.map(BoundingBox::getCenter)
			.max((a, b) -> Double.compare(a.distSqr(from), b.distSqr(from)))
			.orElseThrow();
	}

	private static <T extends AbstractPiglin> T spawn(ServerLevel level, EntityType<T> type, BlockPos pos) {
		T piglin = type.spawn(level, pos, EntitySpawnReason.COMMAND);

		if (piglin == null) throw new IllegalStateException("couldn't spawn a " + type.toShortString() + " at " + pos);

		return piglin;
	}

	private static Optional<UUID> angryAt(AbstractPiglin piglin) {
		return piglin.getBrain().getMemory(MemoryModuleType.ANGRY_AT);
	}

	private static ChunkPos farChunk(int row, int attempt) {
		return new ChunkPos(FAR_CHUNK + attempt * CHUNKS_APART, -FAR_CHUNK - row * CHUNKS_APART);
	}

	private record Layout(Identifier start, ResourceKey<Enchantment> enchantment) {
		Layout(String start, ResourceKey<Enchantment> enchantment) {
			this(Identifier.withDefaultNamespace("bastion/" + start), enchantment);
		}
	}
}
