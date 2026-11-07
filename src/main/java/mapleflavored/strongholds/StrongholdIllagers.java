package mapleflavored.strongholds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import mapleflavored.bloodmoons.Restorable;
import mapleflavored.mixin.StructurePieceInvoker;
import mapleflavored.structurebooks.MobKeepers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.phys.Vec3;

/**
 * The Illagers that guard a Stronghold: Vindicators, Pillagers and Evokers, a fixed garrison per piece, placed as each
 * piece is built. Each stands where a Blood Moon can Restore it, on the Stronghold's Restore blocks with a clear 1×2×1
 * space, inside its piece's walls, so never in a doorway. A library's Evoker keeps to its upper walkway, or to the back
 * of a library without one, and a prison hall's guard to the hall, outside the cells. Every Evoker is a keeper of the
 * Stronghold's book, keyed by the origin of the piece that placed it.
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

	private static final float CORRIDOR_CHANCE = 0.25F;
	// The pieces that place an Evoker.
	private static final Set<StructurePieceType> KEEPER_PIECES = Set.of(StructurePieceType.STRONGHOLD_PORTAL_ROOM, StructurePieceType.STRONGHOLD_LIBRARY);
	// Vanilla's test for a library with an upper walkway.
	private static final int SHORT_LIBRARY_HEIGHT = 6;

	// The ground floor of a piece: guards stand at the lowest open place in their column.
	private static final int GROUND = 1;
	// The feet of a guard on a tall library's upper walkway, over its oak plank floor.
	private static final int LIBRARY_WALKWAY = 6;

	private static final Area ANYWHERE = new Area((x, z) -> true, GROUND);
	private static final Area PORTAL_ROOM_FLOOR = new Area((x, z) -> !portalRoomBlocked(x, z), GROUND);
	private static final Area LIBRARY_FLOOR = new Area((x, z) -> !libraryBlocked(x, z), GROUND);
	private static final Area LIBRARY_BACK = new Area((x, z) -> !libraryBlocked(x, z) && z >= 10, GROUND);
	private static final Area LIBRARY_UPPER_WALKWAY = new Area(StrongholdIllagers::onLibraryWalkway, LIBRARY_WALKWAY);
	// The hall the cells open onto, west of their bars.
	private static final Area PRISON_HALL = new Area((x, z) -> x <= 3, GROUND);

	private static final Garrison CORRIDOR = new Garrison(List.of(new Post(Guard.ROLL, ANYWHERE)), CORRIDOR_CHANCE);

	private static final Map<StructurePieceType, Function<StructurePiece, Garrison>> GARRISONS = Map.ofEntries(
		always(
			StructurePieceType.STRONGHOLD_PORTAL_ROOM,
			new Post(Guard.EVOKER, PORTAL_ROOM_FLOOR),
			new Post(Guard.VINDICATOR, PORTAL_ROOM_FLOOR),
			new Post(Guard.VINDICATOR, PORTAL_ROOM_FLOOR)
		),
		Map.entry(StructurePieceType.STRONGHOLD_LIBRARY, StrongholdIllagers::libraryGarrison),
		always(StructurePieceType.STRONGHOLD_ROOM_CROSSING, new Post(Guard.ROLL, ANYWHERE), new Post(Guard.ROLL, ANYWHERE)),
		always(StructurePieceType.STRONGHOLD_PRISON_HALL, new Post(Guard.VINDICATOR, PRISON_HALL)),
		always(StructurePieceType.STRONGHOLD_FIVE_CROSSING, new Post(Guard.ROLL, ANYWHERE)),
		Map.entry(StructurePieceType.STRONGHOLD_CHEST_CORRIDOR, piece -> CORRIDOR),
		Map.entry(StructurePieceType.STRONGHOLD_FILLER_CORRIDOR, piece -> CORRIDOR),
		Map.entry(StructurePieceType.STRONGHOLD_STRAIGHT, piece -> CORRIDOR),
		Map.entry(StructurePieceType.STRONGHOLD_LEFT_TURN, piece -> CORRIDOR),
		Map.entry(StructurePieceType.STRONGHOLD_RIGHT_TURN, piece -> CORRIDOR),
		Map.entry(StructurePieceType.STRONGHOLD_STAIRS_DOWN, piece -> CORRIDOR),
		Map.entry(StructurePieceType.STRONGHOLD_STRAIGHT_STAIRS_DOWN, piece -> CORRIDOR)
	);

	private StrongholdIllagers() {
	}

	/**
	 * The keys of the Evokers that keep the Stronghold's book: the origins of the pieces that place one, the portal room
	 * and each library.
	 */
	public static List<BlockPos> keeperKeys(List<StructurePiece> pieces) {
		return pieces.stream().flatMap(piece -> keeperKey(piece).stream()).toList();
	}

	/** The key of the Evoker the piece places, if it's a piece that places one. */
	public static Optional<BlockPos> keeperKey(StructurePiece piece) {
		if (!KEEPER_PIECES.contains(piece.getType())) return Optional.empty();

		BoundingBox box = piece.getBoundingBox();

		return Optional.of(new BlockPos(box.minX(), box.minY(), box.minZ()));
	}

	/**
	 * Places the guards of a Stronghold piece that stand in the chunk being built, tagging a keeper Evoker. Call inside
	 * {@code MobKeepers.build}, after the piece is built in each chunk; it does nothing for any other piece, including the
	 * Stronghold's starting staircase. A keeper Evoker with no room has its key unlocked.
	 *
	 * @param chunkBox the part of the world the chunk build may change
	 */
	public static void placeGuards(StructurePiece piece, WorldGenLevel level, BoundingBox chunkBox) {
		Set<BlockPos> taken = new HashSet<>();

		for (PlannedGuard guard : plan(piece, level.getSeed())) {
			if (!guard.standsIn(chunkBox)) continue;

			int lowestFeet = ((StructurePieceInvoker) piece).worldY(guard.lowestFeet());

			Optional<BlockPos> spot = guard.columns().stream()
				.flatMap(column -> standingSpot(level, piece.getBoundingBox(), column, lowestFeet).stream())
				.filter(open -> !taken.contains(open))
				.findFirst();
			boolean keeper = guard.type() == EntityTypes.EVOKER;

			if (spot.isEmpty()) {
				if (keeper) MobKeepers.forgiveIfPlacing();

				continue;
			}

			taken.add(spot.get());
			spawn(level, guard.type(), spot.get(), keeper);
		}
	}

	/** The guards the piece places, each with the columns it may stand in, best first. None for a piece without a garrison. */
	static List<PlannedGuard> plan(StructurePiece piece, long seed) {
		Function<StructurePiece, Garrison> garrisonOf = GARRISONS.get(piece.getType());

		if (garrisonOf == null) return List.of();

		Garrison garrison = garrisonOf.apply(piece);
		BoundingBox box = piece.getBoundingBox();
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));

		random.setLargeFeatureWithSalt(seed, box.minX(), box.minZ(), GARRISON_SALT + box.minY());

		if (random.nextFloat() >= garrison.chance()) return List.of();

		Set<Column> firstChoices = new HashSet<>();
		List<PlannedGuard> guards = new ArrayList<>();

		for (Post post : garrison.posts()) {
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

	private static Map.Entry<StructurePieceType, Function<StructurePiece, Garrison>> always(StructurePieceType type, Post... posts) {
		Garrison garrison = new Garrison(List.of(posts), 1.0F);

		return Map.entry(type, piece -> garrison);
	}

	// The Evoker keeps to the upper walkway when there is one, and to the back of the room when there isn't.
	private static Garrison libraryGarrison(StructurePiece library) {
		boolean tall = library.getBoundingBox().getYSpan() > SHORT_LIBRARY_HEIGHT;
		Area evokerArea = tall ? LIBRARY_UPPER_WALKWAY : LIBRARY_BACK;

		return new Garrison(List.of(new Post(Guard.EVOKER, evokerArea), new Post(Guard.VINDICATOR, LIBRARY_FLOOR)), 1.0F);
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
	private static List<Column> openColumns(StructurePiece piece, Area area) {
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

	// The lava troughs by the door, the End portal with its frame and lava pool, and the silverfish spawner on the stairs.
	private static boolean portalRoomBlocked(int x, int z) {
		boolean lavaTrough = (x == 1 || x == 9) && z <= 3;
		boolean portal = x >= 3 && x <= 7 && z >= 8 && z <= 12;
		boolean spawner = x == 5 && z == 6;

		return lavaTrough || portal || spawner;
	}

	// The bookshelves along both walls and the shelves standing in rows between them.
	private static boolean libraryBlocked(int x, int z) {
		boolean wallShelf = x == 1 || x == 12;
		boolean shelfRow = z % 2 == 1 && z >= 3 && z <= 11 && x >= 3 && x <= 10 && x != 5 && x != 8;

		return wallShelf || shelfRow;
	}

	// The open edge of a tall library's walkway, between its wall shelves and its fence, clear of the ladder.
	private static boolean onLibraryWalkway(int x, int z) {
		boolean betweenShelves = x >= 2 && x <= 11;
		boolean edge = x == 2 || x == 11 || z == 1 || z == 13;
		boolean ladder = x == 10 && z == 13;

		return betweenShelves && edge && !ladder;
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

	// Set up as a Woodland Mansion sets up its Illagers.
	private static void spawn(WorldGenLevel level, EntityType<? extends Mob> type, BlockPos feet, boolean keeper) {
		Mob mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);

		if (mob == null) {
			if (keeper) MobKeepers.forgiveIfPlacing();

			return;
		}

		if (keeper) MobKeepers.tagIfPlacing(mob);

		mob.setPersistenceRequired();
		mob.snapTo(Vec3.atBottomCenterOf(feet), 0.0F, 0.0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(feet), EntitySpawnReason.STRUCTURE, null);
		level.addFreshEntityWithPassengers(mob);
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

	/** The guards a piece places, all or none, with the chance it places them. */
	private record Garrison(List<Post> posts, float chance) {
	}

	/** One guard of a garrison and the part of its piece it stands in. */
	private record Post(Guard guard, Area area) {
	}

	/**
	 * Where in a piece a guard may stand.
	 *
	 * @param columns the piece-local columns open to it
	 * @param lowestFeet the lowest piece-local height its feet may stand at
	 */
	private record Area(LocalColumns columns, int lowestFeet) {
	}

	// A set of columns in a code-built piece's own coordinates.
	@FunctionalInterface
	private interface LocalColumns {
		boolean test(int x, int z);
	}

	private enum Guard {
		VINDICATOR,
		EVOKER,
		/** A Vindicator or a Pillager, 50/50. */
		ROLL;

		EntityType<? extends Mob> roll(WorldgenRandom random) {
			return switch (this) {
				case VINDICATOR -> EntityTypes.VINDICATOR;
				case EVOKER -> EntityTypes.EVOKER;
				case ROLL -> random.nextBoolean() ? EntityTypes.VINDICATOR : EntityTypes.PILLAGER;
			};
		}
	}
}
