package mapleflavored.zephyr;

import java.util.List;
import mapleflavored.MapleFlavored;
import mapleflavored.grounded.Grounded;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Zephyr: a boots enchantment that gives one extra jump per air time.
 *
 * <p>The client moves the player and tells the server. The server decides what the jump does to fall damage: a fall
 * counts from where it started to where the player lands, as if the jump never happened, except that the jump
 * Clutches, cutting the distance fallen before it by 10%, 15% or 25%.
 */
public final class Zephyr {
	/** The Zephyr enchantment, defined in data. */
	public static final ResourceKey<Enchantment> ENCHANTMENT = ResourceKey.create(Registries.ENCHANTMENT, MapleFlavored.id("zephyr"));

	// What each level does, lowest first. The enchantment's max_level in data must equal how many there are, which a
	// GameTest checks.
	private static final List<LevelEffect> LEVELS = List.of(
		new LevelEffect(1.5, 0.10),
		new LevelEffect(2.25, 0.15),
		new LevelEffect(3.0, 0.25)
	);

	/** The highest Zephyr level with behaviour of its own. */
	public static final int MAX_LEVEL = LEVELS.size();

	// Vanilla's vertical air drag, applied after gravity each tick while airborne.
	private static final double AIR_DRAG = 0.98;
	private static final double FASTEST_LAUNCH = 4.0;
	private static final int LAUNCH_SEARCH_STEPS = 50;
	private static final int LONGEST_RISE_TICKS = 200;

	// Lasts one air time, so it is neither saved nor synced; client and server each track their own view of it.
	private static final AttachmentType<AirJump> AIR_JUMP = AttachmentRegistry.create(MapleFlavored.id("zephyr_air_jump"));

	private Zephyr() {
	}

	/** Registers the jump request and its server handler. Call once from mod init. */
	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(ZephyrJumpPayload.TYPE, ZephyrJumpPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ZephyrJumpPayload.TYPE, (payload, context) -> acceptJump(context.player()));
	}

	/** The player's Zephyr level from their feet slot, 0 without Zephyr. */
	public static int level(Player player) {
		return player.registryAccess()
			.lookupOrThrow(Registries.ENCHANTMENT)
			.get(ENCHANTMENT)
			.map(zephyr -> EnchantmentHelper.getEnchantmentLevel(zephyr, player))
			.orElse(0);
	}

	/** Whether the player can make a Zephyr jump right now. Both sides ask this, each with its own view of the player. */
	public static boolean canJump(Player player) {
		return player.isAlive()
			&& !player.isSpectator()
			&& !player.onGround()
			&& !endsAirTime(player)
			&& !player.isFallFlying()
			&& !Grounded.isGrounded(player)
			&& !player.hasAttached(AIR_JUMP)
			&& level(player) > 0;
	}

	/** The upward speed, in blocks per tick, that lifts the player their Zephyr level's height. Needs Zephyr. */
	public static double launchSpeed(Player player) {
		return launchSpeed(levelEffect(player).jumpHeight(), player.getGravity());
	}

	/** Uses up the client player's jump for this air time. The server keeps its own record. */
	public static void markJumped(Player player) {
		player.setAttached(AIR_JUMP, AirJump.Spent.INSTANCE);
	}

	/** Gives the player their jump back once their air time has ended. Call every tick, or every move on the server. */
	public static void endAirTimeIfOver(Player player) {
		if (player.onGround() || endsAirTime(player)) player.removeAttached(AIR_JUMP);
	}

	/**
	 * Applies a jump's fall rules to the fall check for one move. Call before vanilla counts the move.
	 *
	 * @param ya how far this move went up, negative for down
	 */
	public static void onFallCheck(ServerPlayer player, double ya) {
		// A move that neither falls nor lands, such as one sent while hovering at the top, doesn't end the rise.
		if (player.getAttached(AIR_JUMP) instanceof AirJump.Rising rising && (ya < 0.0 || player.onGround())) {
			// Vanilla zeroes the fall distance on every upward move, so the rise has already been dropped; the
			// distance becomes the fall so far measured from the jump's start, less the drop vanilla is about to add.
			double neutralFall = rising.carriedFall() + rising.jumpY() - player.getY();
			double dropVanillaAdds = Math.max(0.0, -ya);

			player.fallDistance = neutralFall - dropVanillaAdds;
			player.setAttached(AIR_JUMP, AirJump.Spent.INSTANCE);
		}

		endAirTimeIfOver(player);
	}

	/**
	 * Drops a pending jump's fall rules when the server moves the player, so a teleport mid-rise isn't counted as part
	 * of the fall. The jump stays used until the air time ends.
	 */
	public static void onTeleport(ServerPlayer player) {
		if (player.getAttached(AIR_JUMP) instanceof AirJump.Rising) player.setAttached(AIR_JUMP, AirJump.Spent.INSTANCE);
	}

	private static void acceptJump(ServerPlayer player) {
		if (!canJump(player)) return;

		double carriedFall = clutched(player.fallDistance, levelEffect(player).clutchCut());

		player.setAttached(AIR_JUMP, new AirJump.Rising(player.getY(), carriedFall));
		ZephyrEffects.showToOthers(player);
	}

	private static boolean endsAirTime(Player player) {
		return player.isInWater()
			|| player.isInLava()
			|| player.onClimbable()
			|| player.getAbilities().flying
			|| player.isPassenger();
	}

	// Boots given a level above the enchantment's highest behave as the highest.
	private static LevelEffect levelEffect(Player player) {
		return LEVELS.get(Math.min(level(player), MAX_LEVEL) - 1);
	}

	/**
	 * @param jumpHeight how high the jump lifts the player, in blocks
	 * @param clutchCut the share of the distance fallen before the jump that the jump takes off
	 */
	private record LevelEffect(double jumpHeight, double clutchCut) {
	}

	private static double clutched(double fallDistance, double cut) {
		return fallDistance > 0.0 ? fallDistance * (1.0 - cut) : fallDistance;
	}

	private static double launchSpeed(double height, double gravity) {
		double slowest = 0.0;
		double fastest = FASTEST_LAUNCH;

		for (int step = 0; step < LAUNCH_SEARCH_STEPS; step++) {
			double speed = (slowest + fastest) / 2.0;

			if (riseHeight(speed, gravity) < height) {
				slowest = speed;
			} else {
				fastest = speed;
			}
		}

		return fastest;
	}

	// Follows vanilla's airborne motion: move by the speed, then apply gravity and drag, until the player stops rising.
	// With zero or negative gravity the rise never ends, so it is cut off after a long rise's worth of ticks.
	private static double riseHeight(double launchSpeed, double gravity) {
		double height = 0.0;
		double speed = launchSpeed;

		for (int tick = 0; tick < LONGEST_RISE_TICKS && speed > 0.0; tick++) {
			height += speed;
			speed = (speed - gravity) * AIR_DRAG;
		}

		return height;
	}
}
