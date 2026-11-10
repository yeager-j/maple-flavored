package mapleflavored.client.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * Generates into {@code src/main/generated} everything that follows from the list of Alloys: their models, equipment
 * assets, recipes, block loot, tags and the advancement for wearing every set. What an Alloy is made from, its
 * textures and its names are written by hand.
 */
public class MapleDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();

		pack.addProvider(AlloyModels::new);
		pack.addProvider(AlloyEquipment::new);
		pack.addProvider(AlloyRecipes::new);
		pack.addProvider(AlloyBlockLoot::new);
		pack.addProvider(AlloyItemTags::new);
		pack.addProvider(AlloyBlockTags::new);
		pack.addProvider(AlloyAdvancements::new);
	}
}
