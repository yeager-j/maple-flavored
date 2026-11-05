package mapleflavored.grounded;

import com.mojang.serialization.Codec;
import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.Vec3;

/**
 * Grounded: an effect that makes flight and long escapes weak. Gliding slows down and Firework Rockets barely push,
 * Ender Pearls thrown while Grounded fall sharply but still teleport, and Riptide and Zephyr's jump don't work.
 * Levitation, Slow Falling and Chorus Fruit are untouched. Its level doesn't matter, so it never stacks.
 *
 * <p>Gliding, rockets, Riptide and Zephyr move the player on their own client, which knows its own effects, so each
 * rule runs on both sides.
 */
public final class Grounded {
	/** The Grounded effect. */
	public static final Holder<MobEffect> EFFECT = Registry.registerForHolder(
		BuiltInRegistries.MOB_EFFECT,
		MapleFlavored.id("grounded"),
		new GroundedEffect()
	);

	// An extra multiplier on horizontal glide movement each tick, on top of vanilla's air drag.
	private static final double GLIDE_KEPT = 0.97;
	private static final double ROCKET_BOOST_KEPT = 0.05;
	// Vanilla's pearl gravity is 0.03. A playtest value: a flat throw lands about 6 blocks out instead of 13, and the
	// longest throw reaches about 17 blocks instead of 51.
	private static final double HEAVY_PEARL_GRAVITY = 0.12;

	// Set on the server when a Grounded thrower's pearl is made, and synced so every client flies it the same way.
	private static final AttachmentType<Boolean> HEAVY_PEARL = AttachmentRegistry.create(
		MapleFlavored.id("heavy_pearl"),
		builder -> builder
			.persistent(Codec.BOOL)
			.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all())
	);

	private Grounded() {
	}

	/** Registers the effect. Call once from mod init. */
	public static void register() {
		// Registration happens when the class loads; this call makes sure it loads during mod init.
	}

	/** Whether the entity is Grounded. */
	public static boolean isGrounded(LivingEntity entity) {
		return entity.hasEffect(EFFECT);
	}

	/** Slows a glider's horizontal movement for one tick, if they are Grounded. */
	public static Vec3 slowGlide(LivingEntity glider, Vec3 movement) {
		return isGrounded(glider) ? movement.multiply(GLIDE_KEPT, 1.0, GLIDE_KEPT) : movement;
	}

	/** Keeps only a small part of a Firework Rocket's push on a Grounded glider. */
	public static Vec3 weakenRocketBoost(LivingEntity glider, Vec3 boosted) {
		if (!isGrounded(glider)) return boosted;

		Vec3 before = glider.getDeltaMovement();

		return before.add(boosted.subtract(before).scale(ROCKET_BOOST_KEPT));
	}

	/** Makes a pearl heavy if its thrower is Grounded. Call when the pearl is made, before it is added to the world. */
	public static void onPearlThrown(ThrownEnderpearl pearl, LivingEntity thrower) {
		if (isGrounded(thrower)) pearl.setAttached(HEAVY_PEARL, true);
	}

	/** The pearl's gravity: much stronger for a pearl thrown while Grounded. */
	public static double pearlGravity(ThrownEnderpearl pearl, double vanillaGravity) {
		return pearl.hasAttached(HEAVY_PEARL) ? HEAVY_PEARL_GRAVITY : vanillaGravity;
	}

	private static final class GroundedEffect extends MobEffect {
		private static final int COLOR = 0x6B4A2B;

		GroundedEffect() {
			super(MobEffectCategory.HARMFUL, COLOR);
		}
	}
}
