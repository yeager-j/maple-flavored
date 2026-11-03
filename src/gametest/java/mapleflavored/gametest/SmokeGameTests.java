package mapleflavored.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

/**
 * Proves the GameTest harness runs: a mock player spawns in a test structure on a headless server.
 */
public class SmokeGameTests {
	@GameTest
	public void freshPlayerHasTwentyHealth(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);

		helper.assertValueEqual(20.0F, player.getMaxHealth(), "max health");
		helper.assertValueEqual(20.0F, player.getHealth(), "health");
		helper.succeed();
	}
}
