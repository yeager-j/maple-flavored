package mapleflavored.materialunits;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.enchantment.Repairable;

/**
 * The material unit of each item: the one material that enchants and repairs it, and how many units a full repair
 * takes.
 */
public final class MaterialUnits {
	/** Materials that replace the item's vanilla repair item, or stand in for one it lacks. */
	private static final Map<Item, Item> MATERIAL_OVERRIDES = Map.ofEntries(
		Map.entry(Items.BOW, Items.STRING),
		Map.entry(Items.CROSSBOW, Items.STRING),
		Map.entry(Items.FISHING_ROD, Items.STRING),
		Map.entry(Items.TRIDENT, Items.PRISMARINE_SHARD),
		Map.entry(Items.SHEARS, Items.IRON_INGOT),
		Map.entry(Items.FLINT_AND_STEEL, Items.IRON_INGOT),
		Map.entry(Items.GOLDEN_APPLE, Items.GOLD_BLOCK),
		Map.entry(Items.ENCHANTED_GOLDEN_APPLE, Items.GOLD_BLOCK),
		// No vanilla repair item: each one's main crafting ingredient. Their recipes use every ingredient once, so
		// the defining one is picked by hand, and a bait rather than a fishing rod, since items never combine.
		Map.entry(Items.BRUSH, Items.COPPER_INGOT),
		Map.entry(Items.CARROT_ON_A_STICK, Items.CARROT),
		Map.entry(Items.WARPED_FUNGUS_ON_A_STICK, Items.WARPED_FUNGUS)
	);
	/** Units for a full repair of items with no crafting recipe, matching vanilla's 25% per unit. */
	private static final Map<Item, Integer> UNCRAFTED_FULL_REPAIR_UNITS = Map.of(Items.ELYTRA, 4, Items.TRIDENT, 4);
	/** Chainmail has no recipe, so it repairs like the iron piece for the same slot. */
	private static final Map<Item, Item> REPAIRS_LIKE = Map.of(
		Items.CHAINMAIL_HELMET, Items.IRON_HELMET,
		Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE,
		Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS,
		Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS
	);

	private MaterialUnits() {
	}

	/** The item's material, or empty if it has none, so it can be neither repaired nor enchanted. */
	public static Optional<Ingredient> materialOf(ItemStack item) {
		Item override = MATERIAL_OVERRIDES.get(item.getItem());

		if (override != null) return Optional.of(Ingredient.of(override));

		Repairable repairable = item.get(DataComponents.REPAIRABLE);

		return repairable == null ? Optional.empty() : Optional.of(Ingredient.of(repairable.items()));
	}

	/**
	 * The units of the item's material a full repair takes, at least 1: one fewer than its cheapest crafting recipe
	 * uses, or 1 for Netherite gear.
	 *
	 * @param recipes the server's recipes, which the client doesn't have
	 * @throws IllegalArgumentException if the item has no material
	 */
	public static int fullRepairUnits(ItemStack item, RecipeManager recipes) {
		Ingredient material = materialOf(item).orElseThrow(() -> new IllegalArgumentException(item + " has no material unit"));

		if (material.test(new ItemStack(Items.NETHERITE_INGOT))) return 1;

		Integer uncraftedUnits = UNCRAFTED_FULL_REPAIR_UNITS.get(item.getItem());

		if (uncraftedUnits != null) return uncraftedUnits;

		Item craftedItem = REPAIRS_LIKE.getOrDefault(item.getItem(), item.getItem());
		OptionalInt craftingUnits = cheapestCraftingUnits(craftedItem, material, recipes);

		return Math.max(craftingUnits.orElse(1) - 1, 1);
	}

	private static OptionalInt cheapestCraftingUnits(Item craftedItem, Ingredient material, RecipeManager recipes) {
		return recipes.getRecipes().stream()
			.map(RecipeHolder::value)
			.filter(recipe -> recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe)
			.map(NormalCraftingRecipe.class::cast)
			.filter(recipe -> recipe.assemble(CraftingInput.EMPTY).is(craftedItem))
			.mapToInt(recipe -> unitsUsedBy(recipe, material))
			.filter(units -> units > 0)
			.min();
	}

	// Ingredient.items() is deprecated, but comparing two ingredients needs their items, and test() takes only a stack.
	@SuppressWarnings("deprecation")
	private static int unitsUsedBy(Recipe<?> recipe, Ingredient material) {
		return (int) recipe.placementInfo().ingredients().stream()
			.filter(ingredient -> ingredient.items().anyMatch(material::acceptsItem))
			.count();
	}
}
