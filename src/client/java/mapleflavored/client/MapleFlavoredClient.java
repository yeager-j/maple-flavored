package mapleflavored.client;

import mapleflavored.client.alloys.AlloyFurnaceScreen;
import mapleflavored.client.deepvariants.DeepVariantRenderers;
import mapleflavored.client.enchanting.EnchantingClient;
import mapleflavored.client.hearts.HeartsClient;
import mapleflavored.client.remnants.RemnantRenderer;
import mapleflavored.client.spawners.SpawnerHeatRenderer;
import mapleflavored.client.structurebooks.StructureBookRenderer;
import mapleflavored.client.subspaces.SubspacesClient;
import mapleflavored.client.zephyr.ZephyrInput;
import net.fabricmc.api.ClientModInitializer;

public class MapleFlavoredClient implements ClientModInitializer {
	/**
	 * Each feature that registers anything on the client has one call here. A feature that is absent is driven only by
	 * mixins, which {@code maple_flavored.client.mixins.json} lists.
	 */
	@Override
	public void onInitializeClient() {
		HeartsClient.register();
		RemnantRenderer.register();
		ZephyrInput.register();
		EnchantingClient.register();
		AlloyFurnaceScreen.register();
		StructureBookRenderer.register();
		SpawnerHeatRenderer.register();
		SubspacesClient.register();
		DeepVariantRenderers.register();
	}
}
