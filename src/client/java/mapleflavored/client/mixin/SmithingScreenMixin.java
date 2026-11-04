package mapleflavored.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mapleflavored.smithing.UpgradeRefusalMenu;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SmithingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** The smithing error tooltip says when an upgrade failed only because the item is enchanted. */
@Mixin(SmithingScreen.class)
abstract class SmithingScreenMixin extends ItemCombinerScreen<SmithingMenu> {
	@Unique
	private static final Component ENCHANTED_ERROR_TOOLTIP = Component.translatable("container.maple_flavored.upgrade.enchanted_error_tooltip");

	private SmithingScreenMixin(SmithingMenu menu, Inventory inventory, Component title, Identifier menuResource) {
		super(menu, inventory, title, menuResource);
	}

	@ModifyExpressionValue(
		method = "extractOnboardingTooltips",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/SmithingScreen;ERROR_TOOLTIP:Lnet/minecraft/network/chat/Component;")
	)
	private Component explainEnchantedRefusal(Component vanillaTooltip) {
		return ((UpgradeRefusalMenu) menu).mapleflavored$refusedEnchantedBase() ? ENCHANTED_ERROR_TOOLTIP : vanillaTooltip;
	}
}
