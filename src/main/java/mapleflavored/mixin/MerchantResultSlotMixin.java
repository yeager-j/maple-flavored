package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import mapleflavored.trades.BookTrade;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Taking the book trade's result rewards villager XP and player XP for each find the paid book held. The trade's
 * multiplier is shared between this mixin's handlers for the one {@code onTake} call it belongs to.
 */
@Mixin(MerchantResultSlot.class)
abstract class MerchantResultSlotMixin {
	@Shadow
	@Final
	private MerchantContainer slots;

	// Read before vanilla takes the payment, which consumes the book.
	@Inject(method = "onTake", at = @At("HEAD"))
	private void readPaidBook(Player player, ItemStack carried, CallbackInfo callback, @Share("multiplier") LocalIntRef multiplier) {
		MerchantOffer offer = slots.getActiveOffer();

		multiplier.set(offer == null ? 1 : BookTrade.multiplier(offer, slots.getItem(0), slots.getItem(1)));
	}

	@WrapOperation(
		method = "onTake",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/Merchant;notifyTrade(Lnet/minecraft/world/item/trading/MerchantOffer;)V")
	)
	private void rewardPerFind(Merchant merchant, MerchantOffer offer, Operation<Void> original, @Share("multiplier") LocalIntRef multiplier) {
		BookTrade.rewardFinds(merchant, offer, multiplier.get());
		original.call(merchant, offer);
	}

	// The client's XP bar, which this line advances as a prediction.
	@ModifyExpressionValue(method = "onTake", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;getXp()I"))
	private int advanceXpBarPerFind(int xp, @Share("multiplier") LocalIntRef multiplier) {
		return xp * multiplier.get();
	}
}
