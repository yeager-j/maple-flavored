package mapleflavored.hearts;

import com.mojang.serialization.Codec;
import mapleflavored.MapleFlavored;
import mapleflavored.advancements.MapleTriggers;
import mapleflavored.alloys.AlloySets;
import mapleflavored.subspaces.WitherSubspaces;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Withered Hearts: hearts the Wither effect makes unusable, one every 5 seconds down to 1 usable heart, or every 10
 * seconds in a full Electrum set. The interval follows the armor worn each tick, so a swap changes it at once.
 *
 * <p>While the player has no Wither effect and is outside the Nether and every Wither's Subspace, one returns every 10
 * seconds whatever armor they wear. That timer resets whenever recovery stops, so it never carries over between visits.
 * Death clears them, as no attachment is copied to the respawned player; logging out keeps them and both timers' progress.
 */
final class WitheredHearts {
	private static final int WITHERING_INTERVAL_TICKS = 5 * 20;
	private static final int RECOVERY_INTERVAL_TICKS = 10 * 20;
	private static final int FEWEST_USABLE_HEARTS = 1;

	private static final AttachmentType<Integer> WITHERED_HEARTS = AttachmentRegistry.create(
		MapleFlavored.id("withered_hearts"),
		builder -> builder
			.persistent(Codec.INT)
			// The owner's client draws them as darkened hearts.
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly())
	);

	private static final AttachmentType<Integer> WITHERING_TICKS = AttachmentRegistry.create(
		MapleFlavored.id("withering_ticks"),
		builder -> builder.persistent(Codec.INT)
	);

	private static final AttachmentType<Integer> RECOVERY_TICKS = AttachmentRegistry.create(
		MapleFlavored.id("recovery_ticks"),
		builder -> builder.persistent(Codec.INT)
	);

	private WitheredHearts() {
	}

	static void register() {
		// Levels tick before players, so this sees every tick an effect lasts, including its last.
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			for (ServerPlayer player : level.players()) {
				tick(player);
			}
		});
	}

	/** The player's Withered Hearts, never more than leaves them 1 usable heart. */
	static int count(Player player) {
		int stored = player.getAttachedOrElse(WITHERED_HEARTS, 0);
		int mostWithered = Math.max(0, Hearts.maxHearts(player) - FEWEST_USABLE_HEARTS);

		return Math.min(stored, mostWithered);
	}

	private static void tick(ServerPlayer player) {
		if (!player.isAlive()) return;

		if (player.hasEffect(MobEffects.WITHER)) {
			player.removeAttached(RECOVERY_TICKS);
			advanceWithering(player);

			return;
		}

		player.removeAttached(WITHERING_TICKS);

		if (!canRecover(player)) {
			player.removeAttached(RECOVERY_TICKS);

			return;
		}

		clearHiddenHearts(player);
		advanceRecovery(player);
	}

	private static void advanceWithering(ServerPlayer player) {
		int ticks = player.getAttachedOrElse(WITHERING_TICKS, 0) + 1;

		if (ticks < witheringInterval(player)) {
			player.setAttached(WITHERING_TICKS, ticks);

			return;
		}

		player.removeAttached(WITHERING_TICKS);

		if (Hearts.usableHearts(player) > FEWEST_USABLE_HEARTS) {
			player.setAttached(WITHERED_HEARTS, count(player) + 1);
			Hearts.applyMaxHealth(player);
		}
	}

	private static int witheringInterval(ServerPlayer player) {
		return AlloySets.witheringInterval(player, WITHERING_INTERVAL_TICKS);
	}

	// Checks the stored count, not the clamped one, so recovery still runs to clear hearts hidden by low Max Hearts.
	private static boolean canRecover(ServerPlayer player) {
		return player.hasAttached(WITHERED_HEARTS)
			&& player.level().dimension() != Level.NETHER
			&& !WitherSubspaces.contains(player.level(), player.position());
	}

	// Drops stored hearts the Max Hearts clamp hides, so a later Max Hearts rise can't bring them back withered.
	private static void clearHiddenHearts(ServerPlayer player) {
		int visible = count(player);

		// Withered Hearts sync to the client, so an unchanged count isn't written back every tick.
		if (player.getAttachedOrElse(WITHERED_HEARTS, 0) > visible) setWitheredHearts(player, visible);
	}

	private static void advanceRecovery(ServerPlayer player) {
		if (!player.hasAttached(WITHERED_HEARTS)) {
			player.removeAttached(RECOVERY_TICKS);

			return;
		}

		int ticks = player.getAttachedOrElse(RECOVERY_TICKS, 0) + 1;

		if (ticks < RECOVERY_INTERVAL_TICKS) {
			player.setAttached(RECOVERY_TICKS, ticks);

			return;
		}

		player.removeAttached(RECOVERY_TICKS);
		setWitheredHearts(player, count(player) - 1);
		Hearts.applyMaxHealth(player);
		MapleTriggers.WITHERED_HEART_RECOVERED.trigger(player);
	}

	private static void setWitheredHearts(ServerPlayer player, int withered) {
		if (withered > 0) {
			player.setAttached(WITHERED_HEARTS, withered);
		} else {
			player.removeAttached(WITHERED_HEARTS);
		}
	}
}
