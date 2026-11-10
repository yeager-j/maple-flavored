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
	private static final double MIN_DISTANCE = 30.0;
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
			.filter(origin -> distance(book, origin) >= MIN_DISTANCE)
			.sorted(Comparator.comparingDouble((BlockPos origin) -> distance(book, origin)).reversed())
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
	 * Where the keeper with this key stands: a summoning shrieker at the key, or on the catalyst above it. Empty when the
	 * keeper isn't standing there or its chunk isn't loaded, which is never loaded to check.
	 */
	public static Optional<BlockPos> standingSpot(ServerLevel level, BlockPos key) {
		if (!BookLocks.isChunkBuilt(level, key)) return Optional.empty();

		return Stream.of(key, key.above()).filter(spot -> isSummoningShrieker(level.getBlockState(spot))).findFirst();
	}

	/**
	 * Whether the keeper with this key is known to be gone: its chunk is loaded and no keeper stands there. A keeper in
	 * a chunk that isn't loaded isn't known to be gone, and the chunk is never loaded to check.
	 */
	static boolean isGone(ServerLevel level, BlockPos key) {
		return BookLocks.isChunkBuilt(level, key) && standingSpot(level, key).isEmpty();
	}

	/** The origins of a city's sculk patches: the positions of its pieces that place one. */
	public static List<BlockPos> origins(List<StructurePiece> pieces) {
		return pieces.stream()
			.filter(ShriekerKeepers::isSculkPatch)
			.map(piece -> ((PoolElementStructurePiece) piece).getPosition())
			.toList();
	}

	/**
	 * Places a keeper shrieker once a keeper's sculk patch has been built: at the patch's origin, or on top of the
	 * catalyst the patch put there. It can summon, and replaces whatever is there. Call after every piece's build.
	 *
	 * @param pieces the structure's pieces, its book last if it has one
	 */
	static void placeIfKeeper(StructurePiece piece, List<StructurePiece> pieces, WorldGenLevel level) {
		if (!isSculkPatch(piece) || !(pieces.getLast() instanceof StructureBookPiece book)) return;

		BlockPos origin = ((PoolElementStructurePiece) piece).getPosition();

		if (!book.keepers().contains(origin)) return;

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

	private static boolean isSculkPatch(StructurePiece piece) {
		if (!(piece instanceof PoolElementStructurePiece poolPiece)) return false;
		if (!(poolPiece.getElement() instanceof FeaturePoolElement feature)) return false;

		return ((FeaturePoolElementAccessor) feature).feature().is(SCULK_PATCH);
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

	private static double distance(BlockPos book, BlockPos origin) {
		return Math.hypot(origin.getX() - book.getX(), origin.getZ() - book.getZ());
	}

	// In degrees, measured around the book on the horizontal plane.
	private static double direction(BlockPos book, BlockPos origin) {
		return Math.toDegrees(Math.atan2(origin.getZ() - book.getZ(), origin.getX() - book.getX()));
	}

	private static double angleBetween(double a, double b) {
		return Math.abs(Mth.wrapDegrees(a - b));
	}
}
