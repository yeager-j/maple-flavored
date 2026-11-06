package mapleflavored.mobs;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Creepers never spawn naturally at Y 63 or above in the Overworld, caves and structures included, so surface builds
 * are safer. Spawners, spawn eggs and commands are unaffected, so creeper farms keep working.
 */
public final class CreeperSpawns {
	private static final int LOWEST_SURFACE_Y = 63;

	private CreeperSpawns() {
	}

	/** Whether this rule lets {@code type} spawn at {@code pos} for {@code reason}. Every type but the creeper is allowed. */
	public static boolean allowed(EntityType<?> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos) {
		boolean natural = reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;

		return type != EntityTypes.CREEPER
			|| !natural
			|| level.getLevel().dimension() != Level.OVERWORLD
			|| pos.getY() < LOWEST_SURFACE_Y;
	}
}
