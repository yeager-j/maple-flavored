package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mapleflavored.spawners.Spawners;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Normal spawners spawn regardless of light, twice as fast until Cleared, and mark what they spawn so its death counts
 * toward Clearing them.
 *
 * <p>The light half pairs with {@link EntitySpawnReasonMixin}. A mob's own spawn rule here only asks about light (a
 * monster wants dark, an animal wants light), so its answer is ignored. Custom spawn rules in the spawn data stay
 * vanilla.
 */
@Mixin(BaseSpawner.class)
public abstract class BaseSpawnerMixin {
	@Shadow
	private int spawnDelay;

	@Inject(method = "delay", at = @At("TAIL"))
	private void hurryWhileUncleared(Level level, BlockPos pos, CallbackInfo ci) {
		spawnDelay = Spawners.spawnDelay(level, pos, (BaseSpawner) (Object) this, spawnDelay);
	}

	@ModifyExpressionValue(
		method = "serverTick",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Mob;checkSpawnRules(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/EntitySpawnReason;)Z"
		)
	)
	private boolean spawnRegardlessOfLight(boolean mobAcceptsSpot) {
		return true;
	}

	@WrapOperation(
		method = "serverTick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z")
	)
	private boolean markSpawned(ServerLevel level, Entity entity, Operation<Boolean> original, @Local(argsOnly = true) BlockPos pos) {
		boolean added = original.call(level, entity);

		if (added) Spawners.markSpawned(level, pos, (BaseSpawner) (Object) this, entity);

		return added;
	}
}
