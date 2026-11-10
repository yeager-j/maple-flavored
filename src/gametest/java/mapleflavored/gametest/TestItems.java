package mapleflavored.gametest;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

/** The enchantments, enchanted books and worn gear that gear and enchanting tests start from. */
final class TestItems {
	private TestItems() {
	}

	/** The registered enchantment. */
	static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
		return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}

	/** An enchanted book holding the one enchantment. */
	static ItemStack book(Holder<Enchantment> enchantment, int level) {
		return EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, level));
	}

	/** The item with 1 durability left. */
	static ItemStack damaged(Item item) {
		ItemStack stack = new ItemStack(item);

		stack.setDamageValue(stack.getMaxDamage() - 1);

		return stack;
	}
}
