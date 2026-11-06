package mapleflavored.alloys;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * An Alloy Furnace recipe: a base and an addition that smelt into a result, burning one Ancient Ember.
 *
 * <p>In data: {@code {"type": "maple_flavored:alloying", "base": <ingredient>, "addition": <ingredient>,
 * "result": <item stack>}}. Like brewing, it has no recipe book entry.
 */
public final class AlloyingRecipe implements Recipe<AlloyingInput> {
	private static final MapCodec<AlloyingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Ingredient.CODEC.fieldOf("base").forGetter(recipe -> recipe.base),
			Ingredient.CODEC.fieldOf("addition").forGetter(recipe -> recipe.addition),
			ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
		)
		.apply(instance, AlloyingRecipe::new)
	);

	private static final StreamCodec<RegistryFriendlyByteBuf, AlloyingRecipe> STREAM_CODEC = StreamCodec.composite(
		Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.base,
		Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.addition,
		ItemStackTemplate.STREAM_CODEC, recipe -> recipe.result,
		AlloyingRecipe::new
	);

	static final RecipeSerializer<AlloyingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	private final Ingredient base;
	private final Ingredient addition;
	private final ItemStackTemplate result;

	private AlloyingRecipe(Ingredient base, Ingredient addition, ItemStackTemplate result) {
		this.base = base;
		this.addition = addition;
		this.result = result;
	}

	/** Whether this recipe takes the stack in the furnace's base slot. */
	public boolean acceptsBase(ItemStack stack) {
		return base.test(stack);
	}

	/** Whether this recipe takes the stack in the furnace's addition slot. */
	public boolean acceptsAddition(ItemStack stack) {
		return addition.test(stack);
	}

	@Override
	public boolean matches(AlloyingInput input, Level level) {
		return acceptsBase(input.base()) && acceptsAddition(input.addition());
	}

	@Override
	public ItemStack assemble(AlloyingInput input) {
		return result.create();
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public boolean showNotification() {
		return false;
	}

	@Override
	public String group() {
		return "";
	}

	@Override
	public RecipeSerializer<AlloyingRecipe> getSerializer() {
		return Alloys.ALLOYING_SERIALIZER;
	}

	@Override
	public RecipeType<AlloyingRecipe> getType() {
		return Alloys.ALLOYING;
	}

	@Override
	public PlacementInfo placementInfo() {
		return PlacementInfo.NOT_PLACEABLE;
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		return RecipeBookCategories.CRAFTING_MISC;
	}
}
