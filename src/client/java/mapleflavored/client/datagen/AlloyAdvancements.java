package mapleflavored.client.datagen;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import mapleflavored.MapleFlavored;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.AlloySets;
import mapleflavored.alloys.GearKind;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;

/** Full Metal Wardrobe: wear a full set of every Alloy, one criterion per Alloy. */
class AlloyAdvancements extends FabricAdvancementProvider {
	private static final String NAME = "nether/wear_every_alloy_set";
	private static final String TRANSLATION = "advancements.maple_flavored.nether.wear_every_alloy_set";
	private static final int EXPERIENCE = 100;

	AlloyAdvancements(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	public void generateAdvancement(HolderLookup.Provider registries, Consumer<AdvancementHolder> advancements) {
		Advancement.Builder wearEverySet = new Advancement.Builder()
			.parent(createPlaceholder(MapleFlavored.id("nether/smelt_alloy_ingot")))
			.display(
				AlloyGear.of(Alloy.ADAMANT, GearKind.CHESTPLATE),
				Component.translatable(TRANSLATION + ".title"),
				Component.translatable(TRANSLATION + ".description"),
				AdvancementType.CHALLENGE,
				true,
				true,
				false
			)
			.rewards(AdvancementRewards.Builder.experience(EXPERIENCE));

		for (Alloy alloy : Alloy.values()) wearEverySet.addCriterion(alloy.path() + "_set", AlloySets.wornCriterion(alloy));

		advancements.accept(wearEverySet.build(MapleFlavored.id(NAME)));
	}
}
