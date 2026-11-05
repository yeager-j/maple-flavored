package mapleflavored.client;

import mapleflavored.client.enchanting.AppleTooltip;
import mapleflavored.client.enchanting.EnchantingTableScreen;
import mapleflavored.client.hearts.DeathScreenMaxHearts;
import mapleflavored.client.items.CrystalHeartActivation;
import mapleflavored.client.remnants.RemnantRenderer;
import mapleflavored.client.spawners.SpawnerForcefieldRenderer;
import mapleflavored.client.structurebooks.StructureBookRenderer;
import mapleflavored.client.zephyr.ZephyrInput;
import mapleflavored.enchanting.Enchanting;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class MapleFlavoredClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		DeathScreenMaxHearts.register();
		CrystalHeartActivation.register();
		RemnantRenderer.register();
		ZephyrInput.register();
		MenuScreens.register(Enchanting.MENU, EnchantingTableScreen::new);
		AppleTooltip.register();
		StructureBookRenderer.register();
		SpawnerForcefieldRenderer.register();
	}
}
