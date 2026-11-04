package mapleflavored.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mapleflavored.MapleFlavored;
import mapleflavored.hearts.Hearts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the player's Withered Hearts as cracked empty hearts after their usable ones, before any absorption hearts. */
@Mixin(Hud.class)
abstract class HudMixin {
	// The crack of the broken heart glyph that marks Milestones, drawn over the empty heart vanilla leaves in the slot.
	private static final Identifier WITHERED_CRACK_SPRITE = MapleFlavored.id("hud/heart/withered_crack");
	private static final int HEART_SIZE = 9;

	// Max health only covers usable hearts. Widening it here, before vanilla compares it with remembered health,
	// makes room for withered slots in the row count, the armor bar's height and the heart containers.
	@WrapOperation(
		method = "extractPlayerHealth",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D")
	)
	private double makeRoomForWitheredHearts(Player player, Holder<Attribute> attribute, Operation<Double> original) {
		return original.call(player, attribute) + Hearts.witheredHearts(player) * 2.0;
	}

	// Health remembered from before a heart withered would otherwise blink in that heart's slot.
	@ModifyVariable(method = "extractHearts", at = @At("HEAD"), argsOnly = true, name = "oldHealth")
	private int keepRememberedHealthToUsableHearts(int oldHealth, @Local(argsOnly = true) Player player) {
		return Math.min(oldHealth, Mth.ceil(player.getMaxHealth()));
	}

	@Inject(
		method = "extractHearts",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/Hud;extractHeart(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Hud$HeartType;IIZZZ)V",
			ordinal = 0,
			shift = At.Shift.AFTER
		)
	)
	private void crackWitheredHeart(
		CallbackInfo ci,
		@Local(argsOnly = true) GuiGraphicsExtractor graphics,
		@Local(argsOnly = true) Player player,
		@Local(name = "containerIndex") int containerIndex,
		@Local(name = "xo") int xo,
		@Local(name = "yo") int yo
	) {
		int usableHearts = Mth.ceil(player.getMaxHealth() / 2.0F);
		boolean withered = containerIndex >= usableHearts && containerIndex < usableHearts + Hearts.witheredHearts(player);

		if (!withered) return;

		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, WITHERED_CRACK_SPRITE, xo, yo, HEART_SIZE, HEART_SIZE);
	}
}
