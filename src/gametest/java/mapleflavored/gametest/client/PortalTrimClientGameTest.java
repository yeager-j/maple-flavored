package mapleflavored.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;

/**
 * PROTOTYPE: screenshots of one Alloy's armor next to Electrum's plain gold, for checking the starfield trim by eye.
 * Run with {@code gradlew runClientGameTest}; the shots land in {@code build/run/clientGameTest/screenshots/}.
 */
public class PortalTrimClientGameTest implements FabricClientGameTest {
	// The Alloy to show. Its trim is only a starfield if it is in PortalTrim's ALLOYS.
	private static final String ALLOY = "adamant";
	private static final String ARMOR = "{head:{id:\"maple_flavored:%1$s_helmet\"},chest:{id:\"maple_flavored:%1$s_chestplate\"},"
		+ "legs:{id:\"maple_flavored:%1$s_leggings\"},feet:{id:\"maple_flavored:%1$s_boots\"}}";

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("time set 13000");
			world.getServer().runCommand("tp @a 0 ~ 0 0 15");
			world.getServer().runCommand(String.format("summon armor_stand -0.8 ~ 2.5 {Rotation:[180f,0f],ShowArms:1b,equipment:%s}", ARMOR.formatted(ALLOY)));
			world.getServer().runCommand(String.format("summon armor_stand 0.8 ~ 2.5 {Rotation:[180f,0f],ShowArms:1b,equipment:%s}", ARMOR.formatted("electrum")));
			world.getServer().runCommand("summon horse 3.5 ~ 4 {Tame:1b,NoAI:1b,Rotation:[90f,0f],equipment:{body:{id:\"maple_flavored:" + ALLOY + "_horse_armor\"}}}");
			for (String slot : new String[] {"head:helmet", "chest:chestplate", "legs:leggings", "feet:boots"}) {
				String[] parts = slot.split(":");
				world.getServer().runCommand("item replace entity @a armor." + parts[0] + " with maple_flavored:" + ALLOY + "_" + parts[1]);
			}
			context.waitTicks(40);
			world.getConnection().waitForChunksRender();

			context.takeScreenshot("portal_trim_stands_1");
			context.waitTicks(10);
			context.takeScreenshot("portal_trim_stands_2");

			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(5);
			context.takeScreenshot("portal_trim_player");
		}
	}
}
