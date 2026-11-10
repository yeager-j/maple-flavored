package mapleflavored.client.enchanting;

import mapleflavored.enchanting.Enchanting;
import net.minecraft.client.gui.screens.MenuScreens;

/** The client's side of enchanting: the enchanting table's screen and the enchanted golden apple's tooltip. */
public final class EnchantingClient {
	private EnchantingClient() {
	}

	/** Registers the screen and the tooltip. Call once from client init. */
	public static void register() {
		MenuScreens.register(Enchanting.MENU, EnchantingTableScreen::new);
		AppleTooltip.register();
	}
}
