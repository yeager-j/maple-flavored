package mapleflavored.gametest;

import mapleflavored.hearts.Hearts;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/** Gives test players Max Hearts and checks the Max Hearts they have. */
final class MaxHearts {
	private MaxHearts() {
	}

	/** A loaded mock player in Survival with the given Max Hearts, at least the 10 every player starts with. */
	static ServerPlayer survivalPlayerWithMaxHearts(GameTestHelper helper, int maxHearts) {
		ServerPlayer player = MockPlayers.survivalPlayer(helper);

		while (Hearts.maxHearts(player) < maxHearts) {
			Hearts.addMaxHeart(player);
		}

		return player;
	}

	/** Fails the test unless the player has these Max Hearts and the max health they give. */
	static void assertMaxHearts(GameTestHelper helper, ServerPlayer player, int maxHearts) {
		helper.assertValueEqual(Hearts.maxHearts(player), maxHearts, "Max Hearts");
		helper.assertValueEqual(player.getMaxHealth(), maxHearts * 2.0F, "max health");
	}
}
