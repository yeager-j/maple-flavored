package mapleflavored;

import mapleflavored.bloodmoons.Restoring;
import mapleflavored.mobs.CommonMobs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * What happens to every entity as it first enters the world, whether it's added to a loaded level or placed while a chunk
 * generates. Both paths call {@link #entering}, so they can't come to differ.
 */
public final class FreshEntities {
	private FreshEntities() {
	}

	/**
	 * Gives a new entity what this mod gives every one: a common mob its stats, and a mob a Restorable structure is
	 * placing its Restore spot. Call for every entity added to a level or a worldgen region, before it's added. Safe to
	 * call from any thread.
	 *
	 * @param dimension the level the entity is entering
	 */
	public static void entering(Entity entity, ResourceKey<Level> dimension) {
		CommonMobs.applySpawnStats(entity);
		Restoring.tagIfPlacing(entity, dimension);
	}
}
