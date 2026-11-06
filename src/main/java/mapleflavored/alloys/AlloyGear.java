package mapleflavored.alloys;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Alloy gear: an Alloy version of every Netherite item, made at a Smithing Table from the Netherite item, or another
 * Alloy's version, and the Alloy's ingot. It is Netherite's item with the Alloy's {@link AlloyTraits}, repaired and
 * enchanted with the Alloy's ingot.
 */
public final class AlloyGear {
	private static final Map<Alloy, Map<GearKind, Item>> ITEMS = new EnumMap<>(Alloy.class);
	private static final Map<Item, Piece> PIECES = new HashMap<>();

	static {
		for (Alloy alloy : Alloy.values()) {
			Map<GearKind, Item> items = new EnumMap<>(GearKind.class);

			for (GearKind kind : GearKind.values()) {
				Item item = registerItem(alloy, kind);

				items.put(kind, item);
				PIECES.put(item, new Piece(alloy, kind));
			}

			ITEMS.put(alloy, items);
		}
	}

	private AlloyGear() {
	}

	/** The Alloy's version of this kind of gear. */
	public static Item of(Alloy alloy, GearKind kind) {
		return ITEMS.get(alloy).get(kind);
	}

	/** The Alloy the item is gear of, or empty if it isn't Alloy gear. */
	public static Optional<Alloy> alloyOf(ItemInstance item) {
		return pieceOf(item).map(Piece::alloy);
	}

	/** The kind of gear the stack is, or empty if it isn't Alloy gear. */
	public static Optional<GearKind> kindOf(ItemInstance item) {
		return pieceOf(item).map(Piece::kind);
	}

	/** How many pieces of the Alloy's gear the entity wears in its armor slots, mob body armor included. */
	public static int piecesWorn(LivingEntity entity, Alloy alloy) {
		int pieces = 0;

		for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
			if (alloyOf(entity.getItemBySlot(slot)).equals(Optional.of(alloy))) pieces++;
		}

		return pieces;
	}

	/**
	 * The damage a smithing result takes from its base, so the result keeps the share of durability the base had left,
	 * though never so much that it breaks.
	 */
	public static int keptDamage(int baseDamage, int baseMaxDamage, int resultMaxDamage) {
		long scaledDamage = Math.round((double) baseDamage * resultMaxDamage / baseMaxDamage);

		return (int) Math.min(scaledDamage, resultMaxDamage - 1);
	}

	/** Registers the gear's trait components and creative tab places. Call once from {@link Alloys#register()}. */
	static void register() {
		DefaultItemComponentEvents.MODIFY.register(context ->
			PIECES.forEach((item, piece) -> context.modify(item, components -> addTraits(components, piece)))
		);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(AlloyGear::addToTab);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(AlloyGear::addToTab);
	}

	private static Item registerItem(Alloy alloy, GearKind kind) {
		AlloyTraits traits = alloy.traits();
		Item.Properties properties = kind.properties(traits.toolMaterial(), traits.armorMaterial(alloy)).fireResistant();

		if (kind.hasDurability()) properties.repairable(alloy.ingot());

		return Alloys.registerItem(alloy.path() + "_" + kind.path(), Item::new, properties);
	}

	// Vanilla's helpers build the base attributes and durability; the traits go on top of them.
	private static void addTraits(DataComponentMap.Builder components, Piece piece) {
		AlloyTraits traits = piece.alloy().traits();
		ItemAttributeModifiers attributes = components.get(DataComponents.ATTRIBUTE_MODIFIERS);
		Integer maxDamage = components.get(DataComponents.MAX_DAMAGE);

		components.set(DataComponents.ATTRIBUTE_MODIFIERS, traits.withModifiers(attributes, piece.kind().slot()));

		if (maxDamage != null) components.set(DataComponents.MAX_DAMAGE, traits.maxDamage(maxDamage));
	}

	// Each kind's Alloy versions follow its Netherite version, in whichever tabs that one is in.
	private static void addToTab(FabricCreativeModeTabOutput output) {
		List<ItemStack> shown = output.getDisplayStacks();

		for (GearKind kind : GearKind.values()) {
			boolean tabHasNetherite = shown.stream().anyMatch(stack -> stack.is(kind.netheriteItem()));

			if (!tabHasNetherite) continue;

			List<ItemStack> alloyVersions = Arrays.stream(Alloy.values()).map(alloy -> new ItemStack(of(alloy, kind))).toList();

			output.insertAfter(kind.netheriteItem(), alloyVersions, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		}
	}

	private static Optional<Piece> pieceOf(ItemInstance item) {
		return Optional.ofNullable(PIECES.get(item.typeHolder().value()));
	}

	private record Piece(Alloy alloy, GearKind kind) {
	}
}
