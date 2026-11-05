package mapleflavored.mixin;

import mapleflavored.grounded.Grounded;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes a pearl thrown by a Grounded thrower heavy. */
@Mixin(ThrownEnderpearl.class)
public abstract class ThrownEnderpearlMixin {
	@Inject(
		method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V",
		at = @At("TAIL")
	)
	private void markGroundedThrow(Level level, LivingEntity thrower, ItemStack itemStack, CallbackInfo ci) {
		Grounded.onPearlThrown((ThrownEnderpearl) (Object) this, thrower);
	}
}
