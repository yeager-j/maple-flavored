package mapleflavored.reinforcement;

import java.util.List;
import java.util.function.BiConsumer;
import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.item.v1.ItemComponentTooltipProviderRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.equipment.Equippable;
import org.apache.commons.lang3.function.TriConsumer;

/**
 * Reinforcement: an armor piece smithed with a Reinforcement template and any chainmail piece is Reinforced, a mark
 * that gives it +1 armor toughness while worn. The mark is a component on the stack rather than an attribute modifier
 * patch, so it carries through every smithing upgrade without freezing the old item's attributes onto the new one.
 */
public final class Reinforcement {
	private static final double TOUGHNESS = 1.0;
	// Both the armor and the chainmail slot take any armor piece, as vanilla's trim template shows them.
	private static final List<Identifier> ARMOR_SLOT_ICONS = List.of(
		Identifier.withDefaultNamespace("container/slot/helmet"),
		Identifier.withDefaultNamespace("container/slot/chestplate"),
		Identifier.withDefaultNamespace("container/slot/leggings"),
		Identifier.withDefaultNamespace("container/slot/boots")
	);
	private static final Component REINFORCED_LINE = Component.translatable("item.maple_flavored.reinforced").withStyle(ChatFormatting.GRAY);

	/** Marks a Reinforced piece. */
	public static final DataComponentType<Unit> REINFORCED = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		MapleFlavored.id("reinforced"),
		DataComponentType.<Unit>builder().persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC).build()
	);

	/** The pieces Reinforcement accepts: worn armor, the turtle helmet and Armored Elytra, but no plain Elytra, heads or mob armor. */
	public static final TagKey<Item> REINFORCEABLE = TagKey.create(Registries.ITEM, MapleFlavored.id("reinforceable"));

	/** Consumed with a chainmail piece to Reinforce an armor piece. */
	public static final Item TEMPLATE = registerTemplate();

	/** The serializer for Reinforcement's smithing recipe, written in data as {@code "type": "maple_flavored:smithing_reinforcement"}. */
	public static final RecipeSerializer<ReinforcementRecipe> SERIALIZER = Registry.register(
		BuiltInRegistries.RECIPE_SERIALIZER, MapleFlavored.id("smithing_reinforcement"), ReinforcementRecipe.SERIALIZER
	);

	private Reinforcement() {
	}

	/** Registers the "Reinforced" tooltip line and the template's creative tab place. Call once from mod init. */
	public static void register() {
		ItemComponentTooltipProviderRegistry.addAfter(
			DataComponents.TRIM, REINFORCED, unit -> (context, lines, flag, components) -> lines.accept(REINFORCED_LINE)
		);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output ->
			output.insertAfter(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, TEMPLATE)
		);
	}

	/** Whether the piece is Reinforced. */
	public static boolean isReinforced(ItemStack stack) {
		return stack.has(REINFORCED);
	}

	/** The piece Reinforced, or empty if it already is. Keeps everything else on it, enchantments and trim included. */
	static ItemStack reinforce(ItemStack base) {
		if (isReinforced(base)) return ItemStack.EMPTY;

		ItemStack reinforced = base.copyWithCount(1);

		reinforced.set(REINFORCED, Unit.INSTANCE);

		return reinforced;
	}

	/** Passes a Reinforced piece's toughness to {@code consumer} when {@code slot} is the armor slot it's worn in. */
	public static void forEachModifier(ItemStack stack, EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
		EquipmentSlot worn = armorSlot(stack);

		if (worn == slot) consumer.accept(Attributes.ARMOR_TOUGHNESS, toughness(worn));
	}

	/**
	 * Passes a Reinforced piece's toughness to {@code consumer} for the group of just its armor slot, as vanilla armor
	 * lists its own, so the tooltip shows it once.
	 */
	public static void forEachModifier(
		ItemStack stack, EquipmentSlotGroup group, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> consumer
	) {
		EquipmentSlot worn = armorSlot(stack);

		if (worn != null && group == EquipmentSlotGroup.bySlot(worn)) {
			consumer.accept(Attributes.ARMOR_TOUGHNESS, toughness(worn), ItemAttributeModifiers.Display.attributeModifiers());
		}
	}

	// The humanoid armor slot a Reinforced piece is worn in, or null if it isn't Reinforced armor.
	private static EquipmentSlot armorSlot(ItemStack stack) {
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);

		if (!isReinforced(stack) || equippable == null || equippable.slot().getType() != EquipmentSlot.Type.HUMANOID_ARMOR) return null;

		return equippable.slot();
	}

	// One id per slot, as an attribute holds each id once and a player can wear four Reinforced pieces.
	private static AttributeModifier toughness(EquipmentSlot slot) {
		return new AttributeModifier(MapleFlavored.id("reinforced." + slot.getSerializedName()), TOUGHNESS, AttributeModifier.Operation.ADD_VALUE);
	}

	private static Item registerTemplate() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, MapleFlavored.id("reinforcement_smithing_template"));
		Item.Properties properties = new Item.Properties().rarity(Rarity.UNCOMMON).setId(key);
		Item template = new SmithingTemplateItem(
			Component.translatable("item.maple_flavored.smithing_template.reinforcement.applies_to").withStyle(ChatFormatting.BLUE),
			Component.translatable("item.maple_flavored.smithing_template.reinforcement.ingredients").withStyle(ChatFormatting.BLUE),
			Component.translatable("item.maple_flavored.smithing_template.reinforcement.base_slot_description"),
			Component.translatable("item.maple_flavored.smithing_template.reinforcement.additions_slot_description"),
			ARMOR_SLOT_ICONS,
			ARMOR_SLOT_ICONS,
			properties
		);

		return Registry.register(BuiltInRegistries.ITEM, key, template);
	}

}
