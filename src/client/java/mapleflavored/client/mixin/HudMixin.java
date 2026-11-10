package mapleflavored.client.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mapleflavored.client.hearts.WitheredHeartsHud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** The health bar shows the player's Withered Hearts, drawn by {@link WitheredHeartsHud}. */
@Mixin(Hud.class)
abstract class HudMixin {
	// Widened here, before vanilla compares max health with remembered health, so the withered slots count towards the
	// row count, the armor bar's height and the heart containers.
	@WrapOperation(
		method = "extractPlayerHealth",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D")
	)
	private double makeRoomForWitheredHearts(Player player, Holder<Attribute> attribute, Operation<Double> original) {
		return WitheredHeartsHud.maxHealthWithWitheredSlots(player, original.call(player, attribute));
	}

	@ModifyVariable(method = "extractHearts", at = @At("HEAD"), argsOnly = true, name = "oldHealth")
	private int keepRememberedHealthToUsableHearts(int oldHealth, @Local(argsOnly = true) Player player) {
		return WitheredHeartsHud.rememberedHealthWithinUsableHearts(player, oldHealth);
	}

	// Stands in for the empty heart vanilla draws under every heart. Health never fills a withered slot, so that is
	// all vanilla would draw there.
	@WrapWithCondition(
		method = "extractHearts",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/Hud;extractHeart(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Hud$HeartType;IIZZZ)V",
			ordinal = 0
		)
	)
	private boolean drawHollowHeartInWitheredSlot(
		Hud hud,
		GuiGraphicsExtractor graphics,
		@Coerce Object containerType,
		int xo,
		int yo,
		boolean isHardcore,
		boolean blinks,
		boolean half,
		@Local(argsOnly = true) Player player,
		@Local(name = "containerIndex") int containerIndex
	) {
		return !WitheredHeartsHud.drawWitheredSlot(graphics, player, containerIndex, xo, yo, isHardcore, blinks);
	}
}
