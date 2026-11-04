package mapleflavored.gametest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Early tiers: stone tools are gone, a wooden pickaxe mines copper ore, copper takes stone's place for everything
 * else, and loot, Wither Skeletons and Getting an Upgrade hand out or ask for copper instead of stone.
 */
public class CopperToolsGameTests {
	private static final Set<Item> STONE_TOOLS = Set.of(
		Items.STONE_SWORD, Items.STONE_SHOVEL, Items.STONE_PICKAXE, Items.STONE_AXE, Items.STONE_HOE, Items.STONE_SPEAR
	);
	private static final List<BlockPos> MINING_SPOTS = List.of(
		new BlockPos(1, 2, 1), new BlockPos(3, 2, 1), new BlockPos(5, 2, 1),
		new BlockPos(1, 2, 4), new BlockPos(3, 2, 4), new BlockPos(5, 2, 4)
	);
	private static final int LOOT_ROLLS = 200;

	/** A tool breaking a block, and the item that should drop, or {@code null} for no drop. */
	private record MiningCase(Item tool, Block block, Item drop) {
	}

	@GameTest
	public void woodAndGoldMineCopperOreButNothingElseAtStoneTier(GameTestHelper helper) {
		assertMiningDrops(helper, List.of(
			new MiningCase(Items.WOODEN_PICKAXE, Blocks.COPPER_ORE, Items.RAW_COPPER),
			new MiningCase(Items.WOODEN_PICKAXE, Blocks.DEEPSLATE_COPPER_ORE, Items.RAW_COPPER),
			new MiningCase(Items.WOODEN_PICKAXE, Blocks.RAW_COPPER_BLOCK, Items.RAW_COPPER_BLOCK),
			new MiningCase(Items.WOODEN_PICKAXE, Blocks.IRON_ORE, null),
			new MiningCase(Items.WOODEN_PICKAXE, Blocks.LAPIS_ORE, null),
			new MiningCase(Items.GOLDEN_PICKAXE, Blocks.COPPER_BLOCK.weathering().unaffected(), null)
		));
	}

	@GameTest
	public void copperMinesStoneTierButNotIronTier(GameTestHelper helper) {
		assertMiningDrops(helper, List.of(
			new MiningCase(Items.WOODEN_PICKAXE, Blocks.COPPER_BLOCK.weathering().unaffected(), null),
			new MiningCase(Items.GOLDEN_PICKAXE, Blocks.IRON_ORE, null),
			new MiningCase(Items.COPPER_PICKAXE, Blocks.IRON_ORE, Items.RAW_IRON),
			new MiningCase(Items.COPPER_PICKAXE, Blocks.LAPIS_ORE, Items.LAPIS_LAZULI),
			new MiningCase(Items.COPPER_PICKAXE, Blocks.COPPER_BLOCK.weathering().unaffected(), Items.COPPER_BLOCK.weathering().unaffected()),
			new MiningCase(Items.COPPER_PICKAXE, Blocks.DIAMOND_ORE, null)
		));
	}

	@GameTest
	public void stoneToolsCannotBeCrafted(GameTestHelper helper) {
		ItemStack cobblestone = new ItemStack(Items.COBBLESTONE);
		ItemStack copper = new ItemStack(Items.COPPER_INGOT);
		ItemStack stick = new ItemStack(Items.STICK);
		ItemStack none = ItemStack.EMPTY;

		Map<String, List<ItemStack>> patterns = Map.of(
			"pickaxe", List.of(cobblestone, cobblestone, cobblestone, none, stick, none, none, stick, none),
			"axe", List.of(cobblestone, cobblestone, none, cobblestone, stick, none, none, stick, none),
			"shovel", List.of(none, cobblestone, none, none, stick, none, none, stick, none),
			"hoe", List.of(cobblestone, cobblestone, none, none, stick, none, none, stick, none),
			"sword", List.of(none, cobblestone, none, none, cobblestone, none, none, stick, none),
			"spear", List.of(none, none, cobblestone, none, stick, none, stick, none, none)
		);

		patterns.forEach((tool, grid) -> helper.assertValueEqual(craft(helper, grid), Items.AIR, "stone " + tool + " crafts"));

		List<ItemStack> copperPickaxe = List.of(copper, copper, copper, none, stick, none, none, stick, none);

		helper.assertValueEqual(craft(helper, copperPickaxe), Items.COPPER_PICKAXE, "copper pickaxe pattern crafts");

		for (String recipeAdvancement : List.of("tools/stone_axe", "tools/stone_hoe", "tools/stone_pickaxe", "tools/stone_shovel", "combat/stone_sword", "combat/stone_spear")) {
			String id = "minecraft:recipes/" + recipeAdvancement;

			helper.assertTrue(advancement(helper, id) == null, id + " is removed");
		}

		helper.succeed();
	}

	@GameTest
	public void chestLootAndGiftsGiveCopperToolsInsteadOfStone(GameTestHelper helper) {
		assertLootSwapsStone(helper, BuiltInLootTables.SPAWN_BONUS_CHEST, Set.of(Items.COPPER_AXE, Items.COPPER_PICKAXE));
		assertLootSwapsStone(helper, BuiltInLootTables.IGLOO_CHEST, Set.of(Items.COPPER_AXE));
		assertLootSwapsStone(helper, BuiltInLootTables.TRIAL_CHAMBERS_CORRIDOR, Set.of(Items.COPPER_AXE, Items.COPPER_PICKAXE));
		assertLootSwapsStone(helper, BuiltInLootTables.TRIAL_CHAMBERS_SUPPLY, Set.of(Items.COPPER_PICKAXE));
		assertLootSwapsStone(helper, BuiltInLootTables.UNDERWATER_RUIN_BIG, Set.of(Items.COPPER_SPEAR));
		assertLootSwapsStone(helper, BuiltInLootTables.UNDERWATER_RUIN_SMALL, Set.of(Items.COPPER_AXE, Items.COPPER_SPEAR));
		assertLootSwapsStone(helper, BuiltInLootTables.TOOLSMITH_GIFT, Set.of(Items.COPPER_PICKAXE, Items.COPPER_AXE, Items.COPPER_HOE, Items.COPPER_SHOVEL));
		assertLootSwapsStone(helper, BuiltInLootTables.WEAPONSMITH_GIFT, Set.of(Items.COPPER_AXE));

		helper.succeed();
	}

	@GameTest
	public void aSwappedToolKeepsItsDamageAndEnchantments(GameTestHelper helper) {
		ResourceKey<LootTable> fixture = ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse("maple_flavored_gametest:stone_pickaxe_with_data"));
		List<ItemStack> drops = roll(helper, lootTable(helper, fixture), RandomSource.create(0L));

		helper.assertValueEqual(drops.size(), 2, "drop count");

		ItemStack pickaxe = drops.get(0);
		ItemStack sticks = drops.get(1);
		var unbreaking = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);

		helper.assertValueEqual(pickaxe.getItem(), Items.COPPER_PICKAXE, "swapped tool");
		helper.assertValueEqual(pickaxe.getCount(), 1, "swapped tool count");
		// The table leaves 75% of a stone pickaxe's 131 durability: floor(0.25 * 131) = 32 damage.
		helper.assertValueEqual(pickaxe.getDamageValue(), 32, "swapped tool damage");
		helper.assertValueEqual(pickaxe.getEnchantments().getLevel(unbreaking), 2, "swapped tool Unbreaking level");
		helper.assertValueEqual(sticks.getItem(), Items.STICK, "unrelated drop");
		helper.assertValueEqual(sticks.getCount(), 3, "unrelated drop count");
		helper.succeed();
	}

	@GameTest
	public void aBonusChestFillsWithCopperTools(GameTestHelper helper) {
		BlockPos chestPos = new BlockPos(1, 2, 1);
		List<Item> found = new ArrayList<>();

		helper.setBlock(chestPos, Blocks.CHEST);

		ChestBlockEntity chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);

		// Every Bonus Chest holds one roll of an axe and one of a pickaxe, each wood or stone in vanilla.
		chest.setLootTable(BuiltInLootTables.SPAWN_BONUS_CHEST, 1L);
		chest.unpackLootTable(null);

		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			found.add(chest.getItem(slot).getItem());
		}

		helper.assertTrue(found.stream().noneMatch(STONE_TOOLS::contains), "the chest holds no stone tool: " + found);
		helper.assertTrue(found.contains(Items.COPPER_AXE) || found.contains(Items.COPPER_PICKAXE), "the chest holds a copper tool: " + found);
		helper.succeed();
	}

	@GameTest
	public void witherSkeletonsSpawnWithACopperSword(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos spawnPos = helper.absolutePos(new BlockPos(2, 2, 2));
		WitherSkeleton skeleton = EntityTypes.WITHER_SKELETON.create(level, EntitySpawnReason.NATURAL);

		skeleton.snapTo(Vec3.atBottomCenterOf(spawnPos));
		skeleton.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.NATURAL, null);

		helper.assertValueEqual(skeleton.getItemBySlot(EquipmentSlot.MAINHAND).getItem(), Items.COPPER_SWORD, "held weapon");
		helper.succeed();
	}

	@GameTest
	public void gettingAnUpgradeAndTheTorchRecipeAskForACopperPickaxe(GameTestHelper helper) {
		ServerPlayer stonePlayer = helper.makeMockServerPlayerInLevel();
		ServerPlayer copperPlayer = helper.makeMockServerPlayerInLevel();
		ResourceKey<Recipe<?>> torch = ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("torch"));

		give(stonePlayer, Items.STONE_PICKAXE);
		give(copperPlayer, Items.COPPER_PICKAXE);

		helper.assertFalse(isDone(helper, stonePlayer, "minecraft:story/upgrade_tools"), "a stone pickaxe grants Getting an Upgrade");
		helper.assertFalse(stonePlayer.getRecipeBook().contains(torch), "a stone pickaxe unlocks the torch recipe");
		helper.assertTrue(isDone(helper, copperPlayer, "minecraft:story/upgrade_tools"), "a copper pickaxe grants Getting an Upgrade");
		helper.assertTrue(copperPlayer.getRecipeBook().contains(torch), "a copper pickaxe unlocks the torch recipe");
		helper.succeed();
	}

	@GameTest
	public void cobblestoneStillGrantsStoneAge(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		give(player, Items.COBBLESTONE);

		helper.assertTrue(isDone(helper, player, "minecraft:story/mine_stone"), "cobblestone grants Stone Age");
		helper.succeed();
	}

	private static void assertMiningDrops(GameTestHelper helper, List<MiningCase> cases) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		player.setGameMode(GameType.SURVIVAL);

		for (int i = 0; i < cases.size(); i++) {
			MiningCase miningCase = cases.get(i);
			BlockPos pos = MINING_SPOTS.get(i);
			String label = miningCase.tool() + " on " + miningCase.block();

			helper.setBlock(pos, miningCase.block());
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(miningCase.tool()));
			player.gameMode.destroyBlock(helper.absolutePos(pos));

			helper.assertBlockPresent(Blocks.AIR, pos);

			List<Item> drops = itemsDroppedAt(helper, pos);
			List<Item> expected = miningCase.drop() == null ? List.of() : List.of(miningCase.drop());

			helper.assertValueEqual(drops.stream().distinct().toList(), expected, label + " drops");
		}

		helper.succeed();
	}

	private static List<Item> itemsDroppedAt(GameTestHelper helper, BlockPos pos) {
		AABB around = new AABB(helper.absolutePos(pos)).inflate(0.5);

		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).stream().map(entity -> entity.getItem().getItem()).toList();
	}

	private static void assertLootSwapsStone(GameTestHelper helper, ResourceKey<LootTable> key, Set<Item> copperTools) {
		LootTable table = lootTable(helper, key);
		RandomSource random = RandomSource.create(key.identifier().hashCode());
		Set<Item> rolled = new HashSet<>();

		for (int i = 0; i < LOOT_ROLLS; i++) {
			roll(helper, table, random).forEach(drop -> rolled.add(drop.getItem()));
		}

		String name = key.identifier().toString();

		helper.assertTrue(rolled.stream().noneMatch(STONE_TOOLS::contains), name + " gives no stone tool");
		helper.assertTrue(rolled.containsAll(copperTools), name + " gives " + copperTools + ", rolled " + rolled);
	}

	private static LootTable lootTable(GameTestHelper helper, ResourceKey<LootTable> key) {
		return helper.getLevel().getServer().reloadableRegistries().getLootTable(key);
	}

	private static List<ItemStack> roll(GameTestHelper helper, LootTable table, RandomSource random) {
		ServerLevel level = helper.getLevel();
		// Gift tables need an entity to give from; chest tables accept one.
		ItemEntity holder = new ItemEntity(level, 0, 0, 0, new ItemStack(Items.STICK));
		LootParams params = new LootParams.Builder(level)
			.withParameter(LootContextParams.ORIGIN, helper.absoluteVec(Vec3.ZERO))
			.withParameter(LootContextParams.THIS_ENTITY, holder)
			.create(table.getParamSet());

		return table.getRandomItems(params, random);
	}

	private static Item craft(GameTestHelper helper, List<ItemStack> grid) {
		ServerLevel level = helper.getLevel();
		CraftingInput input = CraftingInput.of(3, 3, grid);

		return level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
			.map(recipe -> recipe.value().assemble(input).getItem())
			.orElse(Items.AIR);
	}

	private static void give(ServerPlayer player, Item item) {
		player.getInventory().add(new ItemStack(item));
		// A ticking player sends inventory changes to advancement triggers each tick; a mock player is never ticked.
		player.inventoryMenu.broadcastChanges();
	}

	private static AdvancementHolder advancement(GameTestHelper helper, String id) {
		MinecraftServer server = helper.getLevel().getServer();

		return server.getAdvancements().get(Identifier.parse(id));
	}

	private static boolean isDone(GameTestHelper helper, ServerPlayer player, String id) {
		AdvancementHolder holder = advancement(helper, id);

		helper.assertTrue(holder != null, id + " exists");

		return player.getAdvancements().getOrStartProgress(holder).isDone();
	}
}
