package mapleflavored.mixin;

import java.util.Optional;
import mapleflavored.anvil.AnvilResults;
import mapleflavored.experience.ExperiencePoints;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The anvil makes what {@link AnvilResults} says, priced in XP points instead of levels.
 *
 * <p>Results are worked out only on the server, which has the recipes that set repair counts. The client shows the
 * synced result and cost, and its predicted pickup uses the synced material count, so surplus material stays put.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
	@Shadow
	private int repairItemCountCost;
	@Shadow
	private @Nullable String itemName;
	@Shadow
	@Final
	private DataSlot cost;
	@Shadow
	private boolean onlyRenaming;

	@Unique
	private final DataSlot materialUsed = DataSlot.standalone();

	private AnvilMenuMixin(MenuType<?> menuType, int containerId, Inventory inventory, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition slots) {
		super(menuType, containerId, inventory, access, slots);
	}

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
	private void syncMaterialUsed(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo callback) {
		addDataSlot(materialUsed);
	}

	@Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
	private void createMapleResult(CallbackInfo callback) {
		callback.cancel();

		if (!(player.level() instanceof ServerLevel level)) return;

		Optional<AnvilResults.Result> result = AnvilResults.compute(inputSlots.getItem(0), inputSlots.getItem(1), itemName, level.recipeAccess());

		resultSlots.setItem(0, result.map(AnvilResults.Result::item).orElse(ItemStack.EMPTY));
		cost.set(result.map(AnvilResults.Result::xpCost).orElse(0));
		materialUsed.set(result.map(AnvilResults.Result::materialUsed).orElse(0));
		onlyRenaming = false;
		broadcastChanges();
	}

	@Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
	private void affordInPoints(Player taker, boolean hasItem, CallbackInfoReturnable<Boolean> callback) {
		callback.setReturnValue(hasItem && (taker.hasInfiniteMaterials() || ExperiencePoints.total(taker) >= cost.get()));
	}

	@Inject(method = "onTake", at = @At("HEAD"))
	private void useSyncedMaterialCount(Player taker, ItemStack carried, CallbackInfo callback) {
		repairItemCountCost = materialUsed.get();
	}

	// The client runs onTake too, as a prediction; the server's XP sync corrects it.
	@Redirect(method = "onTake", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;giveExperienceLevels(I)V"))
	private void payInPoints(Player taker, int negatedCost) {
		if (taker instanceof ServerPlayer serverTaker) ExperiencePoints.set(serverTaker, ExperiencePoints.total(serverTaker) - cost.get());
	}
}
