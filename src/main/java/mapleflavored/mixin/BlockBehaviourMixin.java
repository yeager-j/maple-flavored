package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mapleflavored.alloys.AlloyMining;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adamant tools mine hard blocks no slower than the hardness cap, and an Adamant pickaxe breaks bedrock in a set time. */
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {
	@Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
	private void breakBedrock(BlockState state, Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> callback) {
		AlloyMining.bedrockProgress(player, state).ifPresent(callback::setReturnValue);
	}

	@ModifyExpressionValue(
		method = "getDestroyProgress",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroySpeed(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F")
	)
	private float capAdamantHardness(float hardness, BlockState state, Player player) {
		return AlloyMining.hardness(player, state, hardness);
	}
}
