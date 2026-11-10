package mapleflavored.access;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Every entity a level holds, which vanilla only shows where players' chunks do. */
@Mixin(ServerLevel.class)
public interface ServerLevelEntityManagerAccessor {
	/** Holds the level's entities, hidden ones included: those in chunks about to unload or not yet shown to players. */
	@Accessor("entityManager")
	PersistentEntitySectionManager<Entity> maple$entityManager();
}
