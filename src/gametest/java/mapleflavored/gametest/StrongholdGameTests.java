package mapleflavored.gametest;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.GearKind;
import mapleflavored.bloodmoons.Restorable;
import mapleflavored.bloodmoons.RestoreSpot;
import mapleflavored.bloodmoons.Restoring;
import mapleflavored.bloodmoons.RestoringAccess;
import mapleflavored.mixin.StructurePieceInvoker;
import mapleflavored.mobs.GoneForGood;
import mapleflavored.strongholds.StrongholdIllagers;
import mapleflavored.strongholds.StrongholdLayout;
import mapleflavored.strongholds.StrongholdRouteGoal;
import mapleflavored.strongholds.StrongholdRoutes;
import mapleflavored.structurebooks.BookLocks;
import mapleflavored.structurebooks.BookSpot;
import mapleflavored.structurebooks.IllagerAlert;
import mapleflavored.structurebooks.MobKeepers;
import mapleflavored.structurebooks.StructureBookPiece;
import mapleflavored.structurebooks.StructureBookRules;
import mapleflavored.structurebooks.StructureBooks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
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
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Strongholds generate whole and full of Illagers, each on a spot a Blood Moon Restores. Their Unbreaking book floats
 * over the End portal, and taking it sets every Illager in the Stronghold hunting the player, Orichalcum or not.
 */
public class StrongholdGameTests {
	private static final int LONE_ROOMS_Y = 100;
	// Far enough apart that no two rooms touch, and near enough that they share the chunks loaded around them.
	private static final int LONE_ROOM_CHUNKS_APART = 3;
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
	private static final int KEEPER_PLANS = 8;
	// The kinds of piece vanilla builds a Stronghold from in this version.
	private static final int STRONGHOLD_PIECE_KINDS = 13;
	// Vanilla plans a five-way crossing's side rooms this far up its side walls and this far along them, from its lowest
	// corner. Facing north or west it swaps the two heights (StrongholdPieces.FiveCrossing.addChildren).
	private static final int LOW_SIDE_ROOM_UP = 3;
	private static final int HIGH_SIDE_ROOM_UP = 5;
	private static final int LOW_SIDE_ROOM_ALONG = 1;
	private static final int HIGH_SIDE_ROOM_ALONG = 7;
	// Every way a crossing can have or lack each of its four side rooms.
	private static final int SIDE_ROOM_PLANS = 16;
	private static final String ATTACHMENTS = "fabric:attachments";
	private static final Set<StructurePieceType> BIG_ROOMS = Set.of(
		StructurePieceType.STRONGHOLD_LIBRARY,
		StructurePieceType.STRONGHOLD_ROOM_CROSSING,
		StructurePieceType.STRONGHOLD_PRISON_HALL
	);
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
		StructureStart start = Structures.place(level, BuiltinStructures.STRONGHOLD, TestSites.of(helper).chunk(0));
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
		StructureStart start = Structures.place(level, BuiltinStructures.STRONGHOLD, TestSites.of(helper).chunk(0));
		List<AbstractIllager> guards = illagersIn(level, start);
		StructurePiece portalRoom = onlyPiece(start, StructurePieceType.STRONGHOLD_PORTAL_ROOM);
		StructurePiece staircase = onlyPiece(start, StructurePieceType.STRONGHOLD_START);
		List<BlockPos> keeperRooms = Books.piece(start).keepers();

		helper.assertTrue(guards.size() >= 3, "the Stronghold has at least its portal room's guards, " + guards.size());

		for (AbstractIllager guard : guards) {
			String which = "the " + guard.getType().toShortString() + " at " + guard.blockPosition();
			Optional<RestoreSpot> spot = RestoringAccess.spotOf(level.getServer(), guard);

			helper.assertTrue(GUARDS.contains(guard.getType()), which + " is a Vindicator, Pillager or Evoker");
			helper.assertTrue(guard.isPersistenceRequired(), which + " is persistent");
			helper.assertFalse(guard instanceof Pillager pillager && pillager.isPatrolLeader(), which + " is a Captain");
			helper.assertTrue(spot.map(RestoreSpot::structure).equals(Optional.of(Restorable.STRONGHOLD)), which + " holds a Stronghold spot");
			helper.assertTrue(Restorable.STRONGHOLD.holds(level, guard.position()), which + " stands on the Stronghold's Restore blocks");
			helper.assertTrue(level.getBlockState(guard.blockPosition()).isAir() && level.getBlockState(guard.blockPosition().above()).isAir(), which + " has room");
			helper.assertFalse(staircase.getBoundingBox().isInside(guard.blockPosition()), which + " is on the starting staircase");
		}

		List<AbstractIllager> inPortalRoom = guards.stream().filter(guard -> portalRoom.getBoundingBox().isInside(guard.blockPosition())).toList();

		helper.assertValueEqual(countOf(inPortalRoom, EntityTypes.EVOKER), 1L, "Evokers in the portal room");
		helper.assertValueEqual(countOf(inPortalRoom, EntityTypes.VINDICATOR), 2L, "Vindicators in the portal room");
		helper.assertTrue(keeperRooms.size() <= 3, "at most three keeper rooms, " + keeperRooms.size());

		for (StructurePiece room : start.getPieces()) {
			BlockPos origin = MobKeepers.roomKey(room);
			long evokersInRoom = guards.stream()
				.filter(guard -> guard.getType() == EntityTypes.EVOKER)
				.filter(guard -> room.getBoundingBox().isInside(guard.blockPosition()))
				.count();

			boolean keeperRoom = keeperRooms.contains(origin);
			boolean library = room.getType() == StructurePieceType.STRONGHOLD_LIBRARY;

			helper.assertValueEqual(evokersInRoom, keeperRoom || library ? 1L : 0L, "Evokers in the " + room.getType() + " at " + origin);
		}

		for (AbstractIllager evoker : guards.stream().filter(guard -> guard.getType() == EntityTypes.EVOKER).toList()) {
			boolean inKeeperRoom = start.getPieces().stream()
				.filter(room -> room.getBoundingBox().isInside(evoker.blockPosition()))
				.anyMatch(room -> keeperRooms.contains(MobKeepers.roomKey(room)));

			helper.assertValueEqual(MobKeepers.keyOf(evoker).isPresent(), inKeeperRoom, "the Evoker at " + evoker.blockPosition() + " is a keeper");
		}
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void aPortalRoomKeepsItsFullGuardWhenAChunkHoldsOnlyItsLava(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Structure stronghold = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.STRONGHOLD).value();

		// Each room's own randomness comes from where it is, so many rooms try many first choices of column.
		for (int room = 0; room < LONE_ROOMS; room++) {
			BlockPos foot = loneRoomChunk(helper, room).getBlockAt(LAVA_CORNER_FOOT_X, LONE_ROOMS_Y, LAVA_CORNER_FOOT_Z);
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

	// Placing a room's guards is a call of its own, after the room is built, with nothing around it naming the keeper.
	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void placingAKeeperRoomsGuardsTagsItsEvokerAsTheRoomsKeeper(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos foot = loneRoomChunk(helper, 0).getBlockAt(0, LONE_ROOMS_Y, 0);
		BoundingBox box = BoundingBox.orientBox(foot.getX(), foot.getY(), foot.getZ(), -4, -1, 0, 14, 6, 15, Direction.SOUTH);
		StructurePiece library = new StrongholdPieces.Library(0, RandomSource.create(0L), box, Direction.SOUTH);
		List<StructurePiece> pieces = withBookKeptBy(library);
		BoundingBox wholeRoom = new BoundingBox(box.minX(), level.getMinY(), box.minZ(), box.maxX(), level.getMaxY(), box.maxZ());

		buildAlone(level, pieces).forEach(AbstractIllager::discard);
		// As part of a Stronghold being built, which is the only time a piece gets guards.
		Restoring.build(Optional.of(Restorable.STRONGHOLD), () -> StrongholdIllagers.placeGuards(library, pieces, level, wholeRoom));

		List<AbstractIllager> evokers = level.getEntitiesOfClass(
			AbstractIllager.class, AABB.of(box), guard -> guard.isAlive() && guard.getType() == EntityTypes.EVOKER
		);

		helper.assertValueEqual(evokers.size(), 1, "Evokers placed in the keeper room");
		helper.assertValueEqual(MobKeepers.keyOf(evokers.getFirst()), Optional.of(MobKeepers.roomKey(library)), "the key the Evoker keeps");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void aLibrarysEvokerKeepsToItsWalkwayOrToTheBackOfAShortLibrary(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		for (int room = 0; room < LONE_ROOMS; room++) {
			boolean tall = room % 2 == 0;
			boolean keeperRoom = room % 4 < 2;
			Direction facing = FACINGS.get(room / 4 % FACINGS.size());
			BlockPos foot = loneRoomChunk(helper, room).getBlockAt(0, LONE_ROOMS_Y, 0);
			BoundingBox box = BoundingBox.orientBox(foot.getX(), foot.getY(), foot.getZ(), -4, -1, 0, 14, tall ? 11 : 6, 15, facing);
			StructurePiece library = new StrongholdPieces.Library(0, RandomSource.create(room), box, facing);
			List<AbstractIllager> guards = keeperRoom ? buildAloneAsKeeperRoom(level, library) : buildAlone(level, library);
			String which = (keeperRoom ? "the keeper " : "the ") + (tall ? "tall library" : "short library") + " facing " + facing + " at " + foot;
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
			BlockPos foot = loneRoomChunk(helper, room).getBlockAt(0, LONE_ROOMS_Y, 0);
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
		StructureStart start = Structures.place(level, BuiltinStructures.STRONGHOLD, TestSites.of(helper).chunk(0));

		for (StructurePiece piece : start.getPieces()) {
			List<BlockPos> doorways = doorwayBlocks(level, piece.getBoundingBox());

			helper.assertTrue(doorways.isEmpty(), "doors and buttons in the " + piece.getType() + ": " + doorways);
		}

		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void everyRoomHasARouteToThePortalRoomOneDoorwayAtATime(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = BastionBookGameTests.placeRegistered(level, Structures.plan(level, BuiltinStructures.STRONGHOLD, TestSites.of(helper).chunk(0)));
		BlockPos portalRoom = onlyPiece(start, StructurePieceType.STRONGHOLD_PORTAL_ROOM).getBoundingBox().getCenter();
		List<StructurePiece> strongholdPieces = start.getPieces().stream().filter(StrongholdLayout::isStrongholdPiece).toList();
		List<StructurePiece> rooms = strongholdPieces.stream().filter(piece -> piece.getType() != StructurePieceType.STRONGHOLD_PORTAL_ROOM).toList();

		for (StructurePiece room : rooms) {
			BlockPos from = room.getBoundingBox().getCenter();
			StrongholdRoutes.Route found = StrongholdRoutes.route(level, from, portalRoom);
			String which = "the route from the " + room.getType() + " at " + from;

			if (!(found instanceof StrongholdRoutes.Doorways(List<BlockPos> route))) throw helper.assertionException(which + " is " + found);

			helper.assertTrue(onlyPiece(start, StructurePieceType.STRONGHOLD_PORTAL_ROOM).getBoundingBox().isInside(route.getLast()), which + " ends in the portal room");

			for (int step = 0; step < route.size() - 1; step++) {
				int left = StrongholdRoutes.route(level, route.get(step), portalRoom).doorwaysToCross().orElse(LOST);

				helper.assertValueEqual(left, route.size() - step - 1, "doorways left after step " + step + " of " + which);
			}
		}

		BlockPos outside = start.getBoundingBox().getCenter().atY(level.getMaxY() - 1);

		helper.assertValueEqual(StrongholdRoutes.route(level, portalRoom, portalRoom), StrongholdRoutes.NoRoute.SAME_ROOM, "a route within one room");
		helper.assertValueEqual(StrongholdRoutes.route(level, outside, portalRoom), StrongholdRoutes.NoRoute.NOT_IN_A_STRONGHOLD, "a route from outside the Stronghold");
		helper.assertValueEqual(
			StrongholdRoutes.route(level, portalRoom, outside),
			StrongholdRoutes.NoRoute.NOT_IN_THE_SAME_STRONGHOLD,
			"a route to somewhere outside the Stronghold"
		);

		walledIn(level, onlyPiece(start, StructurePieceType.STRONGHOLD_PORTAL_ROOM));
		helper.assertValueEqual(
			StrongholdRoutes.route(level, rooms.getFirst().getBoundingBox().getCenter(), portalRoom),
			StrongholdRoutes.NoRoute.NO_OPEN_ROUTE,
			"a route to a portal room with every doorway walled up"
		);
		helper.succeed();
	}

	// Vanilla opens the side doorways of a crossing facing north or west where its other side rooms would be. A whole
	// Stronghold only shows that when it happens to plan such a crossing, so each facing is built here with every plan.
	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void aFiveWayCrossingOpensItsSideDoorwaysWhereItsSideRoomsAreInEveryFacing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		for (Direction facing : FACINGS) {
			for (int plan = 0; plan < SIDE_ROOM_PLANS; plan++) {
				SideRooms sideRooms = SideRooms.plan(plan);
				BlockPos foot = loneRoomChunk(helper, FACINGS.indexOf(facing) * SIDE_ROOM_PLANS + plan).getBlockAt(0, LONE_ROOMS_Y, 0);
				BoundingBox box = BoundingBox.orientBox(foot.getX(), foot.getY(), foot.getZ(), -4, -3, 0, 10, 9, 11, facing);
				StructurePiece crossing = fiveCrossing(level, box, facing, sideRooms);

				buildAlone(level, crossing);

				for (boolean right : List.of(false, true)) {
					for (boolean high : List.of(false, true)) {
						BlockPos doorway = sideDoorway(crossing, right, high);
						String which = "the " + (right ? "right " : "left ") + (high ? "high" : "low") + " side doorway at " + doorway
							+ " of the crossing facing " + facing + " with " + sideRooms;

						helper.assertValueEqual(level.getBlockState(doorway).isAir(), sideRooms.has(right, high), which + " is open");
					}
				}
			}
		}

		helper.succeed();
	}

	@GameTest
	public void everyKindOfPieceVanillaBuildsAStrongholdFromIsAStrongholdPiece(GameTestHelper helper) {
		int kinds = 0;

		for (Field field : StructurePieceType.class.getFields()) {
			if (!field.getName().startsWith("STRONGHOLD_")) continue;

			kinds++;
			helper.assertTrue(StrongholdLayout.isStrongholdPiece(pieceType(field)), field.getName() + " is a Stronghold piece");
		}

		helper.assertValueEqual(kinds, STRONGHOLD_PIECE_KINDS, "kinds of Stronghold piece vanilla has");
		helper.assertFalse(StrongholdLayout.isStrongholdPiece(StructurePieceType.MINE_SHAFT_CORRIDOR), "a Mineshaft corridor is a Stronghold piece");
		helper.succeed();
	}

	// Every Illager that closes in to attack has the route goal, in a Stronghold or not.
	@GameTest(environment = "maple_flavored_gametest:normal")
	public void aHunterOutsideAStrongholdSearchesForNoPath(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = MockPlayers.survivalPlayer(helper);
		AbstractIllager hunter = huntingVindicator(helper, player, helper.absolutePos(new BlockPos(1, 1, 1)));

		player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 1, 4))));
		hunter.setTarget(player);
		// A mob in the air searches for no path at all.
		hunter.setOnGround(true);

		helper.assertTrue(IllagerAlert.isHunting(hunter), "the Vindicator is hunting");
		helper.assertValueEqual(
			StrongholdRoutes.route(level, hunter.blockPosition(), player.blockPosition()),
			StrongholdRoutes.NoRoute.NOT_IN_A_STRONGHOLD,
			"the route for a hunter outside any Stronghold"
		);
		helper.assertFalse(new StrongholdRouteGoal(hunter).canUse(), "the route goal runs outside a Stronghold");
		helper.assertTrue(hunter.getNavigation().getTargetPos() == null, "the route goal searched for a path to " + hunter.getNavigation().getTargetPos());

		// A search would have shown: it leaves the navigation aimed at what it searched for.
		searchFullPathTo(hunter, player);
		helper.assertTrue(hunter.getNavigation().getTargetPos() != null, "a search for a path leaves the navigation aimed");
		helper.succeed();
	}

	// A keeper dropped from its save leaves no death to count, as an Evoker's chunk loading in Peaceful does.
	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void aKeeperEvokerWhoseSaveFailsToLoadUnlocksItsKey(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = BastionBookGameTests.placeRegistered(level, Structures.plan(level, BuiltinStructures.STRONGHOLD, TestSites.of(helper).chunk(0)));
		BlockPos book = Books.only(helper, start);
		Mob keeper = MobKeepers.liveKeepers(level, book).getFirst();
		BlockPos key = MobKeepers.keyOf(keeper).orElseThrow();
		ValueInput savedKeeper = saved(level, keeper);

		keeper.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
		helper.assertTrue(BookLocks.remainingKeys(level, book).contains(key), "a keeper that only unloaded still locks its key");
		GoneForGood.failedToLoad(level, savedKeeper);

		helper.startSequence()
			.thenExecuteAfter(1, () -> helper.assertFalse(BookLocks.remainingKeys(level, book).contains(key), "a keeper that failed to load still locks its key"))
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = CHASE_TICKS + 200)
	public void huntersAcrossTheStrongholdCloseInRoomByRoom(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = BastionBookGameTests.placeRegistered(level, Structures.plan(level, BuiltinStructures.STRONGHOLD, TestSites.of(helper).chunk(0)));
		BlockPos book = Books.only(helper, start);
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
		ForcedChunks.keepLoaded(helper, level, Structures.chunks(start));

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

				helper.assertTrue(closedIn * 4 >= farHunters.size() * 3, closedIn + " of " + farHunters.size() + " far hunters came " + CLOSED_IN_DOORWAYS + " doorways closer");
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void theBookIsLockedUntilEveryEvokerIsDead(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = BastionBookGameTests.placeRegistered(level, Structures.plan(level, BuiltinStructures.STRONGHOLD, TestSites.of(helper).chunk(0)));
		BlockPos book = Books.only(helper, start);
		List<BlockPos> keys = BookLocks.lockAt(level, book).map(BookLocks.Lock::keys).orElse(List.of());
		List<Mob> keepers = MobKeepers.liveKeepers(level, book);
		long bigRooms = start.getPieces().stream().filter(piece -> BIG_ROOMS.contains(piece.getType())).count();
		ServerPlayer player = MockPlayers.survivalPlayer(helper);

		player.setPos(Vec3.atBottomCenterOf(book.below()));

		helper.assertValueEqual(BookLocks.lockAt(level, book).map(BookLocks.Lock::kind), Optional.of(BookLocks.Kind.EVOKERS), "the lock's kind");
		helper.assertValueEqual((long) keys.size(), 1 + Math.min(2, bigRooms), "keys, one for the portal room and two for far big rooms");
		helper.assertValueEqual(keepers.size(), keys.size(), "keepers alive");
		helper.assertTrue(keepers.stream().allMatch(keeper -> keeper.getType() == EntityTypes.EVOKER), "every keeper is an Evoker");
		helper.assertValueEqual(
			keepers.stream().map(keeper -> MobKeepers.keyOf(keeper).orElseThrow()).collect(Collectors.toSet()),
			Set.copyOf(keys),
			"the keys of the Evokers tagged as keepers"
		);

		for (Mob keeper : keepers) {
			helper.assertFalse(StructureBooks.take(player, book), "the book is taken with " + BookLocks.remainingKeys(level, book).size() + " Evokers alive");
			helper.assertTrue(keeper.hasEffect(MobEffects.GLOWING), "a refused take makes the Evoker at " + keeper.blockPosition() + " glow");
			keeper.kill(level);
		}

		helper.assertTrue(StructureBooks.take(player, book), "the book is taken once every Evoker is dead");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void theFarKeepersAreTheBigRoomsFarthestFromThePortalRoomAndEachOther(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		for (int plan = 0; plan < KEEPER_PLANS; plan++) {
			StructureStart start = Structures.plan(level, BuiltinStructures.STRONGHOLD, TestSites.of(helper).chunk(plan));
			List<StructurePiece> rooms = start.getPieces().stream().filter(StrongholdLayout::isStrongholdPiece).toList();
			List<StructurePiece> bigRooms = rooms.stream().filter(room -> BIG_ROOMS.contains(room.getType())).toList();
			List<BlockPos> keys = Books.piece(start).keepers();
			StructurePiece portalRoom = onlyPiece(start, StructurePieceType.STRONGHOLD_PORTAL_ROOM);
			List<StructurePiece> keepers = keys.stream().map(key -> roomWithOrigin(rooms, key)).toList();
			String which = "the Stronghold at " + start.getChunkPos();

			helper.assertTrue(keepers.getFirst() == portalRoom, "the first keeper room of " + which + " is the portal room");
			helper.assertValueEqual(keepers.size(), 1 + Math.min(2, bigRooms.size()), "keeper rooms in " + which);

			for (int keeper = 1; keeper < keepers.size(); keeper++) {
				List<Map<StructurePiece, Integer>> fromEarlier = keepers.subList(0, keeper).stream()
					.map(earlier -> StrongholdRoutes.roomsApart(rooms, earlier))
					.toList();
				List<StructurePiece> chosenBefore = keepers.subList(0, keeper);
				int farthest = bigRooms.stream()
					.filter(room -> !chosenBefore.contains(room))
					.mapToInt(room -> nearestOf(fromEarlier, room))
					.max()
					.orElseThrow();

				helper.assertTrue(BIG_ROOMS.contains(keepers.get(keeper).getType()), "keeper room " + keeper + " of " + which + " is a big room");
				helper.assertValueEqual(nearestOf(fromEarlier, keepers.get(keeper)), farthest, "rooms from keeper room " + keeper + " of " + which + " to the nearest keeper before it");
			}
		}

		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void takingTheBookTurnsEveryIllagerInTheStrongholdOnAnOrichalcumWearer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = BastionBookGameTests.placeRegistered(level, Structures.plan(level, BuiltinStructures.STRONGHOLD, TestSites.of(helper).chunk(0)));
		BlockPos book = Books.only(helper, start);
		StructurePiece portalRoom = onlyPiece(start, StructurePieceType.STRONGHOLD_PORTAL_ROOM);
		ServerPlayer player = MockPlayers.survivalPlayer(helper);

		killKeepers(level, book);

		List<AbstractIllager> guards = illagersIn(level, start).stream().filter(AbstractIllager::isAlive).toList();

		helper.assertFalse(guards.isEmpty(), "Illagers are left once the Evokers are dead");
		player.setItemSlot(EquipmentSlot.FEET, new ItemStack(AlloyGear.of(Alloy.ORICHALCUM, GearKind.BOOTS)));
		player.setPos(Vec3.atBottomCenterOf(book.below()));

		helper.assertValueEqual(Books.enchantmentAt(level, book), Enchantments.UNBREAKING, "the book's enchantment");
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
		StructureStart start = Structures.place(level, BuiltinStructures.STRONGHOLD, TestSites.of(helper).chunk(0));
		AbstractIllager guard = illagersIn(level, start).getFirst();
		RestoreSpot spot = Spots.of(helper, guard);
		ChunkPos chunk = ChunkPos.containing(guard.blockPosition());

		ForcedChunks.keepLoaded(helper, level, List.of(chunk));
		guard.kill(level);
		Spots.riseBloodMoon(helper);

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
			})
			.thenSucceed();
	}

	private static StructurePieceType pieceType(Field field) {
		try {
			return (StructurePieceType) field.get(null);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	// The chunk a room built on its own starts in: one of a line of chunks running from the test's first site.
	private static ChunkPos loneRoomChunk(GameTestHelper helper, int room) {
		ChunkPos first = TestSites.of(helper).chunk(0);

		return new ChunkPos(first.x(), first.z() + room * LONE_ROOM_CHUNKS_APART);
	}

	// The fewest rooms from the room to any of the rooms the distances are from.
	private static int nearestOf(List<Map<StructurePiece, Integer>> roomsApart, StructurePiece room) {
		return roomsApart.stream().mapToInt(apart -> apart.getOrDefault(room, Integer.MAX_VALUE)).min().orElseThrow();
	}

	private static StructurePiece roomWithOrigin(List<StructurePiece> rooms, BlockPos origin) {
		return rooms.stream()
			.filter(room -> MobKeepers.roomKey(room).equals(origin))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("no room at " + origin));
	}

	private static void killKeepers(ServerLevel level, BlockPos book) {
		MobKeepers.liveKeepers(level, book).forEach(keeper -> keeper.kill(level));
	}

	// How many doorways the hunter must cross to reach the spot: none in the same room, and LOST with no route there.
	private static int doorwaysBetween(ServerLevel level, AbstractIllager hunter, BlockPos spot) {
		return StrongholdRoutes.route(level, hunter.blockPosition(), spot).doorwaysToCross().orElse(LOST);
	}

	// Whether the hunter is walking to the doorway into the next room on its way to the spot.
	private static boolean headsForNextDoorway(ServerLevel level, AbstractIllager hunter, BlockPos spot) {
		BlockPos walkingTo = hunter.getNavigation().getTargetPos();

		return StrongholdRoutes.route(level, hunter.blockPosition(), spot) instanceof StrongholdRoutes.Doorways route
			&& walkingTo != null
			&& walkingTo.distManhattan(route.next()) <= 1;
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
		return buildAlone(level, List.of(piece));
	}

	// Builds a Stronghold of just the room, as one of its keeper rooms, with a book floating above it. Only a keeper
	// room's Evoker is a keeper, which this checks doesn't change where a library's stands.
	private static List<AbstractIllager> buildAloneAsKeeperRoom(ServerLevel level, StructurePiece room) {
		return buildAlone(level, withBookKeptBy(room));
	}

	// The room and a book floating above it that the room's Evoker keeps.
	private static List<StructurePiece> withBookKeptBy(StructurePiece room) {
		BoundingBox box = room.getBoundingBox();
		BookSpot aboveTheRoom = new BookSpot(room, new BlockPos(box.getCenter().getX(), box.maxY() + 3, box.getCenter().getZ()));
		BookLocks.Keys lock = new BookLocks.Keys(BookLocks.Kind.EVOKERS, List.of(MobKeepers.roomKey(room)));

		return List.of(room, new StructureBookPiece(List.of(room), aboveTheRoom, Enchantments.UNBREAKING, Optional.of(lock)));
	}

	private static List<AbstractIllager> buildAlone(ServerLevel level, List<StructurePiece> pieces) {
		Structure stronghold = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.STRONGHOLD).value();
		BoundingBox box = pieces.getFirst().getBoundingBox();
		StructureStart start = new StructureStart(stronghold, ChunkPos.containing(box.getCenter()), 0, new PiecesContainer(pieces));

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
		return Spots.current(level, spot);
	}

	// Bricks up the whole shell of the room, doorways and all.
	private static void walledIn(ServerLevel level, StructurePiece room) {
		BoundingBox box = room.getBoundingBox();

		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			boolean shell = pos.getX() == box.minX() || pos.getX() == box.maxX() || pos.getZ() == box.minZ() || pos.getZ() == box.maxZ();

			if (shell) level.setBlockAndUpdate(pos, Blocks.STONE_BRICKS.defaultBlockState());
		}
	}

	// A five-way crossing with exactly the side rooms asked for. Vanilla draws them at random, and saves them.
	private static StructurePiece fiveCrossing(ServerLevel level, BoundingBox box, Direction facing, SideRooms sideRooms) {
		StructurePiece drawn = new StrongholdPieces.FiveCrossing(0, RandomSource.create(0L), box, facing);
		CompoundTag saved = drawn.createTag(StructurePieceSerializationContext.fromLevel(level));

		saved.putBoolean("leftLow", sideRooms.leftLow());
		saved.putBoolean("leftHigh", sideRooms.leftHigh());
		saved.putBoolean("rightLow", sideRooms.rightLow());
		saved.putBoolean("rightHigh", sideRooms.rightHigh());

		return new StrongholdPieces.FiveCrossing(saved);
	}

	// The middle of the doorway of a side room, in the crossing's wall, where vanilla plans the room: against the wall to
	// the crossing's left or right, that far up from its lowest corner and that far along the wall.
	private static BlockPos sideDoorway(StructurePiece crossing, boolean right, boolean high) {
		BoundingBox box = crossing.getBoundingBox();
		Direction facing = crossing.getOrientation();
		boolean swapsHeights = facing == Direction.NORTH || facing == Direction.WEST;
		int up = (high != swapsHeights ? HIGH_SIDE_ROOM_UP : LOW_SIDE_ROOM_UP) + 1;
		int along = (high ? HIGH_SIDE_ROOM_ALONG : LOW_SIDE_ROOM_ALONG) + 1;

		// A crossing facing north or south has its side rooms to the west and east; one facing west or east, to the north
		// and south.
		return facing.getAxis() == Direction.Axis.Z
			? new BlockPos(right ? box.maxX() : box.minX(), box.minY() + up, box.minZ() + along)
			: new BlockPos(box.minX() + along, box.minY() + up, right ? box.maxZ() : box.minZ());
	}

	// A Vindicator saved with a hunt for the player and loaded back, as one alerted by a taken book would be.
	private static AbstractIllager huntingVindicator(GameTestHelper helper, ServerPlayer quarry, BlockPos feet) {
		ServerLevel level = helper.getLevel();
		Mob vindicator = EntityTypes.VINDICATOR.create(level, EntitySpawnReason.STRUCTURE);
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		CompoundTag hunt = new CompoundTag();
		CompoundTag attachments = new CompoundTag();

		vindicator.snapTo(Vec3.atBottomCenterOf(feet), 0.0F, 0.0F);
		vindicator.save(output);

		CompoundTag saved = output.buildResult();

		hunt.putIntArray("quarry", UUIDUtil.uuidToIntArray(quarry.getUUID()));
		hunt.putLong("until", level.getGameTime() + CHASE_TICKS);
		hunt.putInt("quarry_deaths", 0);
		attachments.put(Hunts.ILLAGER_HUNT.toString(), hunt);
		saved.put(ATTACHMENTS, attachments);

		Entity loaded = EntityType.loadEntityRecursive(
			TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved),
			level,
			EntitySpawnReason.LOAD,
			entity -> entity
		);

		helper.assertTrue(loaded instanceof AbstractIllager && level.addFreshEntity(loaded), "the hunting Vindicator loads");

		return (AbstractIllager) loaded;
	}

	// The mob as its chunk would save it.
	private static ValueInput saved(ServerLevel level, Mob mob) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());

		mob.save(output);

		return TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult());
	}

	// Which of its four side rooms a five-way crossing has.
	private record SideRooms(boolean leftLow, boolean leftHigh, boolean rightLow, boolean rightHigh) {
		// Each of the 16 plans, by number.
		static SideRooms plan(int plan) {
			return new SideRooms((plan & 1) != 0, (plan & 2) != 0, (plan & 4) != 0, (plan & 8) != 0);
		}

		boolean has(boolean right, boolean high) {
			return right ? (high ? rightHigh : rightLow) : (high ? leftHigh : leftLow);
		}
	}
}
