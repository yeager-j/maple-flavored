package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mapleflavored.alloys.AlloyDamage;
import mapleflavored.alloys.AlloySets;
import mapleflavored.alloys.ArmoredElytraFlight;
import mapleflavored.alloys.OrichalcumSave;
import mapleflavored.grounded.Grounded;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops a Grounded entity gliding, the way vanilla stops it under Levitation: no glide can start, and the server ends
 * one already under way. Slows an Armored Elytra's glide. Applies Alloy armor's damage reductions last, after armor,
 * Resistance and Protection, so they also reduce damage that bypasses those, such as a sonic boom. Applies the Alloy
 * set bonuses that live on a living entity: Steel's Levitation immunity, Shakudo's freedom from block and frost
 * slowdown, Adamant's armor piercing and Orichalcum's save.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@ModifyReturnValue(method = "canGlide", at = @At("RETURN"))
	private boolean noGlidingWhileGrounded(boolean canGlide) {
		return canGlide && !Grounded.isGrounded((LivingEntity) (Object) this);
	}

	@ModifyReturnValue(method = "updateFallFlyingMovement", at = @At("RETURN"))
	private Vec3 armoredElytraGlidesSlower(Vec3 movement) {
		return ArmoredElytraFlight.afterGlideDrag((LivingEntity) (Object) this, movement);
	}

	@ModifyReturnValue(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"))
	private float applyAlloyArmorReductions(float damage, DamageSource source) {
		return AlloyDamage.afterAlloyArmor((LivingEntity) (Object) this, source, damage);
	}

	@ModifyReturnValue(method = "canBeAffected", at = @At("RETURN"))
	private boolean alloySetRefusesEffect(boolean canBeAffected, MobEffectInstance effect) {
		return canBeAffected && !AlloySets.refusesEffect((LivingEntity) (Object) this, effect);
	}

	@ModifyReturnValue(method = "getBlockSpeedFactor", at = @At("RETURN"))
	private float alloySetIgnoresSlowBlocks(float speedFactor) {
		return AlloySets.blockSpeedFactor((LivingEntity) (Object) this, speedFactor);
	}

	// Vanilla removes the frost slowdown each tick before adding it back, so skipping the add clears it too.
	@Inject(method = "tryAddFrost", at = @At("HEAD"), cancellable = true)
	private void alloySetIgnoresFrost(CallbackInfo ci) {
		if (AlloySets.ignoresFrost((LivingEntity) (Object) this)) ci.cancel();
	}

	// Vanilla's armor runs on the whole hit, so its durability loss and damage-dependent formula stay vanilla's.
	@WrapMethod(method = "getDamageAfterArmorAbsorb")
	private float alloySetPiercesArmor(DamageSource source, float damage, Operation<Float> original) {
		return AlloySets.damageAfterArmor(source, damage, original.call(source, damage));
	}

	// Ahead of vanilla's hand-by-hand Totem check, so a ready set saves before a held Totem or Crystal Heart is used.
	@Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
	private void orichalcumSetSavesFirst(DamageSource killingDamage, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof ServerPlayer player && OrichalcumSave.trySave(player, killingDamage)) cir.setReturnValue(true);
	}
}
