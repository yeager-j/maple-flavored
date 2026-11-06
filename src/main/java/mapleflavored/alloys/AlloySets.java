package mapleflavored.alloys;

import java.util.List;
import java.util.Optional;
import mapleflavored.MapleFlavored;
import mapleflavored.advancements.MapleTriggers;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
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
 * <li>Shakudo: no slowdown from soul sand, honey, cobwebs or powder snow.</li>
 * <li>Adamant: 25% of melee damage ignores armor points, though not Protection.</li>
 * <li>Orichalcum: a lethal-hit save ({@link OrichalcumSave}).</li>
 * </ul>
 *
 * <p>Every bonus but the save applies to any living entity in a full set, as Alloy armor's damage reductions do.
 */
public final class AlloySets {
	private static final List<EquipmentSlot> SET_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
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

	/** Whether the entity is living and wears a full set of the Alloy. */
	public static boolean wearsFullSet(Entity entity, Alloy alloy) {
		return entity instanceof LivingEntity living && fullSet(living).equals(Optional.of(alloy));
	}

	/**
	 * The share of a hit's damage that ignores armor points: a quarter for a direct melee hit from an attacker in full
	 * Adamant, else none.
	 */
	public static float armorPiercingShare(DamageSource source) {
		boolean adamantMelee = source.is(ADAMANT_PIERCING_MELEE) && source.isDirect() && wearsFullSet(source.getEntity(), Alloy.ADAMANT);

		return adamantMelee ? ADAMANT_ARMOR_PIERCING_SHARE : 0.0F;
	}

	/** Whether a full Shakudo set lets the entity pass through the block, such as a cobweb, at full speed. */
	public static boolean ignoresBeingStuckIn(Entity entity, BlockState block) {
		boolean slowsLikeCobweb = block.is(Blocks.COBWEB) || block.is(Blocks.POWDER_SNOW);

		return slowsLikeCobweb && wearsFullSet(entity, Alloy.SHAKUDO);
	}

	/** Registers the equipment-change listener behind the Steel set and the advancement. Call once from {@link Alloys#register()}. */
	static void register() {
		ServerEntityEvents.EQUIPMENT_CHANGE.register((entity, slot, previous, current) -> {
			if (SET_SLOTS.contains(slot)) onSetSlotChanged(entity);
		});
	}

	private static void onSetSlotChanged(LivingEntity entity) {
		Optional<Alloy> set = fullSet(entity);
		boolean steel = set.equals(Optional.of(Alloy.STEEL));

		for (SetModifier modifier : STEEL_MODIFIERS) {
			modifier.apply(entity, steel);
		}

		if (steel) entity.removeEffect(MobEffects.LEVITATION);

		if (entity instanceof ServerPlayer player) set.ifPresent(alloy -> MapleTriggers.ALLOY_SET_WORN.trigger(player, alloy));
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
