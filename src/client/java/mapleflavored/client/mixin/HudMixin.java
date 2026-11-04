package mapleflavored.client.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mapleflavored.MapleFlavored;
import mapleflavored.hearts.Hearts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Draws the player's Withered Hearts as broken hearts after their usable ones, before any absorption hearts. */
@Mixin(Hud.class)
abstract class HudMixin {
	// The broken heart glyph that marks Milestones, added to the GUI atlas by assets/minecraft/atlases/gui.json.
	private static final Identifier WITHERED_HEART_SPRITE = MapleFlavored.id("hud/heart/withered");
	// The red Milestone descriptions give the same glyph.
	private static final int MILESTONE_RED = ARGB.opaque(TextColor.RED.getValue());
	private static final int OUTLINE = ARGB.color(255, 0, 0, 0);
	private static final int GLYPH_SIZE = 8;
	// The glyph is a pixel shorter than a heart; dropping it a pixel puts its point on the heart's point.
	private static final int GLYPH_DROP = 1;

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

	// Wraps the empty container drawn under every heart. Health never fills a withered slot, so the container is all
	// vanilla would draw there.
	@WrapWithCondition(
		method = "extractHearts",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/Hud;extractHeart(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Hud$HeartType;IIZZZ)V",
			ordinal = 0
		)
	)
	private boolean drawWitheredHeartInsteadOfContainer(
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
		int usableHearts = Mth.ceil(player.getMaxHealth() / 2.0F);
		boolean withered = containerIndex >= usableHearts && containerIndex < usableHearts + Hearts.witheredHearts(player);

		if (!withered) return true;

		drawOutlinedGlyph(graphics, xo, yo + GLYPH_DROP);

		return false;
	}

	// The glyph fills its whole 8 px slot, so without an outline like a heart's, neighbouring glyphs run together.
	private static void drawOutlinedGlyph(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, WITHERED_HEART_SPRITE, x - 1, y, GLYPH_SIZE, GLYPH_SIZE, OUTLINE);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, WITHERED_HEART_SPRITE, x + 1, y, GLYPH_SIZE, GLYPH_SIZE, OUTLINE);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, WITHERED_HEART_SPRITE, x, y - 1, GLYPH_SIZE, GLYPH_SIZE, OUTLINE);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, WITHERED_HEART_SPRITE, x, y + 1, GLYPH_SIZE, GLYPH_SIZE, OUTLINE);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, WITHERED_HEART_SPRITE, x, y, GLYPH_SIZE, GLYPH_SIZE, MILESTONE_RED);
	}
}
