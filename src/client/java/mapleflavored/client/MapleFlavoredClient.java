package mapleflavored.client;

import mapleflavored.alloys.Alloys;
import mapleflavored.client.alloys.AlloyFurnaceScreen;
import mapleflavored.client.alloys.PortalTrim;
import mapleflavored.client.deepvariants.DeepCreeperRenderer;
import mapleflavored.client.deepvariants.DeepSkeletonRenderer;
import mapleflavored.client.deepvariants.DeepSpiderRenderer;
import mapleflavored.client.deepvariants.DeepZombieRenderer;
import mapleflavored.client.enchanting.AppleTooltip;
import mapleflavored.client.enchanting.EnchantingTableScreen;
import mapleflavored.client.hearts.DeathScreenMaxHearts;
import mapleflavored.client.items.CrystalHeartActivation;
import mapleflavored.client.remnants.RemnantRenderer;
import mapleflavored.client.spawners.SpawnerHeatRenderer;
import mapleflavored.client.structurebooks.StructureBookRenderer;
import mapleflavored.client.subspaces.SubspaceShellRenderer;
import mapleflavored.client.subspaces.WitherSubspaceLight;
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
		MenuScreens.register(Alloys.ALLOY_FURNACE_MENU, AlloyFurnaceScreen::new);
		AppleTooltip.register();
		StructureBookRenderer.register();
		SpawnerHeatRenderer.register();
		SubspaceShellRenderer.register();
		WitherSubspaceLight.register();
		PortalTrim.register();
		DeepZombieRenderer.register();
		DeepSkeletonRenderer.register();
		DeepCreeperRenderer.register();
		DeepSpiderRenderer.register();
	}
}
