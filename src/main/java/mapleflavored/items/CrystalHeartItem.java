package mapleflavored.items;

import mapleflavored.hearts.Hearts;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Crystal Heart: consuming one adds 1 Max Heart and plays the Totem's activation. At 30 Max Hearts it is refused
 * and kept.
 *
 * <p>Its lethal-hit save comes from its death protection component, which vanilla checks hand by hand.
 */
final class CrystalHeartItem extends Item {
	CrystalHeartItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (Hearts.atMaxHeartsCap(player)) return InteractionResult.FAIL;

		return super.use(level, player, hand);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		// Checked again here, since Max Hearts can reach 30 while the heart is in use.
		if (!(entity instanceof Player player) || Hearts.atMaxHeartsCap(player)) return stack;

		if (player instanceof ServerPlayer serverPlayer) {
			Hearts.addMaxHeart(serverPlayer);
			playTotemActivation(serverPlayer);
		}

		return super.finishUsingItem(stack, level, entity);
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
