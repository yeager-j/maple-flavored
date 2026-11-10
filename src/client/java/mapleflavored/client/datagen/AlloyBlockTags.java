package mapleflavored.client.datagen;

import java.util.concurrent.CompletableFuture;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyBeacons;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;

/** Lists the Alloys' blocks in {@code #maple_flavored:alloy_blocks}, which the hand-written block tags include. */
class AlloyBlockTags extends FabricTagsProvider.BlockTagsProvider {
	AlloyBlockTags(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		var alloyBlocks = builder(AlloyBeacons.ALLOY_BLOCKS);

		for (Alloy alloy : Alloy.values()) alloyBlocks.add(alloy.block().builtInRegistryHolder().key());
	}
}
