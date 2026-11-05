package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mapleflavored.grounded.Grounded;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Gives a pearl thrown while Grounded its stronger gravity. */
@Mixin(ThrowableProjectile.class)
public abstract class ThrowableProjectileMixin {
	@ModifyReturnValue(method = "getDefaultGravity", at = @At("RETURN"))
	private double heavyPearlGravity(double gravity) {
		return (Object) this instanceof ThrownEnderpearl pearl ? Grounded.pearlGravity(pearl, gravity) : gravity;
	}
}
