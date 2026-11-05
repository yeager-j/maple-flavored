package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mapleflavored.grounded.Grounded;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops a Grounded entity gliding, the way vanilla stops it under Levitation: no glide can start, and the server ends
 * one already under way.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@ModifyReturnValue(method = "canGlide", at = @At("RETURN"))
	private boolean noGlidingWhileGrounded(boolean canGlide) {
		return canGlide && !Grounded.isGrounded((LivingEntity) (Object) this);
	}
}
