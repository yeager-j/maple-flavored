package mapleflavored.gametest;

import java.util.Optional;
import mapleflavored.bloodmoons.RestoreSpot;
import mapleflavored.bloodmoons.RestoringAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

/** Restore spots as tests read them, and a Blood Moon that rises over one test's spots alone. */
final class Spots {
	private Spots() {
	}

	/** The spot the entity holds. Fails the test if it holds none. */
	static RestoreSpot of(GameTestHelper helper, Entity entity) {
		Optional<RestoreSpot> spot = RestoringAccess.spotOf(helper.getLevel().getServer(), entity);

		helper.assertTrue(spot.isPresent(), "the mob holds a spot");

		return spot.orElseThrow();
	}

	/** The spot as its level holds it now. */
	static RestoreSpot current(ServerLevel level, RestoreSpot spot) {
		return RestoringAccess.spots(level).stream().filter(saved -> saved.id().equals(spot.id())).findFirst().orElseThrow();
	}

	/**
	 * A Blood Moon over the spots on the test's own {@linkplain TestSites ground}. Tests run side by side in one level, and
	 * a Blood Moon over the whole level would make the other tests' empty spots pending.
	 */
	static void riseBloodMoon(GameTestHelper helper) {
		TestSites sites = TestSites.of(helper);

		RestoringAccess.riseBloodMoonOver(helper.getLevel(), spot -> sites.holds(ChunkPos.containing(BlockPos.containing(spot.position()))));
	}
}
