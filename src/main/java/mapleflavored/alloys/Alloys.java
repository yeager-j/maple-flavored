package mapleflavored.alloys;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import mapleflavored.MapleFlavored;
import mapleflavored.items.MapleItems;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

/**
 * The Materials and Alloys module's registrations: the Ancient Ember, the five {@link Alloy}s' ingots, blocks and
 * {@link AlloyGear}, the Alloy Furnace with its block entity, menu and {@code maple_flavored:alloying} recipe type,
 * {@link AlloyMining} and {@link IllagerNeutrality}.
 */
public final class Alloys {
	// A blast furnace's hardness and light while lit.
	private static final float FURNACE_HARDNESS = 3.5F;
	private static final int LIT_LIGHT = 13;

	/** The five Alloys' ingots. */
	public static final TagKey<Item> ALLOY_INGOTS = TagKey.create(Registries.ITEM, MapleFlavored.id("alloy_ingots"));

	/** The Alloy Furnace's fuel, dropped by Piglin Brutes that a player kills. One makes one Alloy ingot. */
	public static final Item ANCIENT_EMBER = MapleItems.register(
		"ancient_ember", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON).fireResistant()
	);

	/** Smelts a Netherite Ingot and a storage block into an Alloy ingot, burning one Ancient Ember. */
	public static final Block ALLOY_FURNACE = registerBlock(
		"alloy_furnace",
		AlloyFurnaceBlock::new,
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.instrument(NoteBlockInstrument.BASEDRUM)
			.requiresCorrectToolForDrops()
			.strength(FURNACE_HARDNESS)
			.lightLevel(state -> state.getValue(AlloyFurnaceBlock.LIT) ? LIT_LIGHT : 0),
		new Item.Properties()
	);

	/** The Alloy Furnace's block entity, which holds its slots and smelting progress. */
	public static final BlockEntityType<AlloyFurnaceBlockEntity> ALLOY_FURNACE_BLOCK_ENTITY = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		MapleFlavored.id("alloy_furnace"),
		new BlockEntityType<>(AlloyFurnaceBlockEntity::new, Set.of(ALLOY_FURNACE))
	);

	/** The Alloy Furnace's menu. */
	public static final MenuType<AlloyFurnaceMenu> ALLOY_FURNACE_MENU = Registry.register(
		BuiltInRegistries.MENU, MapleFlavored.id("alloy_furnace"), new MenuType<>(AlloyFurnaceMenu::new, FeatureFlags.VANILLA_SET)
	);

	/** The Alloy Furnace's recipes, each a base and an addition that make one result. */
	public static final RecipeType<AlloyingRecipe> ALLOYING = Registry.register(
		BuiltInRegistries.RECIPE_TYPE, MapleFlavored.id("alloying"), new RecipeType<AlloyingRecipe>() {
			@Override
			public String toString() {
				return "alloying";
			}
		}
	);

	/** The serializer for {@link #ALLOYING} recipes, written in data as {@code "type": "maple_flavored:alloying"}. */
	public static final RecipeSerializer<AlloyingRecipe> ALLOYING_SERIALIZER = Registry.register(
		BuiltInRegistries.RECIPE_SERIALIZER, MapleFlavored.id("alloying"), AlloyingRecipe.SERIALIZER
	);

	private Alloys() {
	}

	/**
	 * Registers the Ember, the Alloys and their gear, the Alloy Furnace, the alloying recipes' sync to clients, Alloy
	 * mining, Illager neutrality and the creative tab places. Call once from mod init.
	 */
	public static void register() {
		List<Alloy> alloys = Arrays.asList(Alloy.values());

		alloys.forEach(Alloy::register);
		AlloyGear.register();
		AlloyMining.register();
		IllagerNeutrality.register();
		AlloySets.register();
		OrichalcumSave.register();

		// The furnace's slots read the alloying recipes on the client too, to know what each slot accepts.
		RecipeSynchronization.synchronizeRecipeSerializer(ALLOYING_SERIALIZER);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			output.insertAfter(MapleItems.PITH, ANCIENT_EMBER);
			output.insertAfter(Items.NETHERITE_INGOT, alloys.stream().map(alloy -> new ItemStack(alloy.ingot())).toList());
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output ->
			output.insertAfter(Blocks.NETHERITE_BLOCK, alloys.stream().map(alloy -> new ItemStack(alloy.block())).toList())
		);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output ->
			output.insertAfter(Blocks.BLAST_FURNACE, ALLOY_FURNACE)
		);
	}

	// Registers the block and its block item, as vanilla's Items.registerBlock does.
	static Block registerBlock(
		String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties, Item.Properties itemProperties
	) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, MapleFlavored.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
		BlockItem item = (BlockItem) MapleItems.register(name, itemSettings -> new BlockItem(block, itemSettings), itemProperties.useBlockDescriptionPrefix());

		item.registerBlocks(Item.BY_BLOCK, item);

		return block;
	}
}
