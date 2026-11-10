package mapleflavored.enchanting;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import mapleflavored.advancements.MapleTriggers;
import mapleflavored.materialunits.MaterialUnits;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;

/**
 * The Enchanting Table's menu: a {@link Checklist} over the table's {@link Library}. Put in gear or a golden apple,
 * tick enchantments or effects, and press Save, which costs lapis and, on the item's first save, a deposit from the
 * material slot.
 *
 * <p>The server owns the checklist. Through menu data it sends the client what the Library offers, as each
 * enchantment's level by registry id and the {@link AppleEffect}s; the Library's {@link #library() bookshelves and
 * progress}; and each row's {@link Checklist.State} by row number, for whichever checklist the item gets. It takes
 * clicks as menu buttons: {@link #SAVE_BUTTON}, or a {@link #toggleButton} for a row.
 */
public class EnchantingTableMenu extends AbstractContainerMenu {
	public static final int ITEM_SLOT = 0;
	public static final int LAPIS_SLOT = 1;
	public static final int MATERIAL_SLOT = 2;
	/** The menu button that saves the checklist onto the item. */
	public static final int SAVE_BUTTON = 0;

	private static final Identifier EMPTY_SLOT_LAPIS_LAZULI = Identifier.withDefaultNamespace("container/slot/lapis_lazuli");
	// The smithing table's ingot outline; most materials are ingots, though planks, string and others also fit.
	private static final Identifier EMPTY_SLOT_INGOT = Identifier.withDefaultNamespace("container/slot/ingot");
	private static final int TABLE_SLOT_COUNT = 3;
	private static final int INVENTORY_START = TABLE_SLOT_COUNT;
	private static final int INVENTORY_END = INVENTORY_START + 36;
	private static final int INVENTORY_X = 8;
	private static final int INVENTORY_Y = 144;
	private static final int TABLE_SLOTS_Y = 18;
	private static final int SLOT_SPACING = 18;
	private static final int FIRST_ROW_BUTTON = SAVE_BUTTON + 1;

	private final Container tableSlots = new SimpleContainer(TABLE_SLOT_COUNT) {
		@Override
		public void setChanged() {
			super.setChanged();
			EnchantingTableMenu.this.slotsChanged(this);
		}
	};
	private final ContainerLevelAccess access;
	private final Player player;
	private final Registry<Enchantment> enchantments;
	private final IdMap<Holder<Enchantment>> enchantmentIds;
	private final int[] offeredLevels;
	private final int[] rowStates;
	private final DataSlot bookshelves = DataSlot.standalone();
	private final DataSlot progressLevels = DataSlot.standalone();
	private final DataSlot offeredEffects = DataSlot.standalone();
	// The item the row states were last pre-selected for, so other slot changes keep the player's choices.
	private ItemStack checklistItem = ItemStack.EMPTY;

	/** The client's menu, which shows what the server sends. */
	public EnchantingTableMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	/** The server's menu for the table that {@code access} reaches. */
	public EnchantingTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(Enchanting.MENU, containerId);
		this.access = access;
		this.player = inventory.player;
		this.enchantments = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		this.enchantmentIds = enchantments.asHolderIdMap();
		this.offeredLevels = new int[enchantmentIds.size()];
		this.rowStates = new int[Math.max(enchantmentIds.size(), AppleEffect.values().length)];

		addTableSlots();
		addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);

		for (int id = 0; id < offeredLevels.length; id++) {
			addDataSlot(DataSlot.shared(offeredLevels, id));
		}

		for (int row = 0; row < rowStates.length; row++) {
			addDataSlot(DataSlot.shared(rowStates, row));
		}

		addDataSlot(bookshelves);
		addDataSlot(progressLevels);
		addDataSlot(offeredEffects);
		refreshLibrary();
	}

	/** The menu button that toggles the row numbered {@code row}, as {@link Checklist.Row#id} gives it. */
	public static int toggleButton(int row) {
		return FIRST_ROW_BUTTON + row;
	}

	/**
	 * The checklist for the item in the table and the player using it, from the menu's data as last synced: an
	 * {@link EmptyChecklist} for no item, an {@link AppleChecklist} for a golden apple, and gear's otherwise.
	 */
	public Checklist checklist() {
		ItemStack item = item();
		Library.Status library = library();
		Checklist.Saver saver = Checklist.Saver.of(player, tableSlots.getItem(LAPIS_SLOT), tableSlots.getItem(MATERIAL_SLOT));

		if (item.isEmpty()) return new EmptyChecklist(library, saver);
		if (AppleChecklist.accepts(item)) return new AppleChecklist(item, appleOffers(), effectStates(), library, saver);

		return new EnchantmentChecklist(item, offers(), enchantmentStates(), enchantments, library, saver);
	}

	/** The enchantments the table's Library offers and their levels, as last synced. */
	public Map<Holder<Enchantment>, Integer> offers() {
		Map<Holder<Enchantment>, Integer> offers = new HashMap<>();

		for (int id = 0; id < offeredLevels.length; id++) {
			if (offeredLevels[id] > 0) offers.put(enchantmentIds.byIdOrThrow(id), offeredLevels[id]);
		}

		return offers;
	}

	/** The golden apple effects the table's Library offers, as last synced. */
	public Set<AppleEffect> appleOffers() {
		Set<AppleEffect> offers = EnumSet.noneOf(AppleEffect.class);
		int mask = offeredEffects.get();

		Arrays.stream(AppleEffect.values()).filter(effect -> (mask & 1 << effect.ordinal()) != 0).forEach(offers::add);

		return offers;
	}

	/** The table's bookshelves and Library Progress, as last synced. */
	public Library.Status library() {
		return new Library.Status(bookshelves.get(), new Library.Progress(progressLevels.get(), Library.fullLevels(enchantments)));
	}

	@Override
	public void slotsChanged(Container container) {
		// Before super, which broadcasts, so the client gets the new item's states with the item itself.
		if (container == tableSlots) access.execute((level, pos) -> preselectForNewItem());

		super.slotsChanged(container);
	}

	@Override
	public void broadcastChanges() {
		refreshLibrary();
		super.broadcastChanges();
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		boolean clientSide = player.level().isClientSide();

		refreshLibrary();

		// The client shows nothing until the server's answer syncs back, so it only asks whether to send the click.
		if (buttonId == SAVE_BUTTON) return clientSide || save(player);

		Optional<Checklist> toggled = checklist().toggled(buttonId - FIRST_ROW_BUTTON);

		if (!clientSide) toggled.ifPresent(this::storeRowStates);

		return toggled.isPresent();
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		access.execute((level, pos) -> clearContainer(player, tableSlots));
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, Blocks.ENCHANTING_TABLE);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = slots.get(slotIndex);

		if (!slot.hasItem()) return ItemStack.EMPTY;

		ItemStack stack = slot.getItem();
		ItemStack moved = stack.copy();

		if (slotIndex < TABLE_SLOT_COUNT) {
			if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) return ItemStack.EMPTY;
		} else if (!moveIntoTable(stack)) {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}

		if (stack.getCount() == moved.getCount()) return ItemStack.EMPTY;

		slot.onTake(player, stack);

		return moved;
	}

	private void addTableSlots() {
		addSlot(new Slot(tableSlots, ITEM_SLOT, INVENTORY_X, TABLE_SLOTS_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return Checklist.accepts(stack, enchantments);
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		addSlot(new Slot(tableSlots, LAPIS_SLOT, INVENTORY_X + SLOT_SPACING, TABLE_SLOTS_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(Items.LAPIS_LAZULI);
			}

			@Override
			public Identifier getNoItemIcon() {
				return EMPTY_SLOT_LAPIS_LAZULI;
			}
		});
		addSlot(new Slot(tableSlots, MATERIAL_SLOT, INVENTORY_X + 2 * SLOT_SPACING, TABLE_SLOTS_Y) {
			@Override
			public Identifier getNoItemIcon() {
				return EMPTY_SLOT_INGOT;
			}
		});
	}

	// Lapis goes to its slot, gear or an apple to the empty item slot, and the current item's material to the material slot.
	private boolean moveIntoTable(ItemStack stack) {
		if (stack.is(Items.LAPIS_LAZULI)) return moveItemStackTo(stack, LAPIS_SLOT, LAPIS_SLOT + 1, false);

		Slot itemSlot = slots.get(ITEM_SLOT);

		if (!itemSlot.hasItem() && itemSlot.mayPlace(stack)) {
			itemSlot.setByPlayer(stack.split(1));

			return true;
		}

		boolean isMaterial = MaterialUnits.materialOf(item()).map(material -> material.test(stack)).orElse(false);

		return isMaterial && moveItemStackTo(stack, MATERIAL_SLOT, MATERIAL_SLOT + 1, false);
	}

	private boolean save(Player player) {
		Checklist checklist = checklist();

		if (!checklist.problems().isEmpty()) return false;

		Checklist.Cost cost = checklist.cost();
		ItemStack saved = checklist.saved();

		tableSlots.getItem(LAPIS_SLOT).shrink(cost.lapis());

		if (cost.deposit()) EnchantingDeposit.pay(saved, tableSlots.getItem(MATERIAL_SLOT));

		tableSlots.setItem(ITEM_SLOT, saved);
		player.awardStat(Stats.ENCHANT_ITEM);

		if (player instanceof ServerPlayer serverPlayer) {
			CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, saved, checklist.chosenCount());
		}

		access.execute((level, pos) -> level.playSound(
			null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F
		));
		// Even when the save left the item as it was, everything on it becomes CARRIED, so a later drop in the Library
		// blocks the next save instead of silently lowering it.
		preselect();
		tableSlots.setChanged();

		return true;
	}

	private void preselectForNewItem() {
		if (!ItemStack.matches(item(), checklistItem)) preselect();
	}

	private void preselect() {
		checklistItem = item().copy();
		storeRowStates(checklist().preselected());
	}

	private void storeRowStates(Checklist checklist) {
		for (int row = 0; row < rowStates.length; row++) {
			rowStates[row] = checklist.state(row).ordinal();
		}
	}

	private Checklist.State rowState(int row) {
		return Checklist.State.values()[rowStates[row]];
	}

	private Map<Holder<Enchantment>, Checklist.State> enchantmentStates() {
		Map<Holder<Enchantment>, Checklist.State> states = new HashMap<>();

		for (int id = 0; id < enchantmentIds.size(); id++) {
			if (rowState(id) != Checklist.State.UNSELECTED) states.put(enchantmentIds.byIdOrThrow(id), rowState(id));
		}

		return states;
	}

	private Map<AppleEffect, Checklist.State> effectStates() {
		Map<AppleEffect, Checklist.State> states = new EnumMap<>(AppleEffect.class);

		for (AppleEffect effect : AppleEffect.values()) {
			int row = AppleChecklist.row(effect);

			if (rowState(row) != Checklist.State.UNSELECTED) states.put(effect, rowState(row));
		}

		return states;
	}

	// Reads the Library afresh into the synced offers and progress, and awards a Full one to the player using it.
	private void refreshLibrary() {
		access.execute((level, pos) -> {
			Library library = Library.read(level, pos);
			Map<Holder<Enchantment>, Integer> offers = library.offers();
			Library.Status status = library.status();

			for (int id = 0; id < enchantmentIds.size(); id++) {
				offeredLevels[id] = offers.getOrDefault(enchantmentIds.byIdOrThrow(id), 0);
			}

			bookshelves.set(status.bookshelves());
			progressLevels.set(status.progress().levels());
			offeredEffects.set(mask(library.appleOffers()));

			if (status.progress().isFull() && player instanceof ServerPlayer serverPlayer) MapleTriggers.FULL_LIBRARY.trigger(serverPlayer);
		});
	}

	// Apple effects cross to the client as one bit each, in AppleEffect order.
	private static int mask(Set<AppleEffect> effects) {
		return effects.stream().mapToInt(effect -> 1 << effect.ordinal()).sum();
	}

	private ItemStack item() {
		return tableSlots.getItem(ITEM_SLOT);
	}
}
