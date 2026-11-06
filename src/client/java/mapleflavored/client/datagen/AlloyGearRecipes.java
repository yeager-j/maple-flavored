package mapleflavored.client.datagen;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import mapleflavored.MapleFlavored;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.GearKind;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;

/**
 * The Alloy gear's smithing recipes: a Netherite item, or another Alloy's version of it, and an Alloy ingot make that
 * Alloy's version, with no template.
 */
class AlloyGearRecipes extends FabricRecipeProvider {
	AlloyGearRecipes(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected RecipeProvider createRecipeProvider(
		HolderLookup.Provider registries, BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements
	) {
		return new RecipeProvider(recipes, advancements) {
			@Override
			public void buildRecipes() {
				for (Alloy alloy : Alloy.values()) {
					for (GearKind kind : GearKind.values()) {
						output.accept(recipeKey(alloy, kind), smithingInto(alloy, kind), null);
					}
				}
			}
		};
	}

	@Override
	public String getName() {
		return "Alloy gear smithing recipes";
	}

	private static ResourceKey<Recipe<?>> recipeKey(Alloy alloy, GearKind kind) {
		return ResourceKey.create(Registries.RECIPE, MapleFlavored.id(alloy.path() + "_" + kind.path() + "_smithing"));
	}

	private static SmithingTransformRecipe smithingInto(Alloy alloy, GearKind kind) {
		Stream<Item> otherAlloys = Arrays.stream(Alloy.values()).filter(other -> other != alloy).map(other -> AlloyGear.of(other, kind));
		Ingredient bases = Ingredient.of(Stream.concat(Stream.of(kind.netheriteItem()), otherAlloys));

		return new SmithingTransformRecipe(
			new Recipe.CommonInfo(true),
			Optional.empty(),
			bases,
			Optional.of(Ingredient.of(alloy.ingot())),
			new ItemStackTemplate(AlloyGear.of(alloy, kind))
		);
	}
}
