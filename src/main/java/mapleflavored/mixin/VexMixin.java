package mapleflavored.mixin;

import mapleflavored.alloys.IllagerNeutrality;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Vex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A Vex an Illager summons stays neutral to Orichalcum wearers after the Illager is gone. */
@Mixin(Vex.class)
public abstract class VexMixin {
	@Inject(method = "setOwner", at = @At("TAIL"))
	private void rememberAnIllagerOwner(Mob owner, CallbackInfo info) {
		IllagerNeutrality.ownerSet((Vex) (Object) this, owner);
	}
}
