package mapleflavored.gametest;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.GearKind;
import mapleflavored.bloodmoons.BloodMoons;
import mapleflavored.bloodmoons.Restorable;
import mapleflavored.bloodmoons.RestoreSpot;
import mapleflavored.bloodmoons.RestoreSpots;
import mapleflavored.bloodmoons.Restoring;
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
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;
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
	private static final TagKey<Block> STONE_BRICKS_FAMILY = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("maple_flavored", "stronghold_restore_blocks"));
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
					level.getBlockState(corner).is(STONE_BRICKS_FAMILY),
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
			helper.assertTrue(Restorable.STRONGHOLD.holds(level, guard.position()), which + " stands on the stone bricks family");
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
	public void takingTheBookTurnsEveryIllagerInTheStrongholdOnAnOrichalcumWearer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = BastionBookGameTests.placeRegistered(level, Structures.plan(level, BuiltinStructures.STRONGHOLD, farChunk(2)));
		BlockPos book = BastionBookGameTests.onlyBook(helper, start);
		StructurePiece portalRoom = onlyPiece(start, StructurePieceType.STRONGHOLD_PORTAL_ROOM);
		ServerPlayer player = MockPlayers.survivalPlayer(helper);
		List<AbstractIllager> guards = illagersIn(level, start);

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
