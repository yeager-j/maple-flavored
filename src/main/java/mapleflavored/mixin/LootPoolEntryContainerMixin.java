package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mapleflavored.alloys.OrichalcumDrops;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import org.spongepowered.asm.mixin.Mixin;

/** An entry that gives an item Orichalcum's drop bonus skips tests its conditions with no chance bonus. */
@Mixin(LootPoolEntryContainer.class)
public abstract class LootPoolEntryContainerMixin {
	@WrapMethod(method = "canRun")
	private boolean skipOrichalcumBonusForExcludedDrops(LootContext context, Operation<Boolean> original) {
		if (!OrichalcumDrops.givesOnlyExcludedItems((LootPoolEntryContainer) (Object) this)) return original.call(context);

		return OrichalcumDrops.testWithoutChanceBonus(() -> original.call(context));
	}
}
