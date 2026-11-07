package mapleflavored.strongholds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import mapleflavored.bloodmoons.Restorable;
import mapleflavored.mixin.StructurePieceInvoker;
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
 * piece is built. Each stands where a Blood Moon can Restore it, on the stone bricks family with a clear 1×2×1 space,
 * inside its piece's walls, so never in a doorway.
 *
 * <p>A piece's garrison and where each guard stands are drawn from the world seed and the piece's box alone, so the
 * piece places the same guards whichever of its chunks is built first. Each guard's first choice of column is floor the
 * piece's own layout leaves open, never lava, the End portal or bookshelves. The guard keeps to that column's chunk and
 * stands in the first column of the chunk with room for it, which the first choice nearly always has. A guard with no
 * room, as where a cave or another structure got in first, is left out.
 */
public final class StrongholdIllagers {
	// Seeds the garrison apart from the Stronghold's own randomness, so adding it changes no layout.
	private static final int GARRISON_SALT = 0x5711A6;

	private static final float CORRIDOR_CHANCE = 0.25F;

	private static final Map<StructurePieceType, Garrison> GARRISONS = Map.ofEntries(
		Map.entry(
			StructurePieceType.STRONGHOLD_PORTAL_ROOM,
			Garrison.always(Guard.EVOKER, Guard.VINDICATOR, Guard.VINDICATOR).except(StrongholdIllagers::portalRoomBlocked)
		),
		Map.entry(StructurePieceType.STRONGHOLD_LIBRARY, Garrison.always(Guard.EVOKER, Guard.VINDICATOR).except(StrongholdIllagers::libraryBlocked)),
		Map.entry(StructurePieceType.STRONGHOLD_ROOM_CROSSING, Garrison.always(Guard.ROLL, Guard.ROLL)),
		Map.entry(StructurePieceType.STRONGHOLD_PRISON_HALL, Garrison.always(Guard.VINDICATOR)),
		Map.entry(StructurePieceType.STRONGHOLD_FIVE_CROSSING, Garrison.always(Guard.ROLL)),
		Map.entry(StructurePieceType.STRONGHOLD_CHEST_CORRIDOR, Garrison.corridor()),
		Map.entry(StructurePieceType.STRONGHOLD_FILLER_CORRIDOR, Garrison.corridor()),
		Map.entry(StructurePieceType.STRONGHOLD_STRAIGHT, Garrison.corridor()),
		Map.entry(StructurePieceType.STRONGHOLD_LEFT_TURN, Garrison.corridor()),
		Map.entry(StructurePieceType.STRONGHOLD_RIGHT_TURN, Garrison.corridor()),
		Map.entry(StructurePieceType.STRONGHOLD_STAIRS_DOWN, Garrison.corridor()),
		Map.entry(StructurePieceType.STRONGHOLD_STRAIGHT_STAIRS_DOWN, Garrison.corridor())
	);

	private StrongholdIllagers() {
	}

	/**
	 * Places the guards of a Stronghold piece that stand in the chunk being built. Call after the piece is built in each
	 * chunk; it does nothing for any other piece, including the Stronghold's starting staircase.
	 *
	 * @param chunkBox the part of the world the chunk build may change
	 */
	public static void placeGuards(StructurePiece piece, WorldGenLevel level, BoundingBox chunkBox) {
		Set<BlockPos> taken = new HashSet<>();

		for (PlannedGuard guard : plan(piece, level.getSeed())) {
			if (!guard.standsIn(chunkBox)) continue;

			guard.columns().stream()
				.flatMap(column -> standingSpot(level, piece.getBoundingBox(), column).stream())
				.filter(spot -> !taken.contains(spot))
				.findFirst()
				.ifPresent(spot -> {
					taken.add(spot);
					spawn(level, guard.type(), spot);
				});
		}
	}

	/** The guards the piece places, each with the columns it may stand in, best first. None for a piece without a garrison. */
	static List<PlannedGuard> plan(StructurePiece piece, long seed) {
		Garrison garrison = GARRISONS.get(piece.getType());

		if (garrison == null) return List.of();

		BoundingBox box = piece.getBoundingBox();
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));

		random.setLargeFeatureWithSalt(seed, box.minX(), box.minZ(), GARRISON_SALT + box.minY());

		if (random.nextFloat() >= garrison.chance()) return List.of();

		List<EntityType<? extends Mob>> types = garrison.guards().stream().<EntityType<? extends Mob>>map(guard -> guard.roll(random)).toList();
		List<Column> columns = new ArrayList<>(openColumns(piece, garrison.blocked()));

		Util.shuffle(columns, random);

		return IntStream.range(0, Math.min(types.size(), columns.size()))
			.mapToObj(index -> new PlannedGuard(types.get(index), columnsNear(columns, index)))
			.toList();
	}

	// The guard's first choice, then every other column in the same chunk, in shuffled order.
	private static List<Column> columnsNear(List<Column> shuffled, int firstChoice) {
		Column first = shuffled.get(firstChoice);
		List<Column> rest = new ArrayList<>(shuffled.subList(firstChoice + 1, shuffled.size()));

		rest.addAll(shuffled.subList(0, firstChoice));

		List<Column> near = new ArrayList<>(List.of(first));

		rest.stream().filter(column -> column.sameChunkAs(first)).forEach(near::add);

		return near;
	}

	// Every column inside the piece's walls that its layout doesn't block.
	private static List<Column> openColumns(StructurePiece piece, LocalColumns blocked) {
		BoundingBox box = piece.getBoundingBox();
		boolean alongX = piece.getOrientation() == null || piece.getOrientation().getAxis() == Direction.Axis.Z;
		int width = alongX ? box.getXSpan() : box.getZSpan();
		int depth = alongX ? box.getZSpan() : box.getXSpan();
		StructurePieceInvoker world = (StructurePieceInvoker) piece;
		List<Column> columns = new ArrayList<>();

		for (int x = 1; x < width - 1; x++) {
			for (int z = 1; z < depth - 1; z++) {
				if (!blocked.test(x, z)) columns.add(new Column(world.worldX(x, z), world.worldZ(x, z)));
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

	// The lowest place in the column, inside the piece's walls, where a guard can stand and a Blood Moon could Restore it.
	private static Optional<BlockPos> standingSpot(WorldGenLevel level, BoundingBox box, Column column) {
		for (int y = box.minY() + 1; y + 1 < box.maxY(); y++) {
			BlockPos feet = new BlockPos(column.x(), y, column.z());
			boolean roomToStand = level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir();
			boolean onFloor = !level.getBlockState(feet.below()).isAir();

			if (roomToStand && onFloor && Restorable.STRONGHOLD.holds(level, Vec3.atBottomCenterOf(feet))) return Optional.of(feet);
		}

		return Optional.empty();
	}

	// Set up as a Woodland Mansion sets up its Illagers.
	private static void spawn(WorldGenLevel level, EntityType<? extends Mob> type, BlockPos feet) {
		Mob mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);

		if (mob == null) return;

		mob.setPersistenceRequired();
		mob.snapTo(Vec3.atBottomCenterOf(feet), 0.0F, 0.0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(feet), EntitySpawnReason.STRUCTURE, null);
		level.addFreshEntityWithPassengers(mob);
	}

	/**
	 * One guard a piece places.
	 *
	 * @param columns the columns it may stand in, best first, all in one chunk
	 */
	record PlannedGuard(EntityType<? extends Mob> type, List<Column> columns) {
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

	/**
	 * The guards a kind of piece places, all or none, with the chance it places them.
	 *
	 * @param blocked the piece-local columns its layout leaves no floor to stand on
	 */
	private record Garrison(List<Guard> guards, float chance, LocalColumns blocked) {
		static Garrison always(Guard... guards) {
			return new Garrison(List.of(guards), 1.0F, (x, z) -> false);
		}

		static Garrison corridor() {
			return new Garrison(List.of(Guard.ROLL), CORRIDOR_CHANCE, (x, z) -> false);
		}

		Garrison except(LocalColumns blocked) {
			return new Garrison(guards, chance, blocked);
		}
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
