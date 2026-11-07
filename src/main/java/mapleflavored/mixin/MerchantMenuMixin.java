package mapleflavored.mixin;

import mapleflavored.trades.BookTrade;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shift-clicking the book trade's payout does nothing unless the inventory holds all of it. Vanilla takes the payment
 * after moving any part of the result and drops the rest, which would lose most of a level V book's 64 emeralds.
 */
@Mixin(MerchantMenu.class)
abstract class MerchantMenuMixin {
	@Unique
	private static final int RESULT_SLOT = 2;
	@Unique
	private static final int FIRST_INVENTORY_SLOT = 3;
	@Unique
	private static final int INVENTORY_SLOT_END = 39;

	@Shadow
	@Final
	private MerchantContainer tradeContainer;

	@Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
	private void refusePartialBookPayout(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> callback) {
		MerchantOffer offer = tradeContainer.getActiveOffer();
		ItemStack payout = tradeContainer.getItem(RESULT_SLOT);

		if (slotIndex != RESULT_SLOT || offer == null || !BookTrade.isBookTrade(offer) || payout.isEmpty()) return;
		if (roomFor(payout) < payout.getCount()) callback.setReturnValue(ItemStack.EMPTY);
	}

	@Unique
	private int roomFor(ItemStack stack) {
		AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
		int room = 0;

		for (int index = FIRST_INVENTORY_SLOT; index < INVENTORY_SLOT_END; index++) {
			Slot slot = menu.getSlot(index);
			ItemStack held = slot.getItem();

			if (held.isEmpty()) {
				room += slot.getMaxStackSize(stack);
			} else if (ItemStack.isSameItemSameComponents(held, stack)) {
				room += slot.getMaxStackSize(stack) - held.getCount();
			}
		}

		return room;
	}
}
