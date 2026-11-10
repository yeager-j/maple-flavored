package mapleflavored.enchanting;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

/**
 * An effect the Enchanting Table can put on a golden apple, unlocked once the table's Library reaches a share of a Full
 * Library. Saving any effects onto a golden apple turns it into an enchanted golden apple that gives exactly those
 * effects when eaten.
 */
public enum AppleEffect {
	REGENERATION(MobEffects.REGENERATION, 20, 1, 15),
	FIRE_RESISTANCE(MobEffects.FIRE_RESISTANCE, 300, 0, 30),
	ABSORPTION(MobEffects.ABSORPTION, 120, 3, 50),
	RESISTANCE(MobEffects.RESISTANCE, 300, 0, 75);

	private final Holder<MobEffect> effect;
	private final int seconds;
	private final int amplifier;
	private final int percent;

	AppleEffect(Holder<MobEffect> effect, int seconds, int amplifier, int percent) {
		this.effect = effect;
		this.seconds = seconds;
		this.amplifier = amplifier;
		this.percent = percent;
	}

	/**
	 * The effects stored on an enchanted golden apple, exactly as eating it applies them, or none for any other item.
	 * These are the authority on what the apple does; an effect's current strength and duration here are only what a
	 * new save stores.
	 */
	public static List<MobEffectInstance> stored(ItemStack apple) {
		Consumable consumable = apple.get(DataComponents.CONSUMABLE);

		if (!apple.is(Items.ENCHANTED_GOLDEN_APPLE) || consumable == null) return List.of();

		return consumable.onConsumeEffects().stream()
			.filter(ApplyStatusEffectsConsumeEffect.class::isInstance)
			.map(ApplyStatusEffectsConsumeEffect.class::cast)
			.flatMap(applied -> applied.effects().stream())
			.toList();
	}

	/** Which of the table's effects an enchanted golden apple stores, at any strength, or none for any other item. */
	public static Set<AppleEffect> on(ItemStack apple) {
		Set<AppleEffect> effects = EnumSet.noneOf(AppleEffect.class);

		Arrays.stream(values()).filter(effect -> effect.storedOn(apple).isPresent()).forEach(effects::add);

		return effects;
	}

	/**
	 * A copy of the golden apple, plain or enchanted, as one enchanted golden apple giving exactly {@code effects}. It
	 * keeps the apple's other components, such as a name or a deposit.
	 */
	public static ItemStack enchant(ItemStack apple, List<MobEffectInstance> effects) {
		ItemStack enchanted = apple.transmuteCopy(Items.ENCHANTED_GOLDEN_APPLE, 1);

		enchanted.set(DataComponents.CONSUMABLE, Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(effects)).build());

		return enchanted;
	}

	/** A copy of the apple as one plain golden apple with no effects. It keeps the apple's other components. */
	public static ItemStack plain(ItemStack apple) {
		ItemStack plain = apple.transmuteCopy(Items.GOLDEN_APPLE, 1);

		// Setting the golden apple's own default drops the enchanted apple's effects from the stack's changes.
		plain.set(DataComponents.CONSUMABLE, Items.GOLDEN_APPLE.components().get(DataComponents.CONSUMABLE));

		return plain;
	}

	/** The share of a Full Library, from 1 to 100, the table's Library needs before it offers this effect. */
	public int percent() {
		return percent;
	}

	/** A new instance of the effect, at the strength and duration a new save stores. */
	public MobEffectInstance instance() {
		return new MobEffectInstance(effect, seconds * SharedConstants.TICKS_PER_SECOND, amplifier);
	}

	/** This effect as the apple stores it, or empty if the apple doesn't store it. */
	public Optional<MobEffectInstance> storedOn(ItemStack apple) {
		return stored(apple).stream().filter(instance -> instance.is(effect)).findFirst();
	}

	/** The effect's name with the strength and duration a new save stores, such as "Regeneration II (0:20)". */
	public Component describe() {
		return describe(instance());
	}

	/** A stored effect's name with its strength and duration, such as "Regeneration II (0:20)". */
	public static Component describe(MobEffectInstance instance) {
		return Component.translatable(
			"potion.withDuration",
			PotionContents.getPotionDescription(instance.getEffect(), instance.getAmplifier()),
			MobEffectUtil.formatDuration(instance, 1.0F, SharedConstants.TICKS_PER_SECOND)
		);
	}

	/** The effect's color, for its icon. */
	public int color() {
		return effect.value().getColor();
	}
}
