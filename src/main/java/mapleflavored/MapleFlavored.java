package mapleflavored;

import mapleflavored.advancements.MapleTriggers;
import mapleflavored.alloys.Alloys;
import mapleflavored.bloodmoons.BloodMoons;
import mapleflavored.bosses.BossKillCredit;
import mapleflavored.brewing.Brewing;
import mapleflavored.deepvariants.DeepVariants;
import mapleflavored.echoshard.ShriekerPulses;
import mapleflavored.enchanting.Enchanting;
import mapleflavored.gamerules.MapleGameRules;
import mapleflavored.grounded.Grounded;
import mapleflavored.hearts.Hearts;
import mapleflavored.items.MapleItems;
import mapleflavored.loot.Loot;
import mapleflavored.mobs.Mobs;
import mapleflavored.reinforcement.Reinforcement;
import mapleflavored.remnants.Remnants;
import mapleflavored.spawners.Spawners;
import mapleflavored.strongholds.StrongholdIllagers;
import mapleflavored.structurebooks.StructureBooks;
import mapleflavored.subspaces.Subspaces;
import mapleflavored.trades.BookTrade;
import mapleflavored.wardensummoning.WardenSummoning;
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

	/**
	 * Each feature that registers anything has one call here. A feature that is absent is driven only by mixins, which
	 * {@code maple_flavored.mixins.json} lists.
	 */
	@Override
	public void onInitialize() {
		MapleGameRules.register();
		Mobs.register();
		MapleTriggers.register();
		Hearts.register();
		MapleItems.register();
		Alloys.register();
		Reinforcement.register();
		BookTrade.register();
		Remnants.register();
		Loot.register();
		Grounded.register();
		Zephyr.register();
		Enchanting.register();
		Brewing.register();
		StructureBooks.register();
		StrongholdIllagers.register();
		Spawners.register();
		Subspaces.register();
		WardenSummoning.register();
		BossKillCredit.register();
		ShriekerPulses.register();
		BloodMoons.register();
		DeepVariants.register();

		LOGGER.info("Maple Flavored initialized");
	}
}
