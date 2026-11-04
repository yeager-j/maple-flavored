package mapleflavored.gamerules;

import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

/**
 * The game rules that set what death costs, each on its own. Vanilla's Keep Inventory is the third, defaulting to on.
 */
public final class MapleGameRules {
	/** When on, death costs no Max Hearts. Off by default. */
	public static final GameRule<Boolean> KEEP_HEARTS = GameRuleBuilder.forBoolean(false)
		.category(GameRuleCategory.PLAYER)
		.buildAndRegister(MapleFlavored.id("keep_hearts"));

	/** When on, death keeps all XP and leaves no Remnant. Off by default. Vanilla's Keep Inventory no longer decides XP. */
	public static final GameRule<Boolean> KEEP_XP = GameRuleBuilder.forBoolean(false)
		.category(GameRuleCategory.PLAYER)
		.buildAndRegister(MapleFlavored.id("keep_xp"));

	private MapleGameRules() {
	}

	/** Registers the game rules. Call once from mod init. */
	public static void register() {
	}
}
