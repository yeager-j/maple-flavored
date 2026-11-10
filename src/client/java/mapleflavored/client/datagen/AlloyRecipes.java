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
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;

/**
 * The Alloys' crafting and smithing recipes. Nine ingots craft a block, and a block crafts back into nine.
 *
 * <p>The smithing recipes all have no template: a Netherite item, or another Alloy's version of it, and an Alloy ingot
 * make that Alloy's version. An Alloy chestplate and an Elytra make that Alloy's Armored Elytra, which changes Alloy
 * with an ingot like the rest.
 */
class AlloyRecipes extends FabricRecipeProvider {
	AlloyRecipes(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
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
					String ingot = getItemName(alloy.ingot());

					// Named and grouped as vanilla's Netherite Ingot from a block is, which leaves the ingot's own
					// recipe id to the Alloy Furnace.
					nineBlockStorageRecipesRecipesWithCustomUnpacking(
						RecipeCategory.MISC, alloy.ingot(), RecipeCategory.BUILDING_BLOCKS, alloy.block(),
						ingot + "_from_" + getItemName(alloy.block()), ingot
					);

					for (GearKind kind : GearKind.values()) {
						Item gear = AlloyGear.of(alloy, kind);

						output.accept(smithingKey(getItemName(gear)), smithingInto(alloy, kind), null);
					}

					String armoredElytra = getItemName(AlloyGear.armoredElytra(alloy));

					output.accept(smithingKey(armoredElytra), armoredElytraFromChestplate(alloy), null);
					output.accept(smithingKey(armoredElytra + "_from_alloy"), armoredElytraChangingAlloy(alloy), null);
				}
			}
		};
	}

	@Override
	public String getName() {
		return "Alloy recipes";
	}

	private static ResourceKey<Recipe<?>> smithingKey(String result) {
		return ResourceKey.create(Registries.RECIPE, MapleFlavored.id(result + "_smithing"));
	}

	private static SmithingTransformRecipe smithingInto(Alloy alloy, GearKind kind) {
		Stream<Item> otherAlloys = Arrays.stream(Alloy.values()).filter(other -> other != alloy).map(other -> AlloyGear.of(other, kind));
		Ingredient bases = Ingredient.of(Stream.concat(Stream.of(kind.netheriteItem()), otherAlloys));

		return withNoTemplate(bases, Ingredient.of(alloy.ingot()), AlloyGear.of(alloy, kind));
	}

	// The chestplate is the base, so its trim, name and wear carry over.
	private static SmithingTransformRecipe armoredElytraFromChestplate(Alloy alloy) {
		return withNoTemplate(Ingredient.of(AlloyGear.of(alloy, GearKind.CHESTPLATE)), Ingredient.of(Items.ELYTRA), AlloyGear.armoredElytra(alloy));
	}

	private static SmithingTransformRecipe armoredElytraChangingAlloy(Alloy alloy) {
		Stream<Item> otherAlloys = Arrays.stream(Alloy.values()).filter(other -> other != alloy).map(AlloyGear::armoredElytra);

		return withNoTemplate(Ingredient.of(otherAlloys), Ingredient.of(alloy.ingot()), AlloyGear.armoredElytra(alloy));
	}

	private static SmithingTransformRecipe withNoTemplate(Ingredient base, Ingredient addition, Item result) {
		return new SmithingTransformRecipe(
			new Recipe.CommonInfo(true), Optional.empty(), base, Optional.of(addition), new ItemStackTemplate(result)
		);
	}
}
