package mapleflavored.client.datagen;

import java.util.Map;
import java.util.function.Consumer;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.GearKind;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.world.item.Item;

/**
 * The Alloys' models: each block a plain cube, each ingot a flat item, and the gear made the way vanilla makes the
 * Netherite versions' and the Elytra's.
 */
class AlloyModels extends FabricModelProvider {
	AlloyModels(FabricPackOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators generators) {
		for (Alloy alloy : Alloy.values()) generators.createTrivialCube(alloy.block());
	}

	@Override
	public void generateItemModels(ItemModelGenerators generators) {
		for (Alloy alloy : Alloy.values()) {
			generators.generateFlatItem(alloy.ingot(), ModelTemplates.FLAT_ITEM);

			for (GearKind kind : GearKind.values()) gearModel(generators, kind).accept(AlloyGear.of(alloy, kind));

			generators.generateElytra(AlloyGear.armoredElytra(alloy));
		}
	}

	// A switch over every kind, so a new kind can't compile without a model.
	private static Consumer<Item> gearModel(ItemModelGenerators generators, GearKind kind) {
		return switch (kind) {
			case SWORD, SHOVEL, PICKAXE, AXE, HOE -> item -> generators.generateFlatItem(item, ModelTemplates.FLAT_HANDHELD_ITEM);
			case SPEAR -> generators::generateSpear;
			case HELMET -> item -> generators.generateTrimmableItem(item, ItemModelGenerators.TRIM_PREFIX_HELMET, false, Map.of());
			case CHESTPLATE -> item -> generators.generateTrimmableItem(item, ItemModelGenerators.TRIM_PREFIX_CHESTPLATE, false, Map.of());
			case LEGGINGS -> item -> generators.generateTrimmableItem(item, ItemModelGenerators.TRIM_PREFIX_LEGGINGS, false, Map.of());
			case BOOTS -> item -> generators.generateTrimmableItem(item, ItemModelGenerators.TRIM_PREFIX_BOOTS, false, Map.of());
			case HORSE_ARMOR, NAUTILUS_ARMOR -> item -> generators.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
		};
	}
}
