package mapleflavored.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Screenshots of a Blood Moon's sky and moon next to an ordinary full moon's, for the hand check. Run with
 * {@code gradlew runClientGameTest}; the shots land in {@code build/run/clientGameTest/screenshots/}.
 */
public class BloodMoonSkyClientGameTest implements FabricClientGameTest {
	// Midnight of day 24, the first Blood Moon, and of day 16, the full moon before it.
	private static final long BLOOD_MOON_MIDNIGHT = 24 * 24_000 + 18_000;
	private static final long FULL_MOON_MIDNIGHT = 16 * 24_000 + 18_000;
	// Dusk of day 24, as the Blood Moon rises.
	private static final long BLOOD_MOON_DUSK = 24 * 24_000 + 13_000;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();

			shoot(context, world, FULL_MOON_MIDNIGHT, "-85", "full_moon_overhead");
			shoot(context, world, BLOOD_MOON_MIDNIGHT, "-85", "blood_moon_overhead");
			shoot(context, world, BLOOD_MOON_MIDNIGHT, "0", "blood_moon_horizon");
			shoot(context, world, BLOOD_MOON_DUSK, "-10", "blood_moon_dusk");
		}
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext world, long time, String pitch, String name) {
		world.getServer().runCommand("time set " + time);
		world.getServer().runCommand("tp @a 0 ~ 0 -90 " + pitch);
		context.waitTicks(10);
		context.takeScreenshot("blood_moon_" + name);
	}
}
