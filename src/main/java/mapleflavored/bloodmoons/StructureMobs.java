package mapleflavored.bloodmoons;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * How this mod sets up a mob that belongs to a structure, the way a Woodland Mansion sets up its Illagers. The mobs it
 * places as a structure generates and the mobs a Blood Moon Restores are set up here, so they can't come to differ.
 *
 * <p>A structure that places its own mobs, as vanilla's do, sets them up itself. Whatever it adds beyond this a
 * Restored mob doesn't have, as a Bastion's templates arm its Piglins and keep them from hunting its Hoglins. So a
 * structure Restores only mobs it adds nothing to.
 */
public final class StructureMobs {
	private StructureMobs() {
	}

	/**
	 * A new mob of the type, standing at {@code feet} and facing any way, ready to be added to the level. Empty where the
	 * mob can't spawn now, as a hostile mob can't in Peaceful.
	 *
	 * @param level the level, or the part of it being generated, the mob will be added to
	 * @param persistent whether it never despawns
	 */
	public static Optional<Mob> setUp(ServerLevelAccessor level, EntityType<?> type, Vec3 feet, boolean persistent) {
		if (!(type.create(level.getLevel(), EntitySpawnReason.STRUCTURE) instanceof Mob mob)) return Optional.empty();

		if (persistent) mob.setPersistenceRequired();

		// From the mob's own randomness, so generating one draws nothing from its chunk's.
		mob.snapTo(feet.x, feet.y, feet.z, mob.getRandom().nextFloat() * 360.0F, 0.0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(feet)), EntitySpawnReason.STRUCTURE, null);

		return Optional.of(mob);
	}
}
