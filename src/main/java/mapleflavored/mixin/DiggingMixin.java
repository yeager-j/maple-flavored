package mapleflavored.mixin;

import mapleflavored.bosses.BossKillCredit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.warden.Digging;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A Warden digging back down credits the players near it with surviving it. */
@Mixin(Digging.class)
public abstract class DiggingMixin {
	// The removal here is the Warden digging down. Any other discard reaches no Digging behavior, so it credits nobody.
	@Inject(
		method = "stop(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/monster/warden/Warden;J)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/monster/warden/Warden;remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"
		)
	)
	private void creditSurvivors(ServerLevel level, Warden body, long timestamp, CallbackInfo info) {
		BossKillCredit.creditWardenSurvivors(body);
	}
}
