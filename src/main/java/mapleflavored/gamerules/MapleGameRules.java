package mapleflavored.gamerules;

import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

/**
 * Maple's game rules: those that set what death costs, each on its own, with vanilla's Keep Inventory as the third,
 * defaulting to on; playtest values for Warden and Wither Subspaces, which apply to Subspaces that open after a change,
 * except the Wither's leash and its Subspace's light level, which apply at once; how near a Warden a player must be to
 * survive it; how far a held Echo Shard hears shriekers; and Shakudo's and Adamant's mining, which apply at once.
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

	/**
	 * How far from a Warden a player must be, in blocks, when it digs back down or dies, to be credited with surviving it.
	 * Applies at once.
	 */
	public static final GameRule<Integer> WARDEN_SURVIVAL_RANGE = GameRuleBuilder.forInteger(50)
		.range(1, 256)
		.category(GameRuleCategory.MOBS)
		.buildAndRegister(MapleFlavored.id("warden_survival_range"));

	/** How far a Wither's Subspace reaches, in blocks. */
	public static final GameRule<Integer> WITHER_SUBSPACE_RADIUS = GameRuleBuilder.forInteger(100)
		.range(1, 256)
		.category(GameRuleCategory.MOBS)
		.buildAndRegister(MapleFlavored.id("wither_subspace_radius"));

	/** How far from its Subspace's centre a Wither can move, in blocks. Applies at once. */
	public static final GameRule<Integer> WITHER_LEASH_RADIUS = GameRuleBuilder.forInteger(50)
		.range(1, 256)
		.category(GameRuleCategory.MOBS)
		.buildAndRegister(MapleFlavored.id("wither_leash_radius"));

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

	/** How much faster a Shakudo tool mines, as a percentage of the final break speed. Applies at once. */
	public static final GameRule<Integer> SHAKUDO_MINING_BONUS = GameRuleBuilder.forInteger(30)
		.range(0, 1000)
		.category(GameRuleCategory.PLAYER)
		.buildAndRegister(MapleFlavored.id("shakudo_mining_bonus"));

	/**
	 * The hardness an Adamant tool mines the blocks it's made for at, at most: 3 is deepslate's. Harder blocks mine as
	 * if they were this hard. Applies at once.
	 */
	public static final GameRule<Integer> ADAMANT_HARDNESS_CAP = GameRuleBuilder.forInteger(3)
		.range(1, 100)
		.category(GameRuleCategory.PLAYER)
		.buildAndRegister(MapleFlavored.id("adamant_hardness_cap"));

	/** How many ticks an Adamant pickaxe with Efficiency V, under Haste II, takes to break bedrock. Applies at once. */
	public static final GameRule<Integer> ADAMANT_BEDROCK_BREAK_TIME = GameRuleBuilder.forInteger(300)
		.range(1, 72_000)
		.category(GameRuleCategory.PLAYER)
		.buildAndRegister(MapleFlavored.id("adamant_bedrock_break_time"));

	private MapleGameRules() {
	}

	/** Registers the game rules. Call once from mod init. */
	public static void register() {
	}
}
