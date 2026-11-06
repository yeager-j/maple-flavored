package mapleflavored.gametest;

import java.util.List;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.Alloys;
import mapleflavored.alloys.GearKind;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Orichalcum tools add half a drop again to drops Fortune and Looting affect, at any enchantment level: half the count
 * a count function started with, or half a chance's unenchanted chance. Ancient Embers, Pith and enchanted books never
 * get it.
 *
 * <p>The fixture tables in {@code maple_flavored_gametest:loot_table} start each drop at 10, so the bonus is a whole 5
 * and the counts are exact.
 */
public class OrichalcumDropsGameTests {
	private static final int ROLLS = 200;
	private static final int BRUTE_ROLLS = 2000;
	private static final long SEED = 7L;
	private static final double DROP_RATE_TOLERANCE = 0.03;

	@GameTest
	public void anOrichalcumPickaxeAddsHalfToFortuneDrops(GameTestHelper helper) {
		helper.assertValueEqual(blockDrop(helper, "orichalcum_fortune_stone", AlloyGear.of(Alloy.ORICHALCUM, GearKind.PICKAXE)), 15, "stone from an Orichalcum pickaxe");
		helper.assertValueEqual(blockDrop(helper, "orichalcum_fortune_stone", Items.NETHERITE_PICKAXE), 10, "stone from a Netherite pickaxe");
		helper.succeed();
	}

	@GameTest
	public void fortuneDropsOfAncientEmbersGetNoBonus(GameTestHelper helper) {
		helper.assertValueEqual(blockDrop(helper, "orichalcum_fortune_ember", AlloyGear.of(Alloy.ORICHALCUM, GearKind.PICKAXE)), 10, "Embers from an Orichalcum pickaxe");
		helper.succeed();
	}

	@GameTest
	public void anOrichalcumWeaponAddsHalfToLootingDropsWithinTheirLimit(GameTestHelper helper) {
		Mob victim = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		ServerPlayer orichalcum = holding(helper, AlloyGear.of(Alloy.ORICHALCUM, GearKind.SWORD));
		ServerPlayer netherite = holding(helper, Items.NETHERITE_SWORD);

		helper.assertValueEqual(count(roll(helper, fixture(helper, "orichalcum_looting_stone"), victim, orichalcum), Items.STONE), 15, "stone killed with Orichalcum");
		helper.assertValueEqual(count(roll(helper, fixture(helper, "orichalcum_looting_stone"), victim, netherite), Items.STONE), 10, "stone killed with Netherite");
		helper.assertValueEqual(count(roll(helper, fixture(helper, "orichalcum_looting_stone_limited"), victim, orichalcum), Items.STONE), 12, "stone capped at 12");
		helper.succeed();
	}

	@GameTest
	public void anOrichalcumWeaponAddsHalfTheUnenchantedChanceToLootingChances(GameTestHelper helper) {
		Mob victim = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		LootTable table = fixture(helper, "orichalcum_looting_chance_stone");

		// 70% plus half of 70% is over 100%, so every roll drops; 70% alone misses some of 200.
		helper.assertValueEqual(dropRolls(helper, table, victim, holding(helper, AlloyGear.of(Alloy.ORICHALCUM, GearKind.SWORD)), Items.STONE), ROLLS, "drops with Orichalcum");
		helper.assertTrue(dropRolls(helper, table, victim, holding(helper, Items.NETHERITE_SWORD), Items.STONE) < ROLLS, "every roll drops with Netherite");
		helper.succeed();
	}

	@GameTest
	public void anAncientEmberEntrysChanceGetsNoBonus(GameTestHelper helper) {
		Mob victim = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		LootTable table = fixture(helper, "orichalcum_looting_chance_ember_entry");
		int drops = dropRolls(helper, table, victim, holding(helper, AlloyGear.of(Alloy.ORICHALCUM, GearKind.SWORD)), Alloys.ANCIENT_EMBER);

		helper.assertTrue(drops < ROLLS, "every roll drops an Ember with Orichalcum");
		helper.succeed();
	}

	@GameTest
	public void piglinBrutesDropEmbersNoMoreOftenToOrichalcum(GameTestHelper helper) {
		Mob brute = helper.spawnWithNoFreeWill(EntityTypes.PIGLIN_BRUTE, new BlockPos(1, 1, 1));
		LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(brute.getLootTable().orElseThrow());
		ServerPlayer player = holding(helper, AlloyGear.of(Alloy.ORICHALCUM, GearKind.SWORD));
		RandomSource random = RandomSource.create(SEED);
		int drops = 0;

		for (int roll = 0; roll < BRUTE_ROLLS; roll++) {
			if (count(table.getRandomItems(entityParams(helper, brute, player), random), Alloys.ANCIENT_EMBER) > 0) drops++;
		}

		double rate = (double) drops / BRUTE_ROLLS;

		helper.assertTrue(Math.abs(rate - 0.25) <= DROP_RATE_TOLERANCE, "Ember drop rate " + rate + " is within " + DROP_RATE_TOLERANCE + " of 0.25");
		helper.succeed();
	}

	private static ServerPlayer holding(GameTestHelper helper, Item item) {
		ServerPlayer player = MockPlayers.survivalPlayer(helper);

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));

		return player;
	}

	// Rolls a fixture block table as if the tool broke stone, once, from a seeded random.
	private static int blockDrop(GameTestHelper helper, String name, Item tool) {
		ServerLevel level = helper.getLevel();
		LootParams params = new LootParams.Builder(level)
			.withParameter(LootContextParams.BLOCK_STATE, Blocks.STONE.defaultBlockState())
			.withParameter(LootContextParams.ORIGIN, helper.absoluteVec(Vec3.ZERO))
			.withParameter(LootContextParams.TOOL, new ItemStack(tool))
			.create(LootContextParamSets.BLOCK);
		List<ItemStack> drops = fixture(helper, name).getRandomItems(params, RandomSource.create(SEED));

		return drops.stream().mapToInt(ItemStack::getCount).sum();
	}

	private static List<ItemStack> roll(GameTestHelper helper, LootTable table, Mob victim, ServerPlayer killer) {
		return table.getRandomItems(entityParams(helper, victim, killer), RandomSource.create(SEED));
	}

	// How many of ROLLS rolls drop the item, from one seeded random.
	private static int dropRolls(GameTestHelper helper, LootTable table, Mob victim, ServerPlayer killer, Item item) {
		LootParams params = entityParams(helper, victim, killer);
		RandomSource random = RandomSource.create(SEED);
		int drops = 0;

		for (int roll = 0; roll < ROLLS; roll++) {
			if (count(table.getRandomItems(params, random), item) > 0) drops++;
		}

		return drops;
	}

	// The victim's loot params as LivingEntity.dropFromLootTable builds them, killed by the player.
	private static LootParams entityParams(GameTestHelper helper, Mob victim, ServerPlayer killer) {
		ServerLevel level = helper.getLevel();

		return new LootParams.Builder(level)
			.withParameter(LootContextParams.THIS_ENTITY, victim)
			.withParameter(LootContextParams.ORIGIN, victim.position())
			.withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(killer))
			.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
			.withParameter(LootContextParams.ATTACKING_ENTITY, killer)
			.create(LootContextParamSets.ENTITY);
	}

	private static LootTable fixture(GameTestHelper helper, String name) {
		ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("maple_flavored_gametest", name));

		return helper.getLevel().getServer().reloadableRegistries().getLootTable(key);
	}

	private static int count(List<ItemStack> stacks, Item item) {
		return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
	}
}
