package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mapleflavored.alloys.AlloyMining;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops dying players dropping XP orbs, since their XP drops as a Remnant instead, and adds Shakudo's mining bonus to
 * the final break speed.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@ModifyReturnValue(method = "getBaseExperienceReward", at = @At("RETURN"))
	private int dropNoExperienceOrbs(int experienceReward) {
		return 0;
	}

	@ModifyReturnValue(method = "getDestroySpeed", at = @At("RETURN"))
	private float addShakudoMiningBonus(float speed, BlockState state) {
		return AlloyMining.destroySpeed((Player) (Object) this, speed);
	}
}
