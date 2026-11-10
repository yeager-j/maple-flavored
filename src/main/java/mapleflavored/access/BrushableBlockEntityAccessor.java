package mapleflavored.access;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Sets what a suspicious block gives when brushed. */
@Mixin(BrushableBlockEntity.class)
public interface BrushableBlockEntityAccessor {
	/** The item brushing drops when the block has no loot table. */
	@Accessor("item")
	void setItem(ItemStack item);

	/** The loot table rolled for the item on the first brush, or none to drop the item as it is. */
	@Accessor("lootTable")
	void setLootTable(@Nullable ResourceKey<LootTable> lootTable);
}
