package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mapleflavored.grounded.Grounded;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Weakens a Firework Rocket's push on a Grounded glider. */
@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin {
	@WrapOperation(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V")
	)
	private void weakenGroundedBoost(LivingEntity glider, Vec3 boosted, Operation<Void> original) {
		original.call(glider, Grounded.weakenRocketBoost(glider, boosted));
	}
}
