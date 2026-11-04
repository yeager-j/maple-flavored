package mapleflavored.client.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
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
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Draws the player's Withered Hearts as empty hearts with a see-through crack, after their usable hearts and before
 * any absorption hearts.
 */
@Mixin(Hud.class)
abstract class HudMixin {
	private static final int HEART_SIZE = 9;
	// The crack of the broken heart glyph that marks Milestones, credited in CREDITS.md, left out of a withered slot's
	// empty heart. It runs through the outline's top dip, so only the bottom tip joins the halves. One string per row
	// of the 9 × 9 heart.
	private static final String[] CRACK = {
		".........",
		"....#....",
		"...##....",
		"....##...",
		".....##..",
		"....##...",
		"....#....",
		"....#....",
		"........."
	};

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

	// Replaces the empty heart vanilla draws under every heart. Health never fills a withered slot, so it is all
	// vanilla would draw there.
	@WrapWithCondition(
		method = "extractHearts",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/Hud;extractHeart(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Hud$HeartType;IIZZZ)V",
			ordinal = 0
		)
	)
	private boolean drawCrackedHeartInWitheredSlot(
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

		drawWithoutCrack(graphics, emptyHeartSprite(isHardcore, blinks), xo, yo);

		return false;
	}

	private static Identifier emptyHeartSprite(boolean isHardcore, boolean blinks) {
		String sprite = isHardcore ? "hud/heart/container_hardcore" : "hud/heart/container";

		return Identifier.withDefaultNamespace(blinks ? sprite + "_blinking" : sprite);
	}

	// Draws each row of the sprite in runs that skip the crack's pixels, leaving them transparent.
	private static void drawWithoutCrack(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y) {
		for (int row = 0; row < HEART_SIZE; row++) {
			int runStart = 0;

			for (int column = 0; column <= HEART_SIZE; column++) {
				boolean runEnds = column == HEART_SIZE || CRACK[row].charAt(column) == '#';

				if (!runEnds) continue;

				if (column > runStart) {
					graphics.blitSprite(
						RenderPipelines.GUI_TEXTURED, sprite, HEART_SIZE, HEART_SIZE, runStart, row, x + runStart, y + row, column - runStart, 1
					);
				}

				runStart = column + 1;
			}
		}
	}
}
