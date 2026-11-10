package mapleflavored.gametest;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Looks up loaded advancements and whether a player has earned them. */
final class Advancements {
	private Advancements() {
	}

	/** The loaded advancement, failing the test if it doesn't exist. */
	static AdvancementHolder advancement(GameTestHelper helper, Identifier id) {
		AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(id);

		helper.assertTrue(holder != null, id + " exists");

		return holder;
	}

	/** Whether the player has earned the advancement, failing the test if it doesn't exist. */
	static boolean isDone(GameTestHelper helper, ServerPlayer player, Identifier advancement) {
		return player.getAdvancements().getOrStartProgress(advancement(helper, advancement)).isDone();
	}
}
