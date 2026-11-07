package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mapleflavored.trades.BookTrade;
import mapleflavored.trades.TradeMultiplierHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Taking the book trade's result rewards villager XP and player XP for each find the paid book held. */
@Mixin(MerchantResultSlot.class)
abstract class MerchantResultSlotMixin {
	@Shadow
	@Final
	private MerchantContainer slots;

	@Unique
	private int tradeMultiplier = 1;

	// Read before vanilla takes the payment, which consumes the book.
	@Inject(method = "onTake", at = @At("HEAD"))
	private void readPaidBook(Player player, ItemStack carried, CallbackInfo callback) {
		MerchantOffer offer = slots.getActiveOffer();

		tradeMultiplier = offer == null ? 1 : BookTrade.multiplier(offer, slots.getItem(0), slots.getItem(1));
	}

	@WrapOperation(
		method = "onTake",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/Merchant;notifyTrade(Lnet/minecraft/world/item/trading/MerchantOffer;)V")
	)
	private void rewardPerFind(Merchant merchant, MerchantOffer offer, Operation<Void> original) {
		if (!(merchant instanceof TradeMultiplierHolder holder)) {
			original.call(merchant, offer);
			return;
		}

		holder.mapleflavored$setTradeMultiplier(tradeMultiplier);

		try {
			original.call(merchant, offer);
		} finally {
			holder.mapleflavored$setTradeMultiplier(1);
		}
	}

	// The client's XP bar, which this line advances as a prediction.
	@ModifyExpressionValue(method = "onTake", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;getXp()I"))
	private int advanceXpBarPerFind(int xp) {
		return xp * tradeMultiplier;
	}
}
