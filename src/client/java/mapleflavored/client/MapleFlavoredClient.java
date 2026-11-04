package mapleflavored.client;

import mapleflavored.client.hearts.DeathScreenMaxHearts;
import mapleflavored.client.items.CrystalHeartActivation;
import mapleflavored.client.remnants.RemnantRenderer;
import mapleflavored.client.zephyr.ZephyrInput;
import net.fabricmc.api.ClientModInitializer;

public class MapleFlavoredClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		DeathScreenMaxHearts.register();
		CrystalHeartActivation.register();
		RemnantRenderer.register();
		ZephyrInput.register();
	}
}
