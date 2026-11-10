package mapleflavored.alloys;

import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.Optional;
import mapleflavored.MapleFlavored;
import mapleflavored.advancements.MapleTriggers;
import mapleflavored.gamerules.MapleGameRules;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shakudo's and Adamant's mining: Shakudo tools mine faster, Adamant tools mine hard blocks as if they were deepslate,
 * and an Adamant pickaxe can break bedrock. The client predicts mining and the server checks it, so these work on both
 * sides, reading the game rules from a copy synced to every player.
 */
public final class AlloyMining {
	private static final AttachmentType<Rules> RULES = AttachmentRegistry.create(
		MapleFlavored.id("alloy_mining_rules"),
		builder -> builder.syncWith(Rules.STREAM_CODEC, AttachmentSyncPredicate.all())
	);

	// What breaking bedrock takes, besides an Adamant pickaxe: Efficiency V and Haste II.
	private static final int BEDROCK_EFFICIENCY_LEVEL = 5;
	private static final int BEDROCK_HASTE_AMPLIFIER = 1;

	private AlloyMining() {
	}

	static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(AlloyMining::syncRules);
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> creditBedrock(player, state));
	}

	/** The mining game rules in effect in the level. Works on both the server and the client. */
	public static Rules rules(Level level) {
		return level.getAttachedOrElse(RULES, Rules.DEFAULTS);
	}

	/** The player's final break speed with a Shakudo tool's bonus, given the speed vanilla works out. */
	public static float destroySpeed(Player player, float speed) {
		if (!holdsTool(player, Alloy.SHAKUDO)) return speed;

		return speed * (1.0F + rules(player.level()).shakudoMiningBonus() / 100.0F);
	}

	/**
	 * The hardness the player mines the block at: at most the hardness cap when they hold an Adamant tool made for the
	 * block, else the block's own. Unbreakable blocks, at -1, stay unbreakable.
	 */
	public static float hardness(Player player, BlockState state, float hardness) {
		if (!holdsToolFor(player, state, Alloy.ADAMANT)) return hardness;

		return Math.min(hardness, rules(player.level()).adamantHardnessCap());
	}

	/**
	 * The share of the block the player mines each tick where Adamant sets it: bedrock, for a player who can break it
	 * with an Adamant pickaxe with Efficiency V or higher, under Haste II or higher. Empty for every other block and
	 * player, which leaves the progress to vanilla.
	 */
	public static Optional<Float> bedrockProgress(Player player, BlockState state) {
		if (!state.is(Blocks.BEDROCK) || !breaksBedrock(player)) return Optional.empty();

		return Optional.of(1.0F / rules(player.level()).adamantBedrockBreakTime());
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

	// Game rules stay on the server, so their values reach clients as level data, and a change shows at once.
	private static void syncRules(ServerLevel level) {
		Rules rules = new Rules(
			level.getGameRules().get(MapleGameRules.SHAKUDO_MINING_BONUS),
			level.getGameRules().get(MapleGameRules.ADAMANT_HARDNESS_CAP),
			level.getGameRules().get(MapleGameRules.ADAMANT_BEDROCK_BREAK_TIME)
		);

		if (!Objects.equals(level.getAttached(RULES), rules)) level.setAttached(RULES, rules);
	}

	private static void creditBedrock(Player player, BlockState state) {
		if (state.is(Blocks.BEDROCK) && player instanceof ServerPlayer serverPlayer && !serverPlayer.isCreative()) {
			MapleTriggers.BEDROCK_BROKEN.trigger(serverPlayer);
		}
	}

	/**
	 * The mining game rules.
	 *
	 * @param shakudoMiningBonus how much faster Shakudo tools mine, in percent
	 * @param adamantHardnessCap the most hardness an Adamant tool mines the blocks it's made for at
	 * @param adamantBedrockBreakTime ticks to break bedrock
	 */
	public record Rules(int shakudoMiningBonus, int adamantHardnessCap, int adamantBedrockBreakTime) {
		static final Rules DEFAULTS = new Rules(
			MapleGameRules.SHAKUDO_MINING_BONUS.defaultValue(),
			MapleGameRules.ADAMANT_HARDNESS_CAP.defaultValue(),
			MapleGameRules.ADAMANT_BEDROCK_BREAK_TIME.defaultValue()
		);

		static final StreamCodec<ByteBuf, Rules> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Rules::shakudoMiningBonus,
			ByteBufCodecs.VAR_INT, Rules::adamantHardnessCap,
			ByteBufCodecs.VAR_INT, Rules::adamantBedrockBreakTime,
			Rules::new
		);
	}
}
