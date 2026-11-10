package mapleflavored.reinforcement;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleSmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;

/**
 * The smithing recipe that Reinforces an armor piece: a template, the piece and a chainmail piece give the piece
 * Reinforced, keeping its enchantments, trim and wear. The recipe's data lists the pieces it takes as its base, the
 * {@code maple_flavored:reinforceable} tag. A piece already Reinforced makes nothing, and neither does a base that
 * isn't worn in an armor slot. Unlike a material upgrade, it accepts enchanted armor.
 */
public class ReinforcementRecipe extends SimpleSmithingRecipe {
	static final MapCodec<ReinforcementRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
		Ingredient.CODEC.fieldOf("template").forGetter(recipe -> recipe.template),
		Ingredient.CODEC.fieldOf("base").forGetter(recipe -> recipe.base),
		Ingredient.CODEC.fieldOf("addition").forGetter(recipe -> recipe.addition)
	).apply(i, ReinforcementRecipe::new));

	static final StreamCodec<RegistryFriendlyByteBuf, ReinforcementRecipe> STREAM_CODEC = StreamCodec.composite(
		Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
		Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.template,
		Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.base,
		Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.addition,
		ReinforcementRecipe::new
	);

	static final RecipeSerializer<ReinforcementRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	private final Ingredient template;
	private final Ingredient base;
	private final Ingredient addition;

	/** A recipe that Reinforces {@code base} items, with a {@code template} and an {@code addition}. */
	public ReinforcementRecipe(Recipe.CommonInfo commonInfo, Ingredient template, Ingredient base, Ingredient addition) {
		super(commonInfo);
		this.template = template;
		this.base = base;
		this.addition = addition;
	}

	@Override
	public ItemStack assemble(SmithingRecipeInput input) {
		return Reinforcement.reinforce(input.base());
	}

	@Override
	public Optional<Ingredient> templateIngredient() {
		return Optional.of(template);
	}

	@Override
	public Ingredient baseIngredient() {
		return base;
	}

	@Override
	public Optional<Ingredient> additionIngredient() {
		return Optional.of(addition);
	}

	@Override
	public RecipeSerializer<ReinforcementRecipe> getSerializer() {
		return Reinforcement.SERIALIZER;
	}

	@Override
	protected PlacementInfo createPlacementInfo() {
		return PlacementInfo.create(List.of(template, base, addition));
	}

	// Reinforced looks the same in a slot, so the display shows the base as the result.
	@Override
	public List<RecipeDisplay> display() {
		return List.of(new SmithingRecipeDisplay(
			template.display(), base.display(), addition.display(), base.display(), new SlotDisplay.ItemSlotDisplay(Items.SMITHING_TABLE)
		));
	}
}
