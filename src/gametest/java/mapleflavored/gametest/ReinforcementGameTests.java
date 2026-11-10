package mapleflavored.gametest;

import static mapleflavored.gametest.MockPlayers.survivalPlayer;

import java.util.List;
import java.util.Optional;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.GearKind;
import mapleflavored.items.MapleItems;
import mapleflavored.reinforcement.Reinforcement;
import mapleflavored.reinforcement.ReinforcementRecipe;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.level.block.Blocks;

/**
 * Reinforcement: a template, an armor piece and any chainmail piece make the piece Reinforced, for +1 armor toughness
 * that lasts through every smithing upgrade.
 */
public class ReinforcementGameTests {
	private static final BlockPos STATION_POS = new BlockPos(1, 2, 1);
	private static final Identifier ADVANCEMENT = Identifier.parse("maple_flavored:story/reinforce_armor");
	private static final List<Item> CHAINMAIL = List.of(Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS);

	@GameTest
	public void everyArmorPieceTakesAnyChainmailPiece(GameTestHelper helper) {
		List<Item> armor = List.of(
			Items.LEATHER_HELMET, Items.COPPER_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.GOLDEN_BOOTS, Items.IRON_HELMET,
			Items.DIAMOND_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.TURTLE_HELMET, AlloyGear.of(Alloy.SHAKUDO, GearKind.BOOTS),
			AlloyGear.armoredElytra(Alloy.STEEL)
		);

		for (Item piece : armor) {
			for (Item chainmail : CHAINMAIL) {
				ItemStack result = reinforce(helper, new ItemStack(piece), new ItemStack(chainmail));

				helper.assertValueEqual(result.getItem(), piece, piece + " with " + chainmail);
				helper.assertTrue(Reinforcement.isReinforced(result), piece + " with " + chainmail + " is Reinforced");
			}
		}

		helper.succeed();
	}

	@GameTest
	public void elytraHeadsPumpkinsAndMobArmorAreRefused(GameTestHelper helper) {
		List<Item> refused = List.of(
			Items.ELYTRA, Items.CARVED_PUMPKIN, Items.PLAYER_HEAD, Items.CREEPER_HEAD, Items.IRON_HORSE_ARMOR, Items.WOLF_ARMOR,
			Items.DIAMOND_NAUTILUS_ARMOR, AlloyGear.of(Alloy.STEEL, GearKind.HORSE_ARMOR), Items.DIAMOND_SWORD
		);

		for (Item item : refused) {
			helper.assertTrue(reinforce(helper, new ItemStack(item), new ItemStack(Items.CHAINMAIL_HELMET)).isEmpty(), item + " is Reinforced");
		}

		helper.assertTrue(
			reinforce(helper, new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_HELMET)).isEmpty(), "an iron piece in place of chainmail works"
		);
		helper.assertTrue(
			assemble(helper, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), new ItemStack(Items.IRON_HELMET), new ItemStack(Items.CHAINMAIL_HELMET)).isEmpty(),
			"a Netherite upgrade template Reinforces"
		);
		helper.succeed();
	}

	// The recipe's data lists what it takes; a base listed there by mistake still can't spend a template for nothing.
	@GameTest
	public void aBaseThatIsNotWornAsArmorMakesNothingEvenWhenTheRecipeListsIt(GameTestHelper helper) {
		ReinforcementRecipe recipe = new ReinforcementRecipe(
			new Recipe.CommonInfo(false),
			Ingredient.of(Reinforcement.TEMPLATE),
			Ingredient.of(Items.WOLF_ARMOR, Items.DIAMOND_SWORD, Items.IRON_HELMET),
			Ingredient.of(Items.CHAINMAIL_HELMET)
		);

		for (Item base : List.of(Items.WOLF_ARMOR, Items.DIAMOND_SWORD)) {
			SmithingRecipeInput input = new SmithingRecipeInput(new ItemStack(Reinforcement.TEMPLATE), new ItemStack(base), new ItemStack(Items.CHAINMAIL_HELMET));

			helper.assertTrue(recipe.matches(input, helper.getLevel()), base + " is listed by the recipe");
			helper.assertTrue(recipe.assemble(input).isEmpty(), base + " is Reinforced");
		}

		SmithingRecipeInput helmet = new SmithingRecipeInput(
			new ItemStack(Reinforcement.TEMPLATE), new ItemStack(Items.IRON_HELMET), new ItemStack(Items.CHAINMAIL_HELMET)
		);

		helper.assertTrue(Reinforcement.isReinforced(recipe.assemble(helmet)), "a helmet in the same recipe is Reinforced");
		helper.succeed();
	}

	@GameTest
	public void aPieceIsReinforcedOnlyOnce(GameTestHelper helper) {
		ItemStack once = reinforce(helper, new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.CHAINMAIL_BOOTS));

		helper.assertTrue(reinforce(helper, once, new ItemStack(Items.CHAINMAIL_BOOTS)).isEmpty(), "a Reinforced piece is Reinforced again");
		helper.succeed();
	}

	@GameTest
	public void reinforcingKeepsEnchantmentsTrimNameAndWear(GameTestHelper helper) {
		ItemStack chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
		ArmorTrim trim = trim(helper);

		chestplate.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION), 2);
		chestplate.set(DataComponents.TRIM, trim);
		chestplate.set(DataComponents.CUSTOM_NAME, Component.literal("Mail"));
		chestplate.setDamageValue(50);

		ItemStack reinforced = reinforce(helper, chestplate, new ItemStack(Items.CHAINMAIL_HELMET));

		helper.assertTrue(Reinforcement.isReinforced(reinforced), "the enchanted chestplate is Reinforced");
		helper.assertValueEqual(reinforced.getEnchantments(), chestplate.getEnchantments(), "the enchantments");
		helper.assertValueEqual(reinforced.get(DataComponents.TRIM), trim, "the trim");
		helper.assertValueEqual(reinforced.get(DataComponents.CUSTOM_NAME), Component.literal("Mail"), "the name");
		helper.assertValueEqual(reinforced.getDamageValue(), 50, "the wear");
		helper.succeed();
	}

	@GameTest
	public void reinforcingAtASmithingTableUsesUpItsInputsAndEarnsTheAdvancement(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(ADVANCEMENT);

		helper.assertTrue(advancement != null, ADVANCEMENT + " exists");
		helper.assertValueEqual(advancement.value().parent(), Optional.of(Identifier.withDefaultNamespace("story/cure_zombie_villager")), "its parent");
		helper.assertValueEqual(advancement.value().display().map(DisplayInfo::type), Optional.of(AdvancementType.TASK), "its frame");

		helper.setBlock(STATION_POS, Blocks.SMITHING_TABLE);

		SmithingMenu smithing = new SmithingMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(STATION_POS)));

		smithing.getSlot(SmithingMenu.TEMPLATE_SLOT).set(new ItemStack(Reinforcement.TEMPLATE));
		smithing.getSlot(SmithingMenu.BASE_SLOT).set(new ItemStack(Items.IRON_LEGGINGS));
		smithing.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(new ItemStack(Items.CHAINMAIL_CHESTPLATE));
		helper.assertFalse(player.getAdvancements().getOrStartProgress(advancement).isDone(), "earned before Reinforcing");

		smithing.setCarried(ItemStack.EMPTY);
		smithing.clicked(SmithingMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, player);

		helper.assertTrue(Reinforcement.isReinforced(smithing.getCarried()), "the taken leggings are Reinforced");
		helper.assertTrue(smithing.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem().isEmpty(), "the template is used up");
		helper.assertTrue(smithing.getSlot(SmithingMenu.BASE_SLOT).getItem().isEmpty(), "the leggings are used up");
		helper.assertTrue(smithing.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem().isEmpty(), "the chainmail is used up");
		helper.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(), "earned by Reinforcing");
		helper.succeed();
	}

	@GameTest
	public void eachWornReinforcedPieceAddsOneToughness(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		player.setItemSlot(EquipmentSlot.CHEST, reinforced(helper, Items.IRON_CHESTPLATE));
		player.doTick();
		helper.assertValueEqual(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 1.0, "toughness in a Reinforced iron chestplate");
		helper.assertValueEqual(player.getAttributeValue(Attributes.ARMOR), 6.0, "armor in a Reinforced iron chestplate");

		for (Item piece : List.of(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS)) {
			player.setItemSlot(piece.components().get(DataComponents.EQUIPPABLE).slot(), reinforced(helper, piece));
		}

		player.doTick();
		helper.assertValueEqual(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 12.0, "toughness in Reinforced diamond");

		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
		player.doTick();
		helper.assertValueEqual(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 11.0, "toughness after taking off one Reinforced piece");
		helper.succeed();
	}

	@GameTest
	public void reinforcementStacksWithAdamant(GameTestHelper helper) {
		ServerPlayer plain = survivalPlayer(helper);
		ServerPlayer reinforced = survivalPlayer(helper);
		Item adamantHelmet = AlloyGear.of(Alloy.ADAMANT, GearKind.HELMET);

		plain.setItemSlot(EquipmentSlot.HEAD, new ItemStack(adamantHelmet));
		reinforced.setItemSlot(EquipmentSlot.HEAD, reinforced(helper, adamantHelmet));
		plain.doTick();
		reinforced.doTick();

		helper.assertValueEqual(
			reinforced.getAttributeValue(Attributes.ARMOR_TOUGHNESS), plain.getAttributeValue(Attributes.ARMOR_TOUGHNESS) + 1.0, "Reinforced Adamant's toughness"
		);
		helper.succeed();
	}

	@GameTest
	public void reinforcementCarriesThroughEveryUpgrade(GameTestHelper helper) {
		ItemStack diamond = reinforced(helper, Items.DIAMOND_CHESTPLATE);

		diamond.set(DataComponents.TRIM, trim(helper));

		ItemStack netherite = assemble(helper, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), diamond, new ItemStack(Items.NETHERITE_INGOT));
		ItemStack steel = upgrade(helper, netherite, new ItemStack(Alloy.STEEL.ingot()));
		ItemStack adamant = upgrade(helper, steel, new ItemStack(Alloy.ADAMANT.ingot()));
		ItemStack armoredElytra = upgrade(helper, adamant, new ItemStack(Items.ELYTRA));
		ItemStack orichalcumElytra = upgrade(helper, armoredElytra, new ItemStack(Alloy.ORICHALCUM.ingot()));

		helper.assertValueEqual(netherite.getItem(), Items.NETHERITE_CHESTPLATE, "the Netherite upgrade");
		helper.assertValueEqual(orichalcumElytra.getItem(), AlloyGear.armoredElytra(Alloy.ORICHALCUM), "the last upgrade");

		for (ItemStack upgraded : List.of(netherite, steel, adamant, armoredElytra, orichalcumElytra)) {
			helper.assertTrue(Reinforcement.isReinforced(upgraded), upgraded.getItem() + " stays Reinforced");
			helper.assertValueEqual(upgraded.get(DataComponents.TRIM), diamond.get(DataComponents.TRIM), upgraded.getItem() + " keeps its trim");
		}

		ServerPlayer plain = survivalPlayer(helper);
		ServerPlayer reinforced = survivalPlayer(helper);

		plain.setItemSlot(EquipmentSlot.CHEST, new ItemStack(AlloyGear.armoredElytra(Alloy.ORICHALCUM)));
		reinforced.setItemSlot(EquipmentSlot.CHEST, orichalcumElytra);
		plain.doTick();
		reinforced.doTick();
		helper.assertValueEqual(
			reinforced.getAttributeValue(Attributes.ARMOR_TOUGHNESS),
			plain.getAttributeValue(Attributes.ARMOR_TOUGHNESS) + 1.0,
			"toughness in a Reinforced Orichalcum Armored Elytra"
		);
		helper.succeed();
	}

	@GameTest
	public void trimmingAReinforcedPieceKeepsItReinforced(GameTestHelper helper) {
		ItemStack trimmed = assemble(
			helper, new ItemStack(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE), reinforced(helper, Items.IRON_BOOTS), new ItemStack(Items.QUARTZ)
		);

		helper.assertTrue(trimmed.has(DataComponents.TRIM), "the boots are trimmed");
		helper.assertTrue(Reinforcement.isReinforced(trimmed), "the trimmed boots stay Reinforced");
		helper.succeed();
	}

	@GameTest
	public void aReinforcedPieceSaysSoInItsTooltip(GameTestHelper helper) {
		List<String> lines = reinforced(helper, Items.GOLDEN_HELMET)
			.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), null, TooltipFlag.NORMAL)
			.stream()
			.map(Component::getString)
			.toList();

		helper.assertTrue(lines.contains("Reinforced"), "the tooltip " + lines + " says Reinforced");
		helper.assertTrue(lines.contains("+1 Armor Toughness"), "the tooltip " + lines + " shows the toughness");
		helper.succeed();
	}

	@GameTest
	public void sevenDiamondShardsAndABlockOfIronCopyTheTemplate(GameTestHelper helper) {
		ItemStack copied = copyTemplate(helper, new ItemStack(MapleItems.DIAMOND_SHARD));

		helper.assertValueEqual(copied.getItem(), Reinforcement.TEMPLATE, "the copied template");
		helper.assertValueEqual(copied.getCount(), 2, "templates made");
		helper.assertTrue(copyTemplate(helper, new ItemStack(Items.DIAMOND)).isEmpty(), "seven diamonds copy nothing");
		helper.succeed();
	}

	// The copy recipe's grid, with the material around the template and the block of iron.
	private static ItemStack copyTemplate(GameTestHelper helper, ItemStack material) {
		CraftingInput input = CraftingInput.of(3, 3, List.of(
			material, new ItemStack(Reinforcement.TEMPLATE), material,
			material, new ItemStack(Items.IRON_BLOCK), material,
			material, material, material
		));
		ServerLevel level = helper.getLevel();

		return level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
			.map(recipe -> recipe.value().assemble(input))
			.orElse(ItemStack.EMPTY);
	}

	private static ArmorTrim trim(GameTestHelper helper) {
		var registries = helper.getLevel().registryAccess();

		return new ArmorTrim(
			registries.lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(TrimMaterials.QUARTZ),
			registries.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(TrimPatterns.COAST)
		);
	}

	private static ItemStack reinforced(GameTestHelper helper, Item piece) {
		ItemStack reinforced = reinforce(helper, new ItemStack(piece), new ItemStack(Items.CHAINMAIL_HELMET));

		helper.assertTrue(Reinforcement.isReinforced(reinforced), piece + " is Reinforced");

		return reinforced;
	}

	private static ItemStack reinforce(GameTestHelper helper, ItemStack base, ItemStack chainmail) {
		return assemble(helper, new ItemStack(Reinforcement.TEMPLATE), base, chainmail);
	}

	private static ItemStack upgrade(GameTestHelper helper, ItemStack base, ItemStack addition) {
		return assemble(helper, ItemStack.EMPTY, base, addition);
	}

	private static ItemStack assemble(GameTestHelper helper, ItemStack template, ItemStack base, ItemStack addition) {
		ServerLevel level = helper.getLevel();
		SmithingRecipeInput input = new SmithingRecipeInput(template, base, addition);

		return level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level)
			.map(recipe -> recipe.value().assemble(input))
			.orElse(ItemStack.EMPTY);
	}
}
