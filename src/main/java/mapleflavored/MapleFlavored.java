package mapleflavored;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapleFlavored implements ModInitializer {
	public static final String MOD_ID = "maple_flavored";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Maple Flavored initialized");
	}
}
