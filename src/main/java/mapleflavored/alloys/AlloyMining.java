package mapleflavored.alloys;

import java.util.Optional;
import mapleflavored.advancements.MapleTriggers;
import mapleflavored.gamerules.MapleGameRules;
import mapleflavored.gamerules.SyncedGameRule;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shakudo's and Adamant's mining: Shakudo tools mine faster, Adamant tools mine hard blocks as if they were deepslate,
 * and an Adamant pickaxe can break bedrock. The client predicts mining and the server checks it, so these work on both
 * sides, reading game rules that are synced to every client.
 */
public final class AlloyMining {
	// How much faster Shakudo tools mine, in percent.
	private static final SyncedGameRule SHAKUDO_MINING_BONUS = SyncedGameRule.of(MapleGameRules.SHAKUDO_MINING_BONUS);
	// The most hardness an Adamant tool mines the blocks it's made for at.
	private static final SyncedGameRule ADAMANT_HARDNESS_CAP = SyncedGameRule.of(MapleGameRules.ADAMANT_HARDNESS_CAP);
	// Ticks to break bedrock.
	private static final SyncedGameRule ADAMANT_BEDROCK_BREAK_TIME = SyncedGameRule.of(MapleGameRules.ADAMANT_BEDROCK_BREAK_TIME);

	// What breaking bedrock takes, besides an Adamant pickaxe: Efficiency V and Haste II.
	private static final int BEDROCK_EFFICIENCY_LEVEL = 5;
	private static final int BEDROCK_HASTE_AMPLIFIER = 1;

	private AlloyMining() {
	}

	static void register() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> creditBedrock(player, state));
	}

	/** The player's final break speed with a Shakudo tool's bonus, given the speed vanilla works out. */
	public static float destroySpeed(Player player, float speed) {
		if (!holdsTool(player, Alloy.SHAKUDO)) return speed;

		return speed * (1.0F + SHAKUDO_MINING_BONUS.get(player.level()) / 100.0F);
	}

	/**
	 * The hardness the player mines the block at: at most the hardness cap when they hold an Adamant tool made for the
	 * block, else the block's own. Unbreakable blocks, at -1, stay unbreakable.
	 */
	public static float hardness(Player player, BlockState state, float hardness) {
		if (!holdsToolFor(player, state, Alloy.ADAMANT)) return hardness;

		return Math.min(hardness, ADAMANT_HARDNESS_CAP.get(player.level()));
	}

	/**
	 * The share of the block the player mines each tick where Adamant sets it: bedrock, for a player who can break it
	 * with an Adamant pickaxe with Efficiency V or higher, under Haste II or higher. Empty for every other block and
	 * player, which leaves the progress to vanilla.
	 */
	public static Optional<Float> bedrockProgress(Player player, BlockState state) {
		if (!state.is(Blocks.BEDROCK) || !breaksBedrock(player)) return Optional.empty();

		return Optional.of(1.0F / ADAMANT_BEDROCK_BREAK_TIME.get(player.level()));
	}

	private static boolean breaksBedrock(Player player) {
		ItemStack tool = player.getMainHandItem();
		boolean adamantPickaxe = AlloyGear.pieceOf(tool).equals(Optional.of(new AlloyGear.Piece(Alloy.ADAMANT, GearKind.PICKAXE)));
		Holder<Enchantment> efficiency = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY);
		MobEffectInstance haste = player.getEffect(MobEffects.HASTE);

		return adamantPickaxe
			&& EnchantmentHelper.getItemEnchantmentLevel(efficiency, tool) >= BEDROCK_EFFICIENCY_LEVEL
			&& haste != null
			&& haste.getAmplifier() >= BEDROCK_HASTE_AMPLIFIER;
	}

	private static boolean holdsTool(Player player, Alloy alloy) {
		return AlloyGear.pieceOf(player.getMainHandItem()).filter(piece -> piece.isToolOf(alloy)).isPresent();
	}

	private static boolean holdsToolFor(Player player, BlockState state, Alloy alloy) {
		return AlloyGear.pieceOf(player.getMainHandItem())
			.filter(piece -> piece.alloy() == alloy)
			.flatMap(piece -> piece.kind().mineable())
			.filter(state::is)
			.isPresent();
	}

	private static void creditBedrock(Player player, BlockState state) {
		if (state.is(Blocks.BEDROCK) && player instanceof ServerPlayer serverPlayer && !serverPlayer.isCreative()) {
			MapleTriggers.BEDROCK_BROKEN.trigger(serverPlayer);
		}
	}
}
