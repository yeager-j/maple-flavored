package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.Optional;
import mapleflavored.structurebooks.PiglinAnger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.PiglinBruteAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A Piglin Brute hunts the player who took its Bastion's book from any distance, as {@link PiglinAiMixin} does a Piglin. */
@Mixin(PiglinBruteAi.class)
public abstract class PiglinBruteAiMixin {
	@ModifyReturnValue(method = "findNearestValidAttackTarget", at = @At("RETURN"))
	private static Optional<? extends LivingEntity> putTheHuntFirst(
		Optional<? extends LivingEntity> vanillaTarget,
		ServerLevel level,
		AbstractPiglin body
	) {
		Optional<LivingEntity> quarry = PiglinAnger.updateHunt(level, body);

		return quarry.isPresent() ? quarry : vanillaTarget;
	}
}
