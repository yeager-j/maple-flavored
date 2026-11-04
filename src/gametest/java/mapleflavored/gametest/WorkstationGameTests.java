package mapleflavored.gametest;

import java.util.List;
import mapleflavored.smithing.UpgradeRefusalMenu;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * The grindstone and crafting grid never combine items, the grindstone gives no XP, and smithing material upgrades
 * refuse enchanted gear.
 */
public class WorkstationGameTests {
	private static final BlockPos STATION_POS = new BlockPos(1, 2, 1);

	@GameTest
	public void theGrindstoneStripsAnItemButGivesNoXp(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		GrindstoneMenu grindstone = grindstone(helper, player);
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);

		// Vanilla would award about Sharpness V's minimum cost of 45, halved and rolled, as orbs.
		sword.enchant(enchantment(helper, Enchantments.SHARPNESS), 5);
		grindstone.getSlot(0).set(sword);
		grindstone.clicked(2, 0, ContainerInput.PICKUP, player);

		ItemStack taken = grindstone.getCarried();

		helper.assertValueEqual(taken.getItem(), Items.DIAMOND_SWORD, "taken item");
		helper.assertTrue(taken.getEnchantments().isEmpty(), "the sword is stripped");
		helper.assertEntityNotPresent(EntityTypes.EXPERIENCE_ORB);
		helper.assertValueEqual(player.experienceLevel, 0, "level");
		helper.assertValueEqual(player.experienceProgress, 0.0F, "level progress");
		helper.succeed();
	}

	@GameTest
	public void theGrindstoneNeverMergesTwoItems(GameTestHelper helper) {
		GrindstoneMenu grindstone = grindstone(helper, survivalPlayer(helper));

		grindstone.getSlot(0).set(damaged(Items.IRON_PICKAXE));
		grindstone.getSlot(1).set(damaged(Items.IRON_PICKAXE));

		helper.assertTrue(grindstone.getSlot(2).getItem().isEmpty(), "two pickaxes merge");
		helper.succeed();
	}

	@GameTest
	public void theCraftingGridNeverMergesTwoItems(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CraftingInput input = CraftingInput.of(2, 1, List.of(damaged(Items.IRON_PICKAXE), damaged(Items.IRON_PICKAXE)));

		helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level).isEmpty(), "two pickaxes merge");
		helper.succeed();
	}

	@GameTest
	public void materialUpgradesRefuseEnchantedGear(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		ItemStack enchantedSword = new ItemStack(Items.DIAMOND_SWORD);

		enchantedSword.enchant(enchantment(helper, Enchantments.SHARPNESS), 1);

		ItemStack refused = smith(helper, player, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, enchantedSword, Items.NETHERITE_INGOT);
		ItemStack upgraded = smith(helper, player, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, new ItemStack(Items.DIAMOND_SWORD), Items.NETHERITE_INGOT);

		helper.assertTrue(refused.isEmpty(), "an enchanted sword is upgraded");
		helper.assertValueEqual(upgraded.getItem(), Items.NETHERITE_SWORD, "an unenchanted sword's upgrade");
		helper.succeed();
	}

	@GameTest
	public void trimsStillWorkOnEnchantedGear(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		ItemStack chestplate = new ItemStack(Items.IRON_CHESTPLATE);

		chestplate.enchant(enchantment(helper, Enchantments.PROTECTION), 1);

		ItemStack trimmed = smith(helper, player, Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, chestplate, Items.AMETHYST_SHARD);

		helper.assertValueEqual(trimmed.getItem(), Items.IRON_CHESTPLATE, "the trimmed chestplate");
		helper.assertFalse(trimmed.getEnchantments().isEmpty(), "the trimmed chestplate keeps Protection");
		helper.succeed();
	}

	@GameTest
	public void theSmithingMenuSaysWhenEnchantmentIsTheOnlyProblem(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		ItemStack enchantedDiamondSword = new ItemStack(Items.DIAMOND_SWORD);
		ItemStack enchantedIronSword = new ItemStack(Items.IRON_SWORD);
		ItemStack enchantedChestplate = new ItemStack(Items.IRON_CHESTPLATE);

		enchantedDiamondSword.enchant(sharpness, 1);
		enchantedIronSword.enchant(sharpness, 1);
		enchantedChestplate.enchant(enchantment(helper, Enchantments.PROTECTION), 1);

		SmithingMenu refused = smithing(helper, player, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, enchantedDiamondSword, Items.NETHERITE_INGOT);
		SmithingMenu noUpgrade = smithing(helper, player, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, enchantedIronSword, Items.NETHERITE_INGOT);
		SmithingMenu upgraded = smithing(helper, player, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, new ItemStack(Items.DIAMOND_SWORD), Items.NETHERITE_INGOT);
		SmithingMenu trimmed = smithing(helper, player, Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, enchantedChestplate, Items.AMETHYST_SHARD);

		helper.assertTrue(refusedEnchantedBase(refused), "an enchanted diamond sword's upgrade is refused for its enchantment");
		helper.assertTrue(refused.hasRecipeError(), "the refused upgrade shows the error icon");
		helper.assertFalse(refusedEnchantedBase(noUpgrade), "an enchanted iron sword, which has no upgrade, is refused for its enchantment");
		helper.assertTrue(noUpgrade.hasRecipeError(), "the iron sword shows the error icon");
		helper.assertFalse(refusedEnchantedBase(upgraded), "an unenchanted upgrade is refused for its enchantment");
		helper.assertFalse(refusedEnchantedBase(trimmed), "a trim on enchanted armor is refused for its enchantment");
		helper.succeed();
	}

	private static ItemStack smith(GameTestHelper helper, ServerPlayer player, Item template, ItemStack base, Item addition) {
		return smithing(helper, player, template, base, addition).getSlot(SmithingMenu.RESULT_SLOT).getItem();
	}

	private static SmithingMenu smithing(GameTestHelper helper, ServerPlayer player, Item template, ItemStack base, Item addition) {
		helper.setBlock(STATION_POS, Blocks.SMITHING_TABLE);

		SmithingMenu smithing = new SmithingMenu(0, player.getInventory(), access(helper));

		smithing.getSlot(SmithingMenu.TEMPLATE_SLOT).set(new ItemStack(template));
		smithing.getSlot(SmithingMenu.BASE_SLOT).set(base);
		smithing.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(new ItemStack(addition));

		return smithing;
	}

	// The flag the menu syncs to its screen to pick the error tooltip.
	private static boolean refusedEnchantedBase(SmithingMenu smithing) {
		return ((UpgradeRefusalMenu) smithing).mapleflavored$refusedEnchantedBase();
	}

	private static GrindstoneMenu grindstone(GameTestHelper helper, ServerPlayer player) {
		helper.setBlock(STATION_POS, Blocks.GRINDSTONE);

		return new GrindstoneMenu(0, player.getInventory(), access(helper));
	}

	private static ContainerLevelAccess access(GameTestHelper helper) {
		return ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(STATION_POS));
	}

	private static ServerPlayer survivalPlayer(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		player.setGameMode(GameType.SURVIVAL);

		return player;
	}

	/** The item with 1 durability left. */
	private static ItemStack damaged(Item item) {
		ItemStack stack = new ItemStack(item);

		stack.setDamageValue(stack.getMaxDamage() - 1);

		return stack;
	}

	private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
		return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}
}
