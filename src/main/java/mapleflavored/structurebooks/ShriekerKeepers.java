package mapleflavored.structurebooks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import mapleflavored.mixin.FeaturePoolElementAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.FeaturePoolElement;
import net.minecraft.world.level.material.Fluids;

/**
 * An Ancient City's keepers: up to three sculk shriekers in a ring near the city's edge whose breaking unlocks its book.
 * They're chosen from the city's sculk patch origins when it's planned, since its shriekers don't exist until its
 * patches grow. Each is placed with its patch, and counts as broken once a summoning shrieker leaves its spot by any
 * means. A standing keeper bubbles with sculk charge, so it can be found.
 */
public final class ShriekerKeepers {
	private static final ResourceKey<PlacedFeature> SCULK_PATCH = ResourceKey.create(
		Registries.PLACED_FEATURE,
		Identifier.withDefaultNamespace("sculk_patch_ancient_city")
	);
	// Playtest values: keepers stay well away from the book, each in its own third of the ring around it.
	private static final long MIN_DISTANCE = 30;
	private static final List<Double> TURNS = List.of(120.0, 240.0);
	private static final List<Double> WINDOWS = List.of(30.0, 45.0, 60.0);

	// Playtest values: a few bubbles twice a second, seen within vanilla's 32 blocks for particles.
	private static final int BUBBLE_INTERVAL_TICKS = 10;
	private static final int BUBBLES = 2;
	private static final double BUBBLE_SPREAD = 0.25;
	// The top of a shrieker, which is half a block tall.
	private static final double SHRIEKER_TOP = 0.5;

	private ShriekerKeepers() {
	}

	/**
	 * Chooses up to three keepers among a city's sculk patch origins, by the ring rule. Only origins at least 30 blocks
	 * across from the book can be keepers. The first is picked with equal weight from the farther half of them, the
	 * middle one included. The second and third are the farthest left within 30° of the directions 120° and 240° around
	 * the book from the first, widening to 45° and then 60° when none is.
	 *
	 * @param origins the city's sculk patch origins, in planned order
	 * @param random the book's planning random, so the same world seed picks the same keepers
	 * @return the keepers' origins, first chosen first; none when no origin is far enough
	 */
	public static List<BlockPos> choose(BlockPos book, List<BlockPos> origins, RandomSource random) {
		List<BlockPos> eligible = origins.stream()
			.distinct()
			.filter(origin -> distanceSquared(book, origin) >= MIN_DISTANCE * MIN_DISTANCE)
			.sorted(Comparator.comparingLong((BlockPos origin) -> distanceSquared(book, origin)).reversed())
			.toList();

		if (eligible.isEmpty()) return List.of();

		int fartherHalf = (eligible.size() + 1) / 2;
		BlockPos first = eligible.get(random.nextInt(fartherHalf));
		List<BlockPos> keepers = new ArrayList<>(List.of(first));
		double firstDirection = direction(book, first);

		for (double turn : TURNS) {
			farthestToward(book, eligible, firstDirection + turn, keepers).ifPresent(keepers::add);
		}

		return List.copyOf(keepers);
	}

	/** Makes standing keepers bubble. Call once from mod init. */
	static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % BUBBLE_INTERVAL_TICKS == 0) bubbleStandingKeepers(level);
		});
	}

	/**
	 * How the keeper with this key in the lock is, and where. Never loads the keeper's chunk to look.
	 *
	 * @param lock the lock the key is one of
	 */
	public static Keeper keeper(ServerLevel level, BookLocks.Lock lock, BlockPos key) {
		return lock.done().contains(key) ? new Keeper(Status.BROKEN, key) : inWorld(level, key);
	}

	/**
	 * Where the keeper with this key stands: a summoning shrieker at the key, or on the catalyst above it. Empty when the
	 * keeper isn't standing there or its chunk isn't loaded, which is never loaded to check.
	 */
	public static Optional<BlockPos> standingSpot(ServerLevel level, BlockPos key) {
		Keeper keeper = inWorld(level, key);

		return keeper.status() == Status.STANDING ? Optional.of(keeper.spot()) : Optional.empty();
	}

	/**
	 * Whether the keeper with this key is known to be gone: its chunk is loaded and no keeper stands there. A keeper in
	 * a chunk that isn't loaded isn't known to be gone, and the chunk is never loaded to check.
	 */
	static boolean isGone(ServerLevel level, BlockPos key) {
		return inWorld(level, key).status() == Status.MISSING;
	}

	// The keeper as the world alone shows it, which never says a keeper was counted broken.
	private static Keeper inWorld(ServerLevel level, BlockPos key) {
		if (!BookLocks.isChunkBuilt(level, key)) return new Keeper(Status.UNLOADED, key);

		return Stream.of(key, key.above())
			.filter(spot -> isSummoningShrieker(level.getBlockState(spot)))
			.findFirst()
			.map(spot -> new Keeper(Status.STANDING, spot))
			.orElse(new Keeper(Status.MISSING, key));
	}

	/** The origins of a city's sculk patches: the positions of its pieces that place one. */
	public static List<BlockPos> origins(List<StructurePiece> pieces) {
		return pieces.stream().flatMap(piece -> patchOrigin(piece).stream()).toList();
	}

	/** The origin of the sculk patch the piece places, if it places one: the piece's position. */
	static Optional<BlockPos> patchOrigin(StructurePiece piece) {
		if (!(piece instanceof PoolElementStructurePiece poolPiece)) return Optional.empty();
		if (!(poolPiece.getElement() instanceof FeaturePoolElement feature)) return Optional.empty();

		boolean sculkPatch = ((FeaturePoolElementAccessor) feature).feature().is(SCULK_PATCH);

		return sculkPatch ? Optional.of(poolPiece.getPosition()) : Optional.empty();
	}

	/**
	 * Places a keeper shrieker once a keeper's sculk patch has been built: at the patch's origin, or on top of the
	 * catalyst the patch put there. It can summon, and replaces whatever is there. Call after every piece's build.
	 *
	 * @param pieces the structure's pieces
	 */
	static void placeIfKeeper(StructurePiece piece, List<StructurePiece> pieces, WorldGenLevel level) {
		Optional<BlockPos> keeperOrigin = StructureBookPiece.among(pieces).flatMap(book -> book.keeperShriekerPlacedBy(piece));

		if (keeperOrigin.isEmpty()) return;

		BlockPos origin = keeperOrigin.get();
		BlockPos spot = level.getBlockState(origin).is(Blocks.SCULK_CATALYST) ? origin.above() : origin;
		BlockState keeper = Blocks.SCULK_SHRIEKER.defaultBlockState()
			.setValue(SculkShriekerBlock.CAN_SUMMON, true)
			.setValue(SculkShriekerBlock.WATERLOGGED, level.getFluidState(spot).is(Fluids.WATER));

		level.setBlock(spot, keeper, Block.UPDATE_CLIENTS);
	}

	/**
	 * Counts a keeper broken when a summoning shrieker leaves its spot: its origin, or the block above, where a catalyst
	 * lifts it. A player can't place a summoning shrieker, so only a keeper or a shrieker the city grew counts. Call on
	 * every block change in a loaded chunk.
	 *
	 * @param oldState the block that was at {@code pos}
	 */
	public static void onBlockChanged(ServerLevel level, BlockPos pos, BlockState oldState) {
		if (!isSummoningShrieker(oldState)) return;
		if (level.getBlockState(pos).is(Blocks.SCULK_SHRIEKER)) return;

		BookLocks.markShriekerBroken(level, pos);
		BookLocks.markShriekerBroken(level, pos.below());
	}

	private static void bubbleStandingKeepers(ServerLevel level) {
		for (BookLocks.Lock lock : BookLocks.locksOf(level, BookLocks.Kind.SHRIEKERS)) {
			for (BlockPos key : lock.remainingKeys()) {
				standingSpot(level, key).ifPresent(spot -> level.sendParticles(
					ParticleTypes.SCULK_CHARGE_POP,
					spot.getX() + 0.5,
					spot.getY() + SHRIEKER_TOP,
					spot.getZ() + 0.5,
					BUBBLES,
					BUBBLE_SPREAD,
					BUBBLE_SPREAD,
					BUBBLE_SPREAD,
					0.0
				));
			}
		}
	}

	private static boolean isSummoningShrieker(BlockState state) {
		return state.is(Blocks.SCULK_SHRIEKER) && state.getValue(SculkShriekerBlock.CAN_SUMMON);
	}

	// The farthest origin not yet a keeper within the first window around the direction that has one.
	private static Optional<BlockPos> farthestToward(BlockPos book, List<BlockPos> eligibleFarthestFirst, double direction, List<BlockPos> keepers) {
		for (double window : WINDOWS) {
			Optional<BlockPos> farthest = eligibleFarthestFirst.stream()
				.filter(origin -> !keepers.contains(origin))
				.filter(origin -> angleBetween(direction(book, origin), direction) <= window)
				.findFirst();

			if (farthest.isPresent()) return farthest;
		}

		return Optional.empty();
	}

	// Across the horizontal plane, in whole blocks squared, so comparing two distances is exact.
	private static long distanceSquared(BlockPos book, BlockPos origin) {
		long x = origin.getX() - book.getX();
		long z = origin.getZ() - book.getZ();

		return x * x + z * z;
	}

	// In degrees, measured around the book on the horizontal plane. StrictMath gives every platform the same direction,
	// so the same world seed picks the same keepers on each.
	private static double direction(BlockPos book, BlockPos origin) {
		return StrictMath.toDegrees(StrictMath.atan2(origin.getZ() - book.getZ(), origin.getX() - book.getX()));
	}

	private static double angleBetween(double a, double b) {
		return Math.abs(Mth.wrapDegrees(a - b));
	}

	/** How a keeper is. */
	public enum Status {
		/** A summoning shrieker stands at its spot. */
		STANDING,
		/** Its lock has counted it broken. */
		BROKEN,
		/** Its chunk isn't loaded or isn't generated yet, so nothing shows whether it stands. */
		UNLOADED,
		/** Its chunk is loaded with no summoning shrieker at its spot, and its lock hasn't counted it broken. */
		MISSING
	}

	/**
	 * One keeper, as it is now.
	 *
	 * @param spot where it stands, or its key when it isn't standing
	 */
	public record Keeper(Status status, BlockPos spot) {
	}
}
