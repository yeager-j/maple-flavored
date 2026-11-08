package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import mapleflavored.deepvariants.DeepVariants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Natural spawning makes a Deep variant where it would make its base mob below Y 0. The swap happens once vanilla has
 * picked the type and checked the spot for it, so spawn weights, caps and placement stay vanilla.
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
	@WrapOperation(
		method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/NaturalSpawner;getMobForSpawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/EntityType;)Lnet/minecraft/world/entity/Mob;"
		)
	)
	private static Mob spawnDeepVariants(
		ServerLevel level,
		EntityType<?> picked,
		Operation<Mob> original,
		@Local BlockPos.MutableBlockPos pos,
		@Local LocalRef<SpawnGroupData> groupData
	) {
		DeepVariants.NaturalSpawn spawn = DeepVariants.naturalSpawn(picked, level, pos, groupData.get());

		groupData.set(spawn.groupData());

		return original.call(level, spawn.type());
	}
}
