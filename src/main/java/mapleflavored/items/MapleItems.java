package mapleflavored.items;

import java.util.List;
import java.util.function.Function;
import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

/** The mod's items: the Crystal Heart and Pith. */
public final class MapleItems {
	private static final int TICKS_PER_SECOND = 20;

	// Matcha Flavoured's burst on use: Regeneration XI for 3 seconds and Resistance V for 1 second.
	private static final Consumable CRYSTAL_HEART_CONSUMABLE = Consumable.builder()
		.consumeSeconds(Consumable.DEFAULT_CONSUME_SECONDS)
		.animation(ItemUseAnimation.TOOT_HORN)
		.sound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_CHIME))
		.hasConsumeParticles(false)
		.onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
			new MobEffectInstance(MobEffects.REGENERATION, 3 * TICKS_PER_SECOND, 10),
			new MobEffectInstance(MobEffects.RESISTANCE, TICKS_PER_SECOND, 4)
		)))
		.build();

	/** Consumed for 1 Max Heart; held in either hand, it saves its holder from a lethal hit like a Totem. */
	public static final Item CRYSTAL_HEART = register(
		"crystal_heart",
		CrystalHeartItem::new,
		new Item.Properties()
			.stacksTo(1)
			.rarity(Rarity.RARE)
			.component(DataComponents.CONSUMABLE, CRYSTAL_HEART_CONSUMABLE)
			.component(DataComponents.DEATH_PROTECTION, DeathProtection.TOTEM_OF_UNDYING)
	);

	/** The rare material behind Crystal Hearts and Netherite Ingots. */
	public static final Item PITH = register("pith", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));

	private MapleItems() {
	}

	/** Registers the items, their creative tab places and the Crystal Heart's network payload. Call once from mod init. */
	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(CrystalHeartUsedPayload.TYPE, CrystalHeartUsedPayload.CODEC);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> output.insertAfter(Items.TOTEM_OF_UNDYING, CRYSTAL_HEART));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.insertAfter(Items.NETHER_STAR, PITH));
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, MapleFlavored.id(name));

		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}
}
