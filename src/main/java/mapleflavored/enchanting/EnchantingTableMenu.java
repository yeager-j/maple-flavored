package mapleflavored.enchanting;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * <p>The server owns the checklist. It sends each enchantment's offered level and {@link EnchantmentChecklist.State},
 * indexed by enchantment registry id, the offered and chosen {@link AppleEffect}s, and the Library's
 * {@link #progress() progress} to the client through menu data. It takes clicks as menu buttons: {@link #SAVE_BUTTON},
 * or a {@code toggleButton} for a row.
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
	private static final int FIRST_ENCHANTMENT_BUTTON = 1 + AppleEffect.values().length;

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
	private final int[] states;
	private final DataSlot progressLevels = DataSlot.standalone();
	private final DataSlot offeredEffects = DataSlot.standalone();
	private final DataSlot chosenEffects = DataSlot.standalone();
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
		this.player = inventory.player;
		this.enchantments = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		this.enchantmentIds = enchantments.asHolderIdMap();
		this.offeredLevels = new int[enchantmentIds.size()];
		this.states = new int[enchantmentIds.size()];

		addTableSlots();
		addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);

		for (int id = 0; id < enchantmentIds.size(); id++) {
			addDataSlot(DataSlot.shared(offeredLevels, id));
			addDataSlot(DataSlot.shared(states, id));
		}

		addDataSlot(progressLevels);
		addDataSlot(offeredEffects);
		addDataSlot(chosenEffects);
		refreshLibrary();
	}

	/** The menu button that toggles the enchantment's row. */
	public static int toggleButton(Holder<Enchantment> enchantment, Registry<Enchantment> enchantments) {
		return FIRST_ENCHANTMENT_BUTTON + enchantments.asHolderIdMap().getId(enchantment);
	}

	/** The menu button that toggles the golden apple effect's row. */
	public static int toggleButton(AppleEffect effect) {
		return 1 + effect.ordinal();
	}

	/** The checklist for the item in the table: an {@link AppleChecklist} for a golden apple, and gear's otherwise. */
	public Checklist checklist() {
		return AppleChecklist.accepts(item()) ? appleChecklist() : enchantmentChecklist();
	}

	/** The gear checklist for the item in the table, from the Library's offers and the states as last synced. */
	public EnchantmentChecklist enchantmentChecklist() {
		Map<Holder<Enchantment>, Integer> offers = new HashMap<>();
		Map<Holder<Enchantment>, EnchantmentChecklist.State> checklistStates = new HashMap<>();

		for (int id = 0; id < enchantmentIds.size(); id++) {
			Holder<Enchantment> enchantment = enchantmentIds.byIdOrThrow(id);

			if (offeredLevels[id] > 0) offers.put(enchantment, offeredLevels[id]);
			if (states[id] > 0) checklistStates.put(enchantment, EnchantmentChecklist.State.values()[states[id]]);
		}

		return new EnchantmentChecklist(item(), offers, checklistStates, enchantments);
	}

	/** The golden apple checklist for the item in the table, from the Library's offers and the choices as last synced. */
	public AppleChecklist appleChecklist() {
		return new AppleChecklist(item(), effects(offeredEffects.get()), effects(chosenEffects.get()));
	}

	/** The table's Library Progress, as last synced. */
	public Library.Progress progress() {
		return new Library.Progress(progressLevels.get(), Library.fullLevels(enchantments));
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
		refreshLibrary();
		super.broadcastChanges();
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		boolean isButton = buttonId >= 0 && buttonId < FIRST_ENCHANTMENT_BUTTON + enchantmentIds.size();

		// The client shows nothing until the server's answer syncs back, so it only asks whether to send the click.
		if (!isButton || player.level().isClientSide()) return isButton;

		refreshLibrary();

		if (buttonId == SAVE_BUTTON) return save(player);
		if (buttonId < FIRST_ENCHANTMENT_BUTTON) return toggle(AppleEffect.values()[buttonId - 1]);

		return toggle(enchantmentIds.byIdOrThrow(buttonId - FIRST_ENCHANTMENT_BUTTON));
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

	private boolean toggle(Holder<Enchantment> enchantment) {
		EnchantmentChecklist checklist = enchantmentChecklist();

		if (!checklist.isListed(enchantment)) return false;

		states[enchantmentIds.getId(enchantment)] = checklist.state(enchantment).toggled().ordinal();

		return true;
	}

	private boolean toggle(AppleEffect effect) {
		AppleChecklist checklist = appleChecklist();

		if (!checklist.listed().contains(effect)) return false;

		Set<AppleEffect> chosen = EnumSet.noneOf(AppleEffect.class);

		chosen.addAll(checklist.chosen());

		if (!chosen.remove(effect)) chosen.add(effect);

		chosenEffects.set(mask(chosen));

		return true;
	}

	private boolean save(Player player) {
		Checklist checklist = checklist();

		if (!problems(checklist, player).isEmpty()) return false;

		Checklist.Cost cost = checklist.cost(player.hasInfiniteMaterials());
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
		writeStates(EnchantmentChecklist.preselected(checklistItem));
		chosenEffects.set(mask(AppleEffect.on(checklistItem)));
	}

	// Reads the Library afresh into the synced offers and progress, and awards a Full one to the player using it.
	private void refreshLibrary() {
		access.execute((level, pos) -> {
			Library library = Library.read(level, pos);
			Map<Holder<Enchantment>, Integer> offers = library.offers();
			Library.Progress progress = library.progress();

			for (int id = 0; id < enchantmentIds.size(); id++) {
				offeredLevels[id] = offers.getOrDefault(enchantmentIds.byIdOrThrow(id), 0);
			}

			progressLevels.set(progress.levels());
			offeredEffects.set(mask(library.appleOffers()));

			if (progress.isFull() && player instanceof ServerPlayer serverPlayer) MapleTriggers.FULL_LIBRARY.trigger(serverPlayer);
		});
	}

	private void writeStates(Map<Holder<Enchantment>, EnchantmentChecklist.State> checklistStates) {
		for (int id = 0; id < enchantmentIds.size(); id++) {
			states[id] = checklistStates.getOrDefault(enchantmentIds.byIdOrThrow(id), EnchantmentChecklist.State.UNSELECTED).ordinal();
		}
	}

	// Apple effects cross to the client as one bit each, in AppleEffect order.
	private static int mask(Set<AppleEffect> effects) {
		return effects.stream().mapToInt(effect -> 1 << effect.ordinal()).sum();
	}

	private static Set<AppleEffect> effects(int mask) {
		Set<AppleEffect> effects = EnumSet.noneOf(AppleEffect.class);

		Arrays.stream(AppleEffect.values()).filter(effect -> (mask & 1 << effect.ordinal()) != 0).forEach(effects::add);

		return effects;
	}

	private ItemStack item() {
		return tableSlots.getItem(ITEM_SLOT);
	}
}
