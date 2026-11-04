package mapleflavored.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Optional;
import mapleflavored.anvil.AnvilResults;
import mapleflavored.materialunits.MaterialUnits;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The anvil's cost line reads in XP points, since the menu's cost is in points, and is never "Too Expensive". A free
 * repair shows no line. Hovering the error icon explains two refusals: trying to enchant an item points the player to
 * the Enchanting Table, and offering a damaged item the wrong material names the right one.
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
	// Vanilla's error icon placement, and the smithing table's tooltip width.
	@Unique
	private static final int ERROR_ICON_X = 99;
	@Unique
	private static final int ERROR_ICON_Y = 45;
	@Unique
	private static final int ERROR_ICON_WIDTH = 28;
	@Unique
	private static final int ERROR_ICON_HEIGHT = 21;
	@Unique
	private static final int TOOLTIP_WIDTH = 115;
	@Unique
	private static final Component ENCHANTING_ERROR_TOOLTIP = Component.translatable("container.maple_flavored.anvil.enchanting_error_tooltip");

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

	// The anvil screen has no render hook of its own after its background; tooltips set here show after the frame.
	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void explainRefusal(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
		boolean showsError = !menu.getSlot(AnvilMenu.RESULT_SLOT).hasItem();
		boolean hoversError = isHovering(ERROR_ICON_X, ERROR_ICON_Y, ERROR_ICON_WIDTH, ERROR_ICON_HEIGHT, mouseX, mouseY);

		if (!showsError || !hoversError) return;

		refusalReason().ifPresent(reason -> graphics.setTooltipForNextFrame(font, font.split(reason, TOOLTIP_WIDTH), mouseX, mouseY));
	}

	@Unique
	private Optional<Component> refusalReason() {
		ItemStack input = menu.getSlot(AnvilMenu.INPUT_SLOT).getItem();
		ItemStack addition = menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();

		if (AnvilResults.triesToEnchant(input, addition)) return Optional.of(ENCHANTING_ERROR_TOOLTIP);
		if (!AnvilResults.offersWrongMaterial(input, addition)) return Optional.empty();

		return MaterialUnits.materialName(input)
			.map(material -> Component.translatable("container.maple_flavored.anvil.repair_error_tooltip", material));
	}
}
