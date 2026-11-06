package mapleflavored.alloys;

import static mapleflavored.alloys.AlloyFurnaceSlots.ADDITION;
import static mapleflavored.alloys.AlloyFurnaceSlots.BASE;
import static mapleflavored.alloys.AlloyFurnaceSlots.COUNT;
import static mapleflavored.alloys.AlloyFurnaceSlots.FUEL;
import static mapleflavored.alloys.AlloyFurnaceSlots.RESULT;

import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The Alloy Furnace's menu: base, addition, Ember and result slots, and the smelt's progress for the arrow. */
public class AlloyFurnaceMenu extends AbstractContainerMenu {
	static final int DATA_COUNT = 1;

	private static final int PROGRESS_DATA = 0;
	private static final int INVENTORY_START = COUNT;
	private static final int HOTBAR_START = INVENTORY_START + 27;
	private static final int INVENTORY_END = HOTBAR_START + 9;

	private final Container container;
	private final ContainerData data;
	private final Level level;

	/** The client's copy of the menu, filled by the server. */
	public AlloyFurnaceMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(COUNT), new SimpleContainerData(DATA_COUNT));
	}

	AlloyFurnaceMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
		super(Alloys.ALLOY_FURNACE_MENU, containerId);
		checkContainerSize(container, COUNT);
		checkContainerDataCount(data, DATA_COUNT);
		this.container = container;
		this.data = data;
		this.level = inventory.player.level();

		addSlot(new InputSlot(container, BASE, 38, 17));
		addSlot(new InputSlot(container, ADDITION, 56, 17));
		addSlot(new InputSlot(container, FUEL, 47, 53));
		addSlot(new ResultSlot(container, RESULT, 116, 35));
		addStandardInventorySlots(inventory, 8, 84);
		addDataSlots(data);
	}

	/** How far the current smelt has got, from 0 to 1. */
	public float progress() {
		return Mth.clamp((float) data.get(PROGRESS_DATA) / AlloyFurnaceBlockEntity.SMELT_TICKS, 0.0F, 1.0F);
	}

	/** Whether the furnace is smelting, burning an Ember. */
	public boolean isSmelting() {
		return data.get(PROGRESS_DATA) > 0;
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = slots.get(slotIndex);

		if (!slot.hasItem()) return ItemStack.EMPTY;

		ItemStack stack = slot.getItem();
		ItemStack moved = stack.copy();

		if (!moveOut(slotIndex, stack)) return ItemStack.EMPTY;

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}

		if (stack.getCount() == moved.getCount()) return ItemStack.EMPTY;

		slot.onTake(player, stack);

		return moved;
	}

	// Furnace slots empty into the player's inventory; inventory items go to the furnace slot that takes them, or
	// between the inventory and hotbar otherwise.
	private boolean moveOut(int slotIndex, ItemStack stack) {
		if (slotIndex < INVENTORY_START) return moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, slotIndex == RESULT);

		for (int furnaceSlot : new int[] {BASE, ADDITION, FUEL}) {
			if (AlloyFurnaceSlots.accepts(level, furnaceSlot, stack)) return moveItemStackTo(stack, furnaceSlot, furnaceSlot + 1, false);
		}

		return slotIndex < HOTBAR_START
			? moveItemStackTo(stack, HOTBAR_START, INVENTORY_END, false)
			: moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false);
	}

	private final class InputSlot extends Slot {
		private InputSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return AlloyFurnaceSlots.accepts(level, getContainerSlot(), stack);
		}
	}

	private static final class ResultSlot extends Slot {
		private ResultSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}
	}
}
