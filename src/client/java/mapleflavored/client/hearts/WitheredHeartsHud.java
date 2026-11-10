package mapleflavored.client.hearts;

import mapleflavored.hearts.HeartCounts;
import mapleflavored.hearts.Hearts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Draws a player's Withered Hearts in the health bar as hollow empty hearts, after their usable hearts and before any
 * absorption hearts.
 *
 * <p>A client is only sent its own player's Withered Hearts, so a spectated player's bar shows none.
 */
public final class WitheredHeartsHud {
	private static final int HEART_SIZE = 9;
	private static final double HEALTH_PER_HEART = 2.0;

	// The middle of the empty heart, left out of a withered slot so the heart is hollow: just the outline and a
	// one-pixel rim. One string per row of the 9 × 9 heart.
	private static final String[] HOLLOW = {
		".........",
		".........",
		"..##.##..",
		"..#####..",
		"..#####..",
		"...###...",
		"....#....",
		".........",
		"........."
	};

	private WitheredHeartsHud() {
	}

	/**
	 * The max health the health bar should lay out for: the player's own, which only covers usable hearts, widened to
	 * make room for their withered slots.
	 */
	public static double maxHealthWithWitheredSlots(Player player, double maxHealth) {
		return maxHealth + witheredHearts(player) * HEALTH_PER_HEART;
	}

	/** Health remembered from before a heart withered, cut to the usable hearts so it doesn't blink in a withered slot. */
	public static int rememberedHealthWithinUsableHearts(Player player, int rememberedHealth) {
		return Math.min(rememberedHealth, Mth.ceil(player.getMaxHealth()));
	}

	/**
	 * Draws a hollow heart at {@code x}, {@code y} if the heart container there is one of the player's withered slots.
	 *
	 * @param containerIndex the container's place in the health bar, counted from 0
	 * @return whether it drew, in which case vanilla's own empty heart must not be drawn there
	 */
	public static boolean drawWitheredSlot(
		GuiGraphicsExtractor graphics, Player player, int containerIndex, int x, int y, boolean hardcore, boolean blinks
	) {
		int usableHearts = Mth.ceil(player.getMaxHealth() / 2.0F);
		boolean withered = containerIndex >= usableHearts && containerIndex < usableHearts + witheredHearts(player);

		if (!withered) return false;

		drawHollowed(graphics, emptyHeartSprite(hardcore, blinks), x, y);

		return true;
	}

	private static int witheredHearts(Player player) {
		return Hearts.known(player).map(HeartCounts::witheredHearts).orElse(0);
	}

	// The sprite ids vanilla's Hud.HeartType.CONTAINER resolves to, which it keeps private.
	private static Identifier emptyHeartSprite(boolean hardcore, boolean blinks) {
		String sprite = hardcore ? "hud/heart/container_hardcore" : "hud/heart/container";

		return Identifier.withDefaultNamespace(blinks ? sprite + "_blinking" : sprite);
	}

	// Draws each row of the sprite in runs that skip the hollow's pixels, leaving them transparent.
	private static void drawHollowed(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y) {
		for (int row = 0; row < HEART_SIZE; row++) {
			int runStart = 0;

			for (int column = 0; column <= HEART_SIZE; column++) {
				boolean runEnds = column == HEART_SIZE || HOLLOW[row].charAt(column) == '#';

				if (!runEnds) continue;

				if (column > runStart) {
					graphics.blitSprite(
						RenderPipelines.GUI_TEXTURED, sprite, HEART_SIZE, HEART_SIZE, runStart, row, x + runStart, y + row, column - runStart, 1
					);
				}

				runStart = column + 1;
			}
		}
	}
}
