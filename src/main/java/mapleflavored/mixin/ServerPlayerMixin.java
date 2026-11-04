package mapleflavored.mixin;

import mapleflavored.zephyr.Zephyr;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies Zephyr's fall rules to each move the client reports, before vanilla counts it. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
	@Inject(method = "checkFallDamage", at = @At("HEAD"))
	private void applyZephyrFallRules(double ya, boolean onGround, BlockState onState, BlockPos pos, CallbackInfo ci) {
		Zephyr.onFallCheck((ServerPlayer) (Object) this, ya);
	}
}
