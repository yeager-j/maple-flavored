package mapleflavored.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.GearKind;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Every Woodland Mansion holds a level I Fortune book in the fake End portal of its portal room. Taking it sets every
 * Illager in the mansion hunting the player, Orichalcum or not.
 */
public class MansionBookGameTests {
	// Where vanilla starts a mansion in its chunk.
	private static final int MANSION_START_IN_CHUNK = 7;
	// Each plan draws its own layout, and about 1 in 8 has a portal room without re-planning.
	private static final int PLANS = 16;
	private static final Identifier PORTAL_ROOM = Identifier.withDefaultNamespace("woodland_mansion/1x2_s2");
	private static final Identifier CORRIDOR = Identifier.withDefaultNamespace("woodland_mansion/corridor_floor");
	private static final Identifier HUNT_RANGE = Identifier.fromNamespaceAndPath("maple_flavored", "illager_hunt");
	private static final BlockPos HAND_SET_BOOK = new BlockPos(3, 2, 3);
	private static final int OUTSIDE_THE_MANSION = 4;
	// Beyond a Vindicator's 12-block follow range, so only a hunt would bring one.
	private static final float HUNT_MIN_DISTANCE = 20.0F;
	// Long enough for an Illager's target goals to run several times.
	private static final int TARGETING_TICKS = 10;
	// Time for the hunters to cover ground.
	private static final int CHASE_TICKS = 120;
	private static final double CLOSED_IN = 3.0;
	// Resistance V takes all damage.
	private static final int FULL_RESISTANCE = 4;

	@GameTest
	public void theMansionsBookIsFortune(GameTestHelper helper) {
		helper.assertValueEqual(StructureBookRules.enchantmentsFor(BuiltinStructures.WOODLAND_MANSION), List.of(Enchantments.FORTUNE), "the mansion's books");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void everyMansionIsPlannedWithAPortalRoomHoldingItsBook(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplate portalRoom = level.getStructureTemplateManager().getOrCreate(PORTAL_ROOM);

		for (int plan = 0; plan < PLANS; plan++) {
			StructureStart start = planMansion(level, TestSites.of(helper), plan);
			List<StructureBookPiece> books = Books.pieces(start);

			helper.assertValueEqual(books.size(), 1, "books planned in the mansion at " + start.getChunkPos());
			helper.assertValueEqual(books.getFirst().enchantment(), Enchantments.FORTUNE, "the book planned at " + start.getChunkPos());

			BlockPos spot = books.getFirst().spot();
			boolean inThePortalRoom = templatePieces(start, portalRoom).stream().anyMatch(room -> room.getBoundingBox().isInside(spot));

			helper.assertTrue(inThePortalRoom, "the book at " + spot + " is in the mansion's portal room");
		}

		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void takingTheBookTurnsEveryIllagerInTheMansionOnAnOrichalcumWearer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = BastionBookGameTests.placeRegistered(level, planMansion(level, TestSites.of(helper), PLANS));
		BlockPos book = Books.only(helper, start);
		ServerPlayer player = orichalcumWearer(helper);
		ServerPlayer bystander = orichalcumWearer(helper);

		helper.assertValueEqual(Books.enchantmentAt(level, book), Enchantments.FORTUNE, "the book's enchantment");
		helper.assertTrue(level.getBlockState(book.below(2)).is(Blocks.WOOL.orange()), "the book floats over the fake portal's orange wool");

		BoundingBox box = start.getBoundingBox();
		Vindicator outsider = spawnVindicator(level, new BlockPos(box.maxX() + OUTSIDE_THE_MANSION, book.getY(), box.maxZ() + OUTSIDE_THE_MANSION));
		List<AbstractIllager> mansionsOwn = illagersIn(level, start);

		player.setPos(Vec3.atBottomCenterOf(book.below()));
		helper.assertTrue(!mansionsOwn.isEmpty(), "the mansion generated with Illagers");
		helper.assertTrue(mansionsOwn.stream().noneMatch(illager -> illager.canAttack(player)), "every Illager is neutral before the book is taken");

		helper.assertTrue(StructureBooks.take(player, book), "the book is taken");

		for (AbstractIllager illager : mansionsOwn) {
			String which = "the " + illager.getType().toShortString() + " at " + illager.blockPosition();

			helper.assertTrue(illager.canAttack(player), which + " can attack the player");
			helper.assertFalse(illager.canAttack(bystander), which + " can attack an Orichalcum wearer who didn't take the book");
			helper.assertTrue(illager.getAttribute(Attributes.FOLLOW_RANGE).hasModifier(HUNT_RANGE), which + " has the hunting range");
		}

		helper.assertFalse(outsider.canAttack(player), "the Vindicator outside the mansion can attack the player");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void huntersAcrossTheMansionCloseInUntilTheirQuarryDies(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = BastionBookGameTests.placeRegistered(level, planMansion(level, TestSites.of(helper), PLANS + 1));
		BlockPos book = Books.only(helper, start);
		ServerPlayer player = orichalcumWearer(helper);

		// So the hunters can't end their hunt by killing the player mid-chase.
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, MobEffectInstance.INFINITE_DURATION, FULL_RESISTANCE));

		helper.assertTrue(StructureBooks.take(player, book), "the book is taken");

		// Out of the sealed portal room, which no hunter can reach until the player breaks a way in.
		player.setPos(Vec3.atBottomCenterOf(standingSpot(level, nearestCorridor(level, start, book))));

		List<Vindicator> farAway = illagersIn(level, start).stream()
			.filter(Vindicator.class::isInstance)
			.map(Vindicator.class::cast)
			.filter(vindicator -> vindicator.distanceTo(player) > HUNT_MIN_DISTANCE)
			.toList();
		Map<Vindicator, Float> startingDistances = farAway.stream().collect(Collectors.toMap(vindicator -> vindicator, vindicator -> vindicator.distanceTo(player)));
		Vindicator hunter = farAway.isEmpty() ? null : farAway.getFirst();

		helper.assertTrue(hunter != null, "the mansion has Vindicators more than " + HUNT_MIN_DISTANCE + " blocks from the player");

		// Vanilla's melee goal paces its path checks by game time, so the hunters must tick with the world.
		ForcedChunks.keepLoaded(helper, level, Structures.chunks(start));

		helper.startSequence()
			.thenIdle(CHASE_TICKS)
			.thenExecute(() -> {
				long closedIn = farAway.stream().filter(vindicator -> vindicator.distanceTo(player) < startingDistances.get(vindicator) - CLOSED_IN).count();

				for (Vindicator vindicator : farAway) {
					int before = startingDistances.get(vindicator).intValue();

					helper.assertTrue(vindicator.getTarget() == player, "the Vindicator that was " + before + " blocks away targets " + vindicator.getTarget());
					helper.assertTrue(vindicator.getAttributeValue(Attributes.FOLLOW_RANGE) > before, "the hunter can plan a path " + before + " blocks");
				}

				// Not every hunter: some can only reach the player by a detour that first leads away.
				helper.assertTrue(closedIn * 2 > farAway.size(), closedIn + " of " + farAway.size() + " hunters closed in");

				// A death the hunters never see, as in another dimension or while they're unloaded, still ends the hunt.
				player.awardStat(Stats.DEATHS);
			})
			.thenIdle(TARGETING_TICKS)
			.thenExecute(() -> {
				helper.assertTrue(hunter.getTarget() != player, "the hunter still targets its quarry after their death");
				helper.assertFalse(hunter.getAttribute(Attributes.FOLLOW_RANGE).hasModifier(HUNT_RANGE), "the hunter keeps the hunting range once the hunt ends");
				helper.assertFalse(hunter.canAttack(player), "the hunter can attack the respawned Orichalcum wearer");
			})
			.thenSucceed();
	}

	// The hunt's own clock is all that says it's over: the quarry is alive, and nothing else kept its end time.
	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void anIllagerWhoseHuntHasRunOutLeavesAnOrichalcumWearerAlone(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = BastionBookGameTests.placeRegistered(level, planMansion(level, TestSites.of(helper), PLANS));
		BlockPos book = Books.only(helper, start);
		ServerPlayer player = orichalcumWearer(helper);

		helper.assertTrue(StructureBooks.take(player, book), "the book is taken");

		List<AbstractIllager> hunters = illagersIn(level, start);

		helper.assertTrue(hunters.size() >= 2, "the mansion generated with two Illagers");

		long now = level.getGameTime();
		AbstractIllager stillHunting = Hunts.reloadedWithHuntUntil(helper, hunters.get(0), Hunts.ILLAGER_HUNT, now + 1);
		AbstractIllager ranOut = Hunts.reloadedWithHuntUntil(helper, hunters.get(1), Hunts.ILLAGER_HUNT, now);

		helper.assertTrue(stillHunting.canAttack(player), "the Illager with a tick of its hunt left can attack its quarry");
		helper.assertFalse(ranOut.canAttack(player), "the Illager whose hunt has run out can attack the Orichalcum wearer");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void aRefusedBookAlertsNoIllager(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = BastionBookGameTests.placeRegistered(level, planMansion(level, TestSites.of(helper), PLANS + 2));
		BlockPos book = Books.only(helper, start);
		ServerPlayer player = orichalcumWearer(helper);
		Inventory inventory = player.getInventory();

		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			inventory.setItem(slot, new ItemStack(Items.DIRT));
		}

		helper.assertTrue(!StructureBooks.take(player, book), "a full inventory refuses the book");
		helper.assertTrue(illagersIn(level, start).stream().noneMatch(illager -> illager.canAttack(player)), "no Illager turned on the player");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void aBookOutsideAMansionAlertsNoIllager(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		helper.setBlock(HAND_SET_BOOK, StructureBooks.BLOCK);

		BlockPos book = helper.absolutePos(HAND_SET_BOOK);
		Holder<Enchantment> fortune = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);

		((StructureBookBlockEntity) level.getBlockEntity(book)).setEnchantment(fortune);

		Vindicator vindicator = spawnVindicator(level, book.east());
		ServerPlayer player = orichalcumWearer(helper);

		helper.assertTrue(StructureBooks.take(player, book), "the book is taken");
		helper.assertFalse(vindicator.canAttack(player), "the Vindicator beside a book outside any mansion can attack the player");
		helper.succeed();
	}

	/**
	 * Plans a mansion on the ground at one of the test's sites with the layout vanilla draws for that plan, the mod's
	 * portal room, and the mod's book.
	 * The test world is too low for a mansion to start on its own, as one needs ground at y 60.
	 */
	private static StructureStart planMansion(ServerLevel level, TestSites sites, int plan) {
		ChunkPos chunk = sites.chunk(plan);
		int x = chunk.getBlockX(MANSION_START_IN_CHUNK);
		int z = chunk.getBlockZ(MANSION_START_IN_CHUNK);
		BlockPos origin = new BlockPos(x, level.getChunk(chunk.x(), chunk.z()).getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) + 1, z);
		RandomSource random = RandomSource.create(level.getSeed() + plan);
		List<WoodlandMansionPieces.WoodlandMansionPiece> pieces = new ArrayList<>();

		WoodlandMansionPieces.generateMansion(level.getStructureTemplateManager(), origin, Rotation.getRandom(random), pieces, random);

		return Books.planWithBook(level, BuiltinStructures.WOODLAND_MANSION, chunk, pieces);
	}

	private static ServerPlayer orichalcumWearer(GameTestHelper helper) {
		ServerPlayer player = MockPlayers.survivalPlayer(helper);

		player.setItemSlot(EquipmentSlot.FEET, new ItemStack(AlloyGear.of(Alloy.ORICHALCUM, GearKind.BOOTS)));

		return player;
	}

	private static List<TemplateStructurePiece> templatePieces(StructureStart start, StructureTemplate template) {
		return start.getPieces().stream()
			.filter(TemplateStructurePiece.class::isInstance)
			.map(TemplateStructurePiece.class::cast)
			.filter(piece -> piece.template() == template)
			.toList();
	}

	private static BoundingBox nearestCorridor(ServerLevel level, StructureStart start, BlockPos from) {
		StructureTemplate corridor = level.getStructureTemplateManager().getOrCreate(CORRIDOR);

		return templatePieces(start, corridor).stream()
			.map(StructurePiece::getBoundingBox)
			.min((a, b) -> Double.compare(a.getCenter().distSqr(from), b.getCenter().distSqr(from)))
			.orElseThrow(() -> new IllegalStateException("the mansion has no corridor"));
	}

	// The lowest spot in the box's centre column with ground under it and room for a player.
	private static BlockPos standingSpot(ServerLevel level, BoundingBox box) {
		Predicate<BlockPos> roomToStand = pos -> !level.getBlockState(pos.below()).isAir()
			&& level.getBlockState(pos).isAir()
			&& level.getBlockState(pos.above()).isAir();
		BlockPos centre = box.getCenter();

		for (int y = box.minY() + 1; y <= box.maxY(); y++) {
			BlockPos pos = new BlockPos(centre.getX(), y, centre.getZ());

			if (roomToStand.test(pos)) return pos;
		}

		throw new IllegalStateException("no room to stand in " + box);
	}

	private static List<AbstractIllager> illagersIn(ServerLevel level, StructureStart start) {
		return level.getEntitiesOfClass(
			AbstractIllager.class,
			AABB.of(start.getBoundingBox()),
			illager -> level.structureManager().structureHasPieceAt(illager.blockPosition(), start)
		);
	}

	private static Vindicator spawnVindicator(ServerLevel level, BlockPos pos) {
		Vindicator vindicator = EntityTypes.VINDICATOR.spawn(level, pos, EntitySpawnReason.COMMAND);

		if (vindicator == null) throw new IllegalStateException("no Vindicator spawned at " + pos);

		return vindicator;
	}
}
