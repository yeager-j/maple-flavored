package mapleflavored.gametest.client;

import java.util.Optional;
import mapleflavored.hearts.HeartCounts;
import mapleflavored.hearts.Hearts;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * A client is sent its own player's hearts, and its health bar draws their Withered Hearts hollow. Run with
 * {@code gradlew runClientGameTest}; the shot of the health bar, for checking by eye, lands in
 * {@code build/run/clientGameTest/screenshots/}.
 */
public class WitheredHeartsClientGameTest implements FabricClientGameTest {
	// Two hearts wither, at 5 and 10 seconds, and the effect ends 10 seconds before the first would return.
	private static final int WITHER_SECONDS = 11;
	private static final int TICKS_UNTIL_WITHER_ENDS = (WITHER_SECONDS + 1) * 20;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamemode survival @a");
			context.waitTicks(20);

			assertKnown(context, new HeartCounts(10, 0), "a new player's hearts");

			world.getServer().runCommand("effect give @a minecraft:wither " + WITHER_SECONDS + " 0 true");
			context.waitTicks(TICKS_UNTIL_WITHER_ENDS);

			assertKnown(context, new HeartCounts(10, 2), "hearts after 11 seconds of Wither");
			context.takeScreenshot("withered_hearts_hud");
		}
	}

	private static void assertKnown(ClientGameTestContext context, HeartCounts expected, String what) {
		Optional<HeartCounts> known = context.computeOnClient(client -> Hearts.known(client.player));

		if (!known.equals(Optional.of(expected))) {
			throw new AssertionError(what + " on the client: expected " + expected + ", got " + known);
		}
	}
}
