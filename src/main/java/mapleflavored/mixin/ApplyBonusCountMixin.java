package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mapleflavored.alloys.OrichalcumDrops;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Fortune's count bonus also gets Orichalcum's drop bonus, read from the tool that broke the block. */
@Mixin(ApplyBonusCount.class)
public abstract class ApplyBonusCountMixin {
	@Shadow
	@Final
	private Holder<Enchantment> enchantment;

	@WrapMethod(method = "run")
	private ItemStack addOrichalcumBonus(ItemStack drop, LootContext context, Operation<ItemStack> original) {
		int countBefore = drop.getCount();
		ItemStack result = original.call(drop, context);

		OrichalcumDrops.addCountBonus(result, countBefore, enchantment, context.getOptional(LootContextParams.TOOL), context.getRandom());

		return result;
	}
}
