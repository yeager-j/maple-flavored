package mapleflavored.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import mapleflavored.gametest.mixin.GameTestHelperAccessor;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.RetryOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;

/**
 * Every test has sites of its own off the test grid, the same whatever order the tests come in. A test's site chunks
 * are kept loaded and ticking entities from the moment it asks, and let go when it passes or fails. A test can't keep
 * another test's site loaded, and two runs of one test can't both keep its sites loaded.
 */
public class TestSitesGameTests {
	// Many more than there are tests, and enough that some land on the same region first.
	private static final int TESTS = 2000;

	@GameTest
	public void everyTestHasGroundOfItsOwnWhateverOrderTheTestsComeIn(GameTestHelper helper) {
		List<Identifier> tests = IntStream.range(0, TESTS).mapToObj(index -> Identifier.fromNamespaceAndPath("maple", "test_" + index)).toList();
		Map<Identifier, Integer> regions = TestSites.assignRegions(tests);
		List<TestSites> everyTestsSites = regions.values().stream().map(TestSites::new).toList();

		helper.assertValueEqual(new HashSet<>(regions.values()).size(), TESTS, "regions given to " + TESTS + " tests");
		helper.assertValueEqual(TestSites.assignRegions(tests.reversed()), regions, "the regions of the same tests in reverse order");

		for (TestSites sites : everyTestsSites) {
			for (ChunkPos site : List.of(sites.chunk(0, 0), sites.chunk(TestSites.ROWS - 1, TestSites.COLUMNS - 1))) {
				long holders = everyTestsSites.stream().filter(each -> each.holds(site)).count();

				helper.assertTrue(TestSites.isOffGrid(site), "the site at " + site + " is on ground the test grid can reach");
				helper.assertTrue(sites.holds(site), "the site at " + site + " is outside its own test's ground");
				helper.assertValueEqual(holders, 1L, "tests whose ground holds the site at " + site);
			}
		}

		helper.succeed();
	}

	@GameTest
	public void aTestsGroundEndsAtItsLastRowAndSite(GameTestHelper helper) {
		TestSites sites = TestSites.of(helper);

		assertRefused(helper, () -> sites.chunk(-1), "a site before the first");
		assertRefused(helper, () -> sites.chunk(TestSites.COLUMNS), "a site past the last");
		assertRefused(helper, () -> sites.chunk(TestSites.ROWS, 0), "a row past the last");
		helper.succeed();
	}

	@GameTest
	public void theTestGridIsNotOnSiteGround(GameTestHelper helper) {
		helper.assertFalse(TestSites.isOffGrid(ChunkPos.containing(helper.absolutePos(BlockPos.ZERO))), "the test's own structure is on site ground");
		helper.succeed();
	}

	@GameTest
	public void aSiteKeptLoadedTicksEntitiesAtOnceAndUnloadsWhenLetGo(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ChunkPos site = TestSites.of(helper).chunk(0);

		ForcedChunks.keepLoaded(helper, level, List.of(site));

		helper.assertTrue(isForced(level, site), "the site is forced");
		helper.assertTrue(level.isPositionEntityTicking(site.getMiddleBlockPosition(0)), "the site ticks entities as soon as it is kept loaded");
		helper.assertTrue(level.areEntitiesLoaded(site.pack()), "the site's saved entities are loaded as soon as it is kept loaded");

		ForcedChunks.letUnload(helper, level, List.of(site));

		helper.assertFalse(isForced(level, site), "the site is still forced once it is let go");
		helper.succeed();
	}

	@GameTest
	public void aTestsSitesAreLetGoWhenItPasses(GameTestHelper helper) {
		assertLetGoWhen(helper, "passes", run -> run.getListeners().forEach(listener -> listener.testPassed(run, null)));
		helper.succeed();
	}

	@GameTest
	public void aTestsSitesAreLetGoWhenItFails(GameTestHelper helper) {
		assertLetGoWhen(helper, "fails", run -> run.getListeners().forEach(listener -> listener.testFailed(run, null)));
		helper.succeed();
	}

	@GameTest
	public void aTestCantKeepAnotherTestsSiteLoaded(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TestSites sites = TestSites.of(helper);
		// At most one of two regions is this test's.
		ChunkPos anothers = Stream.of(new TestSites(0), new TestSites(1)).map(other -> other.chunk(0)).filter(site -> !sites.holds(site)).findFirst().orElseThrow();

		try {
			ForcedChunks.keepLoaded(helper, level, List.of(anothers));
		} catch (GameTestAssertException refused) {
			helper.assertFalse(isForced(level, anothers), "another test's site is forced");
			helper.succeed();

			return;
		}

		helper.fail("the test kept another test's site loaded");
	}

	@GameTest
	public void twoRunsOfOneTestCantBothKeepItsSitesLoaded(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<ChunkPos> site = List.of(TestSites.of(helper).chunk(0));
		GameTestInfo first = anotherRun(helper);

		ForcedChunks.keepLoaded(new GameTestHelper(first), level, site);

		try {
			ForcedChunks.keepLoaded(new GameTestHelper(anotherRun(helper)), level, site);
		} catch (GameTestAssertException refused) {
			first.getListeners().forEach(listener -> listener.testPassed(first, null));
			helper.succeed();

			return;
		}

		helper.fail("a second run kept the first run's site loaded");
	}

	// Keeps a site loaded for another run of this test and ends that run.
	private static void assertLetGoWhen(GameTestHelper helper, String ending, Consumer<GameTestInfo> end) {
		ServerLevel level = helper.getLevel();
		GameTestInfo run = anotherRun(helper);
		ChunkPos site = TestSites.of(helper).chunk(0);

		ForcedChunks.keepLoaded(new GameTestHelper(run), level, List.of(site));

		helper.assertTrue(isForced(level, site), "the site is forced while its test runs");
		end.accept(run);
		helper.assertFalse(isForced(level, site), "the site is still forced once its test " + ending);
	}

	// A run of the helper's test that only the test itself drives.
	private static GameTestInfo anotherRun(GameTestHelper helper) {
		GameTestInfo running = ((GameTestHelperAccessor) helper).maple$testInfo();

		return new GameTestInfo(running.getTestHolder(), Rotation.NONE, helper.getLevel(), RetryOptions.noRetries());
	}

	private static void assertRefused(GameTestHelper helper, Runnable asksForASite, String what) {
		try {
			asksForASite.run();
		} catch (IndexOutOfBoundsException refused) {
			return;
		}

		helper.fail("the test was given " + what);
	}

	private static boolean isForced(ServerLevel level, ChunkPos chunk) {
		return level.getForceLoadedChunks().contains(chunk.pack());
	}
}
