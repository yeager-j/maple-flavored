package mapleflavored.gametest;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.GearKind;
import mapleflavored.bloodmoons.BloodMoons;
import mapleflavored.bloodmoons.Restorable;
import mapleflavored.bloodmoons.RestoreSpot;
import mapleflavored.bloodmoons.RestoreSpots;
import mapleflavored.bloodmoons.Restoring;
import mapleflavored.mixin.StructurePieceInvoker;
import mapleflavored.strongholds.StrongholdLayout;
import mapleflavored.strongholds.StrongholdRoutes;
import mapleflavored.structurebooks.BookLocks;
import mapleflavored.structurebooks.MobKeepers;
import mapleflavored.structurebooks.StructureBookRules;
import mapleflavored.structurebooks.StructureBooks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Strongholds generate whole and full of Illagers, each on a spot a Blood Moon Restores. Their Unbreaking book floats
 * over the End portal, and taking it sets every Illager in the Stronghold hunting the player, Orichalcum or not.
 */
public class StrongholdGameTests {
	// Far from every other test's area. Each test places its own Stronghold, which reaches 112 blocks from its start.
	private static final int FAR_CHUNK = -9000;
	private static final int CHUNKS_APART = 40;
	private static final int LONE_ROOMS_X = -150_000;
	private static final int LONE_ROOMS_Z = 150_000;
	private static final int LONE_ROOMS_Y = 100;
	private static final int LONE_ROOMS_APART = 48;
	private static final int LONE_ROOMS = 48;
	// A south-facing portal room from here has a chunk corner holding only one lava trough: the box's west wall and the
	// trough's three columns, where nothing can stand.
	private static final int LAVA_CORNER_FOOT_X = 2;
	private static final int LAVA_CORNER_FOOT_Z = 12;
	private static final int CHASE_TICKS = 600;
	// The doorways to the player from a hunter with no route there.
	private static final int LOST = Integer.MAX_VALUE;
	private static final int FAR_HUNTER_DOORWAYS = 4;
	// Long enough for every hunter's route goal to have checked its way once.
	private static final int HEADING_CHECK_TICKS = 60;
	private static final int CLOSED_IN_DOORWAYS = 2;
	// Resistance V takes all damage.
	private static final int FULL_RESISTANCE = 4;
	private static final int LIBRARY_ROW = 100;
	private static final int PRISON_ROW = 200;
	private static final List<Direction> FACINGS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
	// Where a tall library's upper walkway puts a guard's feet, and where the back of a short library starts.
	private static final int LIBRARY_WALKWAY_FEET = 6;
	private static final int LIBRARY_BACK_Z = 10;
	private static final TagKey<Block> RESTORE_BLOCKS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("maple_flavored", "stronghold_restore_blocks"));
	private static final Set<EntityType<?>> GUARDS = Set.of(EntityTypes.VINDICATOR, EntityTypes.PILLAGER, EntityTypes.EVOKER);
	// Pieces whose doorways and windows never reach their bottom corners.
	private static final Set<StructurePieceType> SOLID_CORNERED = Set.of(
		StructurePieceType.STRONGHOLD_STRAIGHT,
		StructurePieceType.STRONGHOLD_CHEST_CORRIDOR,
		StructurePieceType.STRONGHOLD_LIBRARY,
		StructurePieceType.STRONGHOLD_PRISON_HALL,
		StructurePieceType.STRONGHOLD_ROOM_CROSSING,
		StructurePieceType.STRONGHOLD_PORTAL_ROOM
	);

	@GameTest
	public void theStrongholdsBookIsUnbreaking(GameTestHelper helper) {
		helper.assertValueEqual(StructureBookRules.enchantmentsFor(BuiltinStructures.STRONGHOLD), List.of(Enchantments.UNBREAKING), "the Stronghold's books");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void aStrongholdBuildsWholeOverAir(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// The flat test world is open air above its few layers, as a huge cave would be. Vanilla would build only the
		// parts of each piece inside those layers.
		StructureStart start = Structures.place(level, BuiltinStructures.STRONGHOLD, farChunk(0));
		List<StructurePiece> solidCornered = start.getPieces().stream().filter(piece -> SOLID_CORNERED.contains(piece.getType())).toList();

		helper.assertFalse(solidCornered.isEmpty(), "the Stronghold has pieces to check");

		for (StructurePiece piece : solidCornered) {
			BoundingBox box = piece.getBoundingBox();

			for (BlockPos corner : List.of(new BlockPos(box.minX(), box.maxY(), box.minZ()), new BlockPos(box.maxX(), box.maxY(), box.maxZ()))) {
				helper.assertTrue(
					level.getBlockState(corner).is(RESTORE_BLOCKS),
					"the top corner " + corner + " of the " + piece.getType() + " is built: " + level.getBlockState(corner)
				);
			}
		}

		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void aStrongholdIsGuardedByIllagersOnSpotsABloodMoonRestores(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = Structures.place(level, BuiltinStructures.STRONGHOLD, farChunk(1));
		List<AbstractIllager> guards = illagersIn(level, start);
		StructurePiece portalRoom = onlyPiece(start, StructurePieceType.STRONGHOLD_PORTAL_ROOM);
		StructurePiece staircase = onlyPiece(start, StructurePieceType.STRONGHOLD_START);
		long libraries = start.getPieces().stream().filter(piece -> piece.getType() == StructurePieceType.STRONGHOLD_LIBRARY).count();

		helper.assertTrue(guards.size() >= 3, "the Stronghold has at least its portal room's guards, " + guards.size());

		for (AbstractIllager guard : guards) {
			String which = "the " + guard.getType().toShortString() + " at " + guard.blockPosition();
			Optional<RestoreSpot> spot = Restoring.spotOf(level.getServer(), guard);

			helper.assertTrue(GUARDS.contains(guard.getType()), which + " is a Vindicator, Pillager or Evoker");
			helper.assertTrue(guard.isPersistenceRequired(), which + " is persistent");
			helper.assertFalse(guard instanceof Pillager pillager && pillager.isPatrolLeader(), which + " is a Captain");
			helper.assertTrue(spot.map(RestoreSpot::structure).equals(Optional.of(Restorable.STRONGHOLD)), which + " holds a Stronghold spot");
			helper.assertTrue(Restorable.STRONGHOLD.holds(level, guard.position()), which + " stands on the Stronghold's Restore blocks");
			helper.assertTrue(level.getBlockState(guard.blockPosition()).isAir() && level.getBlockState(guard.blockPosition().above()).isAir(), which + " has room");
			helper.assertFalse(staircase.getBoundingBox().isInside(guard.blockPosition()), which + " is on the starting staircase");
		}

		List<AbstractIllager> inPortalRoom = guards.stream().filter(guard -> portalRoom.getBoundingBox().isInside(guard.blockPosition())).toList();
		long evokers = guards.stream().filter(guard -> guard.getType() == EntityTypes.EVOKER).count();

		helper.assertValueEqual(countOf(inPortalRoom, EntityTypes.EVOKER), 1L, "Evokers in the portal room");
		helper.assertValueEqual(countOf(inPortalRoom, EntityTypes.VINDICATOR), 2L, "Vindicators in the portal room");
		helper.assertTrue(evokers <= 1 + libraries, evokers + " Evokers, at most one in the portal room and in each of " + libraries + " libraries");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void aPortalRoomKeepsItsFullGuardWhenAChunkHoldsOnlyItsLava(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Structure stronghold = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.STRONGHOLD).value();

		// Each room's own randomness comes from where it is, so many rooms try many first choices of column.
		for (int room = 0; room < LONE_ROOMS; room++) {
			BlockPos foot = new BlockPos(LONE_ROOMS_X + room * LONE_ROOMS_APART + LAVA_CORNER_FOOT_X, LONE_ROOMS_Y, LONE_ROOMS_Z + LAVA_CORNER_FOOT_Z);
			BoundingBox box = BoundingBox.orientBox(foot.getX(), foot.getY(), foot.getZ(), -4, -1, 0, 11, 8, 16, Direction.SOUTH);
			StructurePiece portalRoom = new StrongholdPieces.PortalRoom(0, box, Direction.SOUTH);
			StructureStart start = new StructureStart(stronghold, ChunkPos.containing(box.getCenter()), 0, new PiecesContainer(List.of(portalRoom)));

			Structures.chunks(start).forEach(chunk -> Structures.build(level, start, chunk));

			List<AbstractIllager> guards = level.getEntitiesOfClass(AbstractIllager.class, AABB.of(box));

			helper.assertValueEqual(countOf(guards, EntityTypes.EVOKER), 1L, "Evokers in the portal room at " + foot);
			helper.assertValueEqual(countOf(guards, EntityTypes.VINDICATOR), 2L, "Vindicators in the portal room at " + foot);
		}

		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void aLibrarysEvokerKeepsToItsWalkwayOrToTheBackOfAShortLibrary(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		for (int room = 0; room < LONE_ROOMS; room++) {
			boolean tall = room % 2 == 0;
			Direction facing = FACINGS.get(room / 2 % FACINGS.size());
			BlockPos foot = new BlockPos(LONE_ROOMS_X + room * LONE_ROOMS_APART, LONE_ROOMS_Y, LONE_ROOMS_Z + LIBRARY_ROW);
			BoundingBox box = BoundingBox.orientBox(foot.getX(), foot.getY(), foot.getZ(), -4, -1, 0, 14, tall ? 11 : 6, 15, facing);
			StructurePiece library = new StrongholdPieces.Library(0, RandomSource.create(room), box, facing);
			List<AbstractIllager> guards = buildAlone(level, library);
			String which = (tall ? "the tall library" : "the short library") + " facing " + facing + " at " + foot;
			List<AbstractIllager> evokers = guards.stream().filter(guard -> guard.getType() == EntityTypes.EVOKER).toList();

			helper.assertValueEqual(evokers.size(), 1, "Evokers in " + which);
			helper.assertValueEqual(countOf(guards, EntityTypes.VINDICATOR), 1L, "Vindicators in " + which);

			BlockPos evoker = evokers.getFirst().blockPosition();

			if (tall) {
				helper.assertValueEqual(evoker.getY(), box.minY() + LIBRARY_WALKWAY_FEET, "the height of the Evoker's feet in " + which);
			} else {
				helper.assertValueEqual(evoker.getY(), box.minY() + 1, "the height of the Evoker's feet in " + which);
				helper.assertTrue(inLocalColumns(library, evoker, 1, 12, LIBRARY_BACK_Z, 13), "the Evoker at " + evoker + " is at the back of " + which);
			}
		}

		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void aPrisonHallsGuardStandsOutsideTheCellsAndTheCellsHaveNoDoors(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		for (int room = 0; room < LONE_ROOMS; room++) {
			Direction facing = FACINGS.get(room % FACINGS.size());
			BlockPos foot = new BlockPos(LONE_ROOMS_X + room * LONE_ROOMS_APART, LONE_ROOMS_Y, LONE_ROOMS_Z + PRISON_ROW);
			BoundingBox box = BoundingBox.orientBox(foot.getX(), foot.getY(), foot.getZ(), -1, -1, 0, 9, 5, 11, facing);
			StructurePiece prison = new StrongholdPieces.PrisonHall(0, RandomSource.create(room), box, facing);
			List<AbstractIllager> guards = buildAlone(level, prison);
			String which = "the prison hall facing " + facing + " at " + foot;

			helper.assertValueEqual(guards.size(), 1, "guards in " + which);
			helper.assertTrue(inLocalColumns(prison, guards.getFirst().blockPosition(), 1, 3, 1, 9), "the guard in " + which + " is in the hall");
			helper.assertTrue(doorwayBlocks(level, box).isEmpty(), "doors and buttons in " + which + ": " + doorwayBlocks(level, box));
		}

		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void aStrongholdHasNoDoors(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = Structures.place(level, BuiltinStructures.STRONGHOLD, farChunk(4));

		for (StructurePiece piece : start.getPieces()) {
			List<BlockPos> doorways = doorwayBlocks(level, piece.getBoundingBox());

			helper.assertTrue(doorways.isEmpty(), "doors and buttons in the " + piece.getType() + ": " + doorways);
		}

		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void everyRoomHasARouteToThePortalRoomOneDoorwayAtATime(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = BastionBookGameTests.placeRegistered(level, Structures.plan(level, BuiltinStructures.STRONGHOLD, farChunk(5)));
		BlockPos portalRoom = onlyPiece(start, StructurePieceType.STRONGHOLD_PORTAL_ROOM).getBoundingBox().getCenter();
		List<StructurePiece> rooms = start.getPieces().stream()
			.filter(StrongholdLayout::isStrongholdPiece)
			.filter(piece -> piece.getType() != StructurePieceType.STRONGHOLD_PORTAL_ROOM)
			.toList();

		for (StructurePiece room : rooms) {
			BlockPos from = room.getBoundingBox().getCenter();
			List<BlockPos> route = StrongholdRoutes.route(level, from, portalRoom);
			String which = "the route from the " + room.getType() + " at " + from;

			helper.assertFalse(route.isEmpty(), which + " exists");
			helper.assertTrue(onlyPiece(start, StructurePieceType.STRONGHOLD_PORTAL_ROOM).getBoundingBox().isInside(route.getLast()), which + " ends in the portal room");

			for (int step = 0; step < route.size() - 1; step++) {
				int left = StrongholdRoutes.route(level, route.get(step), portalRoom).size();

				helper.assertValueEqual(left, route.size() - step - 1, "doorways left after step " + step + " of " + which);
			}
		}

		helper.assertTrue(StrongholdRoutes.route(level, portalRoom, portalRoom).isEmpty(), "a route within one room is empty");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = CHASE_TICKS + 200)
	public void huntersAcrossTheStrongholdCloseInRoomByRoom(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = BastionBookGameTests.placeRegistered(level, Structures.plan(level, BuiltinStructures.STRONGHOLD, farChunk(6)));
		BlockPos book = BastionBookGameTests.onlyBook(helper, start);
		StructurePieceInvoker portalRoom = (StructurePieceInvoker) onlyPiece(start, StructurePieceType.STRONGHOLD_PORTAL_ROOM);
		BlockPos byTheDoor = new BlockPos(portalRoom.worldX(5, 2), portalRoom.worldY(1), portalRoom.worldZ(5, 2));
		ServerPlayer player = MockPlayers.survivalPlayer(helper);

		// So the hunters can't end their hunt by killing the player mid-chase.
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, MobEffectInstance.INFINITE_DURATION, FULL_RESISTANCE));
		killKeepers(level, book);
		helper.assertTrue(StructureBooks.take(player, book), "the book is taken");
		player.setPos(Vec3.atBottomCenterOf(byTheDoor));

		Map<AbstractIllager, Integer> farHunters = illagersIn(level, start).stream()
			.filter(guard -> guard.getType() != EntityTypes.EVOKER)
			.collect(Collectors.toMap(guard -> guard, guard -> doorwaysBetween(level, guard, byTheDoor)))
			.entrySet().stream()
			.filter(entry -> entry.getValue() >= FAR_HUNTER_DOORWAYS)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		helper.assertTrue(farHunters.size() >= 2, "the Stronghold has hunters " + FAR_HUNTER_DOORWAYS + " or more doorways away, " + farHunters.size());

		// Vanilla's movement goals pace themselves by game time, so the hunters must tick with the world.
		Structures.chunks(start).forEach(chunk -> level.setChunkForced(chunk.x(), chunk.z(), true));

		helper.startSequence()
			.thenIdle(HEADING_CHECK_TICKS)
			.thenExecute(() -> {
				assertHeadForDoorways(helper, level, player, stillFar(level, farHunters.keySet(), byTheDoor), byTheDoor);
			})
			// Several route checks later, on the way through the same rooms.
			.thenIdle(HEADING_CHECK_TICKS)
			.thenExecute(() -> {
				assertHeadForDoorways(helper, level, player, stillFar(level, farHunters.keySet(), byTheDoor), byTheDoor);
			})
			.thenIdle(CHASE_TICKS - 2 * HEADING_CHECK_TICKS)
			.thenExecute(() -> {
				long closedIn = farHunters.entrySet().stream()
					.filter(entry -> entry.getKey().isAlive())
					.filter(entry -> {
						int left = doorwaysBetween(level, entry.getKey(), byTheDoor);

						return left != LOST && left <= entry.getValue() - CLOSED_IN_DOORWAYS;
					})
					.count();

				Structures.chunks(start).forEach(chunk -> level.setChunkForced(chunk.x(), chunk.z(), false));
				helper.assertTrue(closedIn * 4 >= farHunters.size() * 3, closedIn + " of " + farHunters.size() + " far hunters came " + CLOSED_IN_DOORWAYS + " doorways closer");
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void theBookIsLockedUntilEveryEvokerIsDead(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = BastionBookGameTests.placeRegistered(level, Structures.plan(level, BuiltinStructures.STRONGHOLD, farChunk(7)));
		BlockPos book = BastionBookGameTests.onlyBook(helper, start);
		List<BlockPos> keys = BookLocks.lockAt(level, book).map(BookLocks.Lock::keys).orElse(List.of());
		List<Mob> keepers = MobKeepers.liveKeepers(level, book);
		long rooms = start.getPieces().stream()
			.filter(piece -> piece.getType() == StructurePieceType.STRONGHOLD_PORTAL_ROOM || piece.getType() == StructurePieceType.STRONGHOLD_LIBRARY)
			.count();
		ServerPlayer player = MockPlayers.survivalPlayer(helper);

		player.setPos(Vec3.atBottomCenterOf(book.below()));

		helper.assertValueEqual(BookLocks.lockAt(level, book).map(BookLocks.Lock::kind), Optional.of(BookLocks.Kind.EVOKERS), "the lock's kind");
		helper.assertValueEqual((long) keys.size(), rooms, "keys, one for the portal room and one for each library");
		helper.assertValueEqual(keepers.size(), keys.size(), "keepers alive");
		helper.assertTrue(keepers.stream().allMatch(keeper -> keeper.getType() == EntityTypes.EVOKER), "every keeper is an Evoker");
		helper.assertValueEqual(
			keepers.stream().map(keeper -> MobKeepers.keyOf(keeper).orElseThrow()).collect(Collectors.toSet()),
			Set.copyOf(keys),
			"the keys of the Evokers tagged as keepers"
		);
		helper.assertValueEqual(
			countOf(illagersIn(level, start), EntityTypes.EVOKER),
			(long) keepers.size(),
			"Evokers in the Stronghold, every one a keeper"
		);

		for (Mob keeper : keepers) {
			helper.assertFalse(StructureBooks.take(player, book), "the book is taken with " + BookLocks.remainingKeys(level, book).size() + " Evokers alive");
			helper.assertTrue(keeper.hasEffect(MobEffects.GLOWING), "a refused take makes the Evoker at " + keeper.blockPosition() + " glow");
			keeper.kill(level);
		}

		helper.assertTrue(StructureBooks.take(player, book), "the book is taken once every Evoker is dead");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void takingTheBookTurnsEveryIllagerInTheStrongholdOnAnOrichalcumWearer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = BastionBookGameTests.placeRegistered(level, Structures.plan(level, BuiltinStructures.STRONGHOLD, farChunk(2)));
		BlockPos book = BastionBookGameTests.onlyBook(helper, start);
		StructurePiece portalRoom = onlyPiece(start, StructurePieceType.STRONGHOLD_PORTAL_ROOM);
		ServerPlayer player = MockPlayers.survivalPlayer(helper);

		killKeepers(level, book);

		List<AbstractIllager> guards = illagersIn(level, start).stream().filter(AbstractIllager::isAlive).toList();

		helper.assertFalse(guards.isEmpty(), "Illagers are left once the Evokers are dead");
		player.setItemSlot(EquipmentSlot.FEET, new ItemStack(AlloyGear.of(Alloy.ORICHALCUM, GearKind.BOOTS)));
		player.setPos(Vec3.atBottomCenterOf(book.below()));

		helper.assertValueEqual(BastionBookGameTests.enchantmentAt(level, book), Enchantments.UNBREAKING, "the book's enchantment");
		helper.assertTrue(portalRoom.getBoundingBox().isInside(book), "the book is in the portal room");
		helper.assertTrue(
			level.getBlockState(book.below()).is(Blocks.END_PORTAL) || level.getBlockState(book.below()).isAir(),
			"the book floats over the End portal, not " + level.getBlockState(book.below())
		);
		helper.assertTrue(guards.stream().noneMatch(guard -> guard.canAttack(player)), "every Illager is neutral before the book is taken");

		helper.assertTrue(StructureBooks.take(player, book), "the book is taken");

		for (AbstractIllager guard : guards) {
			helper.assertTrue(guard.canAttack(player), "the " + guard.getType().toShortString() + " at " + guard.blockPosition() + " can attack the player");
		}

		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 2000)
	public void aKilledStrongholdIllagerReturnsAfterABloodMoon(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = Structures.place(level, BuiltinStructures.STRONGHOLD, farChunk(3));
		AbstractIllager guard = illagersIn(level, start).getFirst();
		RestoreSpot spot = Restoring.spotOf(level.getServer(), guard).orElseThrow();
		ChunkPos chunk = ChunkPos.containing(guard.blockPosition());

		level.setChunkForced(chunk.x(), chunk.z(), true);
		ForcedChunks.awaitEntityTicking(helper, level, List.of(chunk), guard.blockPosition().getY());
		guard.kill(level);
		BloodMoons.RISEN.invoker().onRisen(level.getServer());

		helper.assertValueEqual(current(level, spot).state(), RestoreSpot.State.PENDING, "the spot as a Blood Moon rises");

		helper.startSequence()
			.thenWaitUntil(() -> helper.assertValueEqual(current(level, spot).state(), RestoreSpot.State.OCCUPIED, "the spot"))
			.thenExecute(() -> {
				Mob restored = level.getEntitiesOfClass(Mob.class, new AABB(BlockPos.containing(spot.position())).inflate(1), mob -> mob.isAlive())
					.stream()
					.filter(mob -> Optional.of(mob.getUUID()).equals(current(level, spot).occupant()))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("the Restored mob isn't at its spot"));

				helper.assertValueEqual(restored.getType(), spot.type(), "the Restored mob");
				helper.assertTrue(restored.isPersistenceRequired(), "the Restored mob is persistent");
				level.setChunkForced(chunk.x(), chunk.z(), false);
			})
			.thenSucceed();
	}

	private static void killKeepers(ServerLevel level, BlockPos book) {
		MobKeepers.liveKeepers(level, book).forEach(keeper -> keeper.kill(level));
	}

	// How many doorways the hunter must cross to reach the spot: none in the same room, and LOST with no route there.
	private static int doorwaysBetween(ServerLevel level, AbstractIllager hunter, BlockPos spot) {
		List<BlockPos> route = StrongholdRoutes.route(level, hunter.blockPosition(), spot);

		if (!route.isEmpty()) return route.size();

		StructureStart stronghold = level.structureManager().getStructureWithPieceAt(spot, structure -> structure.is(BuiltinStructures.STRONGHOLD));
		boolean sameRoom = stronghold.getPieces().stream()
			.filter(StrongholdLayout::isStrongholdPiece)
			.anyMatch(room -> room.getBoundingBox().isInside(spot) && room.getBoundingBox().isInside(hunter.blockPosition()));

		return sameRoom ? 0 : LOST;
	}

	// Whether the hunter is walking to the doorway into the next room on its way to the spot.
	private static boolean headsForNextDoorway(ServerLevel level, AbstractIllager hunter, BlockPos spot) {
		List<BlockPos> route = StrongholdRoutes.route(level, hunter.blockPosition(), spot);
		BlockPos walkingTo = hunter.getNavigation().getTargetPos();

		return !route.isEmpty() && walkingTo != null && walkingTo.distManhattan(route.getFirst()) <= 1;
	}

	// The living hunters still at least CLOSED_IN_DOORWAYS doorways from the spot, or with no route there.
	private static List<AbstractIllager> stillFar(ServerLevel level, Collection<AbstractIllager> hunters, BlockPos spot) {
		return hunters.stream()
			.filter(AbstractIllager::isAlive)
			.filter(hunter -> doorwaysBetween(level, hunter, spot) >= CLOSED_IN_DOORWAYS)
			.toList();
	}

	// Searches for a full path to the player, which leaves the hunter's navigation aimed at them.
	private static boolean searchFullPathTo(AbstractIllager hunter, ServerPlayer player) {
		Path path = hunter.getNavigation().createPath(player, 0);

		return path != null && path.canReach();
	}

	// Vanilla would walk each hunter with no path at the player; routing walks it at the next doorway on its way. The
	// check is a snapshot, which catches some just through a doorway and choosing the next.
	private static void assertHeadForDoorways(
		GameTestHelper helper,
		ServerLevel level,
		ServerPlayer player,
		List<AbstractIllager> stillFar,
		BlockPos spot
	) {
		Map<AbstractIllager, Boolean> heading = stillFar.stream()
			.collect(Collectors.toMap(hunter -> hunter, hunter -> headsForNextDoorway(level, hunter, spot)));
		// Searched after reading where each walks, as searching aims it.
		List<AbstractIllager> withoutAPath = stillFar.stream().filter(hunter -> !searchFullPathTo(hunter, player)).toList();
		long headingForADoorway = withoutAPath.stream().filter(heading::get).count();

		helper.assertFalse(withoutAPath.isEmpty(), "far hunters with no path to check");
		helper.assertTrue(
			headingForADoorway * 3 >= withoutAPath.size(),
			headingForADoorway + " of " + withoutAPath.size() + " far hunters with no path head for the next doorway on their way"
		);
	}

	// Builds a Stronghold of just the piece, and returns the Illagers it placed.
	private static List<AbstractIllager> buildAlone(ServerLevel level, StructurePiece piece) {
		Structure stronghold = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.STRONGHOLD).value();
		BoundingBox box = piece.getBoundingBox();
		StructureStart start = new StructureStart(stronghold, ChunkPos.containing(box.getCenter()), 0, new PiecesContainer(List.of(piece)));

		Structures.chunks(start).forEach(chunk -> Structures.build(level, start, chunk));

		return level.getEntitiesOfClass(AbstractIllager.class, AABB.of(box));
	}

	// Whether the position is in one of the piece's own columns from (minX, minZ) to (maxX, maxZ).
	private static boolean inLocalColumns(StructurePiece piece, BlockPos pos, int minX, int maxX, int minZ, int maxZ) {
		StructurePieceInvoker local = (StructurePieceInvoker) piece;

		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				if (local.worldX(x, z) == pos.getX() && local.worldZ(x, z) == pos.getZ()) return true;
			}
		}

		return false;
	}

	private static List<BlockPos> doorwayBlocks(ServerLevel level, BoundingBox box) {
		return BlockPos.betweenClosedStream(box)
			.filter(pos -> level.getBlockState(pos).getBlock() instanceof DoorBlock || level.getBlockState(pos).getBlock() instanceof ButtonBlock)
			.map(BlockPos::immutable)
			.toList();
	}

	private static ChunkPos farChunk(int row) {
		return new ChunkPos(FAR_CHUNK, FAR_CHUNK + row * CHUNKS_APART);
	}

	private static StructurePiece onlyPiece(StructureStart start, StructurePieceType type) {
		List<StructurePiece> pieces = start.getPieces().stream().filter(piece -> piece.getType() == type).toList();

		if (pieces.size() != 1) throw new IllegalStateException(pieces.size() + " pieces of type " + type);

		return pieces.getFirst();
	}

	private static List<AbstractIllager> illagersIn(ServerLevel level, StructureStart start) {
		return level.getEntitiesOfClass(
			AbstractIllager.class,
			AABB.of(start.getBoundingBox()),
			illager -> level.structureManager().structureHasPieceAt(illager.blockPosition(), start)
		);
	}

	private static long countOf(List<AbstractIllager> illagers, EntityType<?> type) {
		return illagers.stream().filter(illager -> illager.getType() == type).count();
	}

	private static RestoreSpot current(ServerLevel level, RestoreSpot spot) {
		return RestoreSpots.in(level).stream().filter(saved -> saved.id().equals(spot.id())).findFirst().orElseThrow();
	}
}
