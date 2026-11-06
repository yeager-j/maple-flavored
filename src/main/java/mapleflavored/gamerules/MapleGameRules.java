package mapleflavored.gamerules;

import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

/**
 * Maple's game rules: those that set what death costs, each on its own, with vanilla's Keep Inventory as the third,
 * defaulting to on; playtest values for Warden and Wither Subspaces, which apply to Subspaces that open after a change,
 * except the Wither Subspace's light level, which applies at once; and how far a held Echo Shard hears shriekers.
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

	/** How far a Warden's Subspace reaches, in blocks. */
	public static final GameRule<Integer> WARDEN_SUBSPACE_RADIUS = GameRuleBuilder.forInteger(50)
		.range(1, 256)
		.category(GameRuleCategory.MOBS)
		.buildAndRegister(MapleFlavored.id("warden_subspace_radius"));

	/** How far from where a Warden appears its Subspace's sculk patch may spread, in blocks. */
	public static final GameRule<Integer> WARDEN_SCULK_PATCH_RADIUS = GameRuleBuilder.forInteger(5)
		.range(0, 32)
		.category(GameRuleCategory.MOBS)
		.buildAndRegister(MapleFlavored.id("warden_sculk_patch_radius"));

	/**
	 * How much sculk a Warden's Subspace spreads where the Warden appears, as a sculk catalyst's charge: each block turned
	 * to sculk costs 1, and each sensor or shrieker grown 10. 0 spreads none.
	 */
	public static final GameRule<Integer> WARDEN_SCULK_PATCH_CHARGE = GameRuleBuilder.forInteger(150)
		.range(0, 10_000)
		.category(GameRuleCategory.MOBS)
		.buildAndRegister(MapleFlavored.id("warden_sculk_patch_charge"));

	/** How far a Wither's Subspace reaches, in blocks. */
	public static final GameRule<Integer> WITHER_SUBSPACE_RADIUS = GameRuleBuilder.forInteger(100)
		.range(1, 256)
		.category(GameRuleCategory.MOBS)
		.buildAndRegister(MapleFlavored.id("wither_subspace_radius"));

	/** The light level clients draw everything inside a Wither's Subspace at, from 0 to 15. */
	public static final GameRule<Integer> WITHER_SUBSPACE_LIGHT_LEVEL = GameRuleBuilder.forInteger(3)
		.range(0, 15)
		.category(GameRuleCategory.MOBS)
		.buildAndRegister(MapleFlavored.id("wither_subspace_light_level"));

	/** How far a held Echo Shard hears sculk shriekers, in blocks. */
	public static final GameRule<Integer> ECHO_SHARD_SHRIEKER_RANGE = GameRuleBuilder.forInteger(15)
		.range(1, 64)
		.category(GameRuleCategory.PLAYER)
		.buildAndRegister(MapleFlavored.id("echo_shard_shrieker_range"));

	private MapleGameRules() {
	}

	/** Registers the game rules. Call once from mod init. */
	public static void register() {
	}
}
