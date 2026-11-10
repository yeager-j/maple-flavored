package mapleflavored.client.datagen;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.AlloySets;
import mapleflavored.alloys.Alloys;
import mapleflavored.alloys.GearKind;
import mapleflavored.enchanting.EnchantmentSlots;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Lists the Alloys' ingots and Armored Elytras in their own tags. Puts Alloy gear in every item tag that names its
 * Netherite version, and Armored Elytra in the tags that let a chestplate be enchanted. Gives Electrum what gold has:
 * its armor is in Piglins' gold armor tag, and its gear is listed after gold's for the extra Enchantment Slot.
 */
class AlloyItemTags extends FabricTagsProvider.ItemTagsProvider {
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

	// The enchantable tags that hold a chestplate. Not #chest_armor itself, which would make the Armored Elytra trimmable.
	private static final List<TagKey<Item>> CHESTPLATE_ENCHANTABLE_TAGS = List.of(
		ItemTags.CHEST_ARMOR_ENCHANTABLE, ItemTags.DURABILITY_ENCHANTABLE, ItemTags.EQUIPPABLE_ENCHANTABLE
	);

	// Vanilla's gold gear that can be enchanted, which mob armor can't.
	private static final List<Item> GOLD_GEAR = List.of(
		Items.GOLDEN_SWORD, Items.GOLDEN_SPEAR, Items.GOLDEN_SHOVEL, Items.GOLDEN_PICKAXE, Items.GOLDEN_AXE, Items.GOLDEN_HOE,
		Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS
	);

	AlloyItemTags(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		var alloyIngots = builder(Alloys.ALLOY_INGOTS);
		var armoredElytras = builder(AlloyGear.ARMORED_ELYTRAS_TAG);

		for (Alloy alloy : Alloy.values()) {
			alloyIngots.add(key(alloy.ingot()));
			armoredElytras.add(key(AlloyGear.armoredElytra(alloy)));
		}

		NETHERITE_TAGS.forEach((tag, kinds) -> {
			var appender = builder(tag);

			for (GearKind kind : kinds) {
				for (Alloy alloy : Alloy.values()) appender.add(key(AlloyGear.of(alloy, kind)));
			}
		});

		for (TagKey<Item> tag : CHESTPLATE_ENCHANTABLE_TAGS) {
			var appender = builder(tag);

			for (Alloy alloy : Alloy.values()) appender.add(key(AlloyGear.armoredElytra(alloy)));
		}

		// The pieces a player wears, which is all Piglins check; mob body armor can't make Piglins neutral.
		var piglinSafeArmor = builder(ItemTags.PIGLIN_SAFE_ARMOR);

		for (GearKind kind : AlloySets.PIECES) piglinSafeArmor.add(key(AlloyGear.of(Alloy.ELECTRUM, kind)));

		piglinSafeArmor.add(key(AlloyGear.armoredElytra(Alloy.ELECTRUM)));

		var extraEnchantmentSlot = builder(EnchantmentSlots.EXTRA_SLOT_GEAR);

		for (Item gold : GOLD_GEAR) extraEnchantmentSlot.add(key(gold));

		for (GearKind kind : GearKind.values()) {
			if (kind.hasDurability()) extraEnchantmentSlot.add(key(AlloyGear.of(Alloy.ELECTRUM, kind)));
		}

		extraEnchantmentSlot.add(key(AlloyGear.armoredElytra(Alloy.ELECTRUM)));
	}

	private static ResourceKey<Item> key(Item item) {
		return item.builtInRegistryHolder().key();
	}

	private static TagKey<Item> conventionTag(String path) {
		return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
	}
}
