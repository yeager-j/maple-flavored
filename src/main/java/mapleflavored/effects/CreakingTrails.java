package mapleflavored.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Trails of particles from one block to another, drawn like the ones a struck Creaking sends to its heart. */
public final class CreakingTrails {
	private static final int TRAILS = 5;
	private static final int MIN_TICKS = 10;
	private static final int EXTRA_TICKS = 40;

	private CreakingTrails() {
	}

	/**
	 * Sends a burst of trails, each running from somewhere in the block at {@code from} to somewhere in the block at
	 * {@code to}, to every player in the level however far away they are.
	 *
	 * @param color the trails' colour, as RGB
	 */
	public static void show(ServerLevel level, BlockPos from, BlockPos to, int color) {
		RandomSource random = level.getRandom();

		for (int i = 0; i < TRAILS; i++) {
			Vec3 start = withinBlock(from, random);
			Vec3 end = withinBlock(to, random);
			TrailParticleOption trail = new TrailParticleOption(end, color, MIN_TICKS + random.nextInt(EXTRA_TICKS));

			level.sendParticles(trail, true, true, start.x, start.y, start.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	private static Vec3 withinBlock(BlockPos pos, RandomSource random) {
		return Vec3.atLowerCornerOf(pos).add(random.nextDouble(), random.nextDouble(), random.nextDouble());
	}
}
