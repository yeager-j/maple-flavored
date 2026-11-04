package mapleflavored.client.zephyr;

import mapleflavored.zephyr.Zephyr;
import mapleflavored.zephyr.ZephyrEffects;
import mapleflavored.zephyr.ZephyrJumpPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Turns a jump press in the air into a Zephyr jump: the client moves at once and tells the server. */
public final class ZephyrInput {
	private static boolean jumpWasHeld;
	private static boolean wasAirborne;

	private ZephyrInput() {
	}

	/** Registers the client tick that watches the jump key. Call once from client init. */
	public static void register() {
		// Runs after the player's own tick, so a press that started an Elytra glide or a ground jump is already handled.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			LocalPlayer player = client.player;

			if (player == null) return;

			boolean jumpHeld = player.input.keyPresses.jump();
			boolean pressedInAir = jumpHeld && !jumpWasHeld && wasAirborne;

			Zephyr.endAirTimeIfOver(player);

			if (pressedInAir && Zephyr.canJump(player) && ClientPlayNetworking.canSend(ZephyrJumpPayload.TYPE)) jump(player);

			jumpWasHeld = jumpHeld;
			wasAirborne = !player.onGround();
		});
	}

	private static void jump(LocalPlayer player) {
		Vec3 movement = player.getDeltaMovement();

		player.setDeltaMovement(movement.x, Math.max(Zephyr.launchSpeed(player), movement.y), movement.z);
		Zephyr.markJumped(player);
		ZephyrEffects.showToJumper(player);
		ClientPlayNetworking.send(ZephyrJumpPayload.INSTANCE);
	}
}
