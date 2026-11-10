package mapleflavored.gametest;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import mapleflavored.enchanting.AppleChecklist;
import mapleflavored.enchanting.AppleEffect;
import mapleflavored.enchanting.Checklist;
import mapleflavored.enchanting.EnchantingTableMenu;
import mapleflavored.enchanting.EnchantmentChecklist;
import mapleflavored.experience.ExperiencePoints;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;

/**
 * An Enchanting Table with its Library, a grindstone, players with XP levels and the save assertions for GameTests that
 * use the table.
 */
final class EnchantingTables {
	/** Where the table stands in the test structure. */
	static final BlockPos TABLE_POS = new BlockPos(3, 1, 3);
	/** The 16 bookshelf offsets level with the table, where {@link #placeTable} puts Chiseled Bookshelves. */
	static final List<BlockPos> LOWER_RING = ring(0);
	/** The 16 bookshelf offsets a block above the table. */
	static final List<BlockPos> UPPER_RING = ring(1);

	private static final BlockPos GRINDSTONE_POS = new BlockPos(0, 1, 0);

	private EnchantingTables() {
	}

	/**
	 * Places the table with plain bookshelves at {@code plainOffsets} and one Chiseled Bookshelf holding each list of
	 * books at the lower ring's offsets, returning the Chiseled Bookshelves' positions.
	 */
	static List<BlockPos> placeTable(GameTestHelper helper, List<BlockPos> plainOffsets, List<List<ItemStack>> chiseledShelves) {
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

	/** Places a Chiseled Bookshelf at {@code pos} holding exactly {@code books}. */
	static void placeChiseledShelf(GameTestHelper helper, BlockPos pos, List<ItemStack> books) {
		helper.setBlock(pos, Blocks.CHISELED_BOOKSHELF);

		ChiseledBookShelfBlockEntity shelf = helper.getBlockEntity(pos, ChiseledBookShelfBlockEntity.class);

		// Placing a shelf where one stands keeps its block entity, so earlier books would stay.
		shelf.clearContent();

		for (int slot = 0; slot < books.size(); slot++) {
			shelf.setItem(slot, books.get(slot));
		}
	}

	/** One book per enchantment but Mending, each at its max level. */
	static List<ItemStack> fullLibraryBooks(GameTestHelper helper) {
		return fullLibraryLevels(helper).entrySet().stream()
			.map(entry -> TestItems.book(entry.getKey(), entry.getValue()))
			.toList();
	}

	/** Each enchantment but Mending, at its max level. */
	static Map<Holder<Enchantment>, Integer> fullLibraryLevels(GameTestHelper helper) {
		return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
			.filter(enchantment -> !enchantment.is(Enchantments.MENDING))
			.collect(Collectors.toMap(enchantment -> enchantment, enchantment -> enchantment.value().getMaxLevel()));
	}

	/** The books split into Chiseled Bookshelves of 6. */
	static List<List<ItemStack>> shelves(List<ItemStack> books) {
		return Lists.partition(books, ChiseledBookShelfBlockEntity.MAX_BOOKS_IN_STORAGE);
	}

	/** The table's menu, opened by {@code player}. */
	static EnchantingTableMenu table(GameTestHelper helper, ServerPlayer player) {
		return new EnchantingTableMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TABLE_POS)));
	}

	/** Places a grindstone and returns its menu, opened by {@code player}. */
	static GrindstoneMenu grindstone(GameTestHelper helper, ServerPlayer player) {
		helper.setBlock(GRINDSTONE_POS, Blocks.GRINDSTONE);

		return new GrindstoneMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(GRINDSTONE_POS)));
	}

	/** A loaded mock player in Survival with {@code levels} XP levels. */
	static ServerPlayer survivalPlayerWithLevels(GameTestHelper helper, int levels) {
		ServerPlayer player = MockPlayers.survivalPlayer(helper);

		player.setExperienceLevels(levels);

		return player;
	}

	/** Fills the table's item slot, its lapis slot with {@code lapis} lapis, and its material slot. */
	static void fill(EnchantingTableMenu table, ItemStack item, int lapis, ItemStack material) {
		table.getSlot(EnchantingTableMenu.ITEM_SLOT).set(item);
		table.getSlot(EnchantingTableMenu.LAPIS_SLOT).set(lapis == 0 ? ItemStack.EMPTY : new ItemStack(Items.LAPIS_LAZULI, lapis));
		table.getSlot(EnchantingTableMenu.MATERIAL_SLOT).set(material);
	}

	/** The number of the enchantment's row in a gear checklist. */
	static int row(GameTestHelper helper, Holder<Enchantment> enchantment) {
		return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap().getId(enchantment);
	}

	/** The table's checklist, failing unless it is gear's. */
	static EnchantmentChecklist gear(GameTestHelper helper, EnchantingTableMenu table) {
		if (table.checklist() instanceof EnchantmentChecklist gear) return gear;

		throw helper.assertionException(Component.literal("the table's checklist is " + table.checklist()));
	}

	/** The table's checklist, failing unless it is a golden apple's. */
	static AppleChecklist apple(GameTestHelper helper, EnchantingTableMenu table) {
		if (table.checklist() instanceof AppleChecklist apple) return apple;

		throw helper.assertionException(Component.literal("the table's checklist is " + table.checklist()));
	}

	/** Clicks the enchantment's row, failing unless the table toggles it. */
	static void toggle(GameTestHelper helper, EnchantingTableMenu table, ServerPlayer player, Holder<Enchantment> enchantment) {
		boolean toggled = table.clickMenuButton(player, EnchantingTableMenu.toggleButton(row(helper, enchantment)));

		helper.assertTrue(toggled, enchantment.getRegisteredName() + " toggles");
	}

	/** Clicks the golden apple effect's row, failing unless the table toggles it. */
	static void toggle(GameTestHelper helper, EnchantingTableMenu table, ServerPlayer player, AppleEffect effect) {
		boolean toggled = table.clickMenuButton(player, EnchantingTableMenu.toggleButton(AppleChecklist.row(effect)));

		helper.assertTrue(toggled, effect + " toggles");
	}

	/** Saves, failing unless the save is accepted and leaves the player's XP as it was. */
	static void assertSaves(GameTestHelper helper, EnchantingTableMenu table, ServerPlayer player, String label) {
		int levelBefore = player.experienceLevel;
		int pointsBefore = ExperiencePoints.total(player);

		helper.assertTrue(table.clickMenuButton(player, EnchantingTableMenu.SAVE_BUTTON), label + " saves: " + table.checklist().problems());
		helper.assertValueEqual(player.experienceLevel, levelBefore, label + ": level after saving");
		helper.assertValueEqual(ExperiencePoints.total(player), pointsBefore, label + ": points after saving");
	}

	/** Saves, failing unless the save is refused for {@code problem} and nothing in the table changes. */
	static void assertRefuses(GameTestHelper helper, EnchantingTableMenu table, ServerPlayer player, Checklist.Problem problem, String label) {
		// Reads the Library afresh, as each server tick does for an open table.
		table.broadcastChanges();

		List<ItemStack> slotsBefore = tableSlots(table);
		List<Checklist.Row> rowsBefore = table.checklist().rows();
		List<Checklist.Problem> problems = table.checklist().problems();

		helper.assertTrue(problems.contains(problem), label + ": problems " + problems + " lack " + problem);
		helper.assertFalse(table.clickMenuButton(player, EnchantingTableMenu.SAVE_BUTTON), label + " saves");
		helper.assertTrue(ItemStack.listMatches(slotsBefore, tableSlots(table)), label + ": the table's slots are unchanged");
		helper.assertValueEqual(table.checklist().rows(), rowsBefore, label + ": the choices");
	}

	/** Copies of the stacks in the table's item, lapis and material slots. */
	static List<ItemStack> tableSlots(EnchantingTableMenu table) {
		return List.of(EnchantingTableMenu.ITEM_SLOT, EnchantingTableMenu.LAPIS_SLOT, EnchantingTableMenu.MATERIAL_SLOT).stream()
			.map(slot -> table.getSlot(slot).getItem().copy())
			.toList();
	}

	// The 16 vanilla bookshelf offsets at one height around the table, as relative offsets.
	private static List<BlockPos> ring(int height) {
		return EnchantingTableBlock.BOOKSHELF_OFFSETS.stream().filter(offset -> offset.getY() == height).toList();
	}
}
