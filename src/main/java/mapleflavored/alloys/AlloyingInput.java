package mapleflavored.alloys;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** What an {@link AlloyingRecipe} is matched against: the Alloy Furnace's base and addition slots. */
public record AlloyingInput(ItemStack base, ItemStack addition) implements RecipeInput {
	@Override
	public ItemStack getItem(int index) {
		return switch (index) {
			case 0 -> base;
			case 1 -> addition;
			default -> throw new IllegalArgumentException("No item for index " + index);
		};
	}

	@Override
	public int size() {
		return 2;
	}
}
