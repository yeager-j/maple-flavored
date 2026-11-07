package mapleflavored.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;

/**
 * Screenshots of worn Reinforced armor, for checking by eye the chainmail beneath it: plain and Reinforced iron, a
 * Reinforced trimmed diamond set, and a Reinforced Armored Elytra, on armor stands front and back. Run with
 * {@code gradlew runClientGameTest}; the shots land in {@code build/run/clientGameTest/screenshots/}.
 */
public class ReinforcementClientGameTest implements FabricClientGameTest {
	private static final String REINFORCED = "\"maple_flavored:reinforced\":{}";
	private static final String TRIM = "\"minecraft:trim\":{material:\"minecraft:gold\",pattern:\"minecraft:sentry\"}";
	private static final String[][] SETS = {
		set("iron", ""),
		set("iron", REINFORCED),
		set("diamond", REINFORCED + "," + TRIM)
	};

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("tp @a 0 ~ 0 0 15");

			for (int stand = 0; stand < SETS.length; stand++) {
				double x = (stand - 1.5) * 1.5;

				summonStand(world, x, 3.5, 180, SETS[stand]);
				summonStand(world, x, 6.0, 0, SETS[stand]);
			}

			String[] elytra = set("diamond", REINFORCED);

			elytra[1] = "{id:\"maple_flavored:adamant_armored_elytra\",components:{" + REINFORCED + "}}";
			summonStand(world, 2.25, 3.5, 180, elytra);
			summonStand(world, 2.25, 6.0, 0, elytra);

			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			context.waitTicks(40);
			world.getConnection().waitForChunksRender();
			context.takeScreenshot("reinforcement_stands");
		}
	}

	// Helmet, chestplate, leggings and boots of the material, each with the components.
	private static String[] set(String material, String components) {
		String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
		String[] set = new String[pieces.length];

		for (int i = 0; i < pieces.length; i++) {
			set[i] = "{id:\"minecraft:" + material + "_" + pieces[i] + "\",components:{" + components + "}}";
		}

		return set;
	}

	private static void summonStand(TestSingleplayerContext world, double x, double z, int yaw, String[] set) {
		world.getServer().runCommand(String.format(
			"summon armor_stand %s ~ %s {Rotation:[%sf,0f],ShowArms:1b,equipment:{head:%s,chest:%s,legs:%s,feet:%s}}",
			x, z, yaw, set[0], set[1], set[2], set[3]
		));
	}
}
