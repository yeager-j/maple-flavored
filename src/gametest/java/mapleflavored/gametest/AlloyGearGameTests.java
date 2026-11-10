package mapleflavored.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.AlloySets;
import mapleflavored.alloys.GearKind;
import mapleflavored.smithing.UpgradeRefusalMenu;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Every Netherite item has an Alloy version per Alloy, made at a Smithing Table with an Alloy ingot and no template,
 * with its Alloy's plain-number traits on top of Netherite's.
 */
public class AlloyGearGameTests {
	private static final BlockPos STATION_POS = new BlockPos(1, 2, 1);
	// Components that name and draw the item, or that its Alloy's traits and ingot change.
	private static final Set<DataComponentType<?>> ALLOY_COMPONENTS = Set.of(
		DataComponents.ITEM_NAME,
		DataComponents.ITEM_MODEL,
		DataComponents.REPAIRABLE,
		DataComponents.EQUIPPABLE,
		DataComponents.MAX_DAMAGE,
		DataComponents.ATTRIBUTE_MODIFIERS
	);

	@GameTest
	public void everyAlloyItemIsItsNetheriteItemWithItsAlloysChanges(GameTestHelper helper) {
		for (Alloy alloy : Alloy.values()) {
			for (GearKind kind : GearKind.values()) {
				Item netherite = kind.netheriteItem();
				Item alloyItem = AlloyGear.of(alloy, kind);
				String label = alloy.path() + " " + kind.path();
				Set<DataComponentType<?>> types = new HashSet<>(netherite.components().keySet());

				types.addAll(alloyItem.components().keySet());
				types.removeAll(ALLOY_COMPONENTS);

				for (DataComponentType<?> type : types) {
					boolean same = Objects.equals(alloyItem.components().get(type), netherite.components().get(type));

					helper.assertTrue(same, label + " has Netherite's " + type);
				}

				helper.assertValueEqual(
					alloyItem.components().get(DataComponents.EQUIPPABLE) == null,
					netherite.components().get(DataComponents.EQUIPPABLE) == null,
					label + " is equippable"
				);
				helper.assertValueEqual(new ItemStack(alloyItem).isDamageableItem(), kind.hasDurability(), label + " wears down");
				helper.assertValueEqual(
					new ItemStack(alloyItem).isValidRepairItem(new ItemStack(alloy.ingot())), kind.hasDurability(), label + " repairs with its ingot"
				);
				helper.assertFalse(new ItemStack(alloyItem).isValidRepairItem(new ItemStack(Items.NETHERITE_INGOT)), label + " repairs with a Netherite Ingot");
			}
		}

		helper.succeed();
	}

	@GameTest
	public void electrumAndOrichalcumGearHasNetheritesNumbers(GameTestHelper helper) {
		for (Alloy alloy : List.of(Alloy.ELECTRUM, Alloy.ORICHALCUM)) {
			for (GearKind kind : GearKind.values()) {
				Item netherite = kind.netheriteItem();
				Item alloyItem = AlloyGear.of(alloy, kind);
				String label = alloy.path() + " " + kind.path();

				for (DataComponentType<?> type : List.of(DataComponents.ATTRIBUTE_MODIFIERS, DataComponents.MAX_DAMAGE)) {
					boolean same = Objects.equals(alloyItem.components().get(type), netherite.components().get(type));

					helper.assertTrue(same, label + " has Netherite's " + type);
				}
			}
		}

		helper.succeed();
	}

	@GameTest
	public void alloyGearIsInEveryTagItsNetheriteItemIsIn(GameTestHelper helper) {
		for (Alloy alloy : Alloy.values()) {
			for (GearKind kind : GearKind.values()) {
				List<TagKey<Item>> netheriteTags = kind.netheriteItem().builtInRegistryHolder().tags().toList();
				ItemStack alloyItem = new ItemStack(AlloyGear.of(alloy, kind));

				for (TagKey<Item> tag : netheriteTags) {
					helper.assertTrue(alloyItem.is(tag), alloy.path() + " " + kind.path() + " is in " + tag.location());
				}
			}
		}

		helper.succeed();
	}

	@GameTest
	public void netheriteGearAndAnAlloyIngotSmithIntoAlloyGearWithNoTemplate(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		SmithingMenu smithing = smithing(helper, player, ItemStack.EMPTY, new ItemStack(Items.NETHERITE_SWORD), new ItemStack(Alloy.STEEL.ingot(), 2));

		smithing.setCarried(ItemStack.EMPTY);
		smithing.clicked(SmithingMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, player);

		helper.assertValueEqual(smithing.getCarried().getItem(), AlloyGear.of(Alloy.STEEL, GearKind.SWORD), "the smithed sword");
		helper.assertTrue(smithing.getSlot(SmithingMenu.BASE_SLOT).getItem().isEmpty(), "the Netherite sword is used up");
		helper.assertValueEqual(smithing.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem().getCount(), 1, "Steel ingots left");
		helper.succeed();
	}

	@GameTest
	public void everyNetheriteItemSmithsIntoEveryAlloy(GameTestHelper helper) {
		for (Alloy alloy : Alloy.values()) {
			for (GearKind kind : GearKind.values()) {
				ItemStack result = assemble(helper, ItemStack.EMPTY, new ItemStack(kind.netheriteItem()), new ItemStack(alloy.ingot()));

				helper.assertValueEqual(result.getItem(), AlloyGear.of(alloy, kind), "Netherite " + kind.path() + " with " + alloy.path());
			}
		}

		helper.succeed();
	}

	@GameTest
	public void alloyGearChangesAlloyWithADifferentIngotOnly(GameTestHelper helper) {
		ItemStack steelChestplate = new ItemStack(AlloyGear.of(Alloy.STEEL, GearKind.CHESTPLATE));
		ItemStack changed = assemble(helper, ItemStack.EMPTY, steelChestplate, new ItemStack(Alloy.ELECTRUM.ingot()));
		ItemStack same = assemble(helper, ItemStack.EMPTY, new ItemStack(AlloyGear.of(Alloy.STEEL, GearKind.SWORD)), new ItemStack(Alloy.STEEL.ingot()));

		helper.assertValueEqual(changed.getItem(), AlloyGear.of(Alloy.ELECTRUM, GearKind.CHESTPLATE), "a Steel chestplate with an Electrum ingot");
		helper.assertTrue(same.isEmpty(), "a Steel sword with a Steel ingot makes something");
		helper.succeed();
	}

	@GameTest
	public void alloySmithingRefusesATemplateAndEnchantedGear(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		ItemStack enchantedSword = new ItemStack(Items.NETHERITE_SWORD);

		enchantedSword.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 1);

		ItemStack withTemplate = smithing(
			helper, player, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), new ItemStack(Items.NETHERITE_SWORD), new ItemStack(Alloy.STEEL.ingot())
		).getSlot(SmithingMenu.RESULT_SLOT).getItem();
		SmithingMenu enchanted = smithing(helper, player, ItemStack.EMPTY, enchantedSword, new ItemStack(Alloy.STEEL.ingot()));

		helper.assertTrue(withTemplate.isEmpty(), "smithing with a template makes something");
		helper.assertTrue(enchanted.getSlot(SmithingMenu.RESULT_SLOT).getItem().isEmpty(), "an enchanted sword is smithed");
		helper.assertTrue(((UpgradeRefusalMenu) enchanted).mapleflavored$refusedEnchantedInput(), "the menu says the enchantment is why");
		helper.succeed();
	}

	@GameTest
	public void alloySmithingKeepsTrimsAndNames(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ItemStack helmet = new ItemStack(Items.NETHERITE_HELMET);
		ArmorTrim trim = new ArmorTrim(
			level.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(TrimMaterials.QUARTZ),
			level.registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(TrimPatterns.COAST)
		);

		helmet.set(DataComponents.TRIM, trim);
		helmet.set(DataComponents.CUSTOM_NAME, Component.literal("Lid"));

		ItemStack adamant = assemble(helper, ItemStack.EMPTY, helmet, new ItemStack(Alloy.ADAMANT.ingot()));

		helper.assertValueEqual(adamant.getItem(), AlloyGear.of(Alloy.ADAMANT, GearKind.HELMET), "the smithed helmet");
		helper.assertValueEqual(adamant.get(DataComponents.TRIM), trim, "the trim");
		helper.assertValueEqual(adamant.get(DataComponents.CUSTOM_NAME), Component.literal("Lid"), "the name");
		helper.succeed();
	}

	@GameTest
	public void alloySmithingKeepsTheShareOfDurabilityLeft(GameTestHelper helper) {
		ItemStack wornNetherite = new ItemStack(Items.NETHERITE_SWORD);
		ItemStack wornSteel = new ItemStack(AlloyGear.of(Alloy.STEEL, GearKind.SWORD));

		wornNetherite.setDamageValue(1015);
		wornSteel.setDamageValue(3000);

		ItemStack steel = assemble(helper, ItemStack.EMPTY, wornNetherite, new ItemStack(Alloy.STEEL.ingot()));
		ItemStack electrum = assemble(helper, ItemStack.EMPTY, wornSteel, new ItemStack(Alloy.ELECTRUM.ingot()));

		helper.assertValueEqual(steel.getDamageValue(), 1522, "a half-worn Netherite sword's damage as Steel");
		helper.assertValueEqual(electrum.getDamageValue(), 2000, "a worn Steel sword's damage as Electrum");
		helper.succeed();
	}

	@GameTest
	public void alloyGearCantBeSalvaged(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ItemStack steelSword = new ItemStack(AlloyGear.of(Alloy.STEEL, GearKind.SWORD));
		ItemStack withNetheriteIngot = assemble(helper, ItemStack.EMPTY, steelSword, new ItemStack(Items.NETHERITE_INGOT));

		helper.assertTrue(withNetheriteIngot.isEmpty(), "a Steel sword with a Netherite Ingot makes something");

		for (var type : List.of(RecipeType.SMELTING, RecipeType.BLASTING)) {
			boolean smelts = level.getServer().getRecipeManager().getRecipeFor(type, new SingleRecipeInput(steelSword), level).isPresent();

			helper.assertFalse(smelts, "a Steel sword smelts in " + type);
		}

		helper.succeed();
	}

	@GameTest
	public void steelGearLastsLongerAndKnocksBackHarder(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		helper.assertValueEqual(maxDamage(Alloy.STEEL, GearKind.SWORD), 3046, "Steel sword durability");
		helper.assertValueEqual(maxDamage(Alloy.STEEL, GearKind.PICKAXE), 3046, "Steel pickaxe durability");
		helper.assertValueEqual(maxDamage(Alloy.STEEL, GearKind.HELMET), 610, "Steel helmet durability");
		helper.assertValueEqual(maxDamage(Alloy.STEEL, GearKind.CHESTPLATE), 888, "Steel chestplate durability");
		helper.assertValueEqual(maxDamage(Alloy.STEEL, GearKind.LEGGINGS), 832, "Steel leggings durability");
		helper.assertValueEqual(maxDamage(Alloy.STEEL, GearKind.BOOTS), 721, "Steel boots durability");

		equip(player, Alloy.STEEL, GearKind.SWORD);
		helper.assertValueEqual(player.getAttributeValue(Attributes.ATTACK_KNOCKBACK), 0.5, "attack knockback with a Steel sword");

		wearFullSet(player, null);
		helper.assertValueEqual(round(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)), 0.4, "knockback resistance in Netherite");

		// Three pieces, since a full Steel set's bonus makes it immune to knockback.
		wearFullSet(player, Alloy.STEEL);
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
		player.doTick();
		helper.assertValueEqual(round(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)), 0.7, "knockback resistance in three Steel pieces");
		helper.succeed();
	}

	@GameTest
	public void shakudoGearAttacksAndMovesFaster(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		equip(player, null, GearKind.SWORD);
		helper.assertValueEqual(round(player.getAttributeValue(Attributes.ATTACK_SPEED)), 1.6, "attack speed with a Netherite sword");

		equip(player, Alloy.SHAKUDO, GearKind.SWORD);
		helper.assertValueEqual(round(player.getAttributeValue(Attributes.ATTACK_SPEED)), 1.76, "attack speed with a Shakudo sword");

		wearFullSet(player, Alloy.SHAKUDO);
		helper.assertValueEqual(round(player.getAttributeValue(Attributes.MOVEMENT_SPEED)), 0.11, "movement speed in Shakudo");
		helper.succeed();
	}

	@GameTest
	public void adamantGearHitsHarderAndIsTougher(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		equip(player, null, GearKind.SWORD);
		helper.assertValueEqual(player.getAttributeValue(Attributes.ATTACK_DAMAGE), 8.0, "attack damage with a Netherite sword");

		equip(player, Alloy.ADAMANT, GearKind.SWORD);
		helper.assertValueEqual(player.getAttributeValue(Attributes.ATTACK_DAMAGE), 9.0, "attack damage with an Adamant sword");

		wearFullSet(player, null);
		helper.assertValueEqual(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 12.0, "toughness in Netherite");

		wearFullSet(player, Alloy.ADAMANT);
		helper.assertValueEqual(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 16.0, "toughness in Adamant");
		helper.succeed();
	}

	@GameTest
	public void orichalcumGearHasNetheritesStats(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		equip(player, Alloy.ORICHALCUM, GearKind.SWORD);
		wearFullSet(player, Alloy.ORICHALCUM);

		helper.assertValueEqual(player.getAttributeValue(Attributes.ATTACK_DAMAGE), 8.0, "attack damage");
		helper.assertValueEqual(round(player.getAttributeValue(Attributes.ATTACK_SPEED)), 1.6, "attack speed");
		helper.assertValueEqual(player.getAttributeValue(Attributes.ARMOR), 20.0, "armor");
		helper.assertValueEqual(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 12.0, "toughness");
		helper.succeed();
	}

	@GameTest
	public void alloyHorseArmorGivesItsTraitsToTheHorse(GameTestHelper helper) {
		Horse shakudoHorse = helper.spawn(EntityTypes.HORSE, new BlockPos(1, 2, 1));
		Horse adamantHorse = helper.spawn(EntityTypes.HORSE, new BlockPos(3, 2, 1));

		shakudoHorse.setItemSlot(EquipmentSlot.BODY, new ItemStack(AlloyGear.of(Alloy.SHAKUDO, GearKind.HORSE_ARMOR)));
		adamantHorse.setItemSlot(EquipmentSlot.BODY, new ItemStack(AlloyGear.of(Alloy.ADAMANT, GearKind.HORSE_ARMOR)));

		helper.succeedWhen(() -> {
			double baseSpeed = shakudoHorse.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);

			helper.assertValueEqual(round(shakudoHorse.getAttributeValue(Attributes.MOVEMENT_SPEED)), round(baseSpeed * 1.025), "the Shakudo horse's speed");
			helper.assertValueEqual(adamantHorse.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 4.0, "the Adamant horse's toughness");
		});
	}

	private static int maxDamage(Alloy alloy, GearKind kind) {
		return new ItemStack(AlloyGear.of(alloy, kind)).getMaxDamage();
	}

	/** Holds the Alloy's version of the gear, or the Netherite one when {@code alloy} is null, and applies it. */
	private static void equip(ServerPlayer player, Alloy alloy, GearKind kind) {
		Item item = alloy == null ? kind.netheriteItem() : AlloyGear.of(alloy, kind);

		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
		player.doTick();
	}

	/** Wears the Alloy's full armor set, or Netherite's when {@code alloy} is null, and applies it. */
	private static void wearFullSet(ServerPlayer player, Alloy alloy) {
		for (GearKind kind : AlloySets.PIECES) {
			Item item = alloy == null ? kind.netheriteItem() : AlloyGear.of(alloy, kind);

			player.setItemSlot(kind.slot(), new ItemStack(item));
		}

		player.doTick();
	}

	// Attribute sums pick up floating-point noise, such as 0.1 × 1.1.
	private static double round(double value) {
		return Math.round(value * 10_000) / 10_000.0;
	}

	private static ItemStack assemble(GameTestHelper helper, ItemStack template, ItemStack base, ItemStack addition) {
		ServerLevel level = helper.getLevel();
		SmithingRecipeInput input = new SmithingRecipeInput(template, base, addition);

		return level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level)
			.map(recipe -> recipe.value().assemble(input))
			.orElse(ItemStack.EMPTY);
	}

	private static SmithingMenu smithing(GameTestHelper helper, ServerPlayer player, ItemStack template, ItemStack base, ItemStack addition) {
		helper.setBlock(STATION_POS, Blocks.SMITHING_TABLE);

		SmithingMenu smithing = new SmithingMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(STATION_POS)));

		smithing.getSlot(SmithingMenu.TEMPLATE_SLOT).set(template);
		smithing.getSlot(SmithingMenu.BASE_SLOT).set(base);
		smithing.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(addition);

		return smithing;
	}

	private static ServerPlayer survivalPlayer(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		player.setGameMode(GameType.SURVIVAL);

		return player;
	}
}
