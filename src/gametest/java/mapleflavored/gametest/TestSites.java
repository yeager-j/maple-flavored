package mapleflavored.gametest;

import it.unimi.dsi.fastutil.HashCommon;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import mapleflavored.gametest.mixin.GameTestHelperAccessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;

/**
 * One test's own ground away from the test grid, for work too large or too far-reaching for its test structure. It is a
 * grid of sites {@value #CHUNKS_APART} chunks apart, {@value #ROWS} rows of {@value #COLUMNS}, and no other test's
 * sites or test structure come within that distance of them.
 *
 * <p>A test's sites follow from its id alone, so they are the same on every run and every machine, and a structure
 * planned at one is too. Adding or renaming a test moves another test's sites only when the two ids land on the same
 * ground. Two runs of one test at the same time, as the runner's verify mode makes, share their sites, and
 * {@link ForcedChunks} fails the second to keep one loaded.
 *
 * <p>Every site is past block x 15,040,000 in whichever level the test uses. The runner starts the test grid within
 * 14,999,992 blocks of the origin ({@code GameTestServer.startTests}), so the grid never reaches a site.
 */
final class TestSites {
	static final int ROWS = 16;
	static final int COLUMNS = 256;

	private static final int CHUNKS_APART = 40;
	private static final int REGION_CHUNKS_X = ROWS * CHUNKS_APART;
	private static final int REGION_CHUNKS_Z = COLUMNS * CHUNKS_APART;

	// The regions fill a band that ends below block x 2^24, where a float still holds every whole block as it does
	// where the grid runs.
	private static final int FIRST_CHUNK_X = 940_000;
	private static final int FIRST_CHUNK_Z = -896_000;
	private static final int REGIONS_ALONG_X = 156;
	private static final int REGIONS_ALONG_Z = 175;
	private static final int REGION_COUNT = REGIONS_ALONG_X * REGIONS_ALONG_Z;

	private static Map<Identifier, Integer> regions;

	// The region's corner with the least x and z. Each site sits in the middle of its own square of the region.
	private final ChunkPos corner;

	TestSites(int region) {
		this.corner = new ChunkPos(
			FIRST_CHUNK_X + region / REGIONS_ALONG_Z * REGION_CHUNKS_X,
			FIRST_CHUNK_Z + region % REGIONS_ALONG_Z * REGION_CHUNKS_Z
		);
	}

	/** The sites of the test the helper runs. */
	static TestSites of(GameTestHelper helper) {
		if (regions == null) {
			regions = assignRegions(helper.getLevel().registryAccess().lookupOrThrow(Registries.TEST_INSTANCE).keySet());
		}

		return new TestSites(regions.get(((GameTestHelperAccessor) helper).maple$testInfo().id()));
	}

	/** Whether the chunk is on the ground set aside for sites, where the test grid never reaches. */
	static boolean isOffGrid(ChunkPos chunk) {
		return chunk.x() >= FIRST_CHUNK_X;
	}

	/**
	 * Gives every test a region of its own. A test's region follows from its id, and the ids are placed in order, so two
	 * that land on the same region settle the same way every time.
	 */
	static Map<Identifier, Integer> assignRegions(Collection<Identifier> tests) {
		if (tests.size() > REGION_COUNT) throw new IllegalStateException("there are more tests than regions for their sites");

		Map<Identifier, Integer> assigned = new HashMap<>();
		Set<Integer> taken = new HashSet<>();

		for (Identifier test : tests.stream().sorted().toList()) {
			int region = Math.floorMod(HashCommon.murmurHash3(test.toString().hashCode()), REGION_COUNT);

			while (!taken.add(region)) {
				region = (region + 1) % REGION_COUNT;
			}

			assigned.put(test, region);
		}

		return assigned;
	}

	/** The chunk a structure starts in at a site of the first row. */
	ChunkPos chunk(int index) {
		return chunk(0, index);
	}

	/** The chunk a structure starts in at a site. A test that searches twice gives each search a row. */
	ChunkPos chunk(int row, int index) {
		if (row < 0 || row >= ROWS) throw new IndexOutOfBoundsException("a test has " + ROWS + " rows of sites, not a row " + row);
		if (index < 0 || index >= COLUMNS) throw new IndexOutOfBoundsException("a row has " + COLUMNS + " sites, not a site " + index);

		return new ChunkPos(corner.x() + row * CHUNKS_APART + CHUNKS_APART / 2, corner.z() + index * CHUNKS_APART + CHUNKS_APART / 2);
	}

	/** Whether the chunk is part of this test's ground. */
	boolean holds(ChunkPos chunk) {
		int fromCornerX = chunk.x() - corner.x();
		int fromCornerZ = chunk.z() - corner.z();

		return fromCornerX >= 0 && fromCornerX < REGION_CHUNKS_X && fromCornerZ >= 0 && fromCornerZ < REGION_CHUNKS_Z;
	}
}
