package mapleflavored.alloys;

import static mapleflavored.alloys.AlloyFurnaceSlots.ADDITION;
import static mapleflavored.alloys.AlloyFurnaceSlots.BASE;
import static mapleflavored.alloys.AlloyFurnaceSlots.FUEL;
import static mapleflavored.alloys.AlloyFurnaceSlots.RESULT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Alloy Furnace's slots and smelting. It works only while it can finish: an alloying recipe's base and addition,
 * an Ancient Ember, and room for the result. All three inputs are used up together when the ingot is made, so an
 * interrupted smelt loses nothing and restarts from the beginning.
 *
 * <p>Hoppers feed the base and addition from above and the Ember from the sides, and take the result from below.
 */
public class AlloyFurnaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
	/** How long one Alloy ingot takes to smelt: 10 seconds. */
	public static final int SMELT_TICKS = 200;

	private static final Component DEFAULT_NAME = Component.translatable("container.maple_flavored.alloy_furnace");
	private static final int[] SLOTS_FOR_UP = {BASE, ADDITION};
	private static final int[] SLOTS_FOR_SIDES = {FUEL};
	private static final int[] SLOTS_FOR_DOWN = {RESULT};
	private static final String PROGRESS_KEY = "progress";

	private NonNullList<ItemStack> items = NonNullList.withSize(AlloyFurnaceSlots.COUNT, ItemStack.EMPTY);
	private int progress;

	private final RecipeManager.CachedCheck<AlloyingInput, AlloyingRecipe> recipes = RecipeManager.createCheck(Alloys.ALLOYING);

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return progress;
		}

		@Override
		public void set(int index, int value) {
			progress = value;
		}

		@Override
		public int getCount() {
			return AlloyFurnaceMenu.DATA_COUNT;
		}
	};

	public AlloyFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(Alloys.ALLOY_FURNACE_BLOCK_ENTITY, pos, state);
	}

	void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		ItemStack result = nextResult(level);
		boolean smelting = !result.isEmpty();
		int previousProgress = progress;

		if (!smelting) {
			progress = 0;
		} else if (++progress >= SMELT_TICKS) {
			finish(result);
		}

		if (state.getValue(AbstractFurnaceBlock.LIT) != smelting) {
			level.setBlock(pos, state.setValue(AbstractFurnaceBlock.LIT, smelting), Block.UPDATE_ALL);
		}

		if (progress != previousProgress) setChanged();
	}

	// What the furnace would make from its inputs now, or empty when it can't smelt.
	private ItemStack nextResult(ServerLevel level) {
		if (!items.get(FUEL).is(Alloys.ANCIENT_EMBER)) return ItemStack.EMPTY;

		AlloyingInput input = new AlloyingInput(items.get(BASE), items.get(ADDITION));
		ItemStack result = recipes.getRecipeFor(input, level).map(RecipeHolder::value).map(recipe -> recipe.assemble(input)).orElse(ItemStack.EMPTY);

		return fitsInResultSlot(result) ? result : ItemStack.EMPTY;
	}

	private boolean fitsInResultSlot(ItemStack result) {
		ItemStack current = items.get(RESULT);

		if (current.isEmpty()) return true;

		return ItemStack.isSameItemSameComponents(current, result) && current.getCount() + result.getCount() <= getMaxStackSize(current);
	}

	private void finish(ItemStack result) {
		items.get(BASE).shrink(1);
		items.get(ADDITION).shrink(1);
		items.get(FUEL).shrink(1);

		ItemStack current = items.get(RESULT);

		if (current.isEmpty()) {
			items.set(RESULT, result);
		} else {
			current.grow(result.getCount());
		}

		progress = 0;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		ItemStack previous = items.get(slot);
		boolean sameItem = !stack.isEmpty() && ItemStack.isSameItemSameComponents(previous, stack);

		super.setItem(slot, stack);

		// As in vanilla's furnace, a different input starts the smelt over.
		if ((slot == BASE || slot == ADDITION) && !sameItem) progress = 0;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return level != null && AlloyFurnaceSlots.accepts(level, slot, stack);
	}

	@Override
	public int[] getSlotsForFace(Direction direction) {
		return switch (direction) {
			case UP -> SLOTS_FOR_UP;
			case DOWN -> SLOTS_FOR_DOWN;
			default -> SLOTS_FOR_SIDES;
		};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return slot == RESULT;
	}

	@Override
	public int getContainerSize() {
		return items.size();
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	protected Component getDefaultName() {
		return DEFAULT_NAME;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new AlloyFurnaceMenu(containerId, inventory, this, data);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		progress = input.getIntOr(PROGRESS_KEY, 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putInt(PROGRESS_KEY, progress);
	}
}
