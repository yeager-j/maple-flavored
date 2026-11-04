package mapleflavored.gametest;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import mapleflavored.enchanting.EnchantingTableMenu;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;

/** An Enchanting Table with its Library, a grindstone and survival players for GameTests that use the table. */
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
			.map(entry -> book(entry.getKey(), entry.getValue()))
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

	/** A mock player in survival at {@code level}. */
	static ServerPlayer survivalPlayer(GameTestHelper helper, int level) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		player.setGameMode(GameType.SURVIVAL);
		player.setExperienceLevels(level);

		return player;
	}

	/** Fills the table's item slot, its lapis slot with {@code lapis} lapis, and its material slot. */
	static void fill(EnchantingTableMenu table, ItemStack item, int lapis, ItemStack material) {
		table.getSlot(EnchantingTableMenu.ITEM_SLOT).set(item);
		table.getSlot(EnchantingTableMenu.LAPIS_SLOT).set(lapis == 0 ? ItemStack.EMPTY : new ItemStack(Items.LAPIS_LAZULI, lapis));
		table.getSlot(EnchantingTableMenu.MATERIAL_SLOT).set(material);
	}

	/** Whether the player has earned the advancement, failing the test if it doesn't exist. */
	static boolean isDone(GameTestHelper helper, ServerPlayer player, Identifier advancement) {
		AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(advancement);

		helper.assertTrue(holder != null, advancement + " exists");

		return player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	/** An enchanted book holding the one enchantment. */
	static ItemStack book(Holder<Enchantment> enchantment, int level) {
		return EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, level));
	}

	/** The registered enchantment. */
	static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
		return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}

	// The 16 vanilla bookshelf offsets at one height around the table, as relative offsets.
	private static List<BlockPos> ring(int height) {
		return EnchantingTableBlock.BOOKSHELF_OFFSETS.stream().filter(offset -> offset.getY() == height).toList();
	}
}
