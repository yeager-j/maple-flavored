package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyDamage;
import mapleflavored.alloys.AlloySets;
import mapleflavored.alloys.ArmoredElytraFlight;
import mapleflavored.alloys.OrichalcumSave;
import mapleflavored.grounded.Grounded;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
	private boolean steelSetRefusesLevitation(boolean canBeAffected, MobEffectInstance effect) {
		return canBeAffected && !(effect.is(MobEffects.LEVITATION) && AlloySets.wearsFullSet((LivingEntity) (Object) this, Alloy.STEEL));
	}

	@ModifyReturnValue(method = "getBlockSpeedFactor", at = @At("RETURN"))
	private float shakudoSetIgnoresSlowBlocks(float speedFactor) {
		if (!AlloySets.wearsFullSet((LivingEntity) (Object) this, Alloy.SHAKUDO)) return speedFactor;

		return Math.max(speedFactor, 1.0F);
	}

	// Vanilla removes the frost slowdown each tick before adding it back, so skipping the add clears it too.
	@Inject(method = "tryAddFrost", at = @At("HEAD"), cancellable = true)
	private void shakudoSetIgnoresFrost(CallbackInfo ci) {
		if (AlloySets.wearsFullSet((LivingEntity) (Object) this, Alloy.SHAKUDO)) ci.cancel();
	}

	// Armor sees the whole hit, so its durability and damage-dependent formula are vanilla's, and the pierced share
	// can only add damage.
	@WrapMethod(method = "getDamageAfterArmorAbsorb")
	private float adamantSetPiercesArmor(DamageSource source, float damage, Operation<Float> original) {
		float piercingShare = AlloySets.armorPiercingShare(source);
		float afterArmor = original.call(source, damage);

		return piercingShare * damage + (1.0F - piercingShare) * afterArmor;
	}

	// Ahead of vanilla's hand-by-hand Totem check, so a ready set saves before a held Totem or Crystal Heart is used.
	@Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
	private void orichalcumSetSavesFirst(DamageSource killingDamage, CallbackInfoReturnable<Boolean> cir) {
		if (killingDamage.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

		if ((Object) this instanceof ServerPlayer player && OrichalcumSave.trySave(player)) cir.setReturnValue(true);
	}
}
