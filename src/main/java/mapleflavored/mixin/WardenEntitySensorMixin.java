package mapleflavored.mixin;

import mapleflavored.subspaces.WardenSubspaces;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.sensing.WardenEntitySensor;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A Warden is aware of every entity in its Subspace, as well as those its vanilla sensor finds. */
@Mixin(WardenEntitySensor.class)
public abstract class WardenEntitySensorMixin {
	// After the nearest living entities are found and before the nearest attackable one is picked from them.
	@Inject(
		method = "doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/monster/warden/Warden;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/ai/sensing/NearestLivingEntitySensor;doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V",
			shift = At.Shift.AFTER
		)
	)
	private void senseTheWholeSubspace(ServerLevel level, Warden body, CallbackInfo ci) {
		WardenSubspaces.extendAwareness(level, body);
	}
}
