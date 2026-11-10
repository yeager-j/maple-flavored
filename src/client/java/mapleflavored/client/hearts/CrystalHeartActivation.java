package mapleflavored.client.hearts;

import mapleflavored.hearts.CrystalHeartUsedPayload;
import mapleflavored.hearts.Hearts;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/** Plays the Totem's activation, showing a Crystal Heart, when this client's player consumes one. */
public final class CrystalHeartActivation {
	private CrystalHeartActivation() {
	}

	/** Registers the payload receiver. Call once from client init. */
	static void register() {
		ClientPlayNetworking.registerGlobalReceiver(CrystalHeartUsedPayload.TYPE, (payload, context) -> play(context.client(), context.player()));
	}

	// The same particles, sound and on-screen item as vanilla's Totem event in ClientPacketListener.handleEntityEvent.
	private static void play(Minecraft client, LocalPlayer player) {
		client.particleEngine.createTrackingEmitter(player, ParticleTypes.TOTEM_OF_UNDYING, 30);
		player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, player.getSoundSource(), 1.0F, 1.0F, false);
		player.displayItemActivation(new ItemStack(Hearts.CRYSTAL_HEART));
	}
}
