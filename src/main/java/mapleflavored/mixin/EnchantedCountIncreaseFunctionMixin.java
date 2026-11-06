package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mapleflavored.alloys.OrichalcumDrops;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Looting's count bonus also gets Orichalcum's drop bonus, read from the killer's main hand, within the same limit. */
@Mixin(EnchantedCountIncreaseFunction.class)
public abstract class EnchantedCountIncreaseFunctionMixin {
	@Shadow
	@Final
	private Holder<Enchantment> enchantment;

	@Shadow
	@Final
	private int limit;

	@Shadow
	private boolean hasLimit() {
		throw new AssertionError();
	}

	@WrapMethod(method = "run")
	private ItemStack addOrichalcumBonus(ItemStack drop, LootContext context, Operation<ItemStack> original) {
		int countBefore = drop.getCount();
		ItemStack result = original.call(drop, context);

		OrichalcumDrops.addCountBonus(result, countBefore, enchantment, OrichalcumDrops.killerWeapon(context), context.getRandom());

		if (hasLimit()) result.limitSize(limit);

		return result;
	}
}
