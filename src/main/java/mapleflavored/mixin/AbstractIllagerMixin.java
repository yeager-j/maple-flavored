package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mapleflavored.alloys.IllagerNeutrality;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Illagers leave Orichalcum wearers alone. Vanilla asks this both when an Illager picks a target and each tick it keeps
 * one, so a neutral Illager neither picks the player nor keeps chasing them.
 */
@Mixin(AbstractIllager.class)
public abstract class AbstractIllagerMixin {
	@ModifyReturnValue(method = "canAttack", at = @At("RETURN"))
	private boolean leaveOrichalcumWearersAlone(boolean canAttack, LivingEntity target) {
		return canAttack && !IllagerNeutrality.isNeutralTo((AbstractIllager) (Object) this, target);
	}
}
