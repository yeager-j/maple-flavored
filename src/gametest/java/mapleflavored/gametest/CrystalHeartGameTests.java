package mapleflavored.gametest;

import static mapleflavored.gametest.MockPlayers.assertMaxHearts;
import static mapleflavored.gametest.MockPlayers.playerWithMaxHearts;
import static mapleflavored.gametest.MockPlayers.runCommand;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import mapleflavored.items.MapleItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Consuming and being saved by Crystal Hearts, and the recipes and drops for Crystal Hearts, Pith and Netherite. */
public class CrystalHeartGameTests {
	private static final int CONSUME_TICKS = 32;
	private static final int LOOT_ROLLS = 2000;
	private static final long LOOT_SEED = 7L;
	private static final double DROP_RATE_TOLERANCE = 0.03;
	private static final String POSTMORTAL = "minecraft:adventure/totem_of_undying";
	private static final String BREAK_CLEARED_SPAWNER = "maple_flavored:adventure/break_cleared_spawner";
	private static final String CONSUME_CRYSTAL_HEART = "maple_flavored:adventure/consume_crystal_heart";
	private static final String SAVED_BY_CRYSTAL_HEART = "maple_flavored:adventure/saved_by_crystal_heart";
	private static final String MAX_HEARTS_CAP = "maple_flavored:adventure/max_hearts_cap";

	@GameTest
	public void consumingACrystalHeartAddsOneMaxHeart(GameTestHelper helper) {
		ServerPlayer player = survivalPlayerWithMaxHearts(helper, 12);

		hold(player, InteractionHand.MAIN_HAND, MapleItems.CRYSTAL_HEART);

		InteractionResult result = startUsing(player, InteractionHand.MAIN_HAND);

		helper.assertTrue(result.consumesAction(), "using a Crystal Heart starts");
		helper.assertValueEqual(player.getUseItemRemainingTicks(), CONSUME_TICKS, "ticks to consume");

		tickUse(player, CONSUME_TICKS - 1);
		assertMaxHearts(helper, player, 12);
		helper.assertFalse(isDone(helper, player, CONSUME_CRYSTAL_HEART), "Consume a Crystal Heart before it is consumed");

		tickUse(player, 1);
		assertMaxHearts(helper, player, 13);
		helper.assertTrue(isDone(helper, player, CONSUME_CRYSTAL_HEART), "consuming a Crystal Heart grants its advancement");
		helper.assertTrue(player.getMainHandItem().isEmpty(), "the Crystal Heart is used up");
		assertEffect(helper, player, MobEffects.REGENERATION, 10, 60);
		assertEffect(helper, player, MobEffects.RESISTANCE, 4, 20);
		helper.succeed();
	}

	@GameTest
	public void aCrystalHeartIsRefusedAtThirtyMaxHearts(GameTestHelper helper) {
		ServerPlayer player = survivalPlayerWithMaxHearts(helper, 30);

		hold(player, InteractionHand.MAIN_HAND, MapleItems.CRYSTAL_HEART);

		InteractionResult result = startUsing(player, InteractionHand.MAIN_HAND);

		helper.assertFalse(result.consumesAction(), "using a Crystal Heart is refused");
		helper.assertFalse(player.isUsingItem(), "the player is not using it");
		helper.assertTrue(player.getMainHandItem().is(MapleItems.CRYSTAL_HEART), "the Crystal Heart is kept");
		assertMaxHearts(helper, player, 30);
		helper.assertFalse(isDone(helper, player, CONSUME_CRYSTAL_HEART), "a refused Crystal Heart doesn't count as consumed");
		helper.succeed();
	}

	@GameTest
	public void reachingThirtyMaxHeartsGrantsTheChallenge(GameTestHelper helper) {
		ServerPlayer player = survivalPlayerWithMaxHearts(helper, 29);

		helper.assertFalse(isDone(helper, player, MAX_HEARTS_CAP), "Reach 30 Max Hearts at 29");

		runCommand(helper, player, "maxhearts set @s 30");

		helper.assertTrue(isDone(helper, player, MAX_HEARTS_CAP), "Reach 30 Max Hearts at 30");
		helper.succeed();
	}

	@GameTest
	public void theCrystalHeartAdvancementsHangUnderBreakAClearedSpawner(GameTestHelper helper) {
		assertAdvancement(helper, BREAK_CLEARED_SPAWNER, "minecraft:adventure/kill_a_mob", AdvancementType.TASK, 0);
		assertAdvancement(helper, CONSUME_CRYSTAL_HEART, BREAK_CLEARED_SPAWNER, AdvancementType.TASK, 0);
		assertAdvancement(helper, SAVED_BY_CRYSTAL_HEART, CONSUME_CRYSTAL_HEART, AdvancementType.TASK, 0);
		assertAdvancement(helper, MAX_HEARTS_CAP, CONSUME_CRYSTAL_HEART, AdvancementType.CHALLENGE, 100);
		helper.succeed();
	}

	@GameTest
	public void aCrystalHeartIsRefusedWhenThirtyIsReachedDuringUse(GameTestHelper helper) {
		ServerPlayer player = survivalPlayerWithMaxHearts(helper, 29);

		hold(player, InteractionHand.MAIN_HAND, MapleItems.CRYSTAL_HEART);
		startUsing(player, InteractionHand.MAIN_HAND);
		runCommand(helper, player, "maxhearts set @s 30");
		tickUse(player, CONSUME_TICKS);

		helper.assertTrue(player.getMainHandItem().is(MapleItems.CRYSTAL_HEART), "the Crystal Heart is kept");
		assertMaxHearts(helper, player, 30);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void aCrystalHeartInTheMainHandSavesFromALethalHit(GameTestHelper helper) {
		assertSavedByCrystalHeart(helper, InteractionHand.MAIN_HAND);
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void aCrystalHeartInTheOffHandSavesFromALethalHit(GameTestHelper helper) {
		assertSavedByCrystalHeart(helper, InteractionHand.OFF_HAND);
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void aTotemInTheMainHandIsUsedBeforeACrystalHeartInTheOffHand(GameTestHelper helper) {
		ServerPlayer player = survivalPlayerWithMaxHearts(helper, 12);

		hold(player, InteractionHand.MAIN_HAND, Items.TOTEM_OF_UNDYING);
		hold(player, InteractionHand.OFF_HAND, MapleItems.CRYSTAL_HEART);
		hitLethally(helper, player);

		helper.assertTrue(player.isAlive(), "the player is saved");
		helper.assertTrue(player.getMainHandItem().isEmpty(), "the Totem is used up");
		helper.assertTrue(player.getOffhandItem().is(MapleItems.CRYSTAL_HEART), "the Crystal Heart is kept");
		helper.assertTrue(isDone(helper, player, POSTMORTAL), "a Totem save grants Postmortal");
		helper.assertFalse(isDone(helper, player, SAVED_BY_CRYSTAL_HEART), "a Totem save isn't a Crystal Heart save");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void aCrystalHeartInTheMainHandIsUsedBeforeATotemInTheOffHand(GameTestHelper helper) {
		ServerPlayer player = survivalPlayerWithMaxHearts(helper, 12);

		hold(player, InteractionHand.MAIN_HAND, MapleItems.CRYSTAL_HEART);
		hold(player, InteractionHand.OFF_HAND, Items.TOTEM_OF_UNDYING);
		hitLethally(helper, player);

		helper.assertTrue(player.isAlive(), "the player is saved");
		helper.assertTrue(player.getMainHandItem().isEmpty(), "the Crystal Heart is used up");
		helper.assertTrue(player.getOffhandItem().is(Items.TOTEM_OF_UNDYING), "the Totem is kept");
		helper.assertFalse(isDone(helper, player, POSTMORTAL), "a Crystal Heart save doesn't grant Postmortal");
		helper.succeed();
	}

	@GameTest
	public void eightDiamondsAroundPithCraftACrystalHeart(GameTestHelper helper) {
		ItemStack diamond = new ItemStack(Items.DIAMOND);
		ItemStack pith = new ItemStack(MapleItems.PITH);
		List<ItemStack> grid = List.of(diamond, diamond, diamond, diamond, pith, diamond, diamond, diamond, diamond);

		assertCrafts(helper, CraftingInput.of(3, 3, grid), new ItemStack(MapleItems.CRYSTAL_HEART));
		helper.succeed();
	}

	@GameTest
	public void aNetherStarCraftsNinePithAndPithCraftsNoNetherStar(GameTestHelper helper) {
		List<ItemStack> ninePith = Collections.nCopies(9, new ItemStack(MapleItems.PITH));

		assertCrafts(helper, CraftingInput.of(1, 1, List.of(new ItemStack(Items.NETHER_STAR))), new ItemStack(MapleItems.PITH, 9));
		helper.assertTrue(craft(helper, CraftingInput.of(3, 3, ninePith)).isEmpty(), "nine Pith craft nothing");
		helper.succeed();
	}

	@GameTest
	public void aNetheriteIngotTakesFourScrapAndFourPith(GameTestHelper helper) {
		ItemStack scrap = new ItemStack(Items.NETHERITE_SCRAP);
		ItemStack pith = new ItemStack(MapleItems.PITH);
		ItemStack gold = new ItemStack(Items.GOLD_INGOT);
		List<ItemStack> withPith = List.of(scrap, scrap, scrap, scrap, pith, pith, pith, pith, ItemStack.EMPTY);
		List<ItemStack> withGold = List.of(scrap, scrap, scrap, scrap, gold, gold, gold, gold, ItemStack.EMPTY);

		assertCrafts(helper, CraftingInput.of(3, 3, withPith), new ItemStack(Items.NETHERITE_INGOT));
		helper.assertTrue(craft(helper, CraftingInput.of(3, 3, withGold)).isEmpty(), "vanilla's gold recipe is gone");
		helper.succeed();
	}

	@GameTest
	public void evokersDropPithOneTimeInFour(GameTestHelper helper) {
		Mob evoker = helper.spawnWithNoFreeWill(EntityTypes.EVOKER, new BlockPos(1, 1, 1));

		assertDropRate(helper, evoker, MapleItems.PITH, 0.25);
		assertDropRate(helper, evoker, Items.TOTEM_OF_UNDYING, 1.0);
		helper.succeed();
	}

	@GameTest
	public void piglinBrutesDropPithOneTimeInTen(GameTestHelper helper) {
		Mob brute = helper.spawnWithNoFreeWill(EntityTypes.PIGLIN_BRUTE, new BlockPos(1, 1, 1));

		assertDropRate(helper, brute, MapleItems.PITH, 0.10);
		helper.succeed();
	}

	private static void assertSavedByCrystalHeart(GameTestHelper helper, InteractionHand hand) {
		ServerPlayer player = survivalPlayerWithMaxHearts(helper, 12);

		player.giveExperiencePoints(100);
		hold(player, hand, MapleItems.CRYSTAL_HEART);
		hitLethally(helper, player);

		helper.assertTrue(player.isAlive(), "the player is saved");
		helper.assertTrue(player.getItemInHand(hand).isEmpty(), "the Crystal Heart is used up");
		helper.assertTrue(player.hasEffect(MobEffects.REGENERATION), "the save gives the Totem's Regeneration");
		helper.assertTrue(player.hasEffect(MobEffects.ABSORPTION), "the save gives the Totem's Absorption");
		helper.assertTrue(player.hasEffect(MobEffects.FIRE_RESISTANCE), "the save gives the Totem's Fire Resistance");
		assertMaxHearts(helper, player, 12);
		helper.assertValueEqual(player.totalExperience, 100, "XP after a save");
		helper.assertFalse(isDone(helper, player, POSTMORTAL), "a Crystal Heart save doesn't grant Postmortal");
		helper.assertTrue(isDone(helper, player, SAVED_BY_CRYSTAL_HEART), "a Crystal Heart save grants its advancement");
		helper.succeed();
	}

	// Read on the tick the heart is consumed, so the effect has not yet counted down.
	private static void assertEffect(GameTestHelper helper, ServerPlayer player, Holder<MobEffect> effect, int amplifier, int duration) {
		MobEffectInstance instance = player.getEffect(effect);

		helper.assertTrue(instance != null, effect.getRegisteredName() + " is given");
		helper.assertValueEqual(instance.getAmplifier(), amplifier, effect.getRegisteredName() + " amplifier");
		helper.assertValueEqual(instance.getDuration(), duration, effect.getRegisteredName() + " duration");
	}

	// The GameTest mock player reports Creative, so items aren't used up and damage is ignored until it is switched.
	private static ServerPlayer survivalPlayerWithMaxHearts(GameTestHelper helper, int maxHearts) {
		ServerPlayer player = playerWithMaxHearts(helper, maxHearts);

		player.setGameMode(GameType.SURVIVAL);
		helper.assertFalse(player.hasInfiniteMaterials(), "the player is in Survival");

		return player;
	}

	private static void hold(ServerPlayer player, InteractionHand hand, Item item) {
		player.setItemInHand(hand, new ItemStack(item));
	}

	private static InteractionResult startUsing(ServerPlayer player, InteractionHand hand) {
		return player.gameMode.useItem(player, player.level(), player.getItemInHand(hand), hand);
	}

	// A mock player is never ticked by a connection, so its item use advances only when ticked here.
	private static void tickUse(ServerPlayer player, int ticks) {
		for (int i = 0; i < ticks; i++) {
			player.doTick();
		}
	}

	private static void hitLethally(GameTestHelper helper, ServerPlayer player) {
		player.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1000.0F);
	}

	private static Optional<ItemStack> craft(GameTestHelper helper, CraftingInput input) {
		var recipes = helper.getLevel().getServer().getRecipeManager();

		return recipes.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).map(recipe -> recipe.value().assemble(input));
	}

	private static void assertCrafts(GameTestHelper helper, CraftingInput input, ItemStack expected) {
		ItemStack result = craft(helper, input).orElse(ItemStack.EMPTY);

		helper.assertTrue(ItemStack.matches(result, expected), "crafts " + expected + ", got " + result);
	}

	// Rolls the mob's loot table the way LivingEntity.dropFromLootTable does, from one seeded random so the result is
	// fixed. A fresh seed per roll won't do: vanilla's seeded random gives nearly the same first float for nearby seeds.
	private static void assertDropRate(GameTestHelper helper, Mob mob, Item item, double expectedRate) {
		LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(mob.getLootTable().orElseThrow());
		LootParams params = new LootParams.Builder(helper.getLevel())
			.withParameter(LootContextParams.THIS_ENTITY, mob)
			.withParameter(LootContextParams.ORIGIN, mob.position())
			.withParameter(LootContextParams.DAMAGE_SOURCE, helper.getLevel().damageSources().generic())
			.create(LootContextParamSets.ENTITY);
		RandomSource random = RandomSource.create(LOOT_SEED);
		int drops = 0;

		for (int roll = 0; roll < LOOT_ROLLS; roll++) {
			boolean dropped = table.getRandomItems(params, random).stream().anyMatch(stack -> stack.is(item) && stack.getCount() == 1);

			if (dropped) drops++;
		}

		double rate = (double) drops / LOOT_ROLLS;

		helper.assertTrue(
			Math.abs(rate - expectedRate) <= DROP_RATE_TOLERANCE,
			item + " drop rate " + rate + " is within " + DROP_RATE_TOLERANCE + " of " + expectedRate
		);
	}

	private static void assertAdvancement(GameTestHelper helper, String id, String parent, AdvancementType frame, int experience) {
		AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(Identifier.parse(id));

		helper.assertTrue(holder != null, id + " exists");
		helper.assertValueEqual(holder.value().parent(), Optional.of(Identifier.parse(parent)), id + " parent");
		helper.assertValueEqual(holder.value().display().map(DisplayInfo::type), Optional.of(frame), id + " frame");
		helper.assertValueEqual(holder.value().rewards().experience(), experience, id + " XP reward");
	}

	private static boolean isDone(GameTestHelper helper, ServerPlayer player, String advancement) {
		AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(Identifier.parse(advancement));

		helper.assertTrue(holder != null, advancement + " exists");

		return player.getAdvancements().getOrStartProgress(holder).isDone();
	}
}
