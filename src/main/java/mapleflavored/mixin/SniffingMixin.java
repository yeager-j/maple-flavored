package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mapleflavored.subspaces.WardenSubspaces;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.warden.Sniffing;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A target the Warden sniffs out only through its Subspace never keeps it from digging back down. */
@Mixin(Sniffing.class)
public abstract class SniffingMixin {
	// The lambda in stop() that handles the sniffed target, called with the Warden and that target.
	@WrapOperation(
		method = "lambda$stop$0",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/monster/warden/WardenAi;setDisturbanceLocation(Lnet/minecraft/world/entity/monster/warden/Warden;Lnet/minecraft/core/BlockPos;)V"
		)
	)
	private static void investigateWithoutHoldingFarTargets(
		Warden warden,
		BlockPos position,
		Operation<Void> original,
		@Local(argsOnly = true, index = 1) LivingEntity target
	) {
		WardenSubspaces.disturbFromSniff(warden, target, () -> original.call(warden, position));
	}
}
