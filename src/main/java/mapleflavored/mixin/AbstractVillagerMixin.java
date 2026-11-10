package mapleflavored.mixin;

import mapleflavored.trades.BookTrade;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.storage.ValueInput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A villager saved before the book trade carried its marker gets the marker when it loads. */
@Mixin(AbstractVillager.class)
abstract class AbstractVillagerMixin {
	@Shadow
	protected @Nullable MerchantOffers offers;

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void markSavedBookTrade(ValueInput input, CallbackInfo callback) {
		if (offers != null) BookTrade.markSavedOffers(offers);
	}
}
