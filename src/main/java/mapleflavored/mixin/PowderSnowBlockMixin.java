package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mapleflavored.alloys.AlloySets;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lets a full Shakudo set walk on powder snow and climb out of it, as leather boots do. Vanilla asks this both for the
 * block's collision shape and for climbing, on client and server alike.
 */
@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {
	@ModifyReturnValue(method = "canEntityWalkOnPowderSnow", at = @At("RETURN"))
	private static boolean alloySetWalksOnPowderSnow(boolean canWalk, Entity entity) {
		return canWalk || AlloySets.walksOnPowderSnow(entity);
	}
}
