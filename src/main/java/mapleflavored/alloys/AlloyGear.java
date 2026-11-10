package mapleflavored.alloys;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import org.jspecify.annotations.Nullable;

/**
 * Alloy gear: an Alloy version of every Netherite item, made at a Smithing Table from the Netherite item, or another
 * Alloy's version, and the Alloy's ingot. It is Netherite's item with the Alloy's {@link AlloyTraits}, repaired and
 * enchanted with the Alloy's ingot.
 *
 * <p>Each Alloy also has an Armored Elytra: its chestplate, smithed with an Elytra, that glides. It counts as that
 * Alloy's chestplate for everything but flight ({@link ArmoredElytraFlight}).
 *
 * <p>The gear exists once {@link Alloys#register()} has run. Before that no item is a piece, and asking for an Alloy's
 * gear fails.
 */
public final class AlloyGear {
	/** The five Alloys' Armored Elytras. */
	public static final TagKey<Item> ARMORED_ELYTRAS_TAG = TagKey.create(Registries.ITEM, MapleFlavored.id("armored_elytras"));

	private static final Map<Alloy, Map<GearKind, Item>> ITEMS = new EnumMap<>(Alloy.class);
	private static final Map<Alloy, Item> ARMORED_ELYTRAS = new EnumMap<>(Alloy.class);
	private static final Map<Item, Piece> PIECES = new HashMap<>();

	private AlloyGear() {
	}

	/**
	 * A piece of Alloy gear, read from its item. An Armored Elytra is its Alloy's chestplate.
	 *
	 * @param alloy the Alloy the item is gear of
	 * @param kind the kind of gear the item is
	 */
	public record Piece(Alloy alloy, GearKind kind) {
		/** Whether this is the Alloy's tool or weapon, which is the gear its tool traits apply to. */
		public boolean isToolOf(Alloy alloy) {
			return this.alloy == alloy && kind.isHeld();
		}
	}

	/** The piece of Alloy gear the item is, or empty if it isn't Alloy gear. */
	public static Optional<Piece> pieceOf(ItemInstance item) {
		return Optional.ofNullable(PIECES.get(item.typeHolder().value()));
	}

	/** The Alloy's version of this kind of gear. */
	public static Item of(Alloy alloy, GearKind kind) {
		return registered(ITEMS.get(alloy)).get(kind);
	}

	/** The Alloy's Armored Elytra. */
	public static Item armoredElytra(Alloy alloy) {
		return registered(ARMORED_ELYTRAS.get(alloy));
	}

	/** Whether the item is an Armored Elytra, of any Alloy. */
	public static boolean isArmoredElytra(ItemInstance item) {
		return ARMORED_ELYTRAS.containsValue(item.typeHolder().value());
	}

	/** The Alloy the item is gear of, or empty if it isn't Alloy gear. An Armored Elytra is its Alloy's gear. */
	public static Optional<Alloy> alloyOf(ItemInstance item) {
		return pieceOf(item).map(Piece::alloy);
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

	/**
	 * Registers the gear, its trait components and its creative tab places. Call once from {@link Alloys#register()},
	 * after the Alloys' ingots.
	 */
	static void register() {
		for (Alloy alloy : Alloy.values()) registerGear(alloy);

		DefaultItemComponentEvents.MODIFY.register(context ->
			PIECES.forEach((item, piece) -> context.modify(item, components -> addTraits(components, piece)))
		);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(AlloyGear::addToTab);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(AlloyGear::addToTab);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.insertAfter(
			Items.ELYTRA,
			Arrays.stream(Alloy.values()).map(alloy -> new ItemStack(armoredElytra(alloy))).toList(),
			CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
		));
	}

	private static <T> T registered(@Nullable T gear) {
		return Objects.requireNonNull(gear, "Alloys.register() hasn't run");
	}

	private static void registerGear(Alloy alloy) {
		Map<GearKind, Item> items = new EnumMap<>(GearKind.class);

		for (GearKind kind : GearKind.values()) {
			Item item = registerItem(alloy, kind);

			items.put(kind, item);
			PIECES.put(item, new Piece(alloy, kind));
		}

		ITEMS.put(alloy, items);

		Item armoredElytra = registerArmoredElytra(alloy);

		ARMORED_ELYTRAS.put(alloy, armoredElytra);
		PIECES.put(armoredElytra, new Piece(alloy, GearKind.CHESTPLATE));
	}

	private static Item registerItem(Alloy alloy, GearKind kind) {
		AlloyTraits traits = alloy.traits();
		Item.Properties properties = kind.properties(traits.toolMaterial(), traits.armorMaterial(alloy)).fireResistant();

		if (kind.hasDurability()) properties.repairable(alloy.ingot());

		return Alloys.registerItem(alloy.path() + "_" + kind.path(), Item::new, properties);
	}

	// The chestplate, so the same armor, durability and enchantability, that glides as an Elytra does. It is worn
	// with its own equipment asset, which draws the chestplate and the wings.
	private static Item registerArmoredElytra(Alloy alloy) {
		String name = alloy.path() + "_armored_elytra";
		AlloyTraits traits = alloy.traits();
		Equippable chestplate = Equippable.builder(EquipmentSlot.CHEST)
			.setEquipSound(traits.armorMaterial(alloy).equipSound())
			.setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, MapleFlavored.id(name)))
			.build();
		Item.Properties properties = GearKind.CHESTPLATE.properties(traits.toolMaterial(), traits.armorMaterial(alloy))
			.component(DataComponents.GLIDER, Unit.INSTANCE)
			.component(DataComponents.EQUIPPABLE, chestplate)
			.rarity(Rarity.EPIC)
			.fireResistant()
			.repairable(alloy.ingot());

		return Alloys.registerItem(name, Item::new, properties);
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
}
