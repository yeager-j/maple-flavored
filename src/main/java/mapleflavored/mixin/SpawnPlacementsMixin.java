package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mapleflavored.mobs.CreeperSpawns;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Natural spawning asks each type's placement rule; creepers are refused on the Overworld surface. */
@Mixin(SpawnPlacements.class)
public abstract class SpawnPlacementsMixin {
	@ModifyReturnValue(method = "checkSpawnRules", at = @At("RETURN"))
	private static boolean keepCreepersBelowTheSurface(
		boolean allowed, EntityType<?> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random
	) {
		return allowed && CreeperSpawns.allowed(type, level, reason, pos);
	}
}
