package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mapleflavored.grounded.Grounded;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Slows a Grounded glider each tick. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@ModifyReturnValue(method = "updateFallFlyingMovement", at = @At("RETURN"))
	private Vec3 slowGroundedGlide(Vec3 movement) {
		return Grounded.slowGlide((LivingEntity) (Object) this, movement);
	}
}
