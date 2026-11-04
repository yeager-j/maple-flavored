package mapleflavored.enchanting;

import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DamageResistant;

/**
 * Enchanting and the Library: the Enchanting Table's checklist menu, the deposit it takes, and enchanted books that
 * resist fire and explosions.
 */
public final class Enchanting {
	/** The Enchanting Table's checklist menu, which replaces vanilla's random offers. */
	public static final MenuType<EnchantingTableMenu> MENU = Registry.register(
		BuiltInRegistries.MENU, MapleFlavored.id("enchanting_table"), new MenuType<>(EnchantingTableMenu::new, FeatureFlags.VANILLA_SET)
	);

	/** Damage that enchanted book item entities ignore: fire, lava and explosions. */
	private static final TagKey<DamageType> ENCHANTED_BOOK_RESISTS = TagKey.create(Registries.DAMAGE_TYPE, MapleFlavored.id("enchanted_book_resists"));

	private Enchanting() {
	}

	/** Registers the menu, the deposit and the books' resistance. Call once from mod init. */
	public static void register() {
		EnchantingDeposit.register();
		DefaultItemComponentEvents.MODIFY.register(context -> context.modify(Items.ENCHANTED_BOOK, (components, lookup, item) ->
			components.set(DataComponents.DAMAGE_RESISTANT, new DamageResistant(lookup.lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(ENCHANTED_BOOK_RESISTS)))
		));
	}
}
