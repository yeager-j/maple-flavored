package mapleflavored.client.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/** Generates the Alloy gear's item models, smithing recipes and tags into {@code src/main/generated}. */
public class MapleDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();

		pack.addProvider(AlloyGearModels::new);
		pack.addProvider(AlloyGearRecipes::new);
		pack.addProvider(AlloyGearTags::new);
	}
}
