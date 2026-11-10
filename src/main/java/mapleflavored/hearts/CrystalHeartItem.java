package mapleflavored.hearts;

import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;

/**
 * The Crystal Heart: consuming one adds 1 Max Heart and plays the Totem's activation. At 30 Max Hearts it is refused
 * and kept.
 *
 * <p>Its lethal-hit save comes from its death protection component, which vanilla checks hand by hand.
 */
final class CrystalHeartItem extends Item {
	private static final int TICKS_PER_SECOND = 20;

	// Matcha Flavoured's burst on use: Regeneration XI for 3 seconds and Resistance V for 1 second.
	private static final Consumable CONSUMABLE = Consumable.builder()
		.consumeSeconds(Consumable.DEFAULT_CONSUME_SECONDS)
		.animation(ItemUseAnimation.EAT)
		.sound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_CHIME))
		.hasConsumeParticles(false)
		.onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
			new MobEffectInstance(MobEffects.REGENERATION, 3 * TICKS_PER_SECOND, 10),
			new MobEffectInstance(MobEffects.RESISTANCE, TICKS_PER_SECOND, 4)
		)))
		.build();

	CrystalHeartItem(Properties properties) {
		super(properties);
	}

	/** The properties a Crystal Heart is built from, before the key it is registered under. */
	static Properties properties() {
		return new Properties()
			.stacksTo(1)
			.rarity(Rarity.RARE)
			.component(DataComponents.CONSUMABLE, CONSUMABLE)
			.component(DataComponents.DEATH_PROTECTION, DeathProtection.TOTEM_OF_UNDYING);
	}

	// Refused before it starts, so a player at 30 Max Hearts doesn't go through the motions for nothing.
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (knownToBeAtMaxHeartsCap(player)) return InteractionResult.FAIL;

		return super.use(level, player, hand);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!(entity instanceof Player player) || !consume(player)) return stack;

		return super.finishUsingItem(stack, level, entity);
	}

	// The server decides, since Max Hearts can reach 30 while the heart is in use. A client only predicts its answer.
	private static boolean consume(Player player) {
		if (!(player instanceof ServerPlayer serverPlayer)) return !knownToBeAtMaxHeartsCap(player);
		if (!Hearts.addMaxHeart(serverPlayer)) return false;

		playTotemActivation(serverPlayer);

		return true;
	}

	private static boolean knownToBeAtMaxHeartsCap(Player player) {
		return Hearts.known(player).map(HeartCounts::atMaxHeartsCap).orElse(false);
	}

	// Onlookers get vanilla's Totem event. The user's own client gets a payload instead: by the time that event arrived,
	// its predicted use would have emptied the hand, and vanilla would show a Totem rather than the heart.
	private static void playTotemActivation(ServerPlayer player) {
		var totemEvent = new ClientboundEntityEventPacket(player, EntityEvent.PROTECTED_FROM_DEATH);

		player.level().getChunkSource().sendToTrackingPlayers(player, totemEvent);

		if (ServerPlayNetworking.canSend(player, CrystalHeartUsedPayload.TYPE)) {
			ServerPlayNetworking.send(player, CrystalHeartUsedPayload.INSTANCE);
		}
	}
}
