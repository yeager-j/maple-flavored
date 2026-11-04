package mapleflavored.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The anvil's cost line reads in XP points, since the menu's cost is in points, and is never "Too Expensive". A free
 * repair shows no line.
 */
@Mixin(AnvilScreen.class)
abstract class AnvilScreenMixin extends ItemCombinerScreen<AnvilMenu> {
	// Vanilla's cost line colours and placement.
	@Unique
	private static final int AFFORDABLE_COLOR = -8323296;
	@Unique
	private static final int UNAFFORDABLE_COLOR = -40864;
	@Unique
	private static final int BACKDROP_COLOR = 1325400064;

	private AnvilScreenMixin(AnvilMenu menu, Inventory inventory, Component title, Identifier menuResource) {
		super(menu, inventory, title, menuResource);
	}

	// Vanilla's level line is hidden by showing it a cost of 0, and the points line drawn after it.
	@WrapOperation(method = "extractLabels", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AnvilMenu;getCost()I"))
	private int hideLevelCost(AnvilMenu anvil, Operation<Integer> original) {
		return 0;
	}

	@Inject(method = "extractLabels", at = @At("TAIL"))
	private void drawPointsCost(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo callback) {
		int cost = menu.getCost();

		if (cost <= 0 || !menu.getSlot(AnvilMenu.RESULT_SLOT).hasItem()) return;

		Component line = Component.translatable("container.maple_flavored.anvil.cost", cost);
		int color = menu.getSlot(AnvilMenu.RESULT_SLOT).mayPickup(minecraft.player) ? AFFORDABLE_COLOR : UNAFFORDABLE_COLOR;
		int x = imageWidth - 8 - font.width(line) - 2;

		graphics.fill(x - 2, 67, imageWidth - 8, 79, BACKDROP_COLOR);
		graphics.text(font, line, x, 69, color);
	}
}
