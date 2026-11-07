package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mapleflavored.trades.BookTrade;
import mapleflavored.trades.TradeMultiplierHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A villager rewards the book trade per find: its own XP, before vanilla checks for a level up, and an orb of player XP
 * on top of vanilla's trade orb.
 */
@Mixin(Villager.class)
abstract class VillagerMixin implements TradeMultiplierHolder {
	@Unique
	private int tradeMultiplier = 1;

	@Override
	public void mapleflavored$setTradeMultiplier(int multiplier) {
		tradeMultiplier = multiplier;
	}

	@ModifyExpressionValue(method = "rewardTradeXp", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;getXp()I"))
	private int gainXpPerFind(int xp) {
		return xp * tradeMultiplier;
	}

	@Inject(method = "rewardTradeXp", at = @At("TAIL"))
	private void payPlayerXpPerFind(MerchantOffer offer, CallbackInfo callback) {
		Villager villager = (Villager) (Object) this;

		if (!BookTrade.isBookTrade(offer) || !(villager.level() instanceof ServerLevel level)) return;

		ExperienceOrb.award(level, villager.position().add(0, 0.5, 0), BookTrade.PLAYER_XP_PER_FIND * tradeMultiplier);
	}
}
