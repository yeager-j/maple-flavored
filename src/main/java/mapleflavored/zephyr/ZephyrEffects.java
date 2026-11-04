package mapleflavored.zephyr;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A Zephyr jump's look and sound: a wind charge's burst at the jumper's feet, quieter, since a player jumps far more
 * often than they throw. The jumper's client shows it at once; the server shows it to everyone else.
 */
public final class ZephyrEffects {
	private static final float VOLUME = 0.5F;
	private static final float PITCH = 1.1F;

	private ZephyrEffects() {
	}

	/** Shows the jump to the player making it. Call on their client. */
	public static void showToJumper(Player player) {
		Level level = player.level();
		Vec3 feet = player.position();

		level.addParticle(ParticleTypes.GUST_EMITTER_SMALL, feet.x, feet.y, feet.z, 0.0, 0.0, 0.0);
		level.playLocalSound(feet.x, feet.y, feet.z, burst(), SoundSource.PLAYERS, VOLUME, PITCH, false);
	}

	/** Shows an accepted jump to every player but the jumper, whose client already showed it. */
	static void showToOthers(ServerPlayer jumper) {
		ServerLevel level = jumper.level();
		Vec3 feet = jumper.position();

		level.playSound(jumper, feet.x, feet.y, feet.z, burst(), SoundSource.PLAYERS, VOLUME, PITCH);

		for (ServerPlayer viewer : level.players()) {
			if (viewer == jumper) continue;

			level.sendParticles(viewer, ParticleTypes.GUST_EMITTER_SMALL, false, false, feet.x, feet.y, feet.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	private static SoundEvent burst() {
		return SoundEvents.WIND_CHARGE_BURST.value();
	}
}
