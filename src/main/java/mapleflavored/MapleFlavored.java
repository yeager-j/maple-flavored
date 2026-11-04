package mapleflavored;

import mapleflavored.gamerules.MapleGameRules;
import mapleflavored.hearts.Hearts;
import mapleflavored.items.MapleItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapleFlavored implements ModInitializer {
	public static final String MOD_ID = "maple_flavored";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** An identifier in the mod's namespace. */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		MapleGameRules.register();
		Hearts.register();
		MapleItems.register();

		LOGGER.info("Maple Flavored initialized");
	}
}
