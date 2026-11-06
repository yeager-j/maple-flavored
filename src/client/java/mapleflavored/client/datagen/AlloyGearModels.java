package mapleflavored.client.datagen;

import java.util.Map;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.GearKind;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;

/** The Alloy gear's item models, made the way vanilla makes the Netherite versions'. */
class AlloyGearModels extends FabricModelProvider {
	AlloyGearModels(FabricPackOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators generators) {
	}

	@Override
	public void generateItemModels(ItemModelGenerators generators) {
		for (Alloy alloy : Alloy.values()) {
			for (GearKind kind : new GearKind[] {GearKind.SWORD, GearKind.SHOVEL, GearKind.PICKAXE, GearKind.AXE, GearKind.HOE}) {
				generators.generateFlatItem(AlloyGear.of(alloy, kind), ModelTemplates.FLAT_HANDHELD_ITEM);
			}

			generators.generateSpear(AlloyGear.of(alloy, GearKind.SPEAR));
			generators.generateTrimmableArmorSet(
				AlloyGear.of(alloy, GearKind.HELMET),
				AlloyGear.of(alloy, GearKind.CHESTPLATE),
				AlloyGear.of(alloy, GearKind.LEGGINGS),
				AlloyGear.of(alloy, GearKind.BOOTS),
				false,
				Map.of()
			);
			generators.generateFlatItem(AlloyGear.of(alloy, GearKind.HORSE_ARMOR), ModelTemplates.FLAT_ITEM);
			generators.generateFlatItem(AlloyGear.of(alloy, GearKind.NAUTILUS_ARMOR), ModelTemplates.FLAT_ITEM);
		}
	}
}
