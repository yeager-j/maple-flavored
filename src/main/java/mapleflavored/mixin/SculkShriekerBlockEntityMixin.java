package mapleflavored.mixin;

import java.util.OptionalInt;
import mapleflavored.wardensummoning.WardenSummoning;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces vanilla's shrieker warnings and summons with Warden summoning's, which count only Warden Territory. */
@Mixin(SculkShriekerBlockEntity.class)
public abstract class SculkShriekerBlockEntityMixin {
	@Shadow
	private int warningLevel;

	@Redirect(
		method = "tryToWarn",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/monster/warden/WardenSpawnTracker;tryWarn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerPlayer;)Ljava/util/OptionalInt;"
		)
	)
	private OptionalInt warnOnlyInWardenTerritory(ServerLevel level, BlockPos position, ServerPlayer cause) {
		return WardenSummoning.warn(level, (SculkShriekerBlockEntity) (Object) this, cause);
	}

	@Inject(method = "trySummonWarden", at = @At("HEAD"), cancellable = true)
	private void summonNearAPlayerInWardenTerritory(ServerLevel level, CallbackInfoReturnable<Boolean> callback) {
		callback.setReturnValue(WardenSummoning.summon(level, (SculkShriekerBlockEntity) (Object) this, warningLevel));
	}
}
