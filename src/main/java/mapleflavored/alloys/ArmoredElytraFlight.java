package mapleflavored.alloys;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * An Armored Elytra flies worse than a plain Elytra: its glide loses more horizontal speed each tick, and Firework
 * Rockets boost it less. Gliding and rocket boosts move the glider on its own client too, so both rules run on both
 * sides, reading the glider's worn chest piece.
 */
public final class ArmoredElytraFlight {
	// Applied on top of vanilla's own 0.99 horizontal glide drag.
	private static final double GLIDE_HORIZONTAL_DRAG = 0.995;
	private static final double ROCKET_BOOST_SHARE = 0.75;

	private ArmoredElytraFlight() {
	}

	/** A glide step's resulting movement for the glider: slower horizontally in an Armored Elytra. */
	public static Vec3 afterGlideDrag(LivingEntity glider, Vec3 movement) {
		if (!wearsArmoredElytra(glider)) return movement;

		return movement.multiply(GLIDE_HORIZONTAL_DRAG, 1.0, GLIDE_HORIZONTAL_DRAG);
	}

	/**
	 * The glider's movement after a Firework Rocket's boost: in an Armored Elytra, only part of the change from
	 * {@code current} to vanilla's {@code boosted}.
	 */
	public static Vec3 afterRocketBoost(LivingEntity glider, Vec3 current, Vec3 boosted) {
		if (!wearsArmoredElytra(glider)) return boosted;

		return current.add(boosted.subtract(current).scale(ROCKET_BOOST_SHARE));
	}

	private static boolean wearsArmoredElytra(LivingEntity glider) {
		return AlloyGear.isArmoredElytra(glider.getItemBySlot(EquipmentSlot.CHEST));
	}
}
