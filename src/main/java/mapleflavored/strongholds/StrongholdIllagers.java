package mapleflavored.strongholds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import mapleflavored.bloodmoons.Restorable;
import mapleflavored.bloodmoons.StructureMobs;
import mapleflavored.mixin.StructurePieceInvoker;
import mapleflavored.structurebooks.MobKeepers;
import mapleflavored.structurebooks.StructureBookRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.phys.Vec3;

/**
 * The Illagers that guard a Stronghold: Vindicators, Pillagers and Evokers, a fixed garrison per piece, placed as each
 * piece is built. Each stands where a Blood Moon can Restore it, on the Stronghold's Restore blocks with a clear 1×2×1
 * space, inside its piece's walls, so never in a doorway. A library's Evoker keeps to its upper walkway, or to the back
 * of a library without one, and a prison hall's guard to the hall, outside the cells. {@link StrongholdRooms} says which
 * guards each kind of piece has.
 *
 * <p>The Evokers of three keeper rooms keep the Stronghold's book: the portal room,
 * and two big rooms, libraries, room crossings or prison halls, chosen when the Stronghold is planned to draw the player
 * out to its edges. Each is the big room the most rooms away from the portal room and the keeper rooms already chosen.
 * Every library has an Evoker, a keeper or not; a room crossing or prison hall has one only as a keeper room.
 *
 * <p>A piece's garrison and where each guard stands are drawn from the world seed and the piece's box alone, so the
 * piece places the same guards whichever of its chunks is built first. Each guard's first choice of column is floor the
 * piece's own layout leaves open, never lava, the End portal or bookshelves. The guard keeps to that column's chunk and
 * stands in the first column of its area in the chunk with room for it, which the first choice nearly always has. A guard
 * with no room, as where a cave or another structure got in first, is left out.
 */
public final class StrongholdIllagers {
	// Seeds the garrison apart from the Stronghold's own randomness, so adding it changes no layout.
	private static final int GARRISON_SALT = 0x5711A6;

	private static final int FAR_KEEPERS = 2;

	private StrongholdIllagers() {
	}

	/** Has every Stronghold's book locked by the Evokers of its keeper rooms. Call once from mod init. */
	public static void register() {
		StructureBookRules.chooseStrongholdKeeperRoomsWith(StrongholdIllagers::keeperRooms);
	}

	/**
	 * The rooms whose Evokers keep the Stronghold's book, the portal room first. None for a Stronghold without a portal
	 * room. Ties between equally far rooms are drawn from {@code random}.
	 */
	static List<StructurePiece> keeperRooms(List<StructurePiece> pieces, RandomSource random) {
		Optional<StructurePiece> portalRoom = pieces.stream().filter(StrongholdRooms::isPortalRoom).findFirst();

		if (portalRoom.isEmpty()) return List.of();

		List<StructurePiece> rooms = pieces.stream().filter(StrongholdLayout::isStrongholdPiece).toList();
		List<StructurePiece> bigRooms = new ArrayList<>(rooms.stream().filter(StrongholdIllagers::canBeFarKeeperRoom).toList());
		List<Map<StructurePiece, Integer>> roomsFromKeepers = new ArrayList<>(List.of(StrongholdRoutes.roomsApart(rooms, portalRoom.get())));
		List<StructurePiece> keepers = new ArrayList<>(List.of(portalRoom.get()));

		Util.shuffle(bigRooms, random);

		for (int keeper = 0; keeper < FAR_KEEPERS; keeper++) {
			Optional<StructurePiece> farthest = bigRooms.stream()
				.filter(room -> !keepers.contains(room))
				.filter(room -> roomsFromKeepers.stream().allMatch(apart -> apart.containsKey(room)))
				.max(Comparator.comparingInt(room -> roomsFromKeepers.stream().mapToInt(apart -> apart.get(room)).min().orElseThrow()));

			if (farthest.isEmpty()) break;

			keepers.add(farthest.get());
			roomsFromKeepers.add(StrongholdRoutes.roomsApart(rooms, farthest.get()));
		}

		return List.copyOf(keepers);
	}

	/**
	 * Places the guards of a Stronghold piece that stand in the chunk being built, tagging a keeper room's Evoker as the
	 * keeper the book names for that room. Call after every structure's piece is built in each chunk; it does nothing
	 * unless a {@linkplain StrongholdLayout#isBuildingStronghold Stronghold is being built}, nor for a piece without a
	 * garrison, such as the Stronghold's starting staircase. A keeper Evoker with no room has its key unlocked.
	 *
	 * @param pieces the Stronghold's pieces, whose book names its keeper rooms
	 * @param chunkBox the part of the world the chunk build may change
	 */
	public static void placeGuards(StructurePiece piece, List<StructurePiece> pieces, WorldGenLevel level, BoundingBox chunkBox) {
		if (!StrongholdLayout.isBuildingStronghold()) return;

		Optional<MobKeepers.Keeper> roomsKeeper = MobKeepers.keeperPlacedBy(piece, pieces, level.getLevel().dimension());
		boolean keeperRoom = roomsKeeper.isPresent();
		Set<BlockPos> taken = new HashSet<>();

		for (PlannedGuard guard : plan(piece, keeperRoom, level.getSeed())) {
			if (!guard.standsIn(chunkBox)) continue;

			int lowestFeet = ((StructurePieceInvoker) piece).worldY(guard.lowestFeet());

			Optional<BlockPos> spot = guard.columns().stream()
				.flatMap(column -> standingSpot(level, piece.getBoundingBox(), column, lowestFeet).stream())
				.filter(open -> !taken.contains(open))
				.findFirst();
			Optional<MobKeepers.Keeper> keeper = roomsKeeper.filter(unused -> guard.type() == EntityTypes.EVOKER);
			Optional<Mob> mob = spot.flatMap(feet -> StructureMobs.setUp(level, guard.type(), Vec3.atBottomCenterOf(feet), true));

			if (mob.isEmpty()) {
				keeper.ifPresent(MobKeepers.Keeper::forgive);

				continue;
			}

			taken.add(spot.get());
			keeper.ifPresent(tag -> tag.tag(mob.get()));
			level.addFreshEntityWithPassengers(mob.get());
		}
	}

	/**
	 * The guards the piece places, each with the columns it may stand in, best first. None for a piece without a garrison.
	 *
	 * @param keeperRoom whether it's one of the Stronghold's keeper rooms, which gives a big room its Evoker
	 */
	static List<PlannedGuard> plan(StructurePiece piece, boolean keeperRoom, long seed) {
		Optional<StrongholdRooms.Room> room = StrongholdRooms.of(piece);

		if (room.isEmpty()) return List.of();

		StrongholdRooms.Garrison garrison = room.get().garrison(piece, keeperRoom);
		BoundingBox box = piece.getBoundingBox();
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));

		random.setLargeFeatureWithSalt(seed, box.minX(), box.minZ(), GARRISON_SALT + box.minY());

		if (random.nextFloat() >= garrison.chance()) return List.of();

		Set<Column> firstChoices = new HashSet<>();
		List<PlannedGuard> guards = new ArrayList<>();

		for (StrongholdRooms.Post post : garrison.posts()) {
			EntityType<? extends Mob> type = post.guard().roll(random);
			List<Column> columns = new ArrayList<>(openColumns(piece, post.area()));

			Util.shuffle(columns, random);
			columns.stream().filter(column -> !firstChoices.contains(column)).findFirst().ifPresent(first -> {
				firstChoices.add(first);
				guards.add(new PlannedGuard(type, columnsNear(columns, first), post.area().lowestFeet()));
			});
		}

		return guards;
	}

	private static boolean canBeFarKeeperRoom(StructurePiece piece) {
		return StrongholdRooms.of(piece).filter(StrongholdRooms.Room::canBeFarKeeperRoom).isPresent();
	}

	// The guard's first choice, then every other column in the same chunk, in shuffled order.
	private static List<Column> columnsNear(List<Column> shuffled, Column first) {
		int firstIndex = shuffled.indexOf(first);
		List<Column> rest = new ArrayList<>(shuffled.subList(firstIndex + 1, shuffled.size()));

		rest.addAll(shuffled.subList(0, firstIndex));

		List<Column> near = new ArrayList<>(List.of(first));

		rest.stream().filter(column -> column.sameChunkAs(first)).forEach(near::add);

		return near;
	}

	// Every column inside the piece's walls that's in the area.
	private static List<Column> openColumns(StructurePiece piece, StrongholdRooms.Area area) {
		BoundingBox box = piece.getBoundingBox();
		boolean alongX = piece.getOrientation() == null || piece.getOrientation().getAxis() == Direction.Axis.Z;
		int width = alongX ? box.getXSpan() : box.getZSpan();
		int depth = alongX ? box.getZSpan() : box.getXSpan();
		StructurePieceInvoker world = (StructurePieceInvoker) piece;
		List<Column> columns = new ArrayList<>();

		for (int x = 1; x < width - 1; x++) {
			for (int z = 1; z < depth - 1; z++) {
				if (area.columns().test(x, z)) columns.add(new Column(world.worldX(x, z), world.worldZ(x, z)));
			}
		}

		return columns;
	}

	// The lowest place in the column, from {@code lowestFeet} up inside the piece's walls, where a guard can stand and a
	// Blood Moon could Restore it.
	private static Optional<BlockPos> standingSpot(WorldGenLevel level, BoundingBox box, Column column, int lowestFeet) {
		for (int y = lowestFeet; y + 1 < box.maxY(); y++) {
			BlockPos feet = new BlockPos(column.x(), y, column.z());
			boolean roomToStand = level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir();
			boolean onFloor = !level.getBlockState(feet.below()).isAir();

			if (roomToStand && onFloor && Restorable.STRONGHOLD.holds(level, Vec3.atBottomCenterOf(feet))) return Optional.of(feet);
		}

		return Optional.empty();
	}

	/**
	 * One guard a piece places.
	 *
	 * @param columns the columns it may stand in, best first, all in one chunk
	 * @param lowestFeet the lowest piece-local height its feet may stand at
	 */
	record PlannedGuard(EntityType<? extends Mob> type, List<Column> columns, int lowestFeet) {
		boolean standsIn(BoundingBox chunkBox) {
			Column first = columns.getFirst();

			return chunkBox.isInside(first.x(), chunkBox.minY(), first.z());
		}
	}

	record Column(int x, int z) {
		boolean sameChunkAs(Column other) {
			return SectionPos.blockToSectionCoord(x) == SectionPos.blockToSectionCoord(other.x)
				&& SectionPos.blockToSectionCoord(z) == SectionPos.blockToSectionCoord(other.z);
		}
	}
}
