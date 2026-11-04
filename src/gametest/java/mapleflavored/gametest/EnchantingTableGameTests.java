package mapleflavored.gametest;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import mapleflavored.MapleFlavored;
import mapleflavored.enchanting.Checklist;
import mapleflavored.enchanting.EnchantingDeposit;
import mapleflavored.enchanting.EnchantingTableMenu;
import mapleflavored.enchanting.Library;
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
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
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
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;

/**
 * The Enchanting Table reads its Library from the Chiseled Bookshelves around it and works as a checklist: Save applies
 * exactly the chosen enchantments at the Library's levels, within the player's Enchantment Slots, for lapis and a
 * deposit the grindstone hands back. A refused save changes nothing.
 */
public class EnchantingTableGameTests {
	private static final BlockPos TABLE_POS = new BlockPos(3, 1, 3);
	private static final BlockPos GRINDSTONE_POS = new BlockPos(0, 1, 0);
	private static final List<BlockPos> LOWER_RING = ring(0);
	private static final List<BlockPos> UPPER_RING = ring(1);
	private static final Identifier SHELVE_ADVANCEMENT = MapleFlavored.id("story/shelve_enchanted_book");
	private static final Identifier ENCHANTER = Identifier.withDefaultNamespace("story/enchant_item");
	private static final Identifier FULL_LIBRARY_ADVANCEMENT = MapleFlavored.id("story/full_library");
	private static final ResourceKey<Enchantment> BLIGHT = ResourceKey.create(Registries.ENCHANTMENT, MapleFlavored.id("blight"));

	@GameTest
	public void fifteenShelvesOfEitherKindPowerTheLibrary(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		List<BlockPos> plainShelves = UPPER_RING.subList(0, 14);

		placeTable(helper, plainShelves, List.of(List.of(book(sharpness, 2))));

		EnchantingTableMenu table = table(helper, survivalPlayer(helper, 0));

		helper.assertValueEqual(table.checklist().offers(), Map.of(sharpness, 2), "offers with 15 shelves");

		helper.setBlock(TABLE_POS.offset(plainShelves.getFirst()), Blocks.AIR);
		table.broadcastChanges();

		helper.assertValueEqual(table.checklist().offers(), Map.of(), "offers with 14 shelves");
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
			table(helper, survivalPlayer(helper, 0)).checklist().offers(), Map.of(sharpness, 3, smite, 1), "offers (never Mending)"
		);
		helper.succeed();
	}

	@GameTest
	public void aShelfCutOffFromTheTableIsNotInTheLibrary(GameTestHelper helper) {
		Holder<Enchantment> smite = enchantment(helper, Enchantments.SMITE);
		BlockPos chiseledOffset = new BlockPos(2, 0, 0);

		placeTable(helper, UPPER_RING, List.of());
		placeChiseledShelf(helper, TABLE_POS.offset(chiseledOffset), List.of(book(smite, 1)));

		EnchantingTableMenu table = table(helper, survivalPlayer(helper, 0));

		helper.assertValueEqual(table.checklist().offers(), Map.of(smite, 1), "offers with the gap open");

		helper.setBlock(TABLE_POS.offset(1, 0, 0), Blocks.STONE);
		table.broadcastChanges();

		helper.assertValueEqual(table.checklist().offers(), Map.of(), "offers with the gap blocked");
		helper.succeed();
	}

	@GameTest
	public void aSaveAppliesTheChecklistForLapisAndADeposit(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		List<BlockPos> chiseled = placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 3))));
		ServerPlayer player = survivalPlayer(helper, 30);
		EnchantingTableMenu table = table(helper, player);
		List<ItemStack> shelvedBefore = shelvedBooks(helper, chiseled);

		fill(table, new ItemStack(Items.DIAMOND_SWORD), 1, new ItemStack(Items.DIAMOND));
		toggle(helper, table, player, sharpness);

		assertSaves(helper, table, player, "Sharpness on a diamond sword");

		ItemStack sword = table.getSlot(EnchantingTableMenu.ITEM_SLOT).getItem();

		helper.assertValueEqual(sword.getEnchantments().getLevel(sharpness), 3, "Sharpness level");
		helper.assertTrue(table.getSlot(EnchantingTableMenu.LAPIS_SLOT).getItem().isEmpty(), "the lapis is used");
		helper.assertTrue(table.getSlot(EnchantingTableMenu.MATERIAL_SLOT).getItem().isEmpty(), "the diamond is used");
		helper.assertTrue(ItemStack.matches(EnchantingDeposit.refund(sword), new ItemStack(Items.DIAMOND)), "the deposit is the diamond");
		helper.assertValueEqual(table.checklist().state(sharpness), Checklist.State.CARRIED, "Sharpness after saving");
		assertSameBooks(helper, shelvedBefore, shelvedBooks(helper, chiseled));
		helper.assertTrue(isDone(helper, player, ENCHANTER), "saving grants Enchanter");
		helper.succeed();
	}

	@GameTest
	public void incompatibleChoicesAreRefused(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		Holder<Enchantment> smite = enchantment(helper, Enchantments.SMITE);
		ServerPlayer player = survivalPlayer(helper, 60);
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);

		sword.enchant(sharpness, 1);
		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 1), book(smite, 1))));

		EnchantingTableMenu table = table(helper, player);

		fill(table, sword, 2, ItemStack.EMPTY);
		toggle(helper, table, player, smite);

		assertRefuses(helper, table, player, "Sharpness with Smite");
		helper.succeed();
	}

	@GameTest
	public void levelSetsTheEnchantmentSlots(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
		Holder<Enchantment> looting = enchantment(helper, Enchantments.LOOTING);

		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 1), book(unbreaking, 1), book(looting, 1))));

		assertSlotLimit(helper, 29, List.of(sharpness), false);
		assertSlotLimit(helper, 30, List.of(sharpness), true);
		assertSlotLimit(helper, 30, List.of(sharpness, unbreaking), false);
		assertSlotLimit(helper, 60, List.of(sharpness, unbreaking), true);
		assertSlotLimit(helper, 89, List.of(sharpness, unbreaking, looting), false);
		assertSlotLimit(helper, 90, List.of(sharpness, unbreaking, looting), true);
		helper.succeed();
	}

	@GameTest
	public void goldGearGetsAnExtraSlot(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
		Holder<Enchantment> looting = enchantment(helper, Enchantments.LOOTING);

		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 1), book(unbreaking, 1), book(looting, 1))));

		assertGoldSave(helper, 0, List.of(sharpness), true);
		assertGoldSave(helper, 30, List.of(sharpness, unbreaking), true);
		assertGoldSave(helper, 30, List.of(sharpness, unbreaking, looting), false);
		helper.succeed();
	}

	@GameTest
	public void anEnchantmentAboveTheLibraryMustBeLowered(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		ServerPlayer player = survivalPlayer(helper, 30);
		ItemStack sword = new ItemStack(Items.IRON_SWORD);

		sword.enchant(sharpness, 5);
		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 3))));

		EnchantingTableMenu table = table(helper, player);

		fill(table, sword, 1, new ItemStack(Items.IRON_INGOT));

		helper.assertValueEqual(table.checklist().state(sharpness), Checklist.State.CARRIED, "Sharpness on insertion");
		helper.assertValueEqual(table.checklist().level(sharpness), 5, "Sharpness keeps its level");
		assertRefuses(helper, table, player, "Sharpness V from a Sharpness III Library");

		toggle(helper, table, player, sharpness);
		toggle(helper, table, player, sharpness);
		assertSaves(helper, table, player, "Sharpness lowered to III");

		ItemStack saved = table.getSlot(EnchantingTableMenu.ITEM_SLOT).getItem();

		helper.assertValueEqual(saved.getEnchantments().getLevel(sharpness), 3, "Sharpness after lowering");
		helper.assertTrue(table.getSlot(EnchantingTableMenu.LAPIS_SLOT).getItem().isEmpty(), "the lapis is used");
		helper.assertValueEqual(table.getSlot(EnchantingTableMenu.MATERIAL_SLOT).getItem().getCount(), 1, "an enchanted item pays no deposit");
		helper.assertTrue(EnchantingDeposit.refund(saved).isEmpty(), "no deposit is stored");
		helper.succeed();
	}

	@GameTest
	public void anEnchantmentBelowTheLibraryRisesToIt(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		ServerPlayer player = survivalPlayer(helper, 30);
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
		ServerPlayer player = survivalPlayer(helper, 30);
		ItemStack sword = new ItemStack(Items.IRON_SWORD);

		sword.enchant(sharpness, 3);

		BlockPos shelf = placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 3), book(sharpness, 1)))).getFirst();
		EnchantingTableMenu table = table(helper, player);

		fill(table, sword, 2, ItemStack.EMPTY);
		toggle(helper, table, player, sharpness);
		toggle(helper, table, player, sharpness);
		assertSaves(helper, table, player, "Sharpness III saved as it was");

		helper.assertValueEqual(table.checklist().state(sharpness), Checklist.State.CARRIED, "Sharpness after saving");

		helper.getBlockEntity(shelf, ChiseledBookShelfBlockEntity.class).removeItem(0, 1);

		assertRefuses(helper, table, player, "Sharpness III once the Library holds only Sharpness I");
		helper.succeed();
	}

	@GameTest
	public void aChoiceWhoseBookIsTakenCanBeUnticked(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
		ServerPlayer player = survivalPlayer(helper, 60);
		BlockPos shelf = placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 1), book(unbreaking, 1)))).getFirst();
		EnchantingTableMenu table = table(helper, player);

		fill(table, new ItemStack(Items.IRON_SWORD), 2, new ItemStack(Items.IRON_INGOT));
		toggle(helper, table, player, sharpness);
		toggle(helper, table, player, unbreaking);
		helper.getBlockEntity(shelf, ChiseledBookShelfBlockEntity.class).removeItem(0, 1);

		assertRefuses(helper, table, player, "Sharpness after its book is taken");
		helper.assertTrue(table.checklist().rows().contains(sharpness), "Sharpness is still listed");

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
		ServerPlayer player = survivalPlayer(helper, 60);
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
		ServerPlayer player = survivalPlayer(helper, 30);

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
		ServerPlayer enchanter = survivalPlayer(helper, 60);
		ServerPlayer friend = survivalPlayer(helper, 30);

		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 2), book(unbreaking, 3))));

		EnchantingTableMenu enchanterTable = table(helper, enchanter);
		EnchantingTableMenu friendTable = table(helper, friend);

		helper.assertValueEqual(friendTable.checklist().offers(), enchanterTable.checklist().offers(), "both players' offers");

		for (EnchantingTableMenu table : List.of(enchanterTable, friendTable)) {
			ServerPlayer player = table == enchanterTable ? enchanter : friend;

			fill(table, new ItemStack(Items.IRON_SWORD), 2, new ItemStack(Items.IRON_INGOT));
			toggle(helper, table, player, sharpness);
			toggle(helper, table, player, unbreaking);
		}

		assertSaves(helper, enchanterTable, enchanter, "two enchantments at level 60");
		assertRefuses(helper, friendTable, friend, "two enchantments at level 30");

		toggle(helper, friendTable, friend, unbreaking);
		assertSaves(helper, friendTable, friend, "one enchantment at level 30");
		helper.succeed();
	}

	@GameTest
	public void savingNeedsAChoiceAndItsPayment(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		ServerPlayer player = survivalPlayer(helper, 30);

		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 1))));

		EnchantingTableMenu table = table(helper, player);

		fill(table, new ItemStack(Items.IRON_SWORD), 1, new ItemStack(Items.IRON_INGOT));
		assertRefuses(helper, table, player, "nothing chosen");

		toggle(helper, table, player, sharpness);
		table.getSlot(EnchantingTableMenu.LAPIS_SLOT).set(ItemStack.EMPTY);
		assertRefuses(helper, table, player, "no lapis");

		table.getSlot(EnchantingTableMenu.LAPIS_SLOT).set(new ItemStack(Items.LAPIS_LAZULI));
		table.getSlot(EnchantingTableMenu.MATERIAL_SLOT).set(ItemStack.EMPTY);
		assertRefuses(helper, table, player, "no material");

		table.getSlot(EnchantingTableMenu.MATERIAL_SLOT).set(new ItemStack(Items.GOLD_INGOT));
		assertRefuses(helper, table, player, "the wrong material");

		table.getSlot(EnchantingTableMenu.MATERIAL_SLOT).set(new ItemStack(Items.IRON_INGOT));
		assertSaves(helper, table, player, "paid in full");
		helper.succeed();
	}

	@GameTest
	public void theTableTakesOnlyEnchantableGear(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		EnchantingTableMenu table = table(helper, survivalPlayer(helper, 0));

		placeTable(helper, UPPER_RING, List.of());

		for (Item refused : List.of(Items.BOOK, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.DIRT)) {
			helper.assertFalse(table.getSlot(EnchantingTableMenu.ITEM_SLOT).mayPlace(new ItemStack(refused)), refused + " is accepted");
		}

		helper.assertFalse(table.getSlot(EnchantingTableMenu.ITEM_SLOT).mayPlace(book(sharpness, 1)), "an enchanted book is accepted");

		for (Item accepted : List.of(Items.DIAMOND_SWORD, Items.SHIELD, Items.SHEARS, Items.ELYTRA, Items.FLINT_AND_STEEL)) {
			helper.assertTrue(table.getSlot(EnchantingTableMenu.ITEM_SLOT).mayPlace(new ItemStack(accepted)), accepted + " is refused");
		}

		helper.succeed();
	}

	@GameTest
	public void creativePlayersPayNothing(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		player.setGameMode(GameType.CREATIVE);
		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 1), book(unbreaking, 1))));

		EnchantingTableMenu table = table(helper, player);

		fill(table, new ItemStack(Items.IRON_SWORD), 0, ItemStack.EMPTY);
		toggle(helper, table, player, sharpness);
		toggle(helper, table, player, unbreaking);
		assertSaves(helper, table, player, "two enchantments in creative at level 0");

		helper.assertTrue(EnchantingDeposit.refund(table.getSlot(EnchantingTableMenu.ITEM_SLOT).getItem()).isEmpty(), "no deposit is stored");
		helper.succeed();
	}

	@GameTest
	public void onlyListedEnchantmentsToggle(GameTestHelper helper) {
		Holder<Enchantment> protection = enchantment(helper, Enchantments.PROTECTION);
		ServerPlayer player = survivalPlayer(helper, 30);

		placeTable(helper, UPPER_RING, List.of(List.of(book(protection, 1))));

		EnchantingTableMenu table = table(helper, player);

		fill(table, new ItemStack(Items.IRON_SWORD), 1, ItemStack.EMPTY);

		boolean toggled = table.clickMenuButton(player, EnchantingTableMenu.toggleButton(protection, table.enchantments()));

		helper.assertFalse(toggled, "Protection toggles on a sword");
		helper.assertValueEqual(table.checklist().state(protection), Checklist.State.UNSELECTED, "Protection");
		helper.succeed();
	}

	@GameTest
	public void theGrindstoneHandsBackTheDeposit(GameTestHelper helper) {
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		Holder<Enchantment> unbreaking = enchantment(helper, Enchantments.UNBREAKING);
		ServerPlayer player = survivalPlayer(helper, 60);

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
	public void aFullLibraryOffersMending(GameTestHelper helper) {
		Holder<Enchantment> mending = enchantment(helper, Enchantments.MENDING);
		placeTable(helper, UPPER_RING, shelves(fullLibraryBooks(helper)));

		EnchantingTableMenu table = table(helper, survivalPlayer(helper, 0));
		Map<Holder<Enchantment>, Integer> expectedOffers = new HashMap<>(fullLibraryLevels(helper));

		expectedOffers.put(mending, 1);

		helper.assertValueEqual(table.progress(), new Library.Progress(130, 130), "progress");
		helper.assertValueEqual(table.checklist().offers(), expectedOffers, "offers");
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

		EnchantingTableMenu table = table(helper, survivalPlayer(helper, 0));

		helper.assertValueEqual(table.progress(), new Library.Progress(129, 130), "progress with Blight I");
		helper.assertFalse(table.checklist().offers().containsKey(mending), "Mending is offered with Blight I");

		placeTable(helper, UPPER_RING, shelves(noZephyr));
		table.broadcastChanges();

		helper.assertValueEqual(table.progress(), new Library.Progress(127, 130), "progress without Zephyr");
		helper.assertFalse(table.checklist().offers().containsKey(mending), "Mending is offered without Zephyr");
		helper.succeed();
	}

	@GameTest
	public void mendingFollowsTheBooks(GameTestHelper helper) {
		Holder<Enchantment> mending = enchantment(helper, Enchantments.MENDING);
		Holder<Enchantment> zephyr = enchantment(helper, Zephyr.ENCHANTMENT);
		List<BlockPos> chiseled = placeTable(helper, UPPER_RING, shelves(fullLibraryBooks(helper)));
		EnchantingTableMenu table = table(helper, survivalPlayer(helper, 0));
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
		helper.assertTrue(table.checklist().offers().containsKey(mending), "Mending is offered when Full");

		ItemStack zephyrBook = zephyrShelf.removeItem(zephyrSlot, 1);

		table.broadcastChanges();
		helper.assertFalse(table.checklist().offers().containsKey(mending), "Mending is offered once the Zephyr book leaves");

		zephyrShelf.setItem(zephyrSlot, zephyrBook);
		table.broadcastChanges();
		helper.assertTrue(table.checklist().offers().containsKey(mending), "Mending is offered once the Zephyr book returns");
		helper.succeed();
	}

	@GameTest
	public void aFullLibraryWithoutPowerOffersNothingButStillCounts(GameTestHelper helper) {
		placeTable(helper, List.of(), shelves(fullLibraryBooks(helper)));

		ServerPlayer player = survivalPlayer(helper, 0);

		helper.useBlock(TABLE_POS, player);

		if (!(player.containerMenu instanceof EnchantingTableMenu table)) {
			throw helper.assertionException(Component.literal("the table opened " + player.containerMenu));
		}

		helper.assertValueEqual(table.progress(), new Library.Progress(130, 130), "progress with 7 shelves");
		helper.assertValueEqual(table.checklist().offers(), Map.of(), "offers with 7 shelves");
		helper.assertTrue(isDone(helper, player, FULL_LIBRARY_ADVANCEMENT), "a Full Library without power grants Well Read");
		helper.succeed();
	}

	@GameTest
	public void aFullLibraryLetsGearTakeAndKeepMending(GameTestHelper helper) {
		Holder<Enchantment> mending = enchantment(helper, Enchantments.MENDING);
		ServerPlayer player = survivalPlayer(helper, 30);
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
		ServerPlayer player = survivalPlayer(helper, 60);
		ItemStack sword = new ItemStack(Items.IRON_SWORD);

		sword.enchant(mending, 1);
		placeTable(helper, UPPER_RING, List.of(List.of(book(sharpness, 1))));

		EnchantingTableMenu table = table(helper, player);

		fill(table, sword, 1, ItemStack.EMPTY);

		helper.assertValueEqual(table.checklist().state(mending), Checklist.State.CARRIED, "Mending on the inserted sword");
		helper.assertTrue(
			table.problems(player).contains(new Checklist.NotInLibrary(mending, 1, 0)), "Mending is refused without a Full Library"
		);
		assertRefuses(helper, table, player, "carried Mending without a Full Library");

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
		ServerPlayer player = survivalPlayer(helper, 0);
		EnchantingTableMenu table = table(helper, player);

		helper.assertFalse(isDone(helper, player, FULL_LIBRARY_ADVANCEMENT), "a Library one book short grants Well Read");

		helper.getBlockEntity(chiseled.getLast(), ChiseledBookShelfBlockEntity.class).setItem(5, book(zephyr, 3));
		table.broadcastChanges();

		helper.assertTrue(isDone(helper, player, FULL_LIBRARY_ADVANCEMENT), "completing the Library with the table open grants Well Read");
		helper.assertFalse(isDone(helper, survivalPlayer(helper, 0), FULL_LIBRARY_ADVANCEMENT), "a player who never used the table has it");

		ServerPlayer secondPlayer = survivalPlayer(helper, 0);

		table(helper, secondPlayer);
		helper.assertTrue(isDone(helper, secondPlayer, FULL_LIBRARY_ADVANCEMENT), "opening a Full table grants Well Read");

		AdvancementHolder wellRead = helper.getLevel().getServer().getAdvancements().get(FULL_LIBRARY_ADVANCEMENT);

		helper.assertValueEqual(wellRead.value().parent(), Optional.of(ENCHANTER), "Well Read's parent");
		helper.assertValueEqual(wellRead.value().display().map(DisplayInfo::type), Optional.of(AdvancementType.GOAL), "Well Read's frame");
		helper.succeed();
	}

	// The 16 vanilla bookshelf offsets at one height around the table, as relative offsets.
	private static List<BlockPos> ring(int height) {
		return EnchantingTableBlock.BOOKSHELF_OFFSETS.stream().filter(offset -> offset.getY() == height).toList();
	}

	/**
	 * Places the table with plain bookshelves at {@code plainOffsets} and one Chiseled Bookshelf holding each list of
	 * books at the lower ring's offsets, returning the Chiseled Bookshelves' positions.
	 */
	private static List<BlockPos> placeTable(GameTestHelper helper, List<BlockPos> plainOffsets, List<List<ItemStack>> chiseledShelves) {
		List<BlockPos> chiseledPositions = new ArrayList<>();

		helper.setBlock(TABLE_POS, Blocks.ENCHANTING_TABLE);
		plainOffsets.forEach(offset -> helper.setBlock(TABLE_POS.offset(offset), Blocks.BOOKSHELF));

		for (int shelf = 0; shelf < chiseledShelves.size(); shelf++) {
			BlockPos pos = TABLE_POS.offset(LOWER_RING.get(shelf));

			placeChiseledShelf(helper, pos, chiseledShelves.get(shelf));
			chiseledPositions.add(pos);
		}

		return chiseledPositions;
	}

	private static void placeChiseledShelf(GameTestHelper helper, BlockPos pos, List<ItemStack> books) {
		helper.setBlock(pos, Blocks.CHISELED_BOOKSHELF);

		ChiseledBookShelfBlockEntity shelf = helper.getBlockEntity(pos, ChiseledBookShelfBlockEntity.class);

		// Placing a shelf where one stands keeps its block entity, so earlier books would stay.
		shelf.clearContent();

		for (int slot = 0; slot < books.size(); slot++) {
			shelf.setItem(slot, books.get(slot));
		}
	}

	/** One book per enchantment but Mending, each at its max level. */
	private static List<ItemStack> fullLibraryBooks(GameTestHelper helper) {
		return fullLibraryLevels(helper).entrySet().stream()
			.map(entry -> book(entry.getKey(), entry.getValue()))
			.toList();
	}

	private static Map<Holder<Enchantment>, Integer> fullLibraryLevels(GameTestHelper helper) {
		return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
			.filter(enchantment -> !enchantment.is(Enchantments.MENDING))
			.collect(Collectors.toMap(enchantment -> enchantment, enchantment -> enchantment.value().getMaxLevel()));
	}

	// The books split into Chiseled Bookshelves of 6.
	private static List<List<ItemStack>> shelves(List<ItemStack> books) {
		return Lists.partition(books, ChiseledBookShelfBlockEntity.MAX_BOOKS_IN_STORAGE);
	}

	private static boolean holds(ItemStack book, Holder<Enchantment> enchantment) {
		return book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(enchantment) > 0;
	}

	private static void assertProgress(GameTestHelper helper, List<ItemStack> books, int levels, String label) {
		placeTable(helper, UPPER_RING, List.of(books));

		helper.assertValueEqual(table(helper, survivalPlayer(helper, 0)).progress().levels(), levels, label);
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

	private static EnchantingTableMenu table(GameTestHelper helper, ServerPlayer player) {
		return new EnchantingTableMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TABLE_POS)));
	}

	private static ServerPlayer survivalPlayer(GameTestHelper helper, int level) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		player.setGameMode(GameType.SURVIVAL);
		player.setExperienceLevels(level);

		return player;
	}

	private static void fill(EnchantingTableMenu table, ItemStack item, int lapis, ItemStack material) {
		table.getSlot(EnchantingTableMenu.ITEM_SLOT).set(item);
		table.getSlot(EnchantingTableMenu.LAPIS_SLOT).set(lapis == 0 ? ItemStack.EMPTY : new ItemStack(Items.LAPIS_LAZULI, lapis));
		table.getSlot(EnchantingTableMenu.MATERIAL_SLOT).set(material);
	}

	private static void toggle(GameTestHelper helper, EnchantingTableMenu table, ServerPlayer player, Holder<Enchantment> enchantment) {
		boolean toggled = table.clickMenuButton(player, EnchantingTableMenu.toggleButton(enchantment, table.enchantments()));

		helper.assertTrue(toggled, enchantment.getRegisteredName() + " toggles");
	}

	/** Saves, failing unless the save is accepted and leaves the player's XP as it was. */
	private static void assertSaves(GameTestHelper helper, EnchantingTableMenu table, ServerPlayer player, String label) {
		int levelBefore = player.experienceLevel;
		int pointsBefore = ExperiencePoints.total(player);

		helper.assertTrue(table.clickMenuButton(player, EnchantingTableMenu.SAVE_BUTTON), label + " saves");
		helper.assertValueEqual(player.experienceLevel, levelBefore, label + ": level after saving");
		helper.assertValueEqual(ExperiencePoints.total(player), pointsBefore, label + ": points after saving");
	}

	/** Saves, failing unless the save is refused and nothing in the table changes. */
	private static void assertRefuses(GameTestHelper helper, EnchantingTableMenu table, ServerPlayer player, String label) {
		List<ItemStack> slotsBefore = tableSlots(table);
		Map<Holder<Enchantment>, Checklist.State> statesBefore = table.checklist().states();

		helper.assertFalse(table.clickMenuButton(player, EnchantingTableMenu.SAVE_BUTTON), label + " saves");
		helper.assertTrue(ItemStack.listMatches(slotsBefore, tableSlots(table)), label + ": the table's slots are unchanged");
		helper.assertValueEqual(table.checklist().states(), statesBefore, label + ": the choices");
	}

	private static List<ItemStack> tableSlots(EnchantingTableMenu table) {
		return List.of(EnchantingTableMenu.ITEM_SLOT, EnchantingTableMenu.LAPIS_SLOT, EnchantingTableMenu.MATERIAL_SLOT).stream()
			.map(slot -> table.getSlot(slot).getItem().copy())
			.toList();
	}

	private static void assertSlotLimit(GameTestHelper helper, int level, List<Holder<Enchantment>> choices, boolean saves) {
		assertChoicesSave(helper, level, new ItemStack(Items.IRON_SWORD), new ItemStack(Items.IRON_INGOT), choices, saves);
	}

	private static void assertGoldSave(GameTestHelper helper, int level, List<Holder<Enchantment>> choices, boolean saves) {
		assertChoicesSave(helper, level, new ItemStack(Items.GOLDEN_SWORD), new ItemStack(Items.GOLD_INGOT), choices, saves);
	}

	private static void assertChoicesSave(
		GameTestHelper helper, int level, ItemStack item, ItemStack material, List<Holder<Enchantment>> choices, boolean saves
	) {
		ServerPlayer player = survivalPlayer(helper, level);
		EnchantingTableMenu table = table(helper, player);
		String label = choices.size() + " on " + item.getItem() + " at level " + level;

		fill(table, item, choices.size(), material);
		choices.forEach(enchantment -> toggle(helper, table, player, enchantment));

		if (!saves) {
			assertRefuses(helper, table, player, label);

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

		helper.setBlock(GRINDSTONE_POS, Blocks.GRINDSTONE);

		GrindstoneMenu grindstone = new GrindstoneMenu(
			0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(GRINDSTONE_POS))
		);

		grindstone.getSlot(GrindstoneMenu.INPUT_SLOT).set(item);
		grindstone.setCarried(ItemStack.EMPTY);
		grindstone.clicked(GrindstoneMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, player);

		ItemStack ground = grindstone.getCarried();
		String label = "grinding " + item.getItem();

		helper.assertTrue(ground.getEnchantments().isEmpty(), label + " strips it");
		helper.assertTrue(EnchantingDeposit.refund(ground).isEmpty(), label + " removes the deposit");
		helper.assertValueEqual(player.getInventory().countItem(deposit) - before, count, label + " hands back " + deposit);
	}

	/** Shelves the book with a fresh player's click, returning whether that player earned the shelving advancement. */
	private static boolean shelve(GameTestHelper helper, BlockPos shelf, ItemStack book) {
		ServerPlayer player = survivalPlayer(helper, 0);

		player.setItemInHand(InteractionHand.MAIN_HAND, book);
		helper.useBlock(shelf, player);
		helper.assertTrue(player.getMainHandItem().isEmpty(), "the book is shelved");

		return isDone(helper, player, SHELVE_ADVANCEMENT);
	}

	private static boolean isDone(GameTestHelper helper, ServerPlayer player, Identifier advancement) {
		AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(advancement);

		helper.assertTrue(holder != null, advancement + " exists");

		return player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	private static ItemStack book(Holder<Enchantment> enchantment, int level) {
		return EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, level));
	}

	private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
		return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}
}
