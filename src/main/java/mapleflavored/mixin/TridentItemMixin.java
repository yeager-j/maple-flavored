package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mapleflavored.grounded.Grounded;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TridentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops Riptide for a Grounded player by treating them as dry, the way vanilla refuses Riptide out of water and rain.
 * The wet check only matters for Riptide tridents, so plain throws are unchanged.
 */
@Mixin(TridentItem.class)
public abstract class TridentItemMixin {
	@WrapOperation(
		method = {"use", "releaseUsing"},
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z")
	)
	private boolean dryWhileGrounded(Player player, Operation<Boolean> original) {
		return !Grounded.isGrounded(player) && original.call(player);
	}
}
