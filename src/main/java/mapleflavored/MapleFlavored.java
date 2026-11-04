package mapleflavored;

import mapleflavored.advancements.MapleTriggers;
import mapleflavored.enchanting.Enchanting;
import mapleflavored.gamerules.MapleGameRules;
import mapleflavored.hearts.Hearts;
import mapleflavored.items.MapleItems;
import mapleflavored.loot.LootSubstitutions;
import mapleflavored.remnants.Remnants;
import mapleflavored.zephyr.Zephyr;
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
		MapleTriggers.register();
		Hearts.register();
		MapleItems.register();
		Remnants.register();
		LootSubstitutions.register();
		Zephyr.register();
		Enchanting.register();

		LOGGER.info("Maple Flavored initialized");
	}
}
