package mapleflavored.mixin;

import java.util.Optional;
import mapleflavored.anvil.AnvilRefusalMenu;
import mapleflavored.anvil.AnvilResults;
import mapleflavored.experience.ExperiencePoints;
import net.minecraft.core.registries.Registries;
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
 * synced result, cost and refusal, and its predicted pickup uses the synced material count, so surplus material stays
 * put.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu implements AnvilRefusalMenu {
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
	// A refusal's ordinal plus 1, or 0 when the anvil doesn't refuse.
	@Unique
	private final DataSlot refusal = DataSlot.standalone();

	private AnvilMenuMixin(MenuType<?> menuType, int containerId, Inventory inventory, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition slots) {
		super(menuType, containerId, inventory, access, slots);
	}

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
	private void syncMaterialUsedAndRefusal(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo callback) {
		addDataSlot(materialUsed);
		addDataSlot(refusal);
	}

	@Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
	private void createMapleResult(CallbackInfo callback) {
		callback.cancel();

		if (!(player.level() instanceof ServerLevel level)) return;

		AnvilResults.Outcome outcome = AnvilResults.compute(
			inputSlots.getItem(0), inputSlots.getItem(1), itemName, level.recipeAccess(), level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
		);
		Optional<AnvilResults.Made> made = outcome instanceof AnvilResults.Made result ? Optional.of(result) : Optional.empty();

		resultSlots.setItem(0, made.map(AnvilResults.Made::item).orElse(ItemStack.EMPTY));
		cost.set(made.map(AnvilResults.Made::xpCost).orElse(0));
		materialUsed.set(made.map(AnvilResults.Made::materialUsed).orElse(0));
		refusal.set(outcome instanceof AnvilResults.Refused refused ? refused.ordinal() + 1 : 0);
		onlyRenaming = false;
		broadcastChanges();
	}

	@Override
	public Optional<AnvilResults.Refused> mapleflavored$refusal() {
		int synced = refusal.get();

		return synced == 0 ? Optional.empty() : Optional.of(AnvilResults.Refused.values()[synced - 1]);
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
