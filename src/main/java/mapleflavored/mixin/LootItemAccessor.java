package mapleflavored.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the item a loot entry gives, so Orichalcum's drop bonus can skip excluded items. */
@Mixin(LootItem.class)
public interface LootItemAccessor {
	@Accessor
	Holder<Item> getItem();
}
