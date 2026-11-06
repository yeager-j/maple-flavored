package mapleflavored.subspaces;

import java.util.function.Consumer;
import mapleflavored.advancements.MapleTriggers;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Wither's Subspace: every Wither opens one where it appears. Withered Hearts can't return inside it, and the
 * Wither never moves further than 50 blocks from its centre, so it can't be lured away. Every player inside when the
 * Wither dies is credited with the kill.
 */
public final class WitherSubspaces {
	/** How far a Wither's Subspace reaches, in blocks. */
	public static final double RADIUS = 100.0;

	/** How far from its Subspace's centre a Wither can move, in blocks. */
	public static final int LEASH_RADIUS = 50;

	// Rounding in a move's arithmetic, so a move ending on the leash's edge isn't undone.
	private static final double LEASH_TOLERANCE = 1.0E-6;

	private WitherSubspaces() {
	}

	static void register() {
		ServerEntityEvents.ENTITY_LOAD.register(WitherSubspaces::openOrLeash);
		Subspaces.ENDED.register(WitherSubspaces::creditKill);
	}

	/** Whether the position is inside any Wither's Subspace in the level. Works on both the server and the client. */
	public static boolean contains(Level level, Vec3 position) {
		return Subspaces.containing(level, position).stream().anyMatch(subspace -> subspace.kind() == Subspace.Kind.WITHER);
	}

	/**
	 * Makes the Wither's move of {@code delta} with {@code move}, vanilla's call, kept on its leash: it never ends further
	 * from its Subspace's centre than {@link #LEASH_RADIUS}, or than the Wither already is. A Wither without a Subspace
	 * in its level moves freely. A piston's push past the leash is refused rather than turned along its edge.
	 */
	public static void moveLeashed(WitherBoss wither, MoverType type, Vec3 delta, Consumer<Vec3> move) {
		Subspace subspace = wither.level() instanceof ServerLevel level ? Subspaces.ownedBy(level, wither.getUUID()).orElse(null) : null;

		if (subspace == null) {
			move.accept(delta);

			return;
		}

		Vec3 start = wither.position();
		double limit = Math.max(LEASH_RADIUS, start.distanceTo(subspace.centre()));

		// Vanilla keeps only the first axis a piston's push moves along, so turning the push would move the Wither sideways.
		move.accept(type == MoverType.PISTON ? delta : leashed(start.subtract(subspace.centre()), delta, limit));

		// Collisions stop each axis on its own, so a move kept inside can still end outside, such as sliding along a wall
		// at the leash's edge. That move is undone rather than pushed back through whatever blocked it.
		if (wither.position().distanceTo(subspace.centre()) > limit + LEASH_TOLERANCE) wither.setPos(start);
	}

	// Measured from the centre, like the Subspace's sphere, so the Wither can slide along the leash's edge.
	private static Vec3 leashed(Vec3 fromCentre, Vec3 delta, double limit) {
		Vec3 toCentre = fromCentre.add(delta);

		if (toCentre.length() <= limit) return delta;

		return toCentre.normalize().scale(limit).subtract(fromCentre);
	}

	/** Triggers {@code trigger} for every player inside the Wither's Subspace, but spectators. Server only. */
	public static void triggerForPlayersInside(WitherBoss wither, PlayerTrigger trigger) {
		if (!(wither.level() instanceof ServerLevel level)) return;

		Subspaces.ownedBy(level, wither.getUUID()).ifPresent(subspace -> trigger(level, subspace, trigger));
	}

	// Every way a Wither appears adds it to a level, so this catches both building one and /summon. One that already
	// has a Subspace is only loading again; its home keeps its wandering flight inside the leash.
	private static void openOrLeash(Entity entity, ServerLevel level) {
		if (!(entity instanceof WitherBoss wither) || !wither.isAlive()) return;

		if (!Subspaces.ownsAny(level.getServer(), wither.getUUID())) Subspaces.open(level, wither, Subspace.Kind.WITHER, RADIUS);

		Subspaces.ownedBy(level, wither.getUUID())
			.ifPresent(subspace -> wither.setHomeTo(BlockPos.containing(subspace.centre()), LEASH_RADIUS));
	}

	// The Subspace ends while its dying Wither is still in the level. A Wither discarded, such as by Peaceful, isn't
	// dying, so it credits nobody.
	private static void creditKill(ServerLevel level, Subspace subspace) {
		if (subspace.kind() == Subspace.Kind.WITHER && isDying(level, subspace)) trigger(level, subspace, MapleTriggers.WITHER_KILLED);
	}

	private static void trigger(ServerLevel level, Subspace subspace, PlayerTrigger trigger) {
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && subspace.contains(player.position())) trigger.trigger(player);
		}
	}

	// A Wither can carry its UUID to another level by command, while its Subspace stays where it opened.
	private static boolean isDying(ServerLevel level, Subspace subspace) {
		for (ServerLevel candidate : level.getServer().getAllLevels()) {
			if (candidate.getEntity(subspace.boss()) instanceof WitherBoss wither) return wither.isDeadOrDying();
		}

		return false;
	}
}
