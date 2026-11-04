package mapleflavored.gametest;

import mapleflavored.enchantments.RemovedEnchantments;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Curse of Binding and Curse of Vanishing are never registered, and the vanilla tags that listed them are replaced
 * by copies without them, each still holding vanilla's other values.
 */
public class RemovedCursesGameTests {
	// Vanilla's 43, less the 2 curses, plus Zephyr and Blight. A Full Library holds all of them but Mending.
	private static final int ENCHANTMENTS = 43;

	@GameTest
	public void theCursesAreNotRegistered(GameTestHelper helper) {
		Registry<Enchantment> enchantments = enchantments(helper);

		for (ResourceKey<Enchantment> curse : RemovedEnchantments.KEYS) {
			helper.assertFalse(enchantments.containsKey(curse), curse.identifier() + " is registered");
		}

		helper.assertValueEqual(enchantments.size(), ENCHANTMENTS, "registered enchantments");
		helper.succeed();
	}

	@GameTest
	public void theTagsThatListedTheCursesStillLoad(GameTestHelper helper) {
		assertTagHolds(helper, EnchantmentTags.ON_RANDOM_LOOT, Enchantments.MENDING);
		assertTagHolds(helper, EnchantmentTags.TRADEABLE, Enchantments.MENDING);
		assertTagHolds(helper, EnchantmentTags.TREASURE, Enchantments.MENDING);
		assertTagHolds(helper, EnchantmentTags.TOOLTIP_ORDER, Enchantments.SHARPNESS);
		helper.assertTrue(enchantments(helper).get(EnchantmentTags.CURSE).isPresent(), "the curse tag loaded");
		helper.assertValueEqual(enchantments(helper).getTagOrEmpty(EnchantmentTags.CURSE).iterator().hasNext(), false, "the curse tag holds anything");
		helper.succeed();
	}

	private static void assertTagHolds(GameTestHelper helper, TagKey<Enchantment> tag, ResourceKey<Enchantment> kept) {
		boolean holds = enchantments(helper).getOrThrow(kept).is(tag);

		helper.assertTrue(holds, tag.location() + " holds " + kept.identifier());
	}

	private static Registry<Enchantment> enchantments(GameTestHelper helper) {
		return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
	}
}
