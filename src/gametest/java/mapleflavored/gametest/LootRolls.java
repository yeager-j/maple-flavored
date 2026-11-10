package mapleflavored.gametest;

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

/** Rolls loot tables of any non-block type the way the game would, for tests that count what they give. */
final class LootRolls {
	private LootRolls() {
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
