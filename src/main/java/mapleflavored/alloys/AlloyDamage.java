package mapleflavored.alloys;

import java.util.Map;
import mapleflavored.MapleFlavored;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

/**
 * Alloy armor's damage reductions: each piece of Steel takes 10% off sonic booms and explosions, each piece of Electrum
 * 15% off fire, and each piece of Orichalcum 10% off magic and indirect magic. Pieces add up, so a full Electrum set
 * takes 60% off fire. The damage types are the {@code maple_flavored:reduced_by_<alloy>_armor} damage type tags.
 */
public final class AlloyDamage {
	private static final Map<Alloy, Reduction> REDUCTIONS = Map.of(
		Alloy.STEEL, new Reduction(tag("reduced_by_steel_armor"), 0.10F),
		Alloy.ELECTRUM, new Reduction(tag("reduced_by_electrum_armor"), 0.15F),
		Alloy.ORICHALCUM, new Reduction(tag("reduced_by_orichalcum_armor"), 0.10F)
	);

	private AlloyDamage() {
	}

	/**
	 * {@code damage} after the entity's Alloy armor reductions for the source's damage type. Call with the damage left
	 * after armor, Resistance and Protection.
	 */
	public static float afterAlloyArmor(LivingEntity entity, DamageSource source, float damage) {
		float reduction = 0.0F;

		for (Map.Entry<Alloy, Reduction> entry : REDUCTIONS.entrySet()) {
			Reduction alloyReduction = entry.getValue();

			if (source.is(alloyReduction.damageTypes())) reduction += alloyReduction.perPiece() * AlloyGear.piecesWorn(entity, entry.getKey());
		}

		return damage * Math.max(0.0F, 1.0F - reduction);
	}

	private static TagKey<DamageType> tag(String name) {
		return TagKey.create(Registries.DAMAGE_TYPE, MapleFlavored.id(name));
	}

	/** The share of damage of the given types each worn piece of an Alloy takes off. */
	private record Reduction(TagKey<DamageType> damageTypes, float perPiece) {
	}
}
