package mapleflavored.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Proves the GameTest harness runs: a mock player joins a headless server and stands in a test structure.
 */
public class SmokeGameTests {
	@GameTest
	public void freshPlayerHasTwentyHealth(GameTestHelper helper) {
		BlockPos standingPos = new BlockPos(1, 1, 1);
		Vec3 spawn = helper.absoluteVec(Vec3.atBottomCenterOf(standingPos));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		player.setGameMode(GameType.SURVIVAL);
		player.teleportTo(spawn.x, spawn.y, spawn.z);

		helper.assertEntityInstancePresent(player, standingPos);
		helper.assertValueEqual(20.0F, player.getMaxHealth(), "max health");
		helper.assertValueEqual(20.0F, player.getHealth(), "health");
		helper.succeed();
	}
}
