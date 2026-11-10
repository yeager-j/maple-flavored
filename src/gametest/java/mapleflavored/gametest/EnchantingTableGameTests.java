package mapleflavored.gametest;

import static mapleflavored.gametest.Advancements.isDone;
import static mapleflavored.gametest.EnchantingTables.TABLE_POS;
import static mapleflavored.gametest.EnchantingTables.UPPER_RING;
import static mapleflavored.gametest.EnchantingTables.apple;
import static mapleflavored.gametest.EnchantingTables.assertRefuses;
import static mapleflavored.gametest.EnchantingTables.assertSaves;
import static mapleflavored.gametest.EnchantingTables.fill;
import static mapleflavored.gametest.EnchantingTables.fullLibraryBooks;
import static mapleflavored.gametest.EnchantingTables.fullLibraryLevels;
import static mapleflavored.gametest.EnchantingTables.gear;
import static mapleflavored.gametest.EnchantingTables.grindstone;
import static mapleflavored.gametest.EnchantingTables.placeChiseledShelf;
import static mapleflavored.gametest.EnchantingTables.placeTable;
import static mapleflavored.gametest.EnchantingTables.row;
import static mapleflavored.gametest.EnchantingTables.shelves;
import static mapleflavored.gametest.EnchantingTables.survivalPlayerWithLevels;
import static mapleflavored.gametest.EnchantingTables.table;
import static mapleflavored.gametest.EnchantingTables.toggle;
import static mapleflavored.gametest.TestItems.book;
import static mapleflavored.gametest.TestItems.enchantment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import mapleflavored.MapleFlavored;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.GearKind;
import mapleflavored.enchanting.AppleChecklist;
import mapleflavored.enchanting.AppleEffect;
import mapleflavored.enchanting.Checklist;
import mapleflavored.enchanting.EmptyChecklist;
import mapleflavored.enchanting.EnchantmentChecklist;
import mapleflavored.enchanting.EnchantingDeposit;
import mapleflavored.enchanting.EnchantingTableMenu;
import mapleflavored.enchanting.Library;
import mapleflavored.enchanting.LibraryCheckpoint;
import mapleflavored.experience.ExperiencePoints;
import mapleflavored.zephyr.Zephyr;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;

/**
 * The Enchanting Table reads its Library from the Chiseled Bookshelves around it and works as a checklist: Save applies
 * exactly the chosen enchantments at the Library's levels, within the player's Enchantment Slots, for lapis and a
 * deposit the grindstone hands back. A refused save changes nothing.
 */
public class EnchantingTableGameTests {
	private static final Identifier SHELVE_ADVANCEMENT = MapleFlavored.id("story/shelve_enchanted_book");
	private static final Identifier ENCHANTER = Identifier.withDefaultNamespace("story/enchant_item");
	private static final Identifier FULL_LIBRARY_ADVANCEMENT = MapleFlavored.id("story/full_library");
	private static final ResourceKey<Enchantment> BLIGHT = ResourceKey.create(Registries.ENCHANTMENT, MapleFlavored.id("blight"));
	// The first menu slot after the table's own shows the player's first main inventory slot, 9.
	private static final int FIRST_INVENTORY_MENU_SLOT = 3;
	private static final int FIRST_MAIN_INVENTORY_SLOT = 9;

	@GameTest
	public void fifteenShelvesOfEitherKindPowerTheLibrary(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		List<BlockPos> plainShelves = UPPER_RING.subList(0, 14);

		placeTable(helper, plainShelves, List.of(List.of(book(sharpness, 2))));

		EnchantingTableMenu table = table(helper, survivalPlayerWithLevels(helper, 0));

		helper.assertValueEqual(table.offers(), Map.of(sharpness, 2), "offers with 15 shelves");

		helper.setBlock(TABLE_POS.offset(plainShelves.getFirst()), Blocks.AIR);
		table.broadcastChanges();

		helper.assertValueEqual(table.offers(), Map.of(), "offers with 14 shelves");
		helper.succeed();
	}

	@GameTest
	public void theLibraryOffersTheHighestShelvedLevel(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		Holder<Enchantment> smite = enchantment(helper, Enchantments.SMITE);
		Holder<Enchantment> mending = enchantment(helper, Enchantments.MENDING);

		placeTable(helper, UPPER_RING, List.of(
			List.of(book(sharpness, 1), book(sharpness, 3)),
			List.of(book(smite, 1), book(mending, 1))
		));

		helper.assertValueEqual(
			table(helper, survivalPlayerWithLevels(helper, 0)).offers(), Map.of(sharpness, 3, smite, 1), "offers (never Mending)"
		);
		helper.succeed();
	}

	@GameTest
	public void aShelfCutOffFromTheTableIsNotInTheLibrary(GameTestHelper helper) {
		Holder<Enchantment> smite = enchantment(helper, Enchantments.SMITE);
		BlockPos chiseledOffset = new BlockPos(2, 0, 0);

		placeTable(helper, UPPER_RING, List.of());
		placeChiseledShelf(helper, TABLE_POS.offset(chiseledOffset), List.of(book(smite, 1)));

		EnchantingTableMenu table = table(helper, survivalPlayerWithLevels(helper, 0));

		helper.assertValueEqual(table.offers(), Map.of(smite, 1), "offers with the gap open");

		helper.setBlock(TABLE_POS.offset(1, 0, 0), Blocks.STONE);
		table.broadcastChanges();

		helper.assertValueEqual(table.offers(), Map.of(), "offers with the gap blocked");
		helper.succeed();
	}

	@GameTest
	public void aSaveAppliesTheChecklistForLapisAndADeposit(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		List<BlockPos> chiseled = placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 3))));
		ServerPlayer player = survivalPlayerWithLevels(helper, 30);
		EnchantingTableMenu table = table(helper, player);
		List<ItemStack> shelvedBefore = shelvedBooks(helper, chiseled);

		fill(table, new ItemStack(Items.DIAMOND_SWORD), 1, new ItemStack(Items.DIAMOND));
		toggle(helper, table, player, sharpness);

		assertSaves(helper, table, player, "Sharpness on a diamond sword");

		ItemStack sword = table.getSlot(EnchantingTableMenu.ITEM_SLOT).getItem();

		helper.assertValueEqual(sword.getEnchantments().getLevel(sharpness), 3, "Sharpness level");
		helper.assertTrue(table.getSlot(EnchantingTableMenu.LAPIS_SLOT).getItem().isEmpty(), "the lapis is used");
		helper.assertTrue(table.getSlot(EnchantingTableMenu.MATERIAL_SLOT).getItem().isEmpty(), "the diamond is used");
		helper.assertTrue(ItemStack.matches(EnchantingDeposit.held(sword), new ItemStack(Items.DIAMOND)), "the deposit is the diamond");
		helper.assertValueEqual(gear(helper, table).state(sharpness), EnchantmentChecklist.State.CARRIED, "Sharpness after saving");
		assertSameBooks(helper, shelvedBefore, shelvedBooks(helper, chiseled));
		helper.assertTrue(isDone(helper, player, ENCHANTER), "saving grants Enchanter");
		helper.succeed();
	}

	@GameTest
	public void incompatibleChoicesAreRefused(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		Holder<Enchantment> smite = enchantment(helper, Enchantments.SMITE);
		ServerPlayer player = survivalPlayerWithLevels(helper, 60);
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);

		sword.enchant(sharpness, 1);
		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 1), book(smite, 1))));

		EnchantingTableMenu table = table(helper, player);

		fill(table, sword, 2, ItemStack.EMPTY);
		toggle(helper, table, player, smite);

		assertRefuses(helper, table, player, new EnchantmentChecklist.Incompatible(sharpness, smite), "Sharpness with Smite");
		helper.succeed();
	}

	@GameTest
	public void levelSetsTheEnchantmentSlots(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
		Holder<Enchantment> looting = enchantment(helper, Enchantments.LOOTING);

		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 1), book(unbreaking, 1), book(looting, 1))));

		assertSlotLimit(helper, 29, List.of(sharpness), 0);
		assertSlotLimit(helper, 30, List.of(sharpness), 1);
		assertSlotLimit(helper, 30, List.of(sharpness, unbreaking), 1);
		assertSlotLimit(helper, 60, List.of(sharpness, unbreaking), 2);
		assertSlotLimit(helper, 89, List.of(sharpness, unbreaking, looting), 2);
		assertSlotLimit(helper, 90, List.of(sharpness, unbreaking, looting), 3);
		helper.succeed();
	}

	@GameTest
	public void goldGearGetsAnExtraSlot(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
		Holder<Enchantment> looting = enchantment(helper, Enchantments.LOOTING);

		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 1), book(unbreaking, 1), book(looting, 1))));

		assertGoldSave(helper, 0, List.of(sharpness), 1);
		assertGoldSave(helper, 30, List.of(sharpness, unbreaking), 2);
		assertGoldSave(helper, 30, List.of(sharpness, unbreaking, looting), 2);
		helper.succeed();
	}

	@GameTest
	public void electrumGearGetsAnExtraSlot(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		Holder<Enchantment> protection = enchantment(helper, Enchantments.PROTECTION);
		Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);

		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 1), book(protection, 1), book(unbreaking, 1))));

		List<Holder<Enchantment>> swordChoices = List.of(sharpness, unbreaking);
		Item electrumSword = AlloyGear.of(Alloy.ELECTRUM, GearKind.SWORD);
		Item electrumChestplate = AlloyGear.of(Alloy.ELECTRUM, GearKind.CHESTPLATE);
		Item steelSword = AlloyGear.of(Alloy.STEEL, GearKind.SWORD);

		assertChoicesSave(helper, 30, new ItemStack(electrumSword), new ItemStack(Alloy.ELECTRUM.ingot()), swordChoices, 2);
		assertChoicesSave(helper, 30, new ItemStack(electrumChestplate), new ItemStack(Alloy.ELECTRUM.ingot()), List.of(protection, unbreaking), 2);
		assertChoicesSave(helper, 30, new ItemStack(Items.NETHERITE_SWORD), new ItemStack(Items.NETHERITE_INGOT), swordChoices, 1);
		assertChoicesSave(helper, 30, new ItemStack(steelSword), new ItemStack(Alloy.STEEL.ingot()), swordChoices, 1);
		helper.succeed();
	}

	@GameTest
	public void alloyGearsDepositIsItsOwnIngot(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		ServerPlayer player = survivalPlayerWithLevels(helper, 30);
		Item steelSword = AlloyGear.of(Alloy.STEEL, GearKind.SWORD);

		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 1))));

		EnchantingTableMenu table = table(helper, player);

		fill(table, new ItemStack(steelSword), 1, new ItemStack(Items.NETHERITE_INGOT));
		toggle(helper, table, player, sharpness);
		assertRefuses(helper, table, player, new Checklist.NeedsMaterial(new ItemStack(Alloy.STEEL.ingot()).getHoverName()), "Sharpness on a Steel sword paid with a Netherite Ingot");

		ItemStack saved = save(helper, player, new ItemStack(steelSword), new ItemStack(Alloy.STEEL.ingot()), sharpness);

		helper.assertTrue(ItemStack.matches(EnchantingDeposit.held(saved), new ItemStack(Alloy.STEEL.ingot())), "the deposit is a Steel ingot");
		assertGrindingRefunds(helper, player, saved, Alloy.STEEL.ingot(), 1);
		helper.succeed();
	}

	@GameTest
	public void anEnchantmentAboveTheLibraryMustBeLowered(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		ServerPlayer player = survivalPlayerWithLevels(helper, 30);
		ItemStack sword = new ItemStack(Items.IRON_SWORD);

		sword.enchant(sharpness, 5);
		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 3))));

		EnchantingTableMenu table = table(helper, player);

		fill(table, sword, 1, new ItemStack(Items.IRON_INGOT));

		helper.assertValueEqual(gear(helper, table).state(sharpness), EnchantmentChecklist.State.CARRIED, "Sharpness on insertion");
		helper.assertValueEqual(gear(helper, table).level(sharpness), 5, "Sharpness keeps its level");
		assertRefuses(helper, table, player, new EnchantmentChecklist.NotInLibrary(sharpness, 5, 3), "Sharpness V from a Sharpness III Library");

		toggle(helper, table, player, sharpness);
		toggle(helper, table, player, sharpness);
		assertSaves(helper, table, player, "Sharpness lowered to III");

		ItemStack saved = table.getSlot(EnchantingTableMenu.ITEM_SLOT).getItem();

		helper.assertValueEqual(saved.getEnchantments().getLevel(sharpness), 3, "Sharpness after lowering");
		helper.assertTrue(table.getSlot(EnchantingTableMenu.LAPIS_SLOT).getItem().isEmpty(), "the lapis is used");
		helper.assertValueEqual(table.getSlot(EnchantingTableMenu.MATERIAL_SLOT).getItem().getCount(), 1, "an enchanted item pays no deposit");
		helper.assertTrue(EnchantingDeposit.held(saved).isEmpty(), "no deposit is stored");
		helper.succeed();
	}

	@GameTest
	public void anEnchantmentBelowTheLibraryRisesToIt(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		ServerPlayer player = survivalPlayerWithLevels(helper, 30);
		ItemStack sword = new ItemStack(Items.IRON_SWORD);

		sword.enchant(sharpness, 3);
		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 5))));

		EnchantingTableMenu table = table(helper, player);

		fill(table, sword, 1, ItemStack.EMPTY);
		assertSaves(helper, table, player, "Sharpness III at a Sharpness V Library");

		helper.assertValueEqual(table.getSlot(EnchantingTableMenu.ITEM_SLOT).getItem().getEnchantments().getLevel(sharpness), 5, "Sharpness");
		helper.succeed();
	}

	@GameTest
	public void aSavedEnchantmentIsCarriedEvenWhenTheItemIsUnchanged(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		ServerPlayer player = survivalPlayerWithLevels(helper, 30);
		ItemStack sword = new ItemStack(Items.IRON_SWORD);

		sword.enchant(sharpness, 3);

		BlockPos shelf = placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 3), book(sharpness, 1)))).getFirst();
		EnchantingTableMenu table = table(helper, player);

		fill(table, sword, 2, ItemStack.EMPTY);
		toggle(helper, table, player, sharpness);
		toggle(helper, table, player, sharpness);
		assertSaves(helper, table, player, "Sharpness III saved as it was");

		helper.assertValueEqual(gear(helper, table).state(sharpness), EnchantmentChecklist.State.CARRIED, "Sharpness after saving");

		helper.getBlockEntity(shelf, ChiseledBookShelfBlockEntity.class).removeItem(0, 1);

		assertRefuses(helper, table, player, new EnchantmentChecklist.NotInLibrary(sharpness, 3, 1), "Sharpness III once the Library holds only Sharpness I");
		helper.succeed();
	}

	@GameTest
	public void aChoiceWhoseBookIsTakenCanBeUnticked(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
		ServerPlayer player = survivalPlayerWithLevels(helper, 60);
		BlockPos shelf = placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 1), book(unbreaking, 1)))).getFirst();
		EnchantingTableMenu table = table(helper, player);

		fill(table, new ItemStack(Items.IRON_SWORD), 2, new ItemStack(Items.IRON_INGOT));
		toggle(helper, table, player, sharpness);
		toggle(helper, table, player, unbreaking);
		helper.getBlockEntity(shelf, ChiseledBookShelfBlockEntity.class).removeItem(0, 1);

		assertRefuses(helper, table, player, new EnchantmentChecklist.NotInLibrary(sharpness, 1, 0), "Sharpness after its book is taken");
		helper.assertTrue(gear(helper, table).listed().contains(sharpness), "Sharpness is still listed");

		toggle(helper, table, player, sharpness);
		assertSaves(helper, table, player, "Unbreaking alone");

		ItemStack saved = table.getSlot(EnchantingTableMenu.ITEM_SLOT).getItem();

		helper.assertValueEqual(saved.getEnchantments().getLevel(sharpness), 0, "Sharpness");
		helper.assertValueEqual(saved.getEnchantments().getLevel(unbreaking), 1, "Unbreaking");
		helper.succeed();
	}

	@GameTest
	public void untickingAnEnchantmentRemovesIt(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
		ServerPlayer player = survivalPlayerWithLevels(helper, 60);
		ItemStack sword = new ItemStack(Items.IRON_SWORD);

		sword.enchant(sharpness, 1);
		sword.enchant(unbreaking, 1);
		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 1), book(unbreaking, 1))));

		EnchantingTableMenu table = table(helper, player);

		fill(table, sword, 1, ItemStack.EMPTY);
		toggle(helper, table, player, unbreaking);
		assertSaves(helper, table, player, "Sharpness without Unbreaking");

		ItemStack saved = table.getSlot(EnchantingTableMenu.ITEM_SLOT).getItem();

		helper.assertValueEqual(saved.getEnchantments().keySet(), Set.of(sharpness), "enchantments after saving");
		helper.assertTrue(table.getSlot(EnchantingTableMenu.LAPIS_SLOT).getItem().isEmpty(), "1 lapis is used");
		helper.succeed();
	}

	@GameTest
	public void usingTheTableOpensTheChecklist(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		ServerPlayer player = survivalPlayerWithLevels(helper, 30);

		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 1))));
		helper.useBlock(TABLE_POS, player);

		if (!(player.containerMenu instanceof EnchantingTableMenu table)) {
			throw helper.assertionException(Component.literal("the table opened " + player.containerMenu));
		}

		fill(table, new ItemStack(Items.IRON_SWORD), 1, new ItemStack(Items.IRON_INGOT));
		toggle(helper, table, player, sharpness);
		assertSaves(helper, table, player, "Sharpness through the opened table");
		helper.succeed();
	}

	@GameTest
	public void playersShareALibraryButNotTheirSlots(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
		ServerPlayer enchanter = survivalPlayerWithLevels(helper, 60);
		ServerPlayer friend = survivalPlayerWithLevels(helper, 30);

		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 2), book(unbreaking, 3))));

		EnchantingTableMenu enchanterTable = table(helper, enchanter);
		EnchantingTableMenu friendTable = table(helper, friend);

		helper.assertValueEqual(
			friendTable.offers(), enchanterTable.offers(), "both players' offers"
		);

		for (EnchantingTableMenu table : List.of(enchanterTable, friendTable)) {
			ServerPlayer player = table == enchanterTable ? enchanter : friend;

			fill(table, new ItemStack(Items.IRON_SWORD), 2, new ItemStack(Items.IRON_INGOT));
			toggle(helper, table, player, sharpness);
			toggle(helper, table, player, unbreaking);
		}

		assertSaves(helper, enchanterTable, enchanter, "two enchantments at level 60");
		assertRefuses(helper, friendTable, friend, new Checklist.TooMany(2, 1), "two enchantments at level 30");

		toggle(helper, friendTable, friend, unbreaking);
		assertSaves(helper, friendTable, friend, "one enchantment at level 30");
		helper.succeed();
	}

	@GameTest
	public void savingNeedsAChoiceAndItsPayment(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		ServerPlayer player = survivalPlayerWithLevels(helper, 30);

		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 1))));

		EnchantingTableMenu table = table(helper, player);

		fill(table, new ItemStack(Items.IRON_SWORD), 1, new ItemStack(Items.IRON_INGOT));
		assertRefuses(helper, table, player, new EnchantmentChecklist.NothingSelected(), "nothing chosen");

		toggle(helper, table, player, sharpness);
		table.getSlot(EnchantingTableMenu.LAPIS_SLOT).set(ItemStack.EMPTY);
		assertRefuses(helper, table, player, new Checklist.NeedsLapis(1), "no lapis");

		table.getSlot(EnchantingTableMenu.LAPIS_SLOT).set(new ItemStack(Items.LAPIS_LAZULI));
		table.getSlot(EnchantingTableMenu.MATERIAL_SLOT).set(ItemStack.EMPTY);
		assertRefuses(helper, table, player, new Checklist.NeedsMaterial(new ItemStack(Items.IRON_INGOT).getHoverName()), "no material");

		table.getSlot(EnchantingTableMenu.MATERIAL_SLOT).set(new ItemStack(Items.GOLD_INGOT));
		assertRefuses(helper, table, player, new Checklist.NeedsMaterial(new ItemStack(Items.IRON_INGOT).getHoverName()), "the wrong material");

		table.getSlot(EnchantingTableMenu.MATERIAL_SLOT).set(new ItemStack(Items.IRON_INGOT));
		assertSaves(helper, table, player, "paid in full");
		helper.succeed();
	}

	@GameTest
	public void theTableTakesOnlyEnchantableGearAndGoldenApples(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		EnchantingTableMenu table = table(helper, survivalPlayerWithLevels(helper, 0));

		placeTable(helper, UPPER_RING, List.of());

		for (Item refused : List.of(Items.BOOK, Items.APPLE, Items.DIRT)) {
			helper.assertFalse(table.getSlot(EnchantingTableMenu.ITEM_SLOT).mayPlace(new ItemStack(refused)), refused + " is accepted");
		}

		helper.assertFalse(table.getSlot(EnchantingTableMenu.ITEM_SLOT).mayPlace(book(sharpness, 1)), "an enchanted book is accepted");

		List<Item> accepted = List.of(
			Items.DIAMOND_SWORD, Items.SHIELD, Items.SHEARS, Items.ELYTRA, Items.FLINT_AND_STEEL, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE
		);

		for (Item item : accepted) {
			helper.assertTrue(table.getSlot(EnchantingTableMenu.ITEM_SLOT).mayPlace(new ItemStack(item)), item + " is refused");
		}

		helper.succeed();
	}

	@GameTest
	public void creativePlayersPayNothing(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
		ServerPlayer player = MockPlayers.creativePlayer(helper);

		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 1), book(unbreaking, 1))));

		EnchantingTableMenu table = table(helper, player);

		fill(table, new ItemStack(Items.IRON_SWORD), 0, ItemStack.EMPTY);
		toggle(helper, table, player, sharpness);
		toggle(helper, table, player, unbreaking);
		assertSaves(helper, table, player, "two enchantments in creative at level 0");

		helper.assertTrue(EnchantingDeposit.held(table.getSlot(EnchantingTableMenu.ITEM_SLOT).getItem()).isEmpty(), "no deposit is stored");
		helper.succeed();
	}

	@GameTest
	public void onlyListedEnchantmentsToggle(GameTestHelper helper) {
		Holder<Enchantment> protection = enchantment(helper, Enchantments.PROTECTION);
		ServerPlayer player = survivalPlayerWithLevels(helper, 30);

		placeTable(helper, UPPER_RING, List.of(List.of(book(protection, 1))));

		EnchantingTableMenu table = table(helper, player);

		fill(table, new ItemStack(Items.IRON_SWORD), 1, ItemStack.EMPTY);

		boolean toggled = table.clickMenuButton(player, EnchantingTableMenu.toggleButton(row(helper, protection)));

		helper.assertFalse(toggled, "Protection toggles on a sword");
		helper.assertValueEqual(gear(helper, table).state(protection), EnchantmentChecklist.State.UNSELECTED, "Protection");
		helper.succeed();
	}

	@GameTest
	public void theGrindstoneHandsBackTheDeposit(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
		ServerPlayer player = survivalPlayerWithLevels(helper, 60);

		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 1), book(unbreaking, 1))));

		ItemStack ironSword = save(helper, player, new ItemStack(Items.IRON_SWORD), new ItemStack(Items.IRON_INGOT), sharpness);
		ItemStack woodenSword = save(helper, player, new ItemStack(Items.WOODEN_SWORD), new ItemStack(Items.OAK_PLANKS), sharpness);
		ItemStack enchantedSword = new ItemStack(Items.IRON_SWORD);

		enchantedSword.enchant(sharpness, 1);
		enchantedSword = save(helper, player, enchantedSword, ItemStack.EMPTY, unbreaking);

		int pointsBefore = ExperiencePoints.total(player);

		assertGrindingRefunds(helper, player, ironSword, Items.IRON_INGOT, 1);
		assertGrindingRefunds(helper, player, woodenSword, Items.OAK_PLANKS, 1);
		assertGrindingRefunds(helper, player, enchantedSword, Items.IRON_INGOT, 0);
		helper.assertValueEqual(player.getInventory().countItem(Items.LAPIS_LAZULI), 0, "lapis handed back");
		helper.assertValueEqual(ExperiencePoints.total(player), pointsBefore, "points after grinding");
		helper.succeed();
	}

	@GameTest
	public void shelvingABookInALibraryGrantsTheAdvancement(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		List<BlockPos> libraryShelves = placeTable(helper, UPPER_RING, List.of(List.of(), List.of()));
		BlockPos looseShelf = new BlockPos(7, 1, 7);

		placeChiseledShelf(helper, looseShelf, List.of());

		helper.assertFalse(shelve(helper, looseShelf, book(sharpness, 1)), "a shelf outside any Library grants it");
		helper.assertFalse(shelve(helper, libraryShelves.get(0), new ItemStack(Items.BOOK)), "a plain book grants it");
		helper.assertTrue(shelve(helper, libraryShelves.get(1), book(sharpness, 1)), "an enchanted book in a Library grants it");

		AdvancementHolder enchanter = helper.getLevel().getServer().getAdvancements().get(ENCHANTER);

		helper.assertValueEqual(enchanter.value().parent(), Optional.of(SHELVE_ADVANCEMENT), "Enchanter's parent");
		helper.succeed();
	}

	@GameTest
	public void aFullLibraryIsFortyTwoEnchantmentsWorth130Levels(GameTestHelper helper) {
		Registry<Enchantment> enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

		helper.assertValueEqual(fullLibraryBooks(helper).size(), 42, "enchantments in a Full Library");
		helper.assertValueEqual(Library.fullLevels(enchantments), 130, "levels in a Full Library");
		helper.succeed();
	}

	@GameTest
	public void progressCountsTheHighestLevelOfEachEnchantment(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		Holder<Enchantment> smite = enchantment(helper, Enchantments.SMITE);
		Holder<Enchantment> mending = enchantment(helper, Enchantments.MENDING);

		assertProgress(helper, List.of(book(sharpness, 1), book(sharpness, 1)), 1, "two Sharpness I books");
		assertProgress(helper, List.of(book(sharpness, 1), book(sharpness, 2)), 2, "Sharpness I and II");
		assertProgress(helper, List.of(book(sharpness, 1), book(smite, 3), book(mending, 1)), 4, "Sharpness I, Smite III and Mending");
		assertProgress(helper, List.of(book(sharpness, 7)), 5, "Sharpness VII");
		helper.succeed();
	}

	@GameTest
	public void checkpointsUnlockAtTheirShareOfAFullLibrary(GameTestHelper helper) {
		Library.Progress empty = new Library.Progress(0, 130);
		List<Integer> percents = LibraryCheckpoint.ALL.stream().map(LibraryCheckpoint::percent).toList();

		helper.assertValueEqual(empty.levelsFor(15), 20, "levels for 15%");
		helper.assertValueEqual(empty.levelsFor(100), 130, "levels for 100%");
		helper.assertFalse(new Library.Progress(129, 130).reached(LibraryCheckpoint.MENDING.percent()), "Mending is reached at 129 levels");
		helper.assertTrue(new Library.Progress(130, 130).reached(LibraryCheckpoint.MENDING.percent()), "Mending is reached at 130 levels");
		helper.assertValueEqual(percents, List.of(15, 30, 50, 75, 100), "checkpoints: the four apple effects, then Mending");
		helper.succeed();
	}

	@GameTest
	public void aFullLibraryOffersMending(GameTestHelper helper) {
		Holder<Enchantment> mending = enchantment(helper, Enchantments.MENDING);
		placeTable(helper, UPPER_RING, shelves(fullLibraryBooks(helper)));

		EnchantingTableMenu table = table(helper, survivalPlayerWithLevels(helper, 0));
		Map<Holder<Enchantment>, Integer> expectedOffers = new HashMap<>(fullLibraryLevels(helper));

		expectedOffers.put(mending, 1);

		helper.assertValueEqual(table.library().progress(), new Library.Progress(130, 130), "progress");
		helper.assertValueEqual(table.offers(), expectedOffers, "offers");
		helper.succeed();
	}

	@GameTest
	public void everyOtherEnchantmentMustBeAtItsMaxLevel(GameTestHelper helper) {
		Holder<Enchantment> mending = enchantment(helper, Enchantments.MENDING);
		Holder<Enchantment> blight = enchantment(helper, BLIGHT);
		Holder<Enchantment> zephyr = enchantment(helper, Zephyr.ENCHANTMENT);
		List<ItemStack> blightOne = fullLibraryBooks(helper).stream()
			.map(book -> holds(book, blight) ? book(blight, 1) : book)
			.toList();
		List<ItemStack> noZephyr = fullLibraryBooks(helper).stream()
			.filter(book -> !holds(book, zephyr))
			.toList();

		placeTable(helper, UPPER_RING, shelves(blightOne));

		EnchantingTableMenu table = table(helper, survivalPlayerWithLevels(helper, 0));

		helper.assertValueEqual(table.library().progress(), new Library.Progress(129, 130), "progress with Blight I");
		helper.assertFalse(table.offers().containsKey(mending), "Mending is offered with Blight I");

		placeTable(helper, UPPER_RING, shelves(noZephyr));
		table.broadcastChanges();

		helper.assertValueEqual(table.library().progress(), new Library.Progress(127, 130), "progress without Zephyr");
		helper.assertFalse(table.offers().containsKey(mending), "Mending is offered without Zephyr");
		helper.succeed();
	}

	@GameTest
	public void mendingFollowsTheBooks(GameTestHelper helper) {
		Holder<Enchantment> mending = enchantment(helper, Enchantments.MENDING);
		Holder<Enchantment> zephyr = enchantment(helper, Zephyr.ENCHANTMENT);
		List<BlockPos> chiseled = placeTable(helper, UPPER_RING, shelves(fullLibraryBooks(helper)));
		EnchantingTableMenu table = table(helper, survivalPlayerWithLevels(helper, 0));
		ChiseledBookShelfBlockEntity zephyrShelf = null;
		int zephyrSlot = -1;

		for (BlockPos pos : chiseled) {
			ChiseledBookShelfBlockEntity shelf = helper.getBlockEntity(pos, ChiseledBookShelfBlockEntity.class);

			for (int slot = 0; slot < shelf.getContainerSize(); slot++) {
				if (holds(shelf.getItem(slot), zephyr)) {
					zephyrShelf = shelf;
					zephyrSlot = slot;
				}
			}
		}

		helper.assertTrue(zephyrShelf != null, "the Zephyr book is shelved");
		helper.assertTrue(table.offers().containsKey(mending), "Mending is offered when Full");

		ItemStack zephyrBook = zephyrShelf.removeItem(zephyrSlot, 1);

		table.broadcastChanges();
		helper.assertFalse(table.offers().containsKey(mending), "Mending is offered once the Zephyr book leaves");

		zephyrShelf.setItem(zephyrSlot, zephyrBook);
		table.broadcastChanges();
		helper.assertTrue(table.offers().containsKey(mending), "Mending is offered once the Zephyr book returns");
		helper.succeed();
	}

	@GameTest
	public void aFullLibraryWithoutPowerOffersNothingButStillCounts(GameTestHelper helper) {
		placeTable(helper, List.of(), shelves(fullLibraryBooks(helper)));

		ServerPlayer player = survivalPlayerWithLevels(helper, 0);

		helper.useBlock(TABLE_POS, player);

		if (!(player.containerMenu instanceof EnchantingTableMenu table)) {
			throw helper.assertionException(Component.literal("the table opened " + player.containerMenu));
		}

		helper.assertValueEqual(table.library().progress(), new Library.Progress(130, 130), "progress with 7 shelves");
		helper.assertValueEqual(table.offers(), Map.of(), "offers with 7 shelves");
		helper.assertTrue(isDone(helper, player, FULL_LIBRARY_ADVANCEMENT), "a Full Library without power grants Well Read");
		helper.succeed();
	}

	@GameTest
	public void aFullLibraryLetsGearTakeAndKeepMending(GameTestHelper helper) {
		Holder<Enchantment> mending = enchantment(helper, Enchantments.MENDING);
		ServerPlayer player = survivalPlayerWithLevels(helper, 30);
		ItemStack carriedMending = new ItemStack(Items.IRON_SWORD);

		carriedMending.enchant(mending, 1);
		placeTable(helper, UPPER_RING, shelves(fullLibraryBooks(helper)));

		EnchantingTableMenu table = table(helper, player);

		fill(table, new ItemStack(Items.DIAMOND_SWORD), 1, new ItemStack(Items.DIAMOND));
		toggle(helper, table, player, mending);
		assertSaves(helper, table, player, "Mending from a Full Library");
		helper.assertValueEqual(table.getSlot(EnchantingTableMenu.ITEM_SLOT).getItem().getEnchantments().getLevel(mending), 1, "Mending level");

		fill(table, carriedMending, 1, ItemStack.EMPTY);
		assertSaves(helper, table, player, "carried Mending at a Full Library");
		helper.assertValueEqual(table.getSlot(EnchantingTableMenu.ITEM_SLOT).getItem().getEnchantments().getLevel(mending), 1, "kept Mending level");
		helper.succeed();
	}

	@GameTest
	public void gearKeepsMendingOnlyUntilSavedWithoutAFullLibrary(GameTestHelper helper) {
		Holder<Enchantment> mending = enchantment(helper, Enchantments.MENDING);
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		ServerPlayer player = survivalPlayerWithLevels(helper, 60);
		ItemStack sword = new ItemStack(Items.IRON_SWORD);

		sword.enchant(mending, 1);
		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 1))));

		EnchantingTableMenu table = table(helper, player);

		fill(table, sword, 1, ItemStack.EMPTY);

		helper.assertValueEqual(gear(helper, table).state(mending), EnchantmentChecklist.State.CARRIED, "Mending on the inserted sword");
		assertRefuses(helper, table, player, new EnchantmentChecklist.NotInLibrary(mending, 1, 0), "carried Mending without a Full Library");

		toggle(helper, table, player, mending);
		toggle(helper, table, player, sharpness);
		assertSaves(helper, table, player, "Sharpness instead of Mending");

		ItemEnchantments saved = table.getSlot(EnchantingTableMenu.ITEM_SLOT).getItem().getEnchantments();

		helper.assertValueEqual(saved.getLevel(mending), 0, "Mending after saving without a Full Library");
		helper.assertValueEqual(saved.getLevel(sharpness), 1, "Sharpness after saving");
		helper.succeed();
	}

	@GameTest
	public void usingAFullLibraryGrantsWellRead(GameTestHelper helper) {
		Holder<Enchantment> zephyr = enchantment(helper, Zephyr.ENCHANTMENT);
		List<ItemStack> noZephyr = fullLibraryBooks(helper).stream().filter(book -> !holds(book, zephyr)).toList();
		List<BlockPos> chiseled = placeTable(helper, UPPER_RING, shelves(noZephyr));
		ServerPlayer player = survivalPlayerWithLevels(helper, 0);
		EnchantingTableMenu table = table(helper, player);

		helper.assertFalse(isDone(helper, player, FULL_LIBRARY_ADVANCEMENT), "a Library one book short grants Well Read");

		helper.getBlockEntity(chiseled.getLast(), ChiseledBookShelfBlockEntity.class).setItem(5, book(zephyr, 3));
		table.broadcastChanges();

		helper.assertTrue(isDone(helper, player, FULL_LIBRARY_ADVANCEMENT), "completing the Library with the table open grants Well Read");
		helper.assertFalse(isDone(helper, survivalPlayerWithLevels(helper, 0), FULL_LIBRARY_ADVANCEMENT), "a player who never used the table has it");

		ServerPlayer secondPlayer = survivalPlayerWithLevels(helper, 0);

		table(helper, secondPlayer);
		helper.assertTrue(isDone(helper, secondPlayer, FULL_LIBRARY_ADVANCEMENT), "opening a Full table grants Well Read");

		AdvancementHolder wellRead = helper.getLevel().getServer().getAdvancements().get(FULL_LIBRARY_ADVANCEMENT);

		helper.assertValueEqual(wellRead.value().parent(), Optional.of(ENCHANTER), "Well Read's parent");
		helper.assertValueEqual(wellRead.value().display().map(DisplayInfo::type), Optional.of(AdvancementType.GOAL), "Well Read's frame");
		helper.succeed();
	}

	@GameTest
	public void aTableBelowFifteenBookshelvesSaysSo(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		ServerPlayer player = survivalPlayerWithLevels(helper, 30);
		ItemStack sword = new ItemStack(Items.IRON_SWORD);

		sword.enchant(sharpness, 5);
		placeTable(helper, List.of(), shelves(fullLibraryBooks(helper)));

		EnchantingTableMenu table = table(helper, player);
		Library.Status library = table.library();

		helper.assertValueEqual(library, new Library.Status(7, new Library.Progress(130, 130)), "the Library with 7 shelves");

		for (LibraryCheckpoint checkpoint : LibraryCheckpoint.ALL) {
			helper.assertFalse(library.unlocked(checkpoint), checkpoint.percent() + "% is unlocked with 7 shelves");
		}

		fill(table, sword, 1, ItemStack.EMPTY);
		assertRefuses(helper, table, player, new Checklist.NeedsBookshelves(7), "carried Sharpness with 7 shelves");
		helper.assertValueEqual(
			table.checklist().problems(), List.of(new Checklist.NeedsBookshelves(7)), "the one problem is the bookshelves, not the Library"
		);

		UPPER_RING.subList(0, 8).forEach(offset -> helper.setBlock(TABLE_POS.offset(offset), Blocks.BOOKSHELF));
		table.broadcastChanges();

		helper.assertTrue(table.library().unlocked(LibraryCheckpoint.MENDING), "Mending is unlocked with 15 shelves");
		assertSaves(helper, table, player, "carried Sharpness with 15 shelves");
		helper.succeed();
	}

	@GameTest
	public void anEmptyTableOnlyAsksForAnItem(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		ServerPlayer player = survivalPlayerWithLevels(helper, 30);

		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 1))));

		EnchantingTableMenu table = table(helper, player);

		fill(table, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_INGOT));

		helper.assertValueEqual(table.checklist().problems(), List.of(new EmptyChecklist.NoItem()), "an empty table's problems");
		helper.assertValueEqual(table.checklist().rows(), List.of(), "an empty table's rows");
		helper.assertFalse(table.clickMenuButton(player, EnchantingTableMenu.toggleButton(row(helper, sharpness))), "a row toggles with no item");
		assertRefuses(helper, table, player, new EmptyChecklist.NoItem(), "an empty table");
		helper.succeed();
	}

	@GameTest
	public void theItemDecidesWhatAButtonToggles(GameTestHelper helper) {
		ServerPlayer player = survivalPlayerWithLevels(helper, 30);

		placeTable(helper, UPPER_RING, shelves(fullLibraryBooks(helper)));

		EnchantingTableMenu table = table(helper, player);
		int regenerationButton = EnchantingTableMenu.toggleButton(AppleChecklist.row(AppleEffect.REGENERATION));

		fill(table, new ItemStack(Items.GOLDEN_APPLE), 1, new ItemStack(Items.GOLD_BLOCK));
		helper.assertTrue(table.clickMenuButton(player, regenerationButton), "Regeneration toggles on an apple");
		helper.assertValueEqual(apple(helper, table).chosen(), Set.of(AppleEffect.REGENERATION), "the apple's choices");

		table.getSlot(EnchantingTableMenu.ITEM_SLOT).set(new ItemStack(Items.IRON_SWORD));

		helper.assertValueEqual(gear(helper, table).chosen(), List.of(), "a sword's choices after an apple's");
		helper.succeed();
	}

	@GameTest
	public void shiftClickingTheItemsMaterialFillsTheMaterialSlot(GameTestHelper helper) {
		ServerPlayer player = survivalPlayerWithLevels(helper, 30);

		placeTable(helper, UPPER_RING, List.of());

		EnchantingTableMenu table = table(helper, player);

		fill(table, new ItemStack(Items.IRON_SWORD), 0, ItemStack.EMPTY);
		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT, new ItemStack(Items.GOLD_INGOT, 3));
		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT + 1, new ItemStack(Items.IRON_INGOT, 3));
		table.clicked(FIRST_INVENTORY_MENU_SLOT, 0, ContainerInput.QUICK_MOVE, player);

		helper.assertTrue(table.getSlot(EnchantingTableMenu.MATERIAL_SLOT).getItem().isEmpty(), "gold stays out of an iron sword's material slot");

		table.clicked(FIRST_INVENTORY_MENU_SLOT + 1, 0, ContainerInput.QUICK_MOVE, player);

		helper.assertTrue(
			ItemStack.matches(table.getSlot(EnchantingTableMenu.MATERIAL_SLOT).getItem(), new ItemStack(Items.IRON_INGOT, 3)),
			"iron moves to an iron sword's material slot"
		);
		helper.assertValueEqual(player.getInventory().countItem(Items.GOLD_INGOT), 3, "gold ingots left in the inventory");
		helper.succeed();
	}

	@GameTest
	public void theClientBuildsTheServersChecklist(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		Holder<Enchantment> smite = enchantment(helper, Enchantments.SMITE);
		ServerPlayer player = survivalPlayerWithLevels(helper, 30);
		ItemStack sword = new ItemStack(Items.IRON_SWORD);

		sword.enchant(sharpness, 5);
		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 3), book(smite, 2))));

		EnchantingTableMenu table = table(helper, player);

		assertClientMatches(helper, table, player, "an empty table");

		fill(table, sword, 1, new ItemStack(Items.GOLD_INGOT));
		toggle(helper, table, player, smite);
		assertClientMatches(helper, table, player, "a sword with problems");

		fill(table, new ItemStack(Items.GOLDEN_APPLE), 2, new ItemStack(Items.GOLD_BLOCK));
		assertClientMatches(helper, table, player, "an apple with nothing offered");

		placeTable(helper, List.of(), shelves(fullLibraryBooks(helper)));
		UPPER_RING.forEach(offset -> helper.setBlock(TABLE_POS.offset(offset), Blocks.AIR));
		table.broadcastChanges();
		assertClientMatches(helper, table, player, "an apple at a table with 7 shelves");

		UPPER_RING.forEach(offset -> helper.setBlock(TABLE_POS.offset(offset), Blocks.BOOKSHELF));
		table.broadcastChanges();
		toggle(helper, table, player, AppleEffect.ABSORPTION);
		assertClientMatches(helper, table, player, "an apple with an effect chosen");
		helper.succeed();
	}

	/**
	 * Sends the server menu's contents and data to a new client menu, the way opening the table does, and fails unless
	 * the client menu's checklist says what the server's does.
	 */
	private static void assertClientMatches(GameTestHelper helper, EnchantingTableMenu server, ServerPlayer player, String label) {
		EnchantingTableMenu client = new EnchantingTableMenu(0, player.getInventory());

		server.setSynchronizer(new ContainerSynchronizer() {
			@Override
			public void sendInitialData(AbstractContainerMenu menu, List<ItemStack> items, ItemStack carried, int[] data) {
				client.initializeContents(0, items, carried);

				for (int id = 0; id < data.length; id++) {
					client.setData(id, data[id]);
				}
			}

			@Override
			public void sendSlotChange(AbstractContainerMenu menu, int slot, ItemStack item) {
			}

			@Override
			public void sendCarriedChange(AbstractContainerMenu menu, ItemStack carried) {
			}

			@Override
			public void sendDataChange(AbstractContainerMenu menu, int id, int value) {
			}

			@Override
			public RemoteSlot createSlot() {
				return RemoteSlot.PLACEHOLDER;
			}
		});
		server.sendAllDataToRemote();

		Checklist expected = server.checklist();
		Checklist actual = client.checklist();

		helper.assertValueEqual(actual.getClass(), expected.getClass(), label + ": the client's kind of checklist");
		helper.assertValueEqual(actual.library(), expected.library(), label + ": the client's Library");
		helper.assertValueEqual(actual.rows(), expected.rows(), label + ": the client's rows");
		helper.assertValueEqual(actual.problems(), expected.problems(), label + ": the client's problems");
		helper.assertValueEqual(actual.cost(), expected.cost(), label + ": the client's cost");
		helper.assertValueEqual(actual.slots(), expected.slots(), label + ": the client's Enchantment Slots");
	}

	private static boolean holds(ItemStack book, Holder<Enchantment> enchantment) {
		return book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(enchantment) > 0;
	}

	private static void assertProgress(GameTestHelper helper, List<ItemStack> books, int levels, String label) {
		placeTable(helper, UPPER_RING, List.of(books));

		helper.assertValueEqual(table(helper, survivalPlayerWithLevels(helper, 0)).library().progress().levels(), levels, label);
	}

	private static List<ItemStack> shelvedBooks(GameTestHelper helper, List<BlockPos> shelves) {
		return shelves.stream()
			.flatMap(pos -> helper.getBlockEntity(pos, ChiseledBookShelfBlockEntity.class).getItems().stream())
			.map(ItemStack::copy)
			.toList();
	}

	private static void assertSameBooks(GameTestHelper helper, List<ItemStack> before, List<ItemStack> after) {
		helper.assertTrue(ItemStack.listMatches(before, after), "the shelved books are unchanged");
	}

	private static void assertSlotLimit(GameTestHelper helper, int level, List<Holder<Enchantment>> choices, int slots) {
		assertChoicesSave(helper, level, new ItemStack(Items.IRON_SWORD), new ItemStack(Items.IRON_INGOT), choices, slots);
	}

	private static void assertGoldSave(GameTestHelper helper, int level, List<Holder<Enchantment>> choices, int slots) {
		assertChoicesSave(helper, level, new ItemStack(Items.GOLDEN_SWORD), new ItemStack(Items.GOLD_INGOT), choices, slots);
	}

	/** Saves the choices at {@code level}, failing unless they save exactly when they fit the {@code slots}. */
	private static void assertChoicesSave(
		GameTestHelper helper, int level, ItemStack item, ItemStack material, List<Holder<Enchantment>> choices, int slots
	) {
		ServerPlayer player = survivalPlayerWithLevels(helper, level);
		EnchantingTableMenu table = table(helper, player);
		String label = choices.size() + " on " + item.getItem() + " at level " + level;

		fill(table, item, choices.size(), material);
		choices.forEach(enchantment -> toggle(helper, table, player, enchantment));

		if (choices.size() > slots) {
			assertRefuses(helper, table, player, new Checklist.TooMany(choices.size(), slots), label);

			return;
		}

		assertSaves(helper, table, player, label);
		helper.assertValueEqual(table.getSlot(EnchantingTableMenu.ITEM_SLOT).getItem().getEnchantments().size(), choices.size(), label);
	}

	/** Saves {@code item} with one more enchantment at the table, returning the saved item. */
	private static ItemStack save(GameTestHelper helper, ServerPlayer player, ItemStack item, ItemStack material, Holder<Enchantment> enchantment) {
		EnchantingTableMenu table = table(helper, player);

		fill(table, item, item.getEnchantments().size() + 1, material);
		toggle(helper, table, player, enchantment);
		assertSaves(helper, table, player, enchantment.getRegisteredName() + " on " + item.getItem());

		return table.getSlot(EnchantingTableMenu.ITEM_SLOT).getItem().copy();
	}

	private static void assertGrindingRefunds(GameTestHelper helper, ServerPlayer player, ItemStack item, Item deposit, int count) {
		int before = player.getInventory().countItem(deposit);

		GrindstoneMenu grindstone = grindstone(helper, player);

		grindstone.getSlot(GrindstoneMenu.INPUT_SLOT).set(item);
		grindstone.setCarried(ItemStack.EMPTY);
		grindstone.clicked(GrindstoneMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, player);

		ItemStack ground = grindstone.getCarried();
		String label = "grinding " + item.getItem();

		helper.assertTrue(ground.getEnchantments().isEmpty(), label + " strips it");
		helper.assertTrue(EnchantingDeposit.held(ground).isEmpty(), label + " removes the deposit");
		helper.assertValueEqual(player.getInventory().countItem(deposit) - before, count, label + " hands back " + deposit);
	}

	/** Shelves the book with a fresh player's click, returning whether that player earned the shelving advancement. */
	private static boolean shelve(GameTestHelper helper, BlockPos shelf, ItemStack book) {
		ServerPlayer player = survivalPlayerWithLevels(helper, 0);

		player.setItemInHand(InteractionHand.MAIN_HAND, book);
		helper.useBlock(shelf, player);
		helper.assertTrue(player.getMainHandItem().isEmpty(), "the book is shelved");

		return isDone(helper, player, SHELVE_ADVANCEMENT);
	}
}
