package mapleflavored.mixin;

import mapleflavored.smithing.MaterialUpgrades;
import mapleflavored.smithing.UpgradeRefusalMenu;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Syncs to the client whether the inputs match a material upgrade that refused the enchanted base, so the screen can
 * say why. Only the server has the recipes to tell.
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin extends ItemCombinerMenu implements UpgradeRefusalMenu {
	@Shadow
	@Final
	private Level level;

	@Unique
	private final DataSlot refusedEnchantedBase = DataSlot.standalone();

	private SmithingMenuMixin(MenuType<?> menuType, int containerId, Inventory inventory, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition slots) {
		super(menuType, containerId, inventory, access, slots);
	}

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
	private void syncRefusal(int containerId, Inventory inventory, ContainerLevelAccess access, Level level, CallbackInfo callback) {
		addDataSlot(refusedEnchantedBase);
	}

	// The menu records the matched recipe even when it assembled nothing, which is how a refusal shows.
	@Inject(method = "createResult", at = @At("TAIL"))
	private void recordRefusal(CallbackInfo callback) {
		if (!(level instanceof ServerLevel)) return;

		RecipeHolder<?> matched = resultSlots.getRecipeUsed();
		boolean refused = matched != null
			&& matched.value() instanceof SmithingTransformRecipe
			&& MaterialUpgrades.refuses(inputSlots.getItem(SmithingMenu.BASE_SLOT));

		refusedEnchantedBase.set(refused ? 1 : 0);
	}

	@Override
	public boolean mapleflavored$refusedEnchantedBase() {
		return refusedEnchantedBase.get() > 0;
	}
}
