package mapleflavored.alloys;

import java.util.List;
import mapleflavored.MapleFlavored;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * The plain-number traits an Alloy's gear has on top of Netherite's. Electrum's extra Enchantment Slot comes from the
 * {@code maple_flavored:extra_enchantment_slot} tag, and the mining traits from {@link AlloyMining}.
 *
 * @param durabilityPercent max damage as a percentage of Netherite's
 * @param handModifiers attribute modifiers a tool or weapon adds in the main hand
 * @param armorModifiers attribute modifiers each armor piece adds in its own slot
 */
public record AlloyTraits(
	int durabilityPercent,
	float attackDamageBonus,
	float toughnessBonus,
	float knockbackResistanceBonus,
	List<Modifier> handModifiers,
	List<Modifier> armorModifiers
) {
	/** +50% durability, +0.5 attack knockback, +0.1 knockback resistance per armor piece. */
	static final AlloyTraits STEEL = new AlloyTraits(
		150, 0.0F, 0.0F, 0.1F,
		List.of(new Modifier("steel_attack_knockback", Attributes.ATTACK_KNOCKBACK, 0.5, AttributeModifier.Operation.ADD_VALUE)),
		List.of()
	);
	/** Netherite's numbers; its extra Enchantment Slot is a tag. */
	static final AlloyTraits ELECTRUM = netherite();
	/** +10% attack speed and +2.5% movement speed per armor piece; its mining bonus is a game rule. */
	static final AlloyTraits SHAKUDO = new AlloyTraits(
		100, 0.0F, 0.0F, 0.0F,
		List.of(new Modifier("shakudo_attack_speed", Attributes.ATTACK_SPEED, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)),
		List.of(new Modifier("shakudo_movement_speed", Attributes.MOVEMENT_SPEED, 0.025, AttributeModifier.Operation.ADD_MULTIPLIED_BASE))
	);
	/** +1 attack damage and +1 armor toughness per piece. */
	static final AlloyTraits ADAMANT = new AlloyTraits(100, 1.0F, 1.0F, 0.0F, List.of(), List.of());
	/** Netherite's numbers; its traits need code. */
	static final AlloyTraits ORICHALCUM = netherite();

	private static AlloyTraits netherite() {
		return new AlloyTraits(100, 0.0F, 0.0F, 0.0F, List.of(), List.of());
	}

	/** Netherite's tool material with this Alloy's attack damage. */
	ToolMaterial toolMaterial() {
		ToolMaterial netherite = ToolMaterial.NETHERITE;

		return new ToolMaterial(
			netherite.incorrectBlocksForDrops(),
			netherite.durability(),
			netherite.speed(),
			netherite.attackDamageBonus() + attackDamageBonus,
			netherite.enchantmentValue(),
			netherite.repairItems()
		);
	}

	/** Netherite's armor material with this Alloy's toughness and knockback resistance, drawn with its own textures. */
	ArmorMaterial armorMaterial(Alloy alloy) {
		ArmorMaterial netherite = ArmorMaterials.NETHERITE;

		return new ArmorMaterial(
			netherite.durability(),
			netherite.defense(),
			netherite.enchantmentValue(),
			netherite.equipSound(),
			netherite.toughness() + toughnessBonus,
			netherite.knockbackResistance() + knockbackResistanceBonus,
			netherite.repairIngredient(),
			ResourceKey.create(EquipmentAssets.ROOT_ID, MapleFlavored.id(alloy.path()))
		);
	}

	/** The max damage of this Alloy's version of an item whose Netherite version has {@code netheriteMaxDamage}. */
	int maxDamage(int netheriteMaxDamage) {
		return netheriteMaxDamage * durabilityPercent / 100;
	}

	/** {@code attributes} plus this Alloy's modifiers for gear worn or held in {@code slot}. */
	ItemAttributeModifiers withModifiers(ItemAttributeModifiers attributes, EquipmentSlot slot) {
		List<Modifier> modifiers = slot == EquipmentSlot.MAINHAND ? handModifiers : armorModifiers;
		EquipmentSlotGroup slotGroup = EquipmentSlotGroup.bySlot(slot);
		ItemAttributeModifiers withModifiers = attributes;

		for (Modifier modifier : modifiers) {
			withModifiers = withModifiers.withModifierAdded(modifier.attribute(), modifier.inSlot(slot), slotGroup);
		}

		return withModifiers;
	}

	/** One attribute modifier a trait adds. */
	record Modifier(String name, Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
		// Each slot needs its own id, or a full set's pieces would replace each other's modifier on the wearer.
		AttributeModifier inSlot(EquipmentSlot slot) {
			return new AttributeModifier(MapleFlavored.id(name + "." + slot.getName()), amount, operation);
		}
	}
}
