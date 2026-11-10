package mapleflavored.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Rolls loot tables of any non-block type the way the game would, for tests that count what they give, and checks a
 * count against the rate it should come up at.
 */
final class LootRolls {
	// How many binomial standard deviations a count may stray from its expected share.
	private static final double RATE_TOLERANCE = 4.0;

	private LootRolls() {
	}

	/** Each roll's drops, from rolling the table {@code count} times with a seed of the table's own. */
	static List<List<ItemStack>> rollMany(GameTestHelper helper, ResourceKey<LootTable> key, int count) {
		RandomSource random = RandomSource.create(key.identifier().hashCode());
		List<List<ItemStack>> rolls = new ArrayList<>();

		for (int i = 0; i < count; i++) {
			rolls.add(roll(helper, key, random));
		}

		return rolls;
	}

	/** Fails the test unless {@code count} of {@code trials} is about the {@code expected} share of them. */
	static void assertRate(GameTestHelper helper, String what, int count, int trials, double expected) {
		double mean = trials * expected;
		double band = RATE_TOLERANCE * Math.sqrt(trials * expected * (1.0 - expected));

		helper.assertTrue(Math.abs(count - mean) <= band, what + ": " + count + " of " + trials + ", expected about " + Math.round(mean));
	}

	/** One roll of the table, with only the parameters its declared type allows. */
	static List<ItemStack> roll(GameTestHelper helper, ResourceKey<LootTable> key, RandomSource random) {
		ServerLevel level = helper.getLevel();
		LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
		ContextKeySet type = table.getParamSet();
		LootParams.Builder params = new LootParams.Builder(level);

		if (type.allowed().contains(LootContextParams.ORIGIN)) {
			params.withParameter(LootContextParams.ORIGIN, helper.absoluteVec(Vec3.ZERO));
		}

		if (type.allowed().contains(LootContextParams.THIS_ENTITY)) {
			params.withParameter(LootContextParams.THIS_ENTITY, new ItemEntity(level, 0, 0, 0, new ItemStack(Items.STICK)));
		}

		if (type.allowed().contains(LootContextParams.TOOL)) {
			params.withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD));
		}

		return table.getRandomItems(params.create(type), random);
	}
}
