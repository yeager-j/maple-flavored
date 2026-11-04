package mapleflavored.gametest;

import java.util.List;
import mapleflavored.materialunits.MaterialUnits;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * The anvil repairs with material units for free, combines two matching books for 50 points per resulting level,
 * renames for 10 points, and does nothing else: no item combining, no books on gear, no prior-work penalty.
 */
public class AnvilGameTests {
	private static final BlockPos ANVIL_POS = new BlockPos(1, 2, 1);
	private static final int FULL_STACK = 64;

	/** An item repaired with a stack of its material, and how many units its full repair takes. */
	private record RepairCase(Item item, Item material, int fullRepairUnits) {
	}

	@GameTest
	public void aFullRepairTakesOneUnitFewerThanTheRecipe(GameTestHelper helper) {
		assertFullRepairs(helper, List.of(
			new RepairCase(Items.IRON_PICKAXE, Items.IRON_INGOT, 2),
			new RepairCase(Items.DIAMOND_CHESTPLATE, Items.DIAMOND, 7),
			new RepairCase(Items.WOODEN_PICKAXE, Items.SPRUCE_PLANKS, 2),
			new RepairCase(Items.SHIELD, Items.OAK_PLANKS, 5),
			new RepairCase(Items.TURTLE_HELMET, Items.TURTLE_SCUTE, 4),
			new RepairCase(Items.WOLF_ARMOR, Items.ARMADILLO_SCUTE, 5),
			new RepairCase(Items.MACE, Items.BREEZE_ROD, 1)
		));
	}

	@GameTest
	public void itemsWithoutAnIngotUseTheirOverrideMaterial(GameTestHelper helper) {
		assertFullRepairs(helper, List.of(
			new RepairCase(Items.BOW, Items.STRING, 2),
			new RepairCase(Items.CROSSBOW, Items.STRING, 1),
			new RepairCase(Items.FISHING_ROD, Items.STRING, 1),
			new RepairCase(Items.SHEARS, Items.IRON_INGOT, 1),
			new RepairCase(Items.FLINT_AND_STEEL, Items.IRON_INGOT, 1),
			new RepairCase(Items.BRUSH, Items.COPPER_INGOT, 1),
			new RepairCase(Items.CARROT_ON_A_STICK, Items.CARROT, 1),
			new RepairCase(Items.WARPED_FUNGUS_ON_A_STICK, Items.WARPED_FUNGUS, 1)
		));
	}

	@GameTest
	public void itemsWithoutARecipeRepairByTheirOwnRule(GameTestHelper helper) {
		assertFullRepairs(helper, List.of(
			new RepairCase(Items.CHAINMAIL_HELMET, Items.IRON_INGOT, 4),
			new RepairCase(Items.CHAINMAIL_CHESTPLATE, Items.IRON_INGOT, 7),
			new RepairCase(Items.CHAINMAIL_LEGGINGS, Items.IRON_INGOT, 6),
			new RepairCase(Items.CHAINMAIL_BOOTS, Items.IRON_INGOT, 3),
			new RepairCase(Items.ELYTRA, Items.PHANTOM_MEMBRANE, 4),
			new RepairCase(Items.TRIDENT, Items.PRISMARINE_SHARD, 4),
			new RepairCase(Items.NETHERITE_SWORD, Items.NETHERITE_INGOT, 1),
			new RepairCase(Items.NETHERITE_CHESTPLATE, Items.NETHERITE_INGOT, 1)
		));
	}

	@GameTest
	public void eachUnitRestoresAnEqualShare(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		AnvilMenu anvil = anvil(helper, player);

		anvil.getSlot(AnvilMenu.INPUT_SLOT).set(damaged(Items.DIAMOND_CHESTPLATE));
		anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(new ItemStack(Items.DIAMOND));

		// 528 durability over 7 units restores ceil(528 / 7) = 76 per diamond, from 527 damage.
		ItemStack taken = take(anvil, player);

		helper.assertValueEqual(taken.getDamageValue(), 451, "damage after one diamond");
		helper.assertTrue(anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem().isEmpty(), "the diamond is used up");
		helper.succeed();
	}

	@GameTest
	public void repairsCostNoXpAndIgnorePriorWork(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		AnvilMenu anvil = anvil(helper, player);
		ItemStack pickaxe = damaged(Items.IRON_PICKAXE);
		Holder<Enchantment> efficiency = enchantment(helper, Enchantments.EFFICIENCY);

		pickaxe.set(DataComponents.REPAIR_COST, 50);
		pickaxe.enchant(efficiency, 3);
		anvil.getSlot(AnvilMenu.INPUT_SLOT).set(pickaxe);
		anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(new ItemStack(Items.IRON_INGOT, 5));

		helper.assertValueEqual(anvil.getCost(), 0, "repair cost in points");

		ItemStack taken = take(anvil, player);

		helper.assertValueEqual(taken.getItem(), Items.IRON_PICKAXE, "taken at 0 XP");
		helper.assertValueEqual(taken.getDamageValue(), 0, "damage after repair");
		helper.assertValueEqual(taken.getEnchantments().getLevel(efficiency), 3, "Efficiency kept");
		helper.assertValueEqual(anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem().getCount(), 3, "ingots left");
		helper.assertValueEqual(player.experienceLevel, 0, "level after repair");
		helper.succeed();
	}

	@GameTest
	public void theAnvilOnlyRepairsDamagedItemsWithTheirMaterial(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);

		assertNoResult(helper, damaged(Items.IRON_PICKAXE), new ItemStack(Items.DIAMOND), "the wrong material");
		assertNoResult(helper, new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.IRON_INGOT), "an undamaged item");
		assertNoResult(helper, damaged(Items.IRON_PICKAXE), damaged(Items.IRON_PICKAXE), "two items");
		assertNoResult(helper, new ItemStack(Items.DIAMOND_SWORD), book(sharpness, 1), "a book on gear");
		assertNoResult(helper, book(sharpness, 1), new ItemStack(Items.DIAMOND_SWORD), "gear on a book");
		helper.succeed();
	}

	@GameTest
	public void twoMatchingBooksMakeTheNextLevel(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		AnvilMenu anvil = anvil(helper, player);
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);

		player.setExperienceLevels(10);
		player.giveExperiencePoints(-60);
		anvil.getSlot(AnvilMenu.INPUT_SLOT).set(book(sharpness, 1));
		anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(book(sharpness, 1));

		helper.assertValueEqual(anvil.getCost(), 100, "Sharpness II cost in points");
		// Level 10 is 160 points; 60 fewer leaves exactly the 100 the combine costs.
		helper.assertValueEqual(storedLevel(take(anvil, player), sharpness), 2, "combined level");
		helper.assertValueEqual(player.experienceLevel, 0, "level after paying");
		helper.assertValueEqual(pointsIntoLevel(player), 0, "points after paying");
		helper.assertTrue(anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem().isEmpty(), "the second book is used up");

		anvil.getSlot(AnvilMenu.INPUT_SLOT).set(book(sharpness, 4));
		anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(book(sharpness, 4));

		helper.assertValueEqual(anvil.getCost(), 250, "Sharpness V cost in points");
		helper.assertValueEqual(storedLevel(anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem(), sharpness), 5, "combined level");
		helper.succeed();
	}

	@GameTest
	public void payingInPointsLeavesTheRestOfThePlayersXp(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		AnvilMenu anvil = anvil(helper, player);
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);

		// Level 30 is 1395 points. 100 fewer is 1295: level 29 (1288 points) and 7 of the 107 it needs.
		MockPlayers.runCommand(helper, player, "experience set @s 30 levels");
		anvil.getSlot(AnvilMenu.INPUT_SLOT).set(book(sharpness, 1));
		anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(book(sharpness, 1));
		take(anvil, player);

		helper.assertValueEqual(player.experienceLevel, 29, "level after a book combine");
		helper.assertValueEqual(pointsIntoLevel(player), 7, "points into level 29");

		// Level 5 and 3 points is 58. 10 fewer is 48: level 4 (40 points) and 8 of the 15 it needs.
		player.setExperienceLevels(5);
		player.setExperiencePoints(3);
		anvil.getSlot(AnvilMenu.INPUT_SLOT).set(new ItemStack(Items.IRON_SWORD));
		anvil.setItemName("Pointy");
		take(anvil, player);

		helper.assertValueEqual(player.experienceLevel, 4, "level after a rename");
		helper.assertValueEqual(pointsIntoLevel(player), 8, "points into level 4");
		helper.succeed();
	}

	@GameTest
	public void aCombineCantBeTakenWithoutEnoughPoints(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		AnvilMenu anvil = anvil(helper, player);
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);

		// Level 10 is 160 points; 61 fewer leaves 99, one short.
		player.setExperienceLevels(10);
		player.giveExperiencePoints(-61);
		anvil.getSlot(AnvilMenu.INPUT_SLOT).set(book(sharpness, 1));
		anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(book(sharpness, 1));

		helper.assertFalse(anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(), "the result is shown");
		helper.assertTrue(take(anvil, player).isEmpty(), "the result is taken");
		// 99 points is level 7 (91 points) and 8 into it.
		helper.assertValueEqual(player.experienceLevel, 7, "level");
		helper.assertValueEqual(pointsIntoLevel(player), 8, "points into level 7");
		helper.assertFalse(anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem().isEmpty(), "the second book is kept");
		helper.succeed();
	}

	@GameTest
	public void onlyTwoBooksWithTheSameSingleEnchantmentAndLevelCombine(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		Holder<Enchantment> smite = enchantment(helper, Enchantments.SMITE);
		ItemStack twoEnchantments = book(sharpness, 1);
		ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);

		stored.set(sharpness, 1);
		stored.set(enchantment(helper, Enchantments.UNBREAKING), 1);
		EnchantmentHelper.setEnchantments(twoEnchantments, stored.toImmutable());

		assertNoResult(helper, book(sharpness, 5), book(sharpness, 5), "two max-level books");
		assertNoResult(helper, book(sharpness, 1), book(sharpness, 2), "books of different levels");
		assertNoResult(helper, book(sharpness, 1), book(smite, 1), "books of different enchantments");
		assertNoResult(helper, twoEnchantments, book(sharpness, 1), "a book with two enchantments");
		helper.succeed();
	}

	@GameTest
	public void renamingCostsTenPoints(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		AnvilMenu anvil = anvil(helper, player);
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		ItemStack named = new ItemStack(Items.IRON_SWORD);

		anvil.getSlot(AnvilMenu.INPUT_SLOT).set(new ItemStack(Items.IRON_SWORD));
		anvil.setItemName("Iron Sword");

		helper.assertTrue(anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(), "an unchanged name makes a result");

		anvil.setItemName("Pointy");

		helper.assertValueEqual(anvil.getCost(), 10, "rename cost in points");
		helper.assertValueEqual(anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem().getHoverName().getString(), "Pointy", "new name");

		named.set(DataComponents.CUSTOM_NAME, Component.literal("Pointy"));
		anvil.getSlot(AnvilMenu.INPUT_SLOT).set(named);
		anvil.setItemName("");

		helper.assertValueEqual(anvil.getCost(), 10, "name removal cost in points");
		helper.assertFalse(anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem().has(DataComponents.CUSTOM_NAME), "the name is removed");

		anvil.getSlot(AnvilMenu.INPUT_SLOT).set(damaged(Items.IRON_PICKAXE));
		anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(new ItemStack(Items.IRON_INGOT));
		anvil.setItemName("Digger");

		helper.assertValueEqual(anvil.getCost(), 10, "repair and rename cost in points");

		anvil.getSlot(AnvilMenu.INPUT_SLOT).set(book(sharpness, 1));
		anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(book(sharpness, 1));
		anvil.setItemName("Edge");

		helper.assertValueEqual(anvil.getCost(), 110, "book combine and rename cost in points");
		helper.succeed();
	}

	@GameTest
	public void materialUnitsCoverGoldenApplesButNotBooks(GameTestHelper helper) {
		for (Item apple : List.of(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE)) {
			var material = MaterialUnits.materialOf(new ItemStack(apple)).orElseThrow();

			helper.assertTrue(material.test(new ItemStack(Items.GOLD_BLOCK)), apple + " takes a block of gold");
			helper.assertFalse(material.test(new ItemStack(Items.GOLD_INGOT)), apple + " takes a gold ingot");
		}

		helper.assertTrue(MaterialUnits.materialOf(new ItemStack(Items.BOOK)).isEmpty(), "a book has a material");
		helper.succeed();
	}

	private static void assertFullRepairs(GameTestHelper helper, List<RepairCase> cases) {
		ServerPlayer player = survivalPlayer(helper);

		for (RepairCase repairCase : cases) {
			AnvilMenu anvil = anvil(helper, player);
			String label = repairCase.item() + " with " + repairCase.material();

			anvil.getSlot(AnvilMenu.INPUT_SLOT).set(damaged(repairCase.item()));
			anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(new ItemStack(repairCase.material(), FULL_STACK));

			ItemStack taken = take(anvil, player);
			int left = anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem().getCount();

			helper.assertValueEqual(taken.getItem(), repairCase.item(), label + " is taken");
			helper.assertValueEqual(taken.getDamageValue(), 0, label + " damage");
			helper.assertValueEqual(FULL_STACK - left, repairCase.fullRepairUnits(), label + " units used");
		}

		helper.succeed();
	}

	private static void assertNoResult(GameTestHelper helper, ItemStack input, ItemStack addition, String label) {
		AnvilMenu anvil = anvil(helper, survivalPlayer(helper));

		anvil.getSlot(AnvilMenu.INPUT_SLOT).set(input);
		anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(addition);

		helper.assertTrue(anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(), label + " makes a result");
	}

	private static ServerPlayer survivalPlayer(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		player.setGameMode(GameType.SURVIVAL);

		return player;
	}

	private static AnvilMenu anvil(GameTestHelper helper, ServerPlayer player) {
		helper.setBlock(ANVIL_POS, Blocks.ANVIL);

		return new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(ANVIL_POS)));
	}

	/** Clicks the result slot the way a player picks it up, returning what the player then holds. */
	private static ItemStack take(AnvilMenu anvil, ServerPlayer player) {
		anvil.setCarried(ItemStack.EMPTY);
		anvil.clicked(AnvilMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, player);

		return anvil.getCarried();
	}

	/** The item with 1 durability left. */
	private static ItemStack damaged(Item item) {
		ItemStack stack = new ItemStack(item);

		stack.setDamageValue(stack.getMaxDamage() - 1);

		return stack;
	}

	private static ItemStack book(Holder<Enchantment> enchantment, int level) {
		return EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, level));
	}

	private static int storedLevel(ItemStack book, Holder<Enchantment> enchantment) {
		return book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(enchantment);
	}

	private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
		return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}

	private static int pointsIntoLevel(ServerPlayer player) {
		return Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
	}
}
