package mapleflavored.alloys;

import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

/**
 * The Alloy Furnace's slots and what each accepts. The block entity (for hoppers) and the menu (for players, on both
 * sides) share these rules. The base and addition slots accept what some alloying recipe takes, read from the recipes
 * Fabric syncs to clients, so the recipes stay the only list of valid inputs.
 */
public final class AlloyFurnaceSlots {
	/** The Netherite Ingot slot. */
	public static final int BASE = 0;
	/** The storage block slot. */
	public static final int ADDITION = 1;
	/** The Ancient Ember slot. */
	public static final int FUEL = 2;
	/** The Alloy ingot slot. */
	public static final int RESULT = 3;
	/** How many slots the furnace has. */
	public static final int COUNT = 4;

	private AlloyFurnaceSlots() {
	}

	/** Whether the stack can go in the slot. Nothing can be put in the result slot. */
	static boolean accepts(Level level, int slot, ItemStack stack) {
		return switch (slot) {
			case BASE -> anyRecipe(level, recipe -> recipe.acceptsBase(stack));
			case ADDITION -> anyRecipe(level, recipe -> recipe.acceptsAddition(stack));
			case FUEL -> stack.is(Alloys.ANCIENT_EMBER);
			default -> false;
		};
	}

	private static boolean anyRecipe(Level level, Predicate<AlloyingRecipe> test) {
		return level.recipeAccess().getSynchronizedRecipes().getAllOfType(Alloys.ALLOYING).stream()
			.map(RecipeHolder::value)
			.anyMatch(test);
	}
}
