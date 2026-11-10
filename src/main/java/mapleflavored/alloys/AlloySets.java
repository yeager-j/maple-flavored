package mapleflavored.alloys;

import java.util.List;
import java.util.Optional;
import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Alloy set bonuses: a helmet, chestplate, leggings and boots all of one Alloy give that Alloy's bonus.
 *
 * <ul>
 * <li>Steel: knockback and Levitation immunity.</li>
 * <li>Electrum: fire and lava immunity, and Withered Hearts form every 10 seconds.</li>
 * <li>Shakudo: no slowdown from soul sand, honey, cobwebs or powder snow, full-height jumps off honey, and walking on powder snow as leather boots do.</li>
 * <li>Adamant: 25% of melee damage ignores armor points, though not Protection.</li>
 * <li>Orichalcum: a lethal-hit save ({@link OrichalcumSave}).</li>
 * </ul>
 *
 * <p>Every bonus but the save applies to any living entity in a full set, as Alloy armor's damage reductions do. Each
 * is asked here as a question about the entity, so only this package knows which Alloy gives it.
 */
public final class AlloySets {
	/** The kinds of gear that make a set, one of each. */
	public static final List<GearKind> PIECES = List.of(GearKind.HELMET, GearKind.CHESTPLATE, GearKind.LEGGINGS, GearKind.BOOTS);

	/** Fires for a player in a full set; advancement data names it {@code maple_flavored:alloy_set_worn}. */
	static final AlloySetWornTrigger SET_WORN = new AlloySetWornTrigger();

	private static final List<EquipmentSlot> SET_SLOTS = PIECES.stream().map(GearKind::slot).toList();
	private static final Holder<MobEffect> STEEL_REFUSED_EFFECT = MobEffects.LEVITATION;
	private static final int ELECTRUM_WITHERING_SLOWDOWN = 2;
	private static final float ADAMANT_ARMOR_PIERCING_SHARE = 0.25F;
	private static final TagKey<DamageType> ADAMANT_PIERCING_MELEE = TagKey.create(Registries.DAMAGE_TYPE, MapleFlavored.id("pierced_by_adamant_set"));

	// Enough to reach each attribute's cap of 1 from any armor, so no knockback gets through.
	private static final List<SetModifier> STEEL_MODIFIERS = List.of(
		new SetModifier(Attributes.KNOCKBACK_RESISTANCE, MapleFlavored.id("steel_set_knockback_resistance")),
		new SetModifier(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, MapleFlavored.id("steel_set_explosion_knockback_resistance"))
	);

	private AlloySets() {
	}

	/** The Alloy whose helmet, chestplate, leggings and boots the entity all wears, or empty. */
	public static Optional<Alloy> fullSet(LivingEntity entity) {
		Optional<Alloy> helmetAlloy = AlloyGear.alloyOf(entity.getItemBySlot(EquipmentSlot.HEAD));

		for (EquipmentSlot slot : SET_SLOTS) {
			if (!AlloyGear.alloyOf(entity.getItemBySlot(slot)).equals(helmetAlloy)) return Optional.empty();
		}

		return helmetAlloy;
	}

	/** An advancement criterion met by wearing a full set of the Alloy. */
	public static Criterion<?> wornCriterion(Alloy alloy) {
		return SET_WORN.createCriterion(new AlloySetWornTrigger.TriggerInstance(Optional.empty(), alloy));
	}

	/** Whether the entity's set refuses the effect, so it can't be given: Levitation, in full Steel. */
	public static boolean refusesEffect(LivingEntity entity, MobEffectInstance effect) {
		return effect.is(STEEL_REFUSED_EFFECT) && wearsFullSet(entity, Alloy.STEEL);
	}

	/** Whether the entity's set makes it immune to fire and lava, as full Electrum does. */
	public static boolean isFireImmune(Entity entity) {
		return wearsFullSet(entity, Alloy.ELECTRUM);
	}

	/**
	 * The ticks between a player's hearts withering, given the interval without a set: twice as long in full Electrum.
	 */
	public static int witheringInterval(LivingEntity entity, int interval) {
		return wearsFullSet(entity, Alloy.ELECTRUM) ? interval * ELECTRUM_WITHERING_SLOWDOWN : interval;
	}

	/** The entity's jump factor, given the one its block sets: never below a full-height jump in full Shakudo. */
	public static float blockJumpFactor(Entity entity, float jumpFactor) {
		return wearsFullSet(entity, Alloy.SHAKUDO) ? Math.max(jumpFactor, 1.0F) : jumpFactor;
	}

	/** The entity's speed factor, given the one its block sets: never below full speed in full Shakudo. */
	public static float blockSpeedFactor(LivingEntity entity, float speedFactor) {
		return wearsFullSet(entity, Alloy.SHAKUDO) ? Math.max(speedFactor, 1.0F) : speedFactor;
	}

	/** Whether the entity's set keeps frost from slowing it, as full Shakudo does. */
	public static boolean ignoresFrost(LivingEntity entity) {
		return wearsFullSet(entity, Alloy.SHAKUDO);
	}

	/** Whether the entity's set lets it walk on powder snow and climb out of it, as full Shakudo does. */
	public static boolean walksOnPowderSnow(Entity entity) {
		return wearsFullSet(entity, Alloy.SHAKUDO);
	}

	/** Whether a full Shakudo set lets the entity pass through the block, such as a cobweb, at full speed. */
	public static boolean ignoresBeingStuckIn(Entity entity, BlockState block) {
		boolean slowsLikeCobweb = block.is(Blocks.COBWEB) || block.is(Blocks.POWDER_SNOW);

		return slowsLikeCobweb && wearsFullSet(entity, Alloy.SHAKUDO);
	}

	/**
	 * A hit's damage after armor, given the damage before and after vanilla's armor: a quarter of a direct melee hit
	 * from an attacker in full Adamant ignores armor points. Armor still sees the whole hit, so the pierced share can
	 * only add damage.
	 */
	public static float damageAfterArmor(DamageSource source, float damage, float afterArmor) {
		float piercingShare = armorPiercingShare(source);

		return piercingShare * damage + (1.0F - piercingShare) * afterArmor;
	}

	/** Whether the entity is living and wears a full set of the Alloy. */
	static boolean wearsFullSet(Entity entity, Alloy alloy) {
		return entity instanceof LivingEntity living && fullSet(living).equals(Optional.of(alloy));
	}

	/**
	 * Registers the advancement trigger and the equipment-change listener behind it and the Steel set. Call once from
	 * {@link Alloys#register()}.
	 */
	static void register() {
		Registry.register(BuiltInRegistries.TRIGGER_TYPES, MapleFlavored.id("alloy_set_worn"), SET_WORN);

		ServerEntityEvents.EQUIPMENT_CHANGE.register((entity, slot, previous, current) -> {
			if (SET_SLOTS.contains(slot)) onSetSlotChanged(entity);
		});
	}

	private static float armorPiercingShare(DamageSource source) {
		boolean adamantMelee = source.is(ADAMANT_PIERCING_MELEE) && source.isDirect() && wearsFullSet(source.getEntity(), Alloy.ADAMANT);

		return adamantMelee ? ADAMANT_ARMOR_PIERCING_SHARE : 0.0F;
	}

	private static void onSetSlotChanged(LivingEntity entity) {
		Optional<Alloy> set = fullSet(entity);
		boolean steel = set.equals(Optional.of(Alloy.STEEL));

		for (SetModifier modifier : STEEL_MODIFIERS) {
			modifier.apply(entity, steel);
		}

		if (steel) entity.removeEffect(STEEL_REFUSED_EFFECT);

		if (entity instanceof ServerPlayer player) set.ifPresent(alloy -> SET_WORN.trigger(player, alloy));
	}

	/** An attribute modifier a full set adds while it is worn. */
	private record SetModifier(Holder<Attribute> attribute, Identifier id) {
		void apply(LivingEntity entity, boolean worn) {
			AttributeInstance instance = entity.getAttribute(attribute);

			if (instance == null) return;

			instance.removeModifier(id);

			if (worn) instance.addTransientModifier(new AttributeModifier(id, 1.0, AttributeModifier.Operation.ADD_VALUE));
		}
	}
}
