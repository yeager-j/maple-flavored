package mapleflavored.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Reaches a container's item list directly. A loot chest's {@code setItem} rolls its loot first, so a book put in this
 * way leaves the loot to be rolled when the chest is opened.
 */
@Mixin(BaseContainerBlockEntity.class)
public interface BaseContainerBlockEntityInvoker {
	/** The container's slots, changed in place. */
	@Invoker("getItems")
	NonNullList<ItemStack> items();
}
