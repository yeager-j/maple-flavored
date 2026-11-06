package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mapleflavored.alloys.AlloyDamage;
import mapleflavored.grounded.Grounded;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops a Grounded entity gliding, the way vanilla stops it under Levitation: no glide can start, and the server ends
 * one already under way. Applies Alloy armor's damage reductions last, after armor, Resistance and Protection, so they
 * also reduce damage that bypasses those, such as a sonic boom.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@ModifyReturnValue(method = "canGlide", at = @At("RETURN"))
	private boolean noGlidingWhileGrounded(boolean canGlide) {
		return canGlide && !Grounded.isGrounded((LivingEntity) (Object) this);
	}

	@ModifyReturnValue(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"))
	private float applyAlloyArmorReductions(float damage, DamageSource source) {
		return AlloyDamage.afterAlloyArmor((LivingEntity) (Object) this, source, damage);
	}
}
