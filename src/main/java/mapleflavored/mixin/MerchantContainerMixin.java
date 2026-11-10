package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mapleflavored.trades.BookTrade;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The book trade's result slot shows the payout for the book placed, and the XP bar previews its villager XP. Runs on
 * both sides, so the client shows the same payout the server pays.
 */
@Mixin(MerchantContainer.class)
abstract class MerchantContainerMixin {
	@Shadow
	public abstract ItemStack getItem(int slot);

	@WrapOperation(
		method = "updateSellItem",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;assemble()Lnet/minecraft/world/item/ItemStack;")
	)
	private ItemStack payPerFind(MerchantOffer offer, Operation<ItemStack> original) {
		return BookTrade.payout(original.call(offer), BookTrade.multiplier(offer, getItem(0), getItem(1)));
	}

	@WrapOperation(method = "updateSellItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;getXp()I"))
	private int previewXpPerFind(MerchantOffer offer, Operation<Integer> original) {
		return original.call(offer) * BookTrade.multiplier(offer, getItem(0), getItem(1));
	}
}
