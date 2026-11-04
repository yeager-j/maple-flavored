package mapleflavored.loot;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * Mobs are never an enchantment source: gear a mob spawned with drops without its enchantments, while gear it picked
 * up drops exactly as it was. The mob's drop chance tells them apart. Picking gear up, or being handed it by a player
 * or dispenser, makes that slot a preserved drop; spawned gear keeps a chance of 1 or less.
 */
public final class SpawnedGear {
	private SpawnedGear() {
	}

	/** The stack {@code mob} should drop for {@code gear} leaving {@code slot}. */
	public static ItemStack asDropped(Mob mob, EquipmentSlot slot, ItemStack gear) {
		return mob.getDropChances().isPreserved(slot) ? gear : LootEnchantments.plain(gear);
	}
}
