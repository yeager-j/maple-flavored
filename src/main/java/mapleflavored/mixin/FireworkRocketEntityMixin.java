package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mapleflavored.alloys.ArmoredElytraFlight;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A Firework Rocket boosts a glider in an Armored Elytra less. */
@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin {
	// The rocket's only call that moves a living entity: the boost it gives the glider it's attached to.
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
	private void boostArmoredElytraLess(LivingEntity glider, Vec3 boosted, Operation<Void> original) {
		original.call(glider, ArmoredElytraFlight.afterRocketBoost(glider, glider.getDeltaMovement(), boosted));
	}
}
