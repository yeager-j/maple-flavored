package mapleflavored.gametest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mapleflavored.mixin.SinglePoolElementAccessor;
import mapleflavored.structurebooks.BookLocks;
import mapleflavored.structurebooks.MobKeepers;
import mapleflavored.structurebooks.StructureBookBlockEntity;
import mapleflavored.structurebooks.StructureBookPiece;
import mapleflavored.structurebooks.StructureBookRules;
import mapleflavored.structurebooks.StructureBooks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Each Bastion layout holds its own level I book above that layout's chest: Fire Aspect in the treasure room, Soul
 * Speed on the bridge, Flame in the housing units and Thorns in the hoglin stables. Once its keeper Brutes are dead,
 * taking it sets every Piglin and Piglin Brute in the Bastion hunting the player, however far away and whatever the
 * player wears. {@link BastionLockGameTests} covers the keepers.
 */
public class BastionBookGameTests {
	private static final int SCAN_ATTEMPTS = 60;
	private static final int PLANS = 80;
	// Beyond a Piglin's 16-block targeting range, which vanilla's anger is limited to.
	private static final double FAR_FROM_THE_BOOK = 16.0;
	private static final int OUTSIDE_THE_BASTION = 4;
	private static final long TEN_MINUTES = 10L * 60L * 20L;
	private static final long VANILLA_ANGER_TICKS = 600L;
	private static final float HUNT_MIN_DISTANCE = 20.0F;
	// Four rounds of AI_TICKS in all: time to cover ground, still short of zombifying in the Overworld.
	private static final int CHASE_ROUNDS = 3;
	private static final double CLOSED_IN = 3.0;
	private static final Identifier HUNT_RANGE = Identifier.fromNamespaceAndPath("maple_flavored", "bastion_hunt");	private static final BlockPos HAND_SET_BOOK = new BlockPos(3, 2, 3);
	// Long enough for a Piglin's sensors and fight behaviour to run, short of its zombifying in the Overworld.
	private static final int AI_TICKS = 40;
	// The only templates with a hoglin stable chest; both are optional.
	private static final List<Identifier> STABLES_WITH_A_CHEST = List.of(
		Identifier.withDefaultNamespace("bastion/hoglin_stable/large_stables/inner_3"),
		Identifier.withDefaultNamespace("bastion/hoglin_stable/small_stables/inner_2")
	);
	private static final Identifier STABLES_BASE_WALL = Identifier.withDefaultNamespace("bastion/hoglin_stable/walls/wall_base");

	static final Layout TREASURE_ROOM = new Layout("treasure/big_air_full", Enchantments.FIRE_ASPECT);
	static final Layout BRIDGE = new Layout("bridge/starting_pieces/entrance_base", Enchantments.SOUL_SPEED);
	static final Layout HOUSING_UNITS = new Layout("units/air_base", Enchantments.FLAME);
	static final Layout HOGLIN_STABLES = new Layout("hoglin_stable/air_base", Enchantments.THORNS);
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
			StructureStart start = Structures.plan(level, BuiltinStructures.BASTION_REMNANT, TestSites.of(helper).chunk(i));
			Layout layout = layoutOf(start).orElseThrow(() -> new IllegalStateException("an unknown Bastion layout at " + start.getChunkPos()));
			List<StructureBookPiece> books = Books.pieces(start);

			helper.assertValueEqual(books.size(), 1, "books planned in the Bastion at " + start.getChunkPos());
			helper.assertValueEqual(books.getFirst().enchantment(), layout.enchantment(), "the book planned at " + start.getChunkPos());
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void takingTheTreasureRoomsBookAngersTheBastion(GameTestHelper helper) {
		placeTakeAndCheckAnger(helper, TREASURE_ROOM);
	}

	@GameTest(maxTicks = 400)
	public void takingTheBridgesBookAngersTheBastion(GameTestHelper helper) {
		placeTakeAndCheckAnger(helper, BRIDGE);
	}

	@GameTest(maxTicks = 400)
	public void takingTheHousingUnitsBookAngersTheBastion(GameTestHelper helper) {
		placeTakeAndCheckAnger(helper, HOUSING_UNITS);
	}

	@GameTest(maxTicks = 400)
	public void takingTheHoglinStablesBookAngersTheBastion(GameTestHelper helper) {
		placeTakeAndCheckAnger(helper, HOGLIN_STABLES);
	}

	@GameTest(maxTicks = 400)
	public void anAngeredPiglinAttacksAPlayerWearingGold(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = placeUnlocked(helper, planLayout(level, HOUSING_UNITS, TestSites.of(helper)));
		BlockPos book = Books.only(helper, start);
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

	@GameTest(maxTicks = 400)
	public void piglinsAcrossTheBastionHuntThePlayerUntilTheirAngerEnds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = placeUnlocked(helper, planLayout(level, HOUSING_UNITS, TestSites.of(helper)));
		BlockPos book = Books.only(helper, start);
		ServerPlayer player = SpawnerGameTests.survivalPlayer(helper, Items.STONE);

		player.setPos(Vec3.atBottomCenterOf(book.west()));
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));

		List<AbstractPiglin> farAway = piglinsIn(level, start).stream()
			.filter(piglin -> piglin.isAdult() && piglin.distanceTo(player) > HUNT_MIN_DISTANCE)
			.toList();

		helper.assertTrue(!farAway.isEmpty(), "the Bastion has adult Piglins more than " + HUNT_MIN_DISTANCE + " blocks from the book");

		AbstractPiglin vanillaAngry = farAway.getFirst();
		double baseFollowRange = vanillaAngry.getAttributeValue(Attributes.FOLLOW_RANGE);

		vanillaAngry.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, player.getUUID(), VANILLA_ANGER_TICKS);
		tick(vanillaAngry);
		helper.assertValueEqual(attackTarget(vanillaAngry), Optional.empty(), "a far Piglin's target with only vanilla's anger");

		helper.assertTrue(StructureBooks.take(player, book), "the book is taken");

		int closedIn = 0;

		for (AbstractPiglin hunter : farAway) {
			double before = hunter.distanceTo(player);

			tick(hunter);
			helper.assertValueEqual(attackTarget(hunter), Optional.of(player), "the target of the Piglin " + (int) before + " blocks away");
			helper.assertTrue(hunter.getAttributeValue(Attributes.FOLLOW_RANGE) > before, "the hunter can plan a path " + (int) before + " blocks");

			for (int round = 0; round < CHASE_ROUNDS; round++) {
				tick(hunter);
			}

			if (hunter.distanceTo(player) < before - CLOSED_IN) closedIn++;
		}

		// Not every hunter: some can only reach the player by a detour that first leads away.
		helper.assertTrue(closedIn * 2 > farAway.size(), closedIn + " of " + farAway.size() + " hunters closed in");

		AbstractPiglin hunter = farAway.getFirst();
		Vec3 standingAt = player.position();

		player.teleport(new TeleportTransition(level.getServer().getLevel(Level.NETHER), standingAt, Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING));
		tick(hunter);
		helper.assertValueEqual(hunter.getAttributeValue(Attributes.FOLLOW_RANGE), baseFollowRange, "the follow range while the quarry is away");

		player.teleport(new TeleportTransition(level, standingAt, Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING));
		tick(hunter);
		helper.assertValueEqual(attackTarget(hunter), Optional.of(player), "the target once the quarry is back");
		helper.assertTrue(hunter.getAttribute(Attributes.FOLLOW_RANGE).hasModifier(HUNT_RANGE), "the hunting range once the quarry is back");

		AbstractPiglin retargeted = farAway.getLast();
		ServerPlayer bystander = SpawnerGameTests.survivalPlayer(helper, Items.STONE);

		retargeted.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, bystander.getUUID(), VANILLA_ANGER_TICKS);
		tick(retargeted);
		helper.assertTrue(
			!retargeted.getAttribute(Attributes.FOLLOW_RANGE).hasModifier(HUNT_RANGE),
			"no hunting range once a hunter's anger turns to someone else"
		);

		// As vanilla forgets a dead target.
		hunter.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
		tick(hunter);
		helper.assertValueEqual(attackTarget(hunter), Optional.empty(), "the target once the hunter's anger ends");
		helper.assertValueEqual(hunter.getAttributeValue(Attributes.FOLLOW_RANGE), baseFollowRange, "the follow range once the hunt ends");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void aDeathTheHuntersNeverSawEndsTheHunt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = placeUnlocked(helper, planLayout(level, HOUSING_UNITS, TestSites.of(helper)));
		BlockPos book = Books.only(helper, start);
		ServerPlayer player = SpawnerGameTests.survivalPlayer(helper, Items.STONE);

		player.setPos(Vec3.atBottomCenterOf(book.west()));
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
		helper.assertTrue(StructureBooks.take(player, book), "the book is taken");

		List<AbstractPiglin> adults = piglinsIn(level, start).stream().filter(AbstractPiglin::isAdult).toList();

		helper.assertTrue(adults.size() >= 2, "the Bastion has two adult Piglins");

		AbstractPiglin hunter = adults.getFirst();
		AbstractPiglin retargeted = adults.get(1);
		ServerPlayer bystander = SpawnerGameTests.survivalPlayer(helper, Items.STONE);

		tick(hunter);
		helper.assertValueEqual(attackTarget(hunter), Optional.of(player), "the hunter's target once the book is taken");

		// As if the player died in another dimension, or while the Bastion was unloaded, and came back. Meanwhile another
		// Piglin's anger has moved to someone else, which their death leaves alone.
		retargeted.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, bystander.getUUID(), VANILLA_ANGER_TICKS);
		player.awardStat(Stats.DEATHS);
		tick(hunter);
		tick(retargeted);
		helper.assertValueEqual(angryAt(hunter), Optional.empty(), "the hunter is angry at, after its quarry died");
		helper.assertValueEqual(attackTarget(hunter), Optional.empty(), "the hunter's target after its quarry died");
		helper.assertFalse(hunter.getAttribute(Attributes.FOLLOW_RANGE).hasModifier(HUNT_RANGE), "the hunter keeps the hunting range");
		helper.assertValueEqual(angryAt(retargeted), Optional.of(bystander.getUUID()), "the retargeted Piglin is angry at");
		helper.succeed();
	}

	// The optional stables are the only ones with a stable chest, so the rule is given a planned layout without them.
	@GameTest(maxTicks = 400)
	public void hoglinStablesWithoutAStableChestHoldTheBookAboveTheBaseWallsChest(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart planned = planLayout(level, HOGLIN_STABLES, TestSites.of(helper));
		List<StructurePiece> withoutStableChests = planned.getPieces().stream()
			.filter(piece -> !(piece instanceof StructureBookPiece))
			.filter(piece -> templateOf(piece).filter(STABLES_WITH_A_CHEST::contains).isEmpty())
			.toList();
		List<StructureBookPiece> books = Books.pieces(
			Books.planWithBook(level, BuiltinStructures.BASTION_REMNANT, planned.getChunkPos(), withoutStableChests)
		);

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
		StructureStart start = placeUnlocked(helper, planLayout(level, TREASURE_ROOM, TestSites.of(helper)));
		BlockPos book = Books.only(helper, start);
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
	private static void placeTakeAndCheckAnger(GameTestHelper helper, Layout layout) {
		ServerLevel level = helper.getLevel();
		StructureStart start = placeUnlocked(helper, planLayout(level, layout, TestSites.of(helper)));
		BlockPos book = Books.only(helper, start);

		helper.assertValueEqual(Books.enchantmentAt(level, book), layout.enchantment(), "the book's enchantment");
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
		helper.assertValueEqual(farPiglin.getBrain().getTimeUntilExpiry(MemoryModuleType.ANGRY_AT), TEN_MINUTES, "ticks the far Piglin stays angry");
		helper.assertValueEqual(angryAt(brute), taker, "the Brute is angry at");
		helper.assertValueEqual(angryAt(outsider), Optional.empty(), "the Piglin outside the Bastion is angry at");
		helper.assertValueEqual(angryAt(betweenPieces), Optional.empty(), "the Piglin in the Bastion's bounds but none of its pieces is angry at");
		helper.succeed();
	}

	static StructureStart planLayout(ServerLevel level, Layout layout, TestSites sites) {
		for (int i = 0; i < SCAN_ATTEMPTS; i++) {
			StructureStart start = Structures.plan(level, BuiltinStructures.BASTION_REMNANT, sites.chunk(i));

			if (layoutOf(start).equals(Optional.of(layout))) return start;
		}

		throw new IllegalStateException("no " + layout.start() + " Bastion in " + SCAN_ATTEMPTS + " plans");
	}

	static StructureStart placeRegistered(ServerLevel level, StructureStart start) {
		Structures.chunks(start).forEach(chunk -> Structures.build(level, start, chunk));
		Structures.register(level, start);

		return start;
	}

	// The book opens once its keepers are dead.
	private static StructureStart placeUnlocked(GameTestHelper helper, StructureStart planned) {
		ServerLevel level = helper.getLevel();
		StructureStart start = placeRegistered(level, planned);
		BlockPos book = Books.only(helper, start);

		MobKeepers.liveKeepers(level, book).forEach(keeper -> keeper.kill(level));
		helper.assertTrue(!BookLocks.isLocked(level, book), "the book is unlocked once its keepers are dead");

		return start;
	}

	private static Optional<Layout> layoutOf(StructureStart start) {
		Optional<Identifier> template = templateOf(start.getPieces().getFirst());

		return LAYOUTS.stream().filter(layout -> template.equals(Optional.of(layout.start()))).findFirst();
	}

	static Optional<Identifier> templateOf(StructurePiece piece) {
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
	static void tick(AbstractPiglin piglin) {
		for (int i = 0; i < AI_TICKS; i++) {
			piglin.tick();
		}
	}

	private static Optional<LivingEntity> attackTarget(AbstractPiglin piglin) {
		return piglin.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
	}

	static List<AbstractPiglin> piglinsIn(ServerLevel level, StructureStart start) {
		return level.getEntitiesOfClass(
			AbstractPiglin.class,
			AABB.of(start.getBoundingBox()),
			piglin -> piglin.isAlive() && level.structureManager().structureHasPieceAt(piglin.blockPosition(), start)
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

	static Optional<UUID> angryAt(AbstractPiglin piglin) {
		return piglin.getBrain().getMemory(MemoryModuleType.ANGRY_AT);
	}

	record Layout(Identifier start, ResourceKey<Enchantment> enchantment) {
		Layout(String start, ResourceKey<Enchantment> enchantment) {
			this(Identifier.withDefaultNamespace("bastion/" + start), enchantment);
		}
	}
}
