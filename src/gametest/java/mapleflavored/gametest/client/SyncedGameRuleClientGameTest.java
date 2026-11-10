package mapleflavored.gametest.client;

import mapleflavored.alloys.AlloyMining;
import mapleflavored.subspaces.WitherSubspaces;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * A client reads the game rules it is sent: the Wither Subspace's light level, which it draws, and the Shakudo mining
 * bonus, which its mining prediction uses. Run with {@code gradlew runClientGameTest}.
 */
public class SyncedGameRuleClientGameTest implements FabricClientGameTest {
	private static final float EPSILON = 1.0E-4F;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("item replace entity @a weapon.mainhand with maple_flavored:shakudo_pickaxe");
			context.waitTicks(20);

			assertLightLevel(context, 3, "the default light level");
			assertMiningSpeed(context, 1.3F, "a Shakudo tool's default bonus");

			world.getServer().runCommand("gamerule maple_flavored:wither_subspace_light_level 9");
			world.getServer().runCommand("gamerule maple_flavored:shakudo_mining_bonus 100");
			context.waitTicks(20);

			assertLightLevel(context, 9, "the light level after the rule changes");
			assertMiningSpeed(context, 2.0F, "a Shakudo tool's bonus after the rule changes");
		}
	}

	private static void assertLightLevel(ClientGameTestContext context, int expected, String what) {
		int lightLevel = context.computeOnClient(client -> WitherSubspaces.lightLevel(client.level));

		if (lightLevel != expected) {
			throw new AssertionError(what + " on the client: expected " + expected + ", got " + lightLevel);
		}
	}

	private static void assertMiningSpeed(ClientGameTestContext context, float expected, String what) {
		float speed = context.computeOnClient(client -> AlloyMining.destroySpeed(client.player, 1.0F));

		if (Math.abs(speed - expected) > EPSILON) {
			throw new AssertionError(what + " on the client: expected a speed of " + expected + ", got " + speed);
		}
	}
}
