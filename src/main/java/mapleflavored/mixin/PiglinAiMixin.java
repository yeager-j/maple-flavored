package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.Optional;
import mapleflavored.structurebooks.PiglinAnger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A Piglin hunting the player who took its Bastion's book attacks them from any distance. Picking a target and checking
 * the current one both go through this choice, so the hunt is never dropped as out of range.
 *
 * <p>Vanilla's own range check isn't enough even with the hunter's longer follow range: its targeting conditions are
 * shared statics, reset to the follow range of whichever mob's sensor ticked last, so a 16-block mob nearby would make a
 * far hunter drop its target.
 */
@Mixin(PiglinAi.class)
public abstract class PiglinAiMixin {
	@ModifyReturnValue(method = "findNearestValidAttackTarget", at = @At("RETURN"))
	private static Optional<? extends LivingEntity> putTheHuntFirst(Optional<? extends LivingEntity> vanillaTarget, ServerLevel level, Piglin body) {
		Optional<LivingEntity> quarry = PiglinAnger.updateHunt(level, body);

		return quarry.isPresent() ? quarry : vanillaTarget;
	}
}
