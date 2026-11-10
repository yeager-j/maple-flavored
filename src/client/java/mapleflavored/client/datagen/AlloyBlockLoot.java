package mapleflavored.client.datagen;

import java.util.concurrent.CompletableFuture;
import mapleflavored.alloys.Alloy;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.core.HolderLookup;

/** The Alloy blocks' loot tables: each drops itself. */
class AlloyBlockLoot extends FabricBlockLootSubProvider {
	AlloyBlockLoot(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	public void generate() {
		for (Alloy alloy : Alloy.values()) dropSelf(alloy.block());
	}
}
