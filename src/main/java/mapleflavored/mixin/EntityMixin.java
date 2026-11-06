package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloySets;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Alloy set bonuses vanilla decides on every entity: Electrum's fire and lava immunity, and Shakudo passing through
 * cobwebs and powder snow at full speed and jumping full height off honey. Both are read from the worn armor, so a client predicts them for its player.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@ModifyReturnValue(method = "fireImmune", at = @At("RETURN"))
	private boolean electrumSetIsFireImmune(boolean fireImmune) {
		return fireImmune || AlloySets.wearsFullSet((Entity) (Object) this, Alloy.ELECTRUM);
	}

	@ModifyReturnValue(method = "getBlockJumpFactor", at = @At("RETURN"))
	private float shakudoSetJumpsFullHeight(float jumpFactor) {
		if (!AlloySets.wearsFullSet((Entity) (Object) this, Alloy.SHAKUDO)) return jumpFactor;

		return Math.max(jumpFactor, 1.0F);
	}

	@Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
	private void shakudoSetIsNeverStuck(BlockState block, Vec3 speedMultiplier, CallbackInfo ci) {
		if (AlloySets.ignoresBeingStuckIn((Entity) (Object) this, block)) ci.cancel();
	}
}
