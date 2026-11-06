package mapleflavored.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;

/**
 * Screenshots of worn Armored Elytra, for checking by eye: a plain, a trimmed and an enchanted one on armor stands,
 * front and back, and one on the player. Run with {@code gradlew runClientGameTest}; the shots land in
 * {@code build/run/clientGameTest/screenshots/}.
 */
public class ArmoredElytraClientGameTest implements FabricClientGameTest {
	private static final String[] CHEST_PIECES = {
		"{id:\"maple_flavored:steel_armored_elytra\"}",
		"{id:\"maple_flavored:adamant_armored_elytra\",components:{\"minecraft:trim\":{material:\"minecraft:quartz\",pattern:\"minecraft:coast\"}}}",
		"{id:\"maple_flavored:electrum_armored_elytra\",components:{\"minecraft:enchantments\":{\"minecraft:protection\":1}}}"
	};

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("tp @a 0 ~ 0 0 15");

			for (int stand = 0; stand < CHEST_PIECES.length; stand++) {
				double x = (stand - 1) * 2.0;

				summonStand(world, x, 5.0, 180, CHEST_PIECES[stand]);
				summonStand(world, x, 8.0, 0, CHEST_PIECES[stand]);
			}

			for (String piece : new String[] {"head:orichalcum_helmet", "chest:orichalcum_armored_elytra", "legs:orichalcum_leggings", "feet:orichalcum_boots"}) {
				String[] parts = piece.split(":");

				world.getServer().runCommand("item replace entity @a armor." + parts[0] + " with maple_flavored:" + parts[1]);
			}

			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			context.waitTicks(40);
			world.getConnection().waitForChunksRender();
			context.takeScreenshot("armored_elytra_stands");

			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(5);
			context.takeScreenshot("armored_elytra_player_front");

			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(5);
			context.takeScreenshot("armored_elytra_player_back");
		}
	}

	private static void summonStand(TestSingleplayerContext world, double x, double z, int yaw, String chest) {
		world.getServer().runCommand(String.format(
			"summon armor_stand %s ~ %s {Rotation:[%sf,0f],ShowArms:1b,equipment:{chest:%s}}", x, z, yaw, chest
		));
	}
}
