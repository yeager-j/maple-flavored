package mapleflavored.client.datagen;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.GearKind;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Puts Alloy gear in every item tag that names its Netherite version. */
class AlloyGearTags extends FabricTagsProvider.ItemTagsProvider {
	// The item tags, vanilla's and Fabric's convention tags, that list Netherite gear by name. Tags such as the
	// enchantable ones include these.
	private static final Map<TagKey<Item>, List<GearKind>> NETHERITE_TAGS = Map.ofEntries(
		Map.entry(ItemTags.SWORDS, List.of(GearKind.SWORD)),
		Map.entry(ItemTags.SHOVELS, List.of(GearKind.SHOVEL)),
		Map.entry(ItemTags.PICKAXES, List.of(GearKind.PICKAXE)),
		Map.entry(ItemTags.CLUSTER_MAX_HARVESTABLES, List.of(GearKind.PICKAXE)),
		Map.entry(ItemTags.AXES, List.of(GearKind.AXE)),
		Map.entry(ItemTags.HOES, List.of(GearKind.HOE)),
		Map.entry(ItemTags.SPEARS, List.of(GearKind.SPEAR)),
		Map.entry(ItemTags.HEAD_ARMOR, List.of(GearKind.HELMET)),
		Map.entry(ItemTags.CHEST_ARMOR, List.of(GearKind.CHESTPLATE)),
		Map.entry(ItemTags.LEG_ARMOR, List.of(GearKind.LEGGINGS)),
		Map.entry(ItemTags.FOOT_ARMOR, List.of(GearKind.BOOTS)),
		Map.entry(conventionTag("tools/melee_weapon"), List.of(GearKind.SWORD, GearKind.AXE, GearKind.SPEAR)),
		Map.entry(conventionTag("tools/mining_tool"), List.of(GearKind.PICKAXE)),
		Map.entry(conventionTag("armors/horse"), List.of(GearKind.HORSE_ARMOR)),
		Map.entry(conventionTag("armors/nautilus"), List.of(GearKind.NAUTILUS_ARMOR))
	);

	AlloyGearTags(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		NETHERITE_TAGS.forEach((tag, kinds) -> {
			var appender = builder(tag);

			for (GearKind kind : kinds) {
				for (Alloy alloy : Alloy.values()) appender.add(AlloyGear.of(alloy, kind).builtInRegistryHolder().key());
			}
		});
	}

	private static TagKey<Item> conventionTag(String path) {
		return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
	}
}
