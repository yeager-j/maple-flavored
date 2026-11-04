package mapleflavored.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Enchanted books dropped as items survive fire, lava and explosions, like Netherite. Plain books still burn. */
public class EnchantedBookGameTests {
	private static final BlockPos HAZARD_POS = new BlockPos(3, 1, 3);
	private static final int BURN_TICKS = 60;
	private static final double SEARCH_RADIUS = 8.0;

	@GameTest(maxTicks = 100)
	public void enchantedBooksSurviveLava(GameTestHelper helper) {
		helper.setBlock(HAZARD_POS, Blocks.LAVA);
		dropBooks(helper);
		assertOnlyEnchantedBooksRemainAfter(helper, BURN_TICKS);
	}

	@GameTest(maxTicks = 100)
	public void enchantedBooksSurviveFire(GameTestHelper helper) {
		helper.setBlock(HAZARD_POS.below(), Blocks.NETHERRACK);
		helper.setBlock(HAZARD_POS, Blocks.FIRE);
		dropBooks(helper);
		assertOnlyEnchantedBooksRemainAfter(helper, BURN_TICKS);
	}

	@GameTest
	public void enchantedBooksSurviveExplosions(GameTestHelper helper) {
		Vec3 center = helper.absoluteVec(Vec3.atCenterOf(HAZARD_POS));

		dropBooks(helper);
		helper.getLevel().explode(null, center.x, center.y, center.z, 4.0F, Level.ExplosionInteraction.TNT);
		assertOnlyEnchantedBooksRemainAfter(helper, 1);
	}

	private static void assertOnlyEnchantedBooksRemainAfter(GameTestHelper helper, int ticks) {
		helper.runAfterDelay(ticks, () -> {
			helper.assertItemEntityPresent(Items.ENCHANTED_BOOK, HAZARD_POS, SEARCH_RADIUS);
			helper.assertItemEntityNotPresent(Items.BOOK, HAZARD_POS, SEARCH_RADIUS);
			helper.succeed();
		});
	}

	private static void dropBooks(GameTestHelper helper) {
		var sharpness = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);

		drop(helper, EnchantmentHelper.createBook(new EnchantmentInstance(sharpness, 1)));
		drop(helper, new ItemStack(Items.BOOK));
	}

	private static void drop(GameTestHelper helper, ItemStack stack) {
		Vec3 pos = helper.absoluteVec(Vec3.atCenterOf(HAZARD_POS));
		ItemEntity item = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, stack, 0.0, 0.0, 0.0);

		helper.getLevel().addFreshEntity(item);
	}
}
