package mapleflavored.alloys;

import java.util.Locale;
import java.util.Optional;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;

/**
 * The kinds of gear that have a Netherite version, and so an Alloy version. Each builds its item with vanilla's own
 * helper and Netherite's numbers from {@link Items}.
 */
public enum GearKind {
	SWORD(Items.NETHERITE_SWORD, EquipmentSlot.MAINHAND, null),
	SHOVEL(Items.NETHERITE_SHOVEL, EquipmentSlot.MAINHAND, BlockTags.MINEABLE_WITH_SHOVEL),
	PICKAXE(Items.NETHERITE_PICKAXE, EquipmentSlot.MAINHAND, BlockTags.MINEABLE_WITH_PICKAXE),
	AXE(Items.NETHERITE_AXE, EquipmentSlot.MAINHAND, BlockTags.MINEABLE_WITH_AXE),
	HOE(Items.NETHERITE_HOE, EquipmentSlot.MAINHAND, BlockTags.MINEABLE_WITH_HOE),
	SPEAR(Items.NETHERITE_SPEAR, EquipmentSlot.MAINHAND, null),
	HELMET(Items.NETHERITE_HELMET, EquipmentSlot.HEAD, null),
	CHESTPLATE(Items.NETHERITE_CHESTPLATE, EquipmentSlot.CHEST, null),
	LEGGINGS(Items.NETHERITE_LEGGINGS, EquipmentSlot.LEGS, null),
	BOOTS(Items.NETHERITE_BOOTS, EquipmentSlot.FEET, null),
	HORSE_ARMOR(Items.NETHERITE_HORSE_ARMOR, EquipmentSlot.BODY, null),
	NAUTILUS_ARMOR(Items.NETHERITE_NAUTILUS_ARMOR, EquipmentSlot.BODY, null);

	private final Item netheriteItem;
	private final EquipmentSlot slot;
	private final Optional<TagKey<Block>> mineable;

	GearKind(Item netheriteItem, EquipmentSlot slot, TagKey<Block> mineable) {
		this.netheriteItem = netheriteItem;
		this.slot = slot;
		this.mineable = Optional.ofNullable(mineable);
	}

	/** The kind's name in ids, such as {@code horse_armor}. */
	public String path() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** The Netherite version of this kind of gear. */
	public Item netheriteItem() {
		return netheriteItem;
	}

	/** The slot the gear is used from: the main hand for tools and weapons, else the slot it's worn in. */
	public EquipmentSlot slot() {
		return slot;
	}

	/** Whether this is a tool or weapon, used from the main hand. */
	public boolean isHeld() {
		return slot == EquipmentSlot.MAINHAND;
	}

	/** Whether the gear wears down. Mob armor doesn't, so it is never repaired. */
	public boolean hasDurability() {
		return slot != EquipmentSlot.BODY;
	}

	/** The blocks this kind of tool is made to mine, or empty for weapons and armor. */
	public Optional<TagKey<Block>> mineable() {
		return mineable;
	}

	/** Builds the gear's properties as vanilla builds the Netherite version, from the given materials. */
	Item.Properties properties(ToolMaterial tool, ArmorMaterial armor) {
		Item.Properties properties = new Item.Properties();

		return switch (this) {
			case SWORD -> properties.sword(tool, 3.0F, -2.4F);
			case SHOVEL -> properties.shovel(tool, 1.5F, -3.0F);
			case PICKAXE -> properties.pickaxe(tool, 1.0F, -2.8F);
			case AXE -> properties.axe(tool, 5.0F, -3.0F);
			case HOE -> properties.hoe(tool, -4.0F, 0.0F);
			case SPEAR -> properties.spear(tool, 1.15F, 1.2F, 0.4F, 2.5F, 9.0F, 5.5F, 5.1F, 8.75F, 4.6F);
			case HELMET -> properties.humanoidArmor(armor, ArmorType.HELMET);
			case CHESTPLATE -> properties.humanoidArmor(armor, ArmorType.CHESTPLATE);
			case LEGGINGS -> properties.humanoidArmor(armor, ArmorType.LEGGINGS);
			case BOOTS -> properties.humanoidArmor(armor, ArmorType.BOOTS);
			case HORSE_ARMOR -> properties.horseArmor(armor);
			case NAUTILUS_ARMOR -> properties.nautilusArmor(armor);
		};
	}
}
