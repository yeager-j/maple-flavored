package mapleflavored.spawners;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The particle cage around a spawner that isn't Cleared yet. Each of the block's 12 edges is a bar, and bars vanish as
 * the spawner's mobs die, so a solo player breaks one bar per kill. A Cleared spawner has no cage.
 */
public final class SpawnerCage {
	// Tuning values for the hand check.
	private static final int PALE_SOUL_BLUE = 0x8FE3F0;
	private static final float PARTICLE_SCALE = 0.6F;
	private static final int TICKS_BETWEEN_PARTICLES = 2;
	private static final double OUTSET = 0.06;

	private static final double LOW = -OUTSET;
	private static final double HIGH = 1.0 + OUTSET;
	private static final DustParticleOptions PARTICLE = new DustParticleOptions(PALE_SOUL_BLUE, PARTICLE_SCALE);

	// Bars vanish from the end of the list: the bottom edges first, then the top, and the corner posts last.
	private static final List<Bar> BARS = List.of(
		new Bar(new Vec3(LOW, LOW, LOW), new Vec3(LOW, HIGH, LOW)),
		new Bar(new Vec3(HIGH, LOW, LOW), new Vec3(HIGH, HIGH, LOW)),
		new Bar(new Vec3(HIGH, LOW, HIGH), new Vec3(HIGH, HIGH, HIGH)),
		new Bar(new Vec3(LOW, LOW, HIGH), new Vec3(LOW, HIGH, HIGH)),
		new Bar(new Vec3(LOW, HIGH, LOW), new Vec3(HIGH, HIGH, LOW)),
		new Bar(new Vec3(HIGH, HIGH, LOW), new Vec3(HIGH, HIGH, HIGH)),
		new Bar(new Vec3(HIGH, HIGH, HIGH), new Vec3(LOW, HIGH, HIGH)),
		new Bar(new Vec3(LOW, HIGH, HIGH), new Vec3(LOW, HIGH, LOW)),
		new Bar(new Vec3(LOW, LOW, LOW), new Vec3(HIGH, LOW, LOW)),
		new Bar(new Vec3(HIGH, LOW, LOW), new Vec3(HIGH, LOW, HIGH)),
		new Bar(new Vec3(HIGH, LOW, HIGH), new Vec3(LOW, LOW, HIGH)),
		new Bar(new Vec3(LOW, LOW, HIGH), new Vec3(LOW, LOW, LOW))
	);

	private SpawnerCage() {
	}

	/** Adds the cage's particles for this tick. Call each client tick of the spawner. */
	public static void draw(Level level, BlockPos pos, SpawnerBlockEntity spawner) {
		if (level.getGameTime() % TICKS_BETWEEN_PARTICLES != 0) return;

		int standingBars = Spawners.clearingOf(spawner).cageBars();

		if (standingBars == 0) return;

		// Drawn only while the spawner is active, like vanilla's own flames.
		double range = Spawners.activationRange(spawner);
		boolean active = level.players().stream().anyMatch(player -> Spawners.isInRange(player, Vec3.atCenterOf(pos), range));

		if (!active) return;

		RandomSource random = level.getRandom();
		Vec3 corner = Vec3.atLowerCornerOf(pos);

		for (Bar bar : BARS.subList(0, standingBars)) {
			Vec3 point = corner.add(bar.pointAt(random.nextDouble()));

			level.addParticle(PARTICLE, point.x, point.y, point.z, 0.0, 0.0, 0.0);
		}
	}

	private record Bar(Vec3 from, Vec3 to) {
		Vec3 pointAt(double along) {
			return from.lerp(to, along);
		}
	}
}
