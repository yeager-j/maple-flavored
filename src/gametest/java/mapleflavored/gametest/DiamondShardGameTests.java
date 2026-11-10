package mapleflavored.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.GearKind;
import mapleflavored.items.MapleItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Diamond scarcity: diamond ore gives a Diamond Shard, 4 shards craft a diamond, and safe loot gives shards where
 * dangerous structures keep whole diamonds.
 */
public class DiamondShardGameTests {
	private static final List<Block> DIAMOND_ORES = List.of(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
	private static final int ORE_ROLLS = 1000;
	private static final int TABLE_ROLLS = 1000;
	// Fortune III's ore formula gives 1, 1, 2, 3 or 4 at even odds.
	private static final double FORTUNE_III_MEAN = 2.2;
	private static final double MEAN_TOLERANCE = 0.1;
	private static final String MINE_DIAMOND = "minecraft:story/mine_diamond";

	// The tables Jackson moved to shards, safe and early sources whose book diamonds become shards too.
	private static final List<String> SHARD_TABLES = List.of(
		"chests/buried_treasure",
		"chests/shipwreck_treasure",
		"chests/desert_pyramid",
		"archaeology/desert_pyramid",
		"chests/abandoned_camp_secret_chest",
		"chests/abandoned_mineshaft",
		"chests/trial_chambers/intersection",
		"chests/trial_chambers/intersection_barrel",
		"pots/trial_chambers/corridor"
	);

	// Every other table the survey found giving diamonds, directly or for a chest book.
	private static final List<String> WHOLE_DIAMOND_TABLES = List.of(
		"chests/bastion_treasure",
		"chests/bastion_other",
		"chests/end_city_treasure",
		"chests/nether_bridge",
		"chests/jungle_temple",
		"chests/stronghold_corridor",
		"chests/stronghold_crossing",
		"chests/stronghold_library",
		"chests/ancient_city",
		"chests/woodland_mansion",
		"chests/simple_dungeon",
		"chests/pillager_outpost",
		"chests/underwater_ruin_big",
		"chests/trial_chambers/reward_common",
		"chests/trial_chambers/reward_rare",
		"chests/trial_chambers/reward_ominous_common",
		"chests/trial_chambers/reward_ominous_rare"
	);

	@GameTest
	public void diamondOresDropOneShardWithoutFortune(GameTestHelper helper) {
		for (Block ore : DIAMOND_ORES) {
			List<Integer> counts = shardCounts(helper, ore, new ItemStack(Items.DIAMOND_PICKAXE));

			helper.assertTrue(counts.stream().allMatch(count -> count == 1), ore + " drops exactly 1 shard each time");
		}

		helper.succeed();
	}

	@GameTest
	public void diamondOresDropShardsWithVanillasFortuneBonus(GameTestHelper helper) {
		for (Block ore : DIAMOND_ORES) {
			List<Integer> counts = shardCounts(helper, ore, enchantAtMaxLevel(helper, new ItemStack(Items.DIAMOND_PICKAXE), Enchantments.FORTUNE));
			double mean = counts.stream().mapToInt(Integer::intValue).average().orElseThrow();

			helper.assertTrue(counts.stream().allMatch(count -> count >= 1 && count <= 4), ore + " drops 1 to 4 shards with Fortune III");
			helper.assertTrue(Math.abs(mean - FORTUNE_III_MEAN) <= MEAN_TOLERANCE, ore + " averages " + mean + " shards with Fortune III");
		}

		helper.succeed();
	}

	@GameTest
	public void silkTouchStillDropsTheOre(GameTestHelper helper) {
		for (Block ore : DIAMOND_ORES) {
			List<ItemStack> drops = oreDrops(helper, ore, enchantAtMaxLevel(helper, new ItemStack(Items.DIAMOND_PICKAXE), Enchantments.SILK_TOUCH), RandomSource.create(0L));

			helper.assertValueEqual(drops.size(), 1, ore + " stacks with Silk Touch");
			helper.assertTrue(ItemStack.matches(drops.getFirst(), new ItemStack(ore)), ore + " drops itself with Silk Touch");
		}

		helper.succeed();
	}

	@GameTest
	public void anOrichalcumPickaxeAddsHalfAShardToFortune(GameTestHelper helper) {
		for (Block ore : DIAMOND_ORES) {
			ItemStack orichalcum = enchantAtMaxLevel(helper, new ItemStack(AlloyGear.of(Alloy.ORICHALCUM, GearKind.PICKAXE)), Enchantments.FORTUNE);
			ItemStack netherite = enchantAtMaxLevel(helper, new ItemStack(Items.NETHERITE_PICKAXE), Enchantments.FORTUNE);
			List<Integer> withBonus = shardCounts(helper, ore, orichalcum);
			List<Integer> without = shardCounts(helper, ore, netherite);
			int extra = 0;

			// The same seed gives both pickaxes the same Fortune roll, so each difference is the bonus alone.
			for (int roll = 0; roll < ORE_ROLLS; roll++) {
				int difference = withBonus.get(roll) - without.get(roll);

				helper.assertTrue(difference == 0 || difference == 1, ore + " bonus of " + difference + " on roll " + roll);
				extra += difference;
			}

			double mean = (double) extra / ORE_ROLLS;

			helper.assertTrue(Math.abs(mean - 0.5) <= MEAN_TOLERANCE, ore + " Orichalcum adds " + mean + " shards on average");
		}

		helper.succeed();
	}

	@GameTest
	public void smeltingOrBlastingADiamondOreGivesAShard(GameTestHelper helper) {
		for (Block ore : DIAMOND_ORES) {
			SingleRecipeInput input = new SingleRecipeInput(new ItemStack(ore));

			assertCooks(helper, RecipeType.SMELTING, input, "smelting " + ore);
			assertCooks(helper, RecipeType.BLASTING, input, "blasting " + ore);
		}

		helper.succeed();
	}

	@GameTest
	public void fourShardsCraftADiamond(GameTestHelper helper) {
		ItemStack shard = new ItemStack(MapleItems.DIAMOND_SHARD);

		assertCrafts(helper, CraftingInput.of(2, 2, List.of(shard, shard, shard, shard)), new ItemStack(Items.DIAMOND));
		assertCrafts(helper, CraftingInput.of(3, 3, List.of(shard, ItemStack.EMPTY, shard, ItemStack.EMPTY, shard, ItemStack.EMPTY, shard, ItemStack.EMPTY, ItemStack.EMPTY)), new ItemStack(Items.DIAMOND));
		helper.assertTrue(craft(helper, CraftingInput.of(3, 1, List.of(shard, shard, shard))).isEmpty(), "three shards craft nothing");
		helper.succeed();
	}

	@GameTest
	public void theShardRecipeUnlocksWithTheFirstShard(GameTestHelper helper) {
		ServerPlayer player = MockPlayers.survivalPlayer(helper);
		ResourceKey<Recipe<?>> recipe = ResourceKey.create(Registries.RECIPE, Identifier.parse("maple_flavored:diamond_from_diamond_shards"));

		helper.assertFalse(player.getRecipeBook().contains(recipe), "the shard recipe is locked at first");

		pickUp(player, new ItemStack(MapleItems.DIAMOND_SHARD));

		helper.assertTrue(player.getRecipeBook().contains(recipe), "picking up a shard unlocks the recipe");
		helper.succeed();
	}

	@GameTest
	public void theDiamondMilestoneWaitsForAWholeDiamond(GameTestHelper helper) {
		ServerPlayer player = MockPlayers.survivalPlayer(helper);

		pickUp(player, new ItemStack(MapleItems.DIAMOND_SHARD, 4));
		helper.assertFalse(isDone(helper, player, MINE_DIAMOND), "Diamonds! with only shards");

		pickUp(player, new ItemStack(Items.DIAMOND));
		helper.assertTrue(isDone(helper, player, MINE_DIAMOND), "Diamonds! with a diamond");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void safeLootGivesShardsAndNoDiamonds(GameTestHelper helper) {
		for (String table : SHARD_TABLES) {
			List<ItemStack> drops = rollMany(helper, table);

			helper.assertTrue(count(drops, Items.DIAMOND) == 0, table + " gave a diamond");
			helper.assertTrue(count(drops, MapleItems.DIAMOND_SHARD) > 0, table + " gave no shards in " + TABLE_ROLLS + " rolls");
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void dangerousLootKeepsWholeDiamonds(GameTestHelper helper) {
		for (String table : WHOLE_DIAMOND_TABLES) {
			List<ItemStack> drops = rollMany(helper, table);

			helper.assertTrue(count(drops, MapleItems.DIAMOND_SHARD) == 0, table + " gave a shard");
			helper.assertTrue(count(drops, Items.DIAMOND) > 0, table + " gave no diamonds in " + TABLE_ROLLS + " rolls");
		}

		helper.succeed();
	}

	@GameTest
	public void aChestBookInASafeTableGivesOneShard(GameTestHelper helper) {
		// The desert pyramid's first pool rolls 2 to 4 times. Its book entries, weight 20 of 247, now give 1 shard each.
		// Its diamond entry, weight 5, gives 1 to 3 shards, a single one a third of the time. Without the books, single
		// shards would come only from that entry, at about a thirteenth of this rate.
		double expectedSingles = TABLE_ROLLS * 3.0 * (20.0 + 5.0 / 3.0) / 247.0;
		List<ItemStack> drops = rollMany(helper, "chests/desert_pyramid");
		long singles = drops.stream().filter(drop -> drop.is(MapleItems.DIAMOND_SHARD) && drop.getCount() == 1).count();

		helper.assertTrue(Math.abs(singles - expectedSingles) <= 0.2 * expectedSingles, singles + " single shards, expected about " + Math.round(expectedSingles));
		helper.assertTrue(drops.stream().noneMatch(drop -> drop.is(Items.ENCHANTED_BOOK)), "the pyramid gave an enchanted book");
		helper.succeed();
	}

	private static List<Integer> shardCounts(GameTestHelper helper, Block ore, ItemStack tool) {
		List<Integer> counts = new ArrayList<>();

		for (int roll = 0; roll < ORE_ROLLS; roll++) {
			List<ItemStack> drops = oreDrops(helper, ore, tool, RandomSource.create(roll));

			helper.assertTrue(count(drops, Items.DIAMOND) == 0, ore + " dropped a diamond");
			counts.add(count(drops, MapleItems.DIAMOND_SHARD));
		}

		return counts;
	}

	// Rolls the ore's own block table as if the tool broke it, from the given random.
	private static List<ItemStack> oreDrops(GameTestHelper helper, Block ore, ItemStack tool, RandomSource random) {
		LootParams params = new LootParams.Builder(helper.getLevel())
			.withParameter(LootContextParams.BLOCK_STATE, ore.defaultBlockState())
			.withParameter(LootContextParams.ORIGIN, helper.absoluteVec(Vec3.ZERO))
			.withParameter(LootContextParams.TOOL, tool)
			.create(LootContextParamSets.BLOCK);
		LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(ore.getLootTable().orElseThrow());

		return table.getRandomItems(params, random);
	}

	private static ItemStack enchantAtMaxLevel(GameTestHelper helper, ItemStack stack, ResourceKey<Enchantment> enchantment) {
		var holder = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment);

		stack.enchant(holder, holder.value().getMaxLevel());

		return stack;
	}

	private static List<ItemStack> rollMany(GameTestHelper helper, String path) {
		ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace(path));
		RandomSource random = RandomSource.create(path.hashCode());
		List<ItemStack> drops = new ArrayList<>();

		for (int roll = 0; roll < TABLE_ROLLS; roll++) {
			drops.addAll(LootRolls.roll(helper, key, random));
		}

		return drops;
	}

	private static void assertCooks(GameTestHelper helper, RecipeType<? extends AbstractCookingRecipe> type, SingleRecipeInput input, String what) {
		ItemStack result = helper.getLevel().getServer().getRecipeManager().getRecipeFor(type, input, helper.getLevel())
			.map(recipe -> recipe.value().assemble(input))
			.orElse(ItemStack.EMPTY);

		helper.assertTrue(ItemStack.matches(result, new ItemStack(MapleItems.DIAMOND_SHARD)), what + " gives " + result);
	}

	private static Optional<ItemStack> craft(GameTestHelper helper, CraftingInput input) {
		var recipes = helper.getLevel().getServer().getRecipeManager();

		return recipes.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).map(recipe -> recipe.value().assemble(input));
	}

	private static void assertCrafts(GameTestHelper helper, CraftingInput input, ItemStack expected) {
		ItemStack result = craft(helper, input).orElse(ItemStack.EMPTY);

		helper.assertTrue(ItemStack.matches(result, expected), "crafts " + expected + ", got " + result);
	}

	// A mock player's inventory isn't watched by a ticking menu, so the inventory trigger is fired here as pickup would.
	private static void pickUp(ServerPlayer player, ItemStack stack) {
		ItemStack added = stack.copy();

		player.getInventory().add(stack);
		CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), added);
	}

	private static boolean isDone(GameTestHelper helper, ServerPlayer player, String advancement) {
		AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(Identifier.parse(advancement));

		helper.assertTrue(holder != null, advancement + " exists");

		return player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	private static int count(List<ItemStack> stacks, Item item) {
		return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
	}
}
