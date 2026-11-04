package mapleflavored.enchanting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;

/**
 * The Enchanting Table's menu: a checklist over the table's {@link Library}. Put in gear, tick enchantments, and press
 * Save, which costs lapis and, on the first save of an unenchanted item, a deposit from the material slot.
 *
 * <p>The server owns the checklist. It sends each enchantment's offered level and {@link Checklist.State} to the client
 * through menu data, indexed by enchantment registry id, and takes clicks as menu buttons: {@link #SAVE_BUTTON}, or
 * {@link #toggleButton} for a row.
 */
public class EnchantingTableMenu extends AbstractContainerMenu {
	public static final int ITEM_SLOT = 0;
	public static final int LAPIS_SLOT = 1;
	public static final int MATERIAL_SLOT = 2;
	/** The menu button that saves the checklist onto the item. */
	public static final int SAVE_BUTTON = 0;

	private static final Identifier EMPTY_SLOT_LAPIS_LAZULI = Identifier.withDefaultNamespace("container/slot/lapis_lazuli");
	private static final int TABLE_SLOT_COUNT = 3;
	private static final int INVENTORY_START = TABLE_SLOT_COUNT;
	private static final int INVENTORY_END = INVENTORY_START + 36;
	private static final int INVENTORY_X = 8;
	private static final int INVENTORY_Y = 124;
	private static final int TABLE_SLOTS_Y = 18;
	private static final int SLOT_SPACING = 18;

	private final Container tableSlots = new SimpleContainer(TABLE_SLOT_COUNT) {
		@Override
		public void setChanged() {
			super.setChanged();
			EnchantingTableMenu.this.slotsChanged(this);
		}
	};
	private final ContainerLevelAccess access;
	private final Registry<Enchantment> enchantments;
	private final IdMap<Holder<Enchantment>> enchantmentIds;
	private final int[] offeredLevels;
	private final int[] states;
	// The item the states were last pre-selected for, so other slot changes keep the player's choices.
	private ItemStack checklistItem = ItemStack.EMPTY;

	/** The client's menu, which shows what the server sends. */
	public EnchantingTableMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	/** The server's menu for the table that {@code access} reaches. */
	public EnchantingTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(Enchanting.MENU, containerId);
		this.access = access;
		this.enchantments = inventory.player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		this.enchantmentIds = enchantments.asHolderIdMap();
		this.offeredLevels = new int[enchantmentIds.size()];
		this.states = new int[enchantmentIds.size()];

		addTableSlots();
		addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);

		for (int id = 0; id < enchantmentIds.size(); id++) {
			addDataSlot(DataSlot.shared(offeredLevels, id));
			addDataSlot(DataSlot.shared(states, id));
		}

		refreshOffers();
	}

	/** The menu button that toggles the enchantment's row. */
	public static int toggleButton(Holder<Enchantment> enchantment, Registry<Enchantment> enchantments) {
		return 1 + enchantments.asHolderIdMap().getId(enchantment);
	}

	/** The checklist for the item in the table, from the Library's offers and the states as last synced. */
	public Checklist checklist() {
		Map<Holder<Enchantment>, Integer> offers = new HashMap<>();
		Map<Holder<Enchantment>, Checklist.State> checklistStates = new HashMap<>();

		for (int id = 0; id < enchantmentIds.size(); id++) {
			Holder<Enchantment> enchantment = enchantmentIds.byIdOrThrow(id);

			if (offeredLevels[id] > 0) offers.put(enchantment, offeredLevels[id]);
			if (states[id] > 0) checklistStates.put(enchantment, Checklist.State.values()[states[id]]);
		}

		return new Checklist(item(), offers, checklistStates, enchantments);
	}

	/** Every reason the table would refuse {@code player}'s save right now, empty when it can save. */
	public List<Checklist.Problem> problems(Player player) {
		return problems(checklist(), player);
	}

	/** The enchantment registry the menu indexes its data and buttons by. */
	public Registry<Enchantment> enchantments() {
		return enchantments;
	}

	@Override
	public void slotsChanged(Container container) {
		// Before super, which broadcasts, so the client gets the new item's states with the item itself.
		if (container == tableSlots) access.execute((level, pos) -> preselectForNewItem());

		super.slotsChanged(container);
	}

	@Override
	public void broadcastChanges() {
		refreshOffers();
		super.broadcastChanges();
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		boolean isButton = buttonId >= 0 && buttonId <= enchantmentIds.size();

		// The client shows nothing until the server's answer syncs back, so it only asks whether to send the click.
		if (!isButton || player.level().isClientSide()) return isButton;

		refreshOffers();

		return buttonId == SAVE_BUTTON ? save(player) : toggle(enchantmentIds.byIdOrThrow(buttonId - 1));
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
		addSlot(new Slot(tableSlots, MATERIAL_SLOT, INVENTORY_X + 2 * SLOT_SPACING, TABLE_SLOTS_Y));
	}

	// Lapis goes to its slot, gear to the empty item slot, and the current item's material to the material slot.
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

	private boolean toggle(Holder<Enchantment> enchantment) {
		Checklist checklist = checklist();

		if (!checklist.isRow(enchantment)) return false;

		states[enchantmentIds.getId(enchantment)] = checklist.state(enchantment).toggled().ordinal();

		return true;
	}

	private boolean save(Player player) {
		Checklist checklist = checklist();

		if (!problems(checklist, player).isEmpty()) return false;

		Checklist.Cost cost = checklist.cost(player.hasInfiniteMaterials());
		ItemStack item = item();
		int chosen = checklist.chosen().size();

		EnchantmentHelper.setEnchantments(item, checklist.selection());
		tableSlots.getItem(LAPIS_SLOT).shrink(cost.lapis());

		if (cost.deposit()) EnchantingDeposit.pay(item, tableSlots.getItem(MATERIAL_SLOT));

		player.awardStat(Stats.ENCHANT_ITEM);

		if (player instanceof ServerPlayer serverPlayer) CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, item, chosen);

		access.execute((level, pos) -> level.playSound(
			null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F
		));
		// Even when the save left the item as it was, every enchantment on it becomes CARRIED, so a later drop in the
		// Library blocks the next save instead of silently lowering it.
		preselect();
		tableSlots.setChanged();

		return true;
	}

	private List<Checklist.Problem> problems(Checklist checklist, Player player) {
		return checklist.problems(
			player.experienceLevel, tableSlots.getItem(LAPIS_SLOT), tableSlots.getItem(MATERIAL_SLOT), player.hasInfiniteMaterials()
		);
	}

	private void preselectForNewItem() {
		if (!ItemStack.matches(item(), checklistItem)) preselect();
	}

	private void preselect() {
		checklistItem = item().copy();
		writeStates(Checklist.preselected(checklistItem));
	}

	private void refreshOffers() {
		access.execute((level, pos) -> {
			Map<Holder<Enchantment>, Integer> offers = Library.read(level, pos).offers();

			for (int id = 0; id < enchantmentIds.size(); id++) {
				offeredLevels[id] = offers.getOrDefault(enchantmentIds.byIdOrThrow(id), 0);
			}
		});
	}

	private void writeStates(Map<Holder<Enchantment>, Checklist.State> checklistStates) {
		for (int id = 0; id < enchantmentIds.size(); id++) {
			states[id] = checklistStates.getOrDefault(enchantmentIds.byIdOrThrow(id), Checklist.State.UNSELECTED).ordinal();
		}
	}

	private ItemStack item() {
		return tableSlots.getItem(ITEM_SLOT);
	}
}
