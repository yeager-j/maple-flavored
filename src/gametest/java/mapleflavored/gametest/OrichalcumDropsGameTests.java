package mapleflavored.gametest;

import java.util.List;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.Alloys;
import mapleflavored.alloys.GearKind;
import mapleflavored.items.MapleItems;
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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Orichalcum tools add half a drop again to drops Fortune and Looting affect, at any enchantment level: half the count
 * a count function started with, or half a chance's unenchanted chance. Ancient Embers, Pith and enchanted books never
 * get it, and no loot table guards one of them with a chance the bonus would raise.
 *
 * <p>The fixture tables in {@code maple_flavored_gametest:loot_table} start each counted drop at 10, so the bonus is a
 * whole 5 and the counts are exact.
 */
public class OrichalcumDropsGameTests {
	private static final String FIXTURE_NAMESPACE = "maple_flavored_gametest";
	private static final int ROLLS = 200;
	private static final int RATE_ROLLS = 2000;
	private static final long SEED = 7L;
	private static final double DROP_RATE_TOLERANCE = 0.03;
	private static final BlockPos VICTIM_POS = new BlockPos(1, 1, 1);

	@GameTest
	public void anOrichalcumPickaxeAddsHalfToFortuneDrops(GameTestHelper helper) {
		helper.assertValueEqual(blockDrop(helper, "orichalcum_fortune_stone", tool(Alloy.ORICHALCUM, GearKind.PICKAXE)), 15, "stone from an Orichalcum pickaxe");
		helper.assertValueEqual(blockDrop(helper, "orichalcum_fortune_stone", new ItemStack(Items.NETHERITE_PICKAXE)), 10, "stone from a Netherite pickaxe");
		helper.succeed();
	}

	@GameTest
	public void heldOrichalcumArmorGivesNoBonus(GameTestHelper helper) {
		helper.assertValueEqual(blockDrop(helper, "orichalcum_fortune_stone", tool(Alloy.ORICHALCUM, GearKind.BOOTS)), 10, "stone from held Orichalcum boots");
		helper.succeed();
	}

	@GameTest
	public void theBonusAddsHalfTheCountBeforeFortuneOnTopOfFortune(GameTestHelper helper) {
		// Vanilla's roll comes first from the same seed, so Fortune III gives both pickaxes the same count before the bonus.
		int orichalcum = blockDrop(helper, "orichalcum_fortune_stone", enchanted(helper, tool(Alloy.ORICHALCUM, GearKind.PICKAXE), Enchantments.FORTUNE));
		int netherite = blockDrop(helper, "orichalcum_fortune_stone", enchanted(helper, new ItemStack(Items.NETHERITE_PICKAXE), Enchantments.FORTUNE));

		helper.assertValueEqual(orichalcum - netherite, 5, "the stone Orichalcum adds to Fortune III's " + netherite);
		helper.succeed();
	}

	@GameTest
	public void fortuneDropsOfEmbersPithAndEnchantedBooksGetNoBonus(GameTestHelper helper) {
		List<ItemStack> drops = blockDrops(helper, "orichalcum_fortune_excluded", tool(Alloy.ORICHALCUM, GearKind.PICKAXE));

		helper.assertValueEqual(count(drops, Alloys.ANCIENT_EMBER), 10, "Embers from an Orichalcum pickaxe");
		helper.assertValueEqual(count(drops, MapleItems.PITH), 10, "Pith from an Orichalcum pickaxe");
		helper.assertValueEqual(count(drops, Items.ENCHANTED_BOOK), 10, "enchanted books from an Orichalcum pickaxe");
		helper.succeed();
	}

	@GameTest
	public void anOrichalcumWeaponAddsHalfToLootingDropsWithinTheirLimit(GameTestHelper helper) {
		Mob victim = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, VICTIM_POS);
		ServerPlayer orichalcum = holding(helper, tool(Alloy.ORICHALCUM, GearKind.SWORD));
		ServerPlayer netherite = holding(helper, new ItemStack(Items.NETHERITE_SWORD));

		helper.assertValueEqual(count(roll(helper, "orichalcum_looting_stone", victim, orichalcum), Items.STONE), 15, "stone killed with Orichalcum");
		helper.assertValueEqual(count(roll(helper, "orichalcum_looting_stone", victim, netherite), Items.STONE), 10, "stone killed with Netherite");
		helper.assertValueEqual(count(roll(helper, "orichalcum_looting_stone_limited", victim, orichalcum), Items.STONE), 12, "stone capped at 12");
		helper.succeed();
	}

	@GameTest
	public void aLootingLimitStillOnlyCapsVanillasOwnBonusWithoutOrichalcum(GameTestHelper helper) {
		Mob victim = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, VICTIM_POS);
		ServerPlayer netherite = holding(helper, new ItemStack(Items.NETHERITE_SWORD));

		// Vanilla skips its limit of 5 without Looting.
		helper.assertValueEqual(count(roll(helper, "orichalcum_looting_stone_low_limit", victim, netherite), Items.STONE), 10, "stone killed with Netherite");
		helper.succeed();
	}

	@GameTest
	public void theBonusAddsHalfTheCountBeforeLootingOnTopOfLooting(GameTestHelper helper) {
		Mob victim = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, VICTIM_POS);
		ServerPlayer orichalcum = holding(helper, enchanted(helper, tool(Alloy.ORICHALCUM, GearKind.SWORD), Enchantments.LOOTING));
		ServerPlayer netherite = holding(helper, enchanted(helper, new ItemStack(Items.NETHERITE_SWORD), Enchantments.LOOTING));

		// The fixture adds 1 per Looting level to 10.
		helper.assertValueEqual(count(roll(helper, "orichalcum_looting_stone", victim, netherite), Items.STONE), 13, "stone killed with Netherite and Looting III");
		helper.assertValueEqual(count(roll(helper, "orichalcum_looting_stone", victim, orichalcum), Items.STONE), 18, "stone killed with Orichalcum and Looting III");
		helper.succeed();
	}

	@GameTest
	public void anOrichalcumWeaponCanLiftALootingChanceToCertain(GameTestHelper helper) {
		Mob victim = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, VICTIM_POS);
		LootTable table = fixture(helper, "orichalcum_looting_chance_stone");

		// 70% plus half of 70% is over 100%, so every roll drops; 70% alone misses some of 200.
		helper.assertValueEqual(dropRolls(helper, table, victim, holding(helper, tool(Alloy.ORICHALCUM, GearKind.SWORD)), Items.STONE), ROLLS, "rolls that drop with Orichalcum");
		helper.assertTrue(dropRolls(helper, table, victim, holding(helper, new ItemStack(Items.NETHERITE_SWORD)), Items.STONE) < ROLLS, "every roll drops with Netherite");
		helper.succeed();
	}

	@GameTest
	public void theChanceBonusIsHalfTheUnenchantedChanceWithOrWithoutLooting(GameTestHelper helper) {
		Mob victim = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, VICTIM_POS);
		LootTable table = fixture(helper, "orichalcum_looting_chance_low");
		ServerPlayer netherite = holding(helper, new ItemStack(Items.NETHERITE_SWORD));
		ServerPlayer orichalcum = holding(helper, tool(Alloy.ORICHALCUM, GearKind.SWORD));
		ServerPlayer orichalcumLooting = holding(helper, enchanted(helper, tool(Alloy.ORICHALCUM, GearKind.SWORD), Enchantments.LOOTING));

		// 20% unenchanted, 40% with Looting; Orichalcum adds half of 20% to each.
		assertNear(helper, dropRate(helper, table, victim, netherite, Items.STONE), 0.2, "the drop rate with Netherite");
		assertNear(helper, dropRate(helper, table, victim, orichalcum, Items.STONE), 0.3, "the drop rate with Orichalcum");
		assertNear(helper, dropRate(helper, table, victim, orichalcumLooting, Items.STONE), 0.5, "the drop rate with Orichalcum and Looting III");
		helper.succeed();
	}

	@GameTest
	public void anEmberPoolsChanceGetsNoBonusAndTheNextPoolDoes(GameTestHelper helper) {
		Mob victim = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, VICTIM_POS);
		LootTable table = fixture(helper, "orichalcum_looting_chance_after_ember");
		ServerPlayer orichalcum = holding(helper, tool(Alloy.ORICHALCUM, GearKind.SWORD));

		helper.assertTrue(dropRolls(helper, table, victim, orichalcum, Alloys.ANCIENT_EMBER) < ROLLS, "every roll drops an Ember with Orichalcum");
		helper.assertValueEqual(dropRolls(helper, table, victim, orichalcum, Items.STONE), ROLLS, "rolls that drop stone after the Ember pool");
		helper.succeed();
	}

	@GameTest
	public void anAncientEmberEntrysChanceGetsNoBonus(GameTestHelper helper) {
		Mob victim = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, VICTIM_POS);
		LootTable table = fixture(helper, "orichalcum_looting_chance_ember_entry");
		int drops = dropRolls(helper, table, victim, holding(helper, tool(Alloy.ORICHALCUM, GearKind.SWORD)), Alloys.ANCIENT_EMBER);

		helper.assertTrue(drops < ROLLS, "every roll drops an Ember with Orichalcum");
		helper.succeed();
	}

	@GameTest
	public void piglinBrutesDropEmbersNoMoreOftenToOrichalcum(GameTestHelper helper) {
		Mob brute = helper.spawnWithNoFreeWill(EntityTypes.PIGLIN_BRUTE, VICTIM_POS);
		LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(brute.getLootTable().orElseThrow());
		ServerPlayer player = holding(helper, tool(Alloy.ORICHALCUM, GearKind.SWORD));

		assertNear(helper, dropRate(helper, table, brute, player, Alloys.ANCIENT_EMBER), 0.25, "the Ember drop rate with Orichalcum");
		helper.succeed();
	}

	@GameTest
	public void theScanFindsAnExcludedDropSharingAnEnchantedChance(GameTestHelper helper) {
		var registries = helper.getLevel().getServer().reloadableRegistries().lookup();

		helper.assertValueEqual(ExcludedDropScan.leaks(registries, fixtureKey("orichalcum_looting_chance_mixed_pool")).size(), 1, "leaks in a mixed pool");
		helper.assertValueEqual(ExcludedDropScan.leaks(registries, fixtureKey("orichalcum_looting_chance_ember_entry")).size(), 0, "leaks on an Ember's own entry");
		helper.succeed();
	}

	// The test fixtures are skipped: some hold the forbidden shape on purpose.
	@GameTest
	public void noLootTableSharesAnEnchantedChanceWithAnExcludedDrop(GameTestHelper helper) {
		var registries = helper.getLevel().getServer().reloadableRegistries().lookup();
		List<String> leaks = registries.lookupOrThrow(Registries.LOOT_TABLE).listElementIds()
			.filter(key -> !key.identifier().getNamespace().equals(FIXTURE_NAMESPACE))
			.flatMap(key -> ExcludedDropScan.leaks(registries, key).stream())
			.toList();

		helper.assertValueEqual(leaks, List.of(), "excluded drops that would get Orichalcum's chance bonus");
		helper.succeed();
	}

	private static ItemStack tool(Alloy alloy, GearKind kind) {
		return new ItemStack(AlloyGear.of(alloy, kind));
	}

	// The stack with the enchantment at level III.
	private static ItemStack enchanted(GameTestHelper helper, ItemStack stack, ResourceKey<Enchantment> enchantment) {
		stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment), 3);

		return stack;
	}

	private static ServerPlayer holding(GameTestHelper helper, ItemStack item) {
		ServerPlayer player = MockPlayers.survivalPlayer(helper);

		player.setItemInHand(InteractionHand.MAIN_HAND, item);

		return player;
	}

	private static int blockDrop(GameTestHelper helper, String name, ItemStack tool) {
		return blockDrops(helper, name, tool).stream().mapToInt(ItemStack::getCount).sum();
	}

	// Rolls a fixture block table as if the tool broke stone, once, from a seeded random.
	private static List<ItemStack> blockDrops(GameTestHelper helper, String name, ItemStack tool) {
		LootParams params = new LootParams.Builder(helper.getLevel())
			.withParameter(LootContextParams.BLOCK_STATE, Blocks.STONE.defaultBlockState())
			.withParameter(LootContextParams.ORIGIN, helper.absoluteVec(Vec3.ZERO))
			.withParameter(LootContextParams.TOOL, tool)
			.create(LootContextParamSets.BLOCK);

		return fixture(helper, name).getRandomItems(params, RandomSource.create(SEED));
	}

	private static List<ItemStack> roll(GameTestHelper helper, String name, Mob victim, ServerPlayer killer) {
		return fixture(helper, name).getRandomItems(entityParams(helper, victim, killer), RandomSource.create(SEED));
	}

	private static int dropRolls(GameTestHelper helper, LootTable table, Mob victim, ServerPlayer killer, Item item) {
		return droppingRolls(helper, table, victim, killer, item, ROLLS);
	}

	private static double dropRate(GameTestHelper helper, LootTable table, Mob victim, ServerPlayer killer, Item item) {
		return (double) droppingRolls(helper, table, victim, killer, item, RATE_ROLLS) / RATE_ROLLS;
	}

	// How many of the rolls drop the item, from one seeded random.
	private static int droppingRolls(GameTestHelper helper, LootTable table, Mob victim, ServerPlayer killer, Item item, int rolls) {
		LootParams params = entityParams(helper, victim, killer);
		RandomSource random = RandomSource.create(SEED);
		int drops = 0;

		for (int roll = 0; roll < rolls; roll++) {
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
		return helper.getLevel().getServer().reloadableRegistries().getLootTable(fixtureKey(name));
	}

	private static ResourceKey<LootTable> fixtureKey(String name) {
		return ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(FIXTURE_NAMESPACE, name));
	}

	private static void assertNear(GameTestHelper helper, double rate, double expected, String what) {
		helper.assertTrue(Math.abs(rate - expected) <= DROP_RATE_TOLERANCE, what + " " + rate + " is within " + DROP_RATE_TOLERANCE + " of " + expected);
	}

	private static int count(List<ItemStack> stacks, Item item) {
		return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
	}
}
