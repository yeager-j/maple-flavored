package mapleflavored.subspaces;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import mapleflavored.grounded.Grounded;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.AABB;

/**
 * The Warden's Subspace: every Warden opens one where it appears. Players inside get Darkness and Grounded, and the
 * Warden is aware of every entity inside, so it hunts down the nearest player wherever they hide.
 *
 * <p>Anger and sniffing stay vanilla. Awareness only steers the Warden: a target it senses through its Subspace alone,
 * outside vanilla's awareness box, never keeps it from digging back down.
 */
public final class WardenSubspaces {
	static final double RADIUS = 100.0;

	// Playtest values. Long enough to bridge top-ups, short enough to wear off soon after a player leaves. Topping up
	// above Darkness's 22-tick fade keeps it from flickering.
	private static final int EFFECT_TICKS = 60;
	private static final int TOP_UP_TICKS = 40;

	private WardenSubspaces() {
	}

	static void register() {
		ServerEntityEvents.ENTITY_LOAD.register(WardenSubspaces::openIfNew);
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			for (ServerPlayer player : level.players()) {
				applyEffects(level, player);
			}
		});
	}

	/**
	 * Makes the Warden aware of every living entity inside its Subspace, on top of what its vanilla sensor found. Call
	 * right after the Warden's sensor fills its nearest living entities.
	 */
	public static void extendAwareness(ServerLevel level, Warden warden) {
		Subspace subspace = Subspaces.ownedBy(level, warden.getUUID()).orElse(null);

		if (subspace == null) return;

		Brain<Warden> brain = warden.getBrain();
		List<LivingEntity> sensed = brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).orElse(List.of());
		List<LivingEntity> aware = Stream.concat(sensed.stream(), livingEntitiesInside(level, subspace, warden).stream())
			.distinct()
			.sorted(Comparator.comparingDouble(warden::distanceToSqr))
			.toList();

		brain.setMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES, aware);
	}

	/**
	 * Sends the Warden to investigate the target it just sniffed, by running {@code disturb}, vanilla's call. Only a
	 * target inside vanilla's awareness box resets the Warden's timer for digging down.
	 */
	public static void disturbFromSniff(Warden warden, LivingEntity target, Runnable disturb) {
		Brain<Warden> brain = warden.getBrain();

		if (isWithinVanillaAwareness(warden, target) || !brain.hasMemoryValue(MemoryModuleType.DIG_COOLDOWN)) {
			disturb.run();

			return;
		}

		long ticksUntilDigging = brain.getTimeUntilExpiry(MemoryModuleType.DIG_COOLDOWN);

		disturb.run();
		brain.setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, ticksUntilDigging);
	}

	// Every way a Warden appears adds it to a level. One that already has a Subspace is only loading again, perhaps after
	// carrying its UUID through a portal, and a dying one, saved mid-animation, no longer has a Subspace to open.
	private static void openIfNew(Entity entity, ServerLevel level) {
		if (!(entity instanceof Warden warden) || !warden.isAlive()) return;
		if (Subspaces.ownsAny(level.getServer(), warden.getUUID())) return;

		Subspaces.open(level, warden, Subspace.Kind.WARDEN, RADIUS);
	}

	// Vanilla's Warden Darkness skips creative and spectator players too.
	private static void applyEffects(ServerLevel level, ServerPlayer player) {
		if (!player.isAlive() || player.isCreative() || player.isSpectator()) return;
		if (!isInsideWardenSubspace(level, player)) return;

		topUp(player, MobEffects.DARKNESS);
		topUp(player, Grounded.EFFECT);
	}

	private static boolean isInsideWardenSubspace(ServerLevel level, ServerPlayer player) {
		return Subspaces.containing(level, player.position()).stream()
			.anyMatch(subspace -> subspace.kind() == Subspace.Kind.WARDEN);
	}

	// Leaves a longer effect alone, such as the Darkness a Warden gives nearby players.
	private static void topUp(ServerPlayer player, Holder<MobEffect> effect) {
		MobEffectInstance current = player.getEffect(effect);

		if (current != null && !current.endsWithin(TOP_UP_TICKS - 1)) return;

		player.addEffect(new MobEffectInstance(effect, EFFECT_TICKS, 0, false, false));
	}

	private static List<LivingEntity> livingEntitiesInside(ServerLevel level, Subspace subspace, Warden warden) {
		double diameter = subspace.radius() * 2.0;
		AABB bounds = AABB.ofSize(subspace.centre(), diameter, diameter, diameter);

		return level.getEntitiesOfClass(
			LivingEntity.class,
			bounds,
			entity -> entity != warden && entity.isAlive() && subspace.contains(entity.position())
		);
	}

	// The box vanilla's sensor searches: the Warden's hitbox grown by its follow range, about 49 by 51 by 49 blocks.
	private static boolean isWithinVanillaAwareness(Warden warden, LivingEntity target) {
		double followRange = warden.getAttributeValue(Attributes.FOLLOW_RANGE);

		return warden.getBoundingBox().inflate(followRange).intersects(target.getBoundingBox());
	}
}
