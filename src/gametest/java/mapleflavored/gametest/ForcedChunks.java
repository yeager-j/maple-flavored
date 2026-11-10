package mapleflavored.gametest;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import mapleflavored.gametest.mixin.GameTestHelperAccessor;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Keeps chunks loaded for a test, and is the only test code that forces a chunk.
 *
 * <p>Who lets a chunk go depends on where it is:
 * <ul>
 * <li>A chunk of the test's {@linkplain TestSites sites}, in any level, is let go when the test passes or fails. Only
 * that test may keep it loaded.
 * <li>A chunk on the test grid, in the test's own level, is left to the runner. Minecraft 26.3's {@code GameTestRunner} unforces every forced
 * chunk in a batch's level as the batch's last test finishes, pass or fail, then forces the next batch's test
 * structures straight away. Tests on the grid share chunks with their neighbours, and forcing isn't counted, so
 * letting one go here could unload a neighbour's test structure, or the next batch's.
 * </ul>
 *
 * <p>The runner lets go only in the batch's own level, so a grid chunk can't be kept loaded in any other. A run that
 * is cancelled, as starting another from a test block does, tells no test it ended, and its chunks stay forced.
 */
final class ForcedChunks {
	// Wall-clock limits on waiting for forced chunks, which load in real time rather than ticks.
	private static final long TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(30);
	private static final long PARK_NANOS = TimeUnit.MICROSECONDS.toNanos(100);

	private static final Map<GameTestInfo, Set<SiteChunk>> SITE_CHUNKS = new HashMap<>();

	private ForcedChunks() {
	}

	/**
	 * Forces the chunks, then runs the chunk system until every one ticks entities and has loaded its saved entities.
	 * Fails the test if a chunk is on another test's sites, or on the grid in a level other than the test's own.
	 *
	 * <p>Forcing a chunk only adds a ticket: worker threads generate it and its neighbours, and it starts ticking
	 * entities some ticks later. The test server ticks as fast as it can, so how many ticks that takes depends on the
	 * machine. Until then an entity added there sits in a hidden entity section: it doesn't tick, no entity search finds
	 * it, and removing it fires no unload event. Waiting here, before the test places anything, starts every test with
	 * its chunks ticking, however slow the machine.
	 */
	static void keepLoaded(GameTestHelper helper, ServerLevel level, Collection<ChunkPos> chunks) {
		Set<SiteChunk> siteChunks = checkedSiteChunks(helper, level, chunks);

		if (!siteChunks.isEmpty()) trackedUntilDone(helper).addAll(siteChunks);

		chunks.forEach(chunk -> level.setChunkForced(chunk.x(), chunk.z(), true));
		awaitEntityTicking(helper, level, chunks);
	}

	/** Lets the chunks unload now, for a test of what unloading does. Fails the test wherever {@link #keepLoaded} would. */
	static void letUnload(GameTestHelper helper, ServerLevel level, Collection<ChunkPos> chunks) {
		Set<SiteChunk> siteChunks = checkedSiteChunks(helper, level, chunks);

		if (!siteChunks.isEmpty()) trackedUntilDone(helper).removeAll(siteChunks);

		chunks.forEach(chunk -> level.setChunkForced(chunk.x(), chunk.z(), false));
	}

	// The chunks among these that are on the test's sites. Fails the test if one is a chunk it may not keep loaded.
	private static Set<SiteChunk> checkedSiteChunks(GameTestHelper helper, ServerLevel level, Collection<ChunkPos> chunks) {
		TestSites sites = TestSites.of(helper);
		Set<SiteChunk> siteChunks = new HashSet<>();

		for (ChunkPos chunk : chunks) {
			if (sites.holds(chunk)) {
				siteChunks.add(new SiteChunk(level, chunk));
			} else if (TestSites.isOffGrid(chunk)) {
				helper.fail("the chunk " + chunk + " is on another test's sites");
			} else if (level != helper.getLevel()) {
				helper.fail("the runner never lets go of the grid chunk " + chunk + " in " + level.dimension().identifier());
			}
		}

		return siteChunks;
	}

	// The site chunks the test keeps loaded. The first call for a test starts tracking them, and they are let go when it
	// passes or fails.
	private static Set<SiteChunk> trackedUntilDone(GameTestHelper helper) {
		GameTestInfo test = ((GameTestHelperAccessor) helper).maple$testInfo();

		return SITE_CHUNKS.computeIfAbsent(test, started -> {
			// Two runs of one test share its sites, so the first to end would let go of the other's chunks.
			if (SITE_CHUNKS.keySet().stream().anyMatch(other -> other.id().equals(test.id()))) {
				helper.fail("another run of this test is keeping its sites loaded");
			}

			test.addListener(new GameTestListener() {
				@Override
				public void testStructureLoaded(GameTestInfo info) {
				}

				@Override
				public void testPassed(GameTestInfo info, GameTestRunner runner) {
					release(info);
				}

				@Override
				public void testFailed(GameTestInfo info, GameTestRunner runner) {
					release(info);
				}

				@Override
				public void testAddedForRerun(GameTestInfo original, GameTestInfo copy, GameTestRunner runner) {
				}
			});

			return new HashSet<>();
		});
	}

	private static void release(GameTestInfo test) {
		SITE_CHUNKS.remove(test).forEach(held -> held.level.setChunkForced(held.chunk.x(), held.chunk.z(), false));
	}

	private static void awaitEntityTicking(GameTestHelper helper, ServerLevel level, Collection<ChunkPos> chunks) {
		long deadline = System.nanoTime() + TIMEOUT_NANOS;

		while (!chunks.stream().allMatch(chunk -> level.isPositionEntityTicking(chunk.getMiddleBlockPosition(0)))) {
			if (System.nanoTime() > deadline) helper.fail("the test's forced chunks didn't start ticking entities in time");

			// Polled directly, since the server's own wait skips chunk tasks once a tick runs past its time.
			if (!level.getChunkSource().pollTask()) LockSupport.parkNanos(PARK_NANOS);
		}

		chunks.forEach(chunk -> level.waitForEntities(chunk, 0));
	}

	private record SiteChunk(ServerLevel level, ChunkPos chunk) {
	}
}
