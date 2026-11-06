package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mapleflavored.alloys.IllagerNeutrality;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.raid.Raider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** An Illager a player hurts turns on them, Orichalcum or not, along with the Illagers that see it. */
@Mixin(Raider.class)
public abstract class RaiderMixin {
	@ModifyReturnValue(method = "hurtServer", at = @At("RETURN"))
	private boolean angerIllagersAtTheAttacker(boolean hurt, ServerLevel level, DamageSource source, float damage) {
		if (hurt && (Object) this instanceof AbstractIllager illager) IllagerNeutrality.wasHurt(level, illager, source);

		return hurt;
	}
}
