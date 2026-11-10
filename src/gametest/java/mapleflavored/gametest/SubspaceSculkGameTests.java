package mapleflavored.gametest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import mapleflavored.gamerules.MapleGameRules;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Sculk in Warden Subspaces: a weak patch spreads where the Warden appeared, over the blocks a sculk catalyst could turn
 * and no further than a few blocks, only while its Warden is loaded and its Subspace lasts. The shriekers it grows can
 * summon until their own Subspace ends, even if they were unloaded when it did.
 *
 * <p>Each test runs alone in its batch, in a {@link SubspaceArena} whose Wardens stay put and never dig down, over stone
 * floors laid where sculk should grow.
 *
 * <p>Sculk grows a shrieker one time in eleven, and a default patch rarely grows anything. The tests that need a
 * shrieker run under game rules for a patch wide and charged enough to grow dozens of things, so one of them is a
 * shrieker all but every time.
 */
public class SubspaceSculkGameTests {
	// How far a patch's own cursors may go by default, plus the √5 blocks further a vein can wrap around a block they
	// convert.
	private static final double PATCH_REACH = MapleGameRules.WARDEN_SCULK_PATCH_RADIUS.defaultValue() + Math.sqrt(5.0);
	// Wider than any patch, so a test sees everything one spreads.
	private static final int FLOOR_REACH = 10;
	// How long a test lets a patch spread before checking where it hasn't gone, longer than any patch spreads for.
	private static final int SETTLING_TICKS = 400;
	// How far past a patch's cursors a floor reaches: the √5 blocks a vein can wrap, rounded up.
	private static final int VEIN_REACH = 3;
	// How long a patch under the growth tests' game rules is left to grow a shrieker. Measured over 4 such patches: by
	// 800 ticks each had grown 24 to 36 things, at least 3 of them shriekers.
	private static final int GROWING_TICKS = 1200;
	// Room for a patch that grew no shrieker to be started over a few times.
	private static final int GROWTH_TEST_TICKS = 8000;
	// Where the unloading test's patch grows, along the arena's x axis.
	private static final double FAR_FROM_THE_TEST = 100.0;

	@RunsAlone
	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 600)
	public void aPatchSpreadsAroundTheWardensSpawnOverReplaceableBlocksOnly(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.clearedWithoutRunway(helper);
		ServerLevel level = arena.level();
		Vec3 centre = arena.centre();

		arena.stillWarden(Vec3.ZERO);

		BoundingBox floor = arena.sculkFloor(-FLOOR_REACH, -FLOOR_REACH, FLOOR_REACH, FLOOR_REACH);
		BoundingBox bricks = BoundingBox.fromCorners(arena.blockAt(2, -1, -1), arena.blockAt(3, -1, 1));

		BlockPos.betweenClosedStream(bricks).forEach(position -> level.setBlock(position, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 3));

		helper.startSequence()
			.thenExecuteAfter(SETTLING_TICKS, () -> {
				List<BlockPos> sculk = sculkIn(level, floor);
				List<BlockPos> outside = sculk.stream().filter(position -> distance(centre, position) > PATCH_REACH).toList();

				helper.assertFalse(sculk.isEmpty(), "No sculk spread");
				helper.assertValueEqual(outside, List.of(), "sculk beyond the patch");
				BlockPos.betweenClosedStream(bricks).forEach(position -> helper.assertTrue(
					level.getBlockState(position).is(Blocks.DEEPSLATE_BRICKS),
					"Deepslate bricks, which sculk can't replace, became " + level.getBlockState(position)
				));
			})
			.thenSucceed();
	}

	// Set by the environment's game rules: a Subspace smaller than the patch would otherwise be.
	@GameTest(environment = "maple_flavored_gametest:subspace_sculk_small_subspace", maxTicks = 600)
	public void aPatchStaysInsideASubspaceSmallerThanIt(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.clearedWithoutRunway(helper);
		ServerLevel level = arena.level();
		Vec3 centre = arena.centre();

		arena.stillWarden(Vec3.ZERO);

		BoundingBox floor = arena.sculkFloor(-FLOOR_REACH, -FLOOR_REACH, FLOOR_REACH, FLOOR_REACH);
		double subspaceRadius = level.getGameRules().get(MapleGameRules.WARDEN_SUBSPACE_RADIUS);

		helper.startSequence()
			.thenExecuteAfter(SETTLING_TICKS, () -> {
				List<BlockPos> sculk = sculkIn(level, floor);
				List<BlockPos> outside = sculk.stream().filter(position -> distance(centre, position) > subspaceRadius).toList();

				helper.assertFalse(sculk.isEmpty(), "No sculk spread");
				helper.assertValueEqual(outside, List.of(), "sculk outside the Subspace");
			})
			.thenSucceed();
	}

	@RunsAlone
	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 1500)
	public void aPatchSpreadsOnlyWhileItsWardenIsLoadedAndItsSubspaceLasts(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.clearedWithoutRunway(helper);
		ServerLevel level = arena.level();
		Warden warden = arena.stillWarden(Vec3.ZERO);
		BoundingBox floor = arena.sculkFloor(-FLOOR_REACH, -FLOOR_REACH, FLOOR_REACH, FLOOR_REACH);
		AtomicReference<CompoundTag> savedWarden = new AtomicReference<>();
		AtomicReference<Entity> reloaded = new AtomicReference<>();
		AtomicInteger sculk = new AtomicInteger();

		helper.startSequence()
			.thenWaitUntil(() -> helper.assertFalse(sculkIn(level, floor).isEmpty(), "No sculk has spread yet"))
			.thenExecute(() -> {
				savedWarden.set(SubspaceArena.unload(helper, warden));
				sculk.set(sculkIn(level, floor).size());
			})
			.thenExecuteAfter(SETTLING_TICKS, () -> {
				helper.assertValueEqual(sculkIn(level, floor).size(), sculk.get(), "sculk while the Warden is unloaded");
				reloaded.set(arena.track(SubspaceArena.load(helper, savedWarden.get())));
			})
			.thenWaitUntil(() -> helper.assertTrue(sculkIn(level, floor).size() > sculk.get(), "Sculk hasn't spread since the Warden reloaded"))
			.thenExecute(() -> {
				reloaded.get().kill(level);
				sculk.set(sculkIn(level, floor).size());
			})
			.thenExecuteAfter(SETTLING_TICKS, () -> helper.assertValueEqual(sculkIn(level, floor).size(), sculk.get(), "sculk once the Subspace ended"))
			.thenSucceed();
	}

	// The two patches are too far apart to touch, so each shrieker belongs to the Warden it grew beside, while each
	// Subspace, as wide as the environment's game rule sets it, covers both.
	@GameTest(environment = "maple_flavored_gametest:subspace_sculk_overlap", maxTicks = GROWTH_TEST_TICKS)
	public void endingASubspaceSilencesOnlyTheShriekersItGrew(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.clearedWithoutRunway(helper);
		ServerLevel level = arena.level();
		int reach = ShriekerGrowth.floorReach(level);
		int apart = 2 * reach + 2;
		ShriekerGrowth first = new ShriekerGrowth(arena, Vec3.ZERO);
		ShriekerGrowth second = new ShriekerGrowth(arena, new Vec3(apart, 0.0, 0.0));
		BlockPos placed = arena.blockAt(apart / 2, 0, reach + 4);

		// On a floor of its own, beyond both patches, so teardown clears it.
		arena.sculkFloor(apart / 2, reach + 4, apart / 2, reach + 4);
		helper.assertTrue(level.getGameRules().get(MapleGameRules.WARDEN_SUBSPACE_RADIUS) >= apart + reach, "Each Subspace doesn't cover both patches");
		level.setBlock(placed, Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, true), 3);
		helper.onEachTick(() -> {
			first.tick(helper);
			second.tick(helper);
		});

		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(first.hasGrown() && second.hasGrown(), "Not both patches have grown a shrieker yet"))
			.thenExecute(() -> {
				assertSummoning(helper, level, first.shrieker(), true, "the first Subspace's shrieker while it lasts");
				assertSummoning(helper, level, second.shrieker(), true, "the second Subspace's shrieker while it lasts");
				first.warden().kill(level);
			})
			.thenExecuteAfter(1, () -> {
				assertSummoning(helper, level, first.shrieker(), false, "the first Subspace's shrieker once it ends");
				assertSummoning(helper, level, second.shrieker(), true, "the second Subspace's shrieker once the first ends");
				assertSummoning(helper, level, placed, true, "a shrieker no Subspace grew");
				second.warden().kill(level);
			})
			.thenExecuteAfter(1, () -> {
				assertSummoning(helper, level, second.shrieker(), false, "the second Subspace's shrieker once it ends");
				assertSummoning(helper, level, placed, true, "a shrieker no Subspace grew");
			})
			.thenSucceed();
	}

	// The test's own chunks always stay loaded, so the Warden opens its Subspace far from them and then waits in them,
	// where its Subspace lasts. Its patch is then over three chunks from every chunk kept loaded, so releasing the
	// patch's chunks lets them unload for real.
	@GameTest(environment = "maple_flavored_gametest:subspace_sculk_unloaded_shrieker", maxTicks = GROWTH_TEST_TICKS)
	public void aShriekerUnloadedWhenItsSubspaceEndsIsSilencedWhenItLoads(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.clearedWithoutRunway(helper);
		ServerLevel level = arena.level();
		ShriekerGrowth growth = new ShriekerGrowth(arena, new Vec3(FAR_FROM_THE_TEST, 0.0, 0.0));

		helper.onEachTick(() -> growth.tick(helper));

		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(growth.hasGrown(), "No shrieker has grown yet"))
			.thenExecute(() -> {
				Vec3 centre = arena.centre();

				growth.warden().snapTo(centre.x, centre.y, centre.z);
				ForcedChunks.letUnload(helper, level, chunksOf(growth.floor()));
			})
			.thenWaitUntil(() -> helper.assertFalse(isLoaded(level, growth.shrieker()), "The shrieker's chunk is still loaded"))
			.thenExecute(() -> ForcedChunks.keepLoaded(helper, level, chunksOf(growth.floor())))
			.thenWaitUntil(() -> helper.assertTrue(isLoaded(level, growth.shrieker()), "The shrieker's chunk hasn't loaded again"))
			.thenExecuteAfter(2, () -> {
				assertSummoning(helper, level, growth.shrieker(), true, "a shrieker reloaded while its Subspace lasts");
				ForcedChunks.letUnload(helper, level, chunksOf(growth.floor()));
			})
			.thenWaitUntil(() -> helper.assertFalse(isLoaded(level, growth.shrieker()), "The shrieker's chunk is still loaded"))
			.thenExecute(() -> {
				growth.warden().kill(level);
				ForcedChunks.keepLoaded(helper, level, chunksOf(growth.floor()));
			})
			.thenWaitUntil(() -> {
				helper.assertTrue(isLoaded(level, growth.shrieker()), "The shrieker's chunk hasn't loaded again");
				assertSummoning(helper, level, growth.shrieker(), false, "a shrieker loaded after its Subspace ended");
			})
			.thenSucceed();
	}

	private static double distance(Vec3 centre, BlockPos position) {
		return centre.distanceTo(Vec3.atCenterOf(position));
	}

	private static boolean isSculk(BlockState state) {
		return state.is(Blocks.SCULK) || state.is(Blocks.SCULK_VEIN) || state.is(Blocks.SCULK_SENSOR) || state.is(Blocks.SCULK_SHRIEKER);
	}

	private static List<BlockPos> sculkIn(ServerLevel level, BoundingBox volume) {
		return BlockPos.betweenClosedStream(volume)
			.filter(position -> isSculk(level.getBlockState(position)))
			.map(BlockPos::immutable)
			.toList();
	}

	private static void assertSummoning(GameTestHelper helper, ServerLevel level, BlockPos shrieker, boolean expected, String which) {
		BlockState state = level.getBlockState(shrieker);

		helper.assertTrue(state.is(Blocks.SCULK_SHRIEKER), "The shrieker at " + shrieker + " is gone");
		helper.assertValueEqual(state.getValue(SculkShriekerBlock.CAN_SUMMON), expected, "whether " + which + " can summon");
	}

	private static boolean isLoaded(ServerLevel level, BlockPos position) {
		return level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(position.getX()), SectionPos.blockToSectionCoord(position.getZ())) != null;
	}

	private static List<ChunkPos> chunksOf(BoundingBox volume) {
		return ChunkPos.rangeClosed(
			new ChunkPos(SectionPos.blockToSectionCoord(volume.minX()), SectionPos.blockToSectionCoord(volume.minZ())),
			new ChunkPos(SectionPos.blockToSectionCoord(volume.maxX()), SectionPos.blockToSectionCoord(volume.maxZ()))
		).toList();
	}

	/**
	 * Grows a shrieker in a Warden's patch at one spot, under the game rules of the test's environment. The rare patch
	 * that grows none is started over: each tick checks the current patch and, once it has had time to spread, kills its
	 * Warden and lays a fresh floor.
	 */
	private static final class ShriekerGrowth {
		private final SubspaceArena arena;
		private final Vec3 spot;
		private final BoundingBox floor;
		private @Nullable Warden warden;
		private @Nullable BlockPos shrieker;
		private long startedAt;

		ShriekerGrowth(SubspaceArena arena, Vec3 spot) {
			BlockPos fromCentre = BlockPos.containing(spot);
			int reach = floorReach(arena.level());

			this.arena = arena;
			this.spot = spot;
			this.floor = arena.sculkFloor(
				fromCentre.getX() - reach,
				fromCentre.getZ() - reach,
				fromCentre.getX() + reach,
				fromCentre.getZ() + reach
			);
		}

		/** How far a floor reaches from its Warden: past everything a patch spreads under the level's game rules. */
		static int floorReach(ServerLevel level) {
			return level.getGameRules().get(MapleGameRules.WARDEN_SCULK_PATCH_RADIUS) + VEIN_REACH;
		}

		void tick(GameTestHelper helper) {
			if (shrieker != null) return;

			if (warden != null) {
				shrieker = grownShrieker().orElse(null);

				if (shrieker != null || helper.getTick() - startedAt < GROWING_TICKS) return;

				warden.kill(arena.level());
			}

			arena.renew(floor);
			warden = arena.stillWarden(spot);
			startedAt = helper.getTick();
		}

		private Optional<BlockPos> grownShrieker() {
			return BlockPos.betweenClosedStream(floor)
				.filter(position -> arena.level().getBlockState(position).is(Blocks.SCULK_SHRIEKER))
				.map(BlockPos::immutable)
				.findFirst();
		}

		boolean hasGrown() {
			return shrieker != null;
		}

		BlockPos shrieker() {
			return shrieker;
		}

		Warden warden() {
			return warden;
		}

		BoundingBox floor() {
			return floor;
		}
	}
}
