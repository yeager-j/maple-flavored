package mapleflavored.client;

import mapleflavored.client.hearts.DeathScreenMaxHearts;
import net.fabricmc.api.ClientModInitializer;

public class MapleFlavoredClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		DeathScreenMaxHearts.register();
	}
}
