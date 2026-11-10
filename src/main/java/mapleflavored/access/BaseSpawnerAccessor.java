package mapleflavored.access;

import net.minecraft.world.level.BaseSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads a spawner's activation range, which Spawner Clearing also uses to count players. */
@Mixin(BaseSpawner.class)
public interface BaseSpawnerAccessor {
	/** The distance within which a player activates the spawner, 16 by default. */
	@Accessor
	int getRequiredPlayerRange();
}
