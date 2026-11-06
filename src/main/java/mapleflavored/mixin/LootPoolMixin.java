package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.List;
import java.util.function.Consumer;
import mapleflavored.alloys.OrichalcumDrops;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** A pool that gives only items Orichalcum's drop bonus skips, such as the Piglin Brute's Ember, gets no chance bonus. */
@Mixin(LootPool.class)
public abstract class LootPoolMixin {
	@Shadow
	@Final
	public List<LootPoolEntryContainer> entries;

	@WrapMethod(method = "addRandomItems")
	private void skipOrichalcumBonusForExcludedDrops(Consumer<ItemStack> result, LootContext context, Operation<Void> original) {
		if (OrichalcumDrops.givesOnlyExcludedItems(entries)) {
			OrichalcumDrops.rollWithoutChanceBonus(() -> original.call(result, context));
		} else {
			original.call(result, context);
		}
	}
}
