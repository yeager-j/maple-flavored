package mapleflavored.gametest;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/** Waits for chunks a test forces to load to tick entities and hold their saved ones, before it places anything. */
final class ForcedChunks {
	// Wall-clock limits on waiting for forced chunks, which load in real time rather than ticks.
	private static final long TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(30);
	private static final long PARK_NANOS = TimeUnit.MICROSECONDS.toNanos(100);

	private ForcedChunks() {
	}

	/**
	 * Runs the chunk system until every one of the forced chunks ticks entities, then until each has loaded its saved
	 * entities.
	 *
	 * <p>Forcing a chunk only adds a ticket: worker threads generate it and its neighbours, and it starts ticking
	 * entities some ticks later. The test server ticks as fast as it can, so how many ticks that takes depends on the
	 * machine. Until then an entity added there sits in a hidden entity section: it doesn't tick, no entity search finds
	 * it, and removing it fires no unload event. Waiting here, before the test places anything, starts every test with
	 * its chunks ticking, however slow the machine.
	 */
	static void awaitEntityTicking(GameTestHelper helper, ServerLevel level, List<ChunkPos> chunks, int y) {
		long deadline = System.nanoTime() + TIMEOUT_NANOS;

		while (!chunks.stream().allMatch(chunk -> level.isPositionEntityTicking(chunk.getMiddleBlockPosition(y)))) {
			if (System.nanoTime() > deadline) helper.fail("the test's forced chunks didn't start ticking entities in time");

			// Polled directly, since the server's own wait skips chunk tasks once a tick runs past its time.
			if (!level.getChunkSource().pollTask()) LockSupport.parkNanos(PARK_NANOS);
		}

		chunks.forEach(chunk -> level.waitForEntities(chunk, 0));
	}
}
