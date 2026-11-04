package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Stops dying players dropping XP orbs, since their XP drops as a Remnant instead. */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@ModifyReturnValue(method = "getBaseExperienceReward", at = @At("RETURN"))
	private int dropNoExperienceOrbs(int experienceReward) {
		return 0;
	}
}
