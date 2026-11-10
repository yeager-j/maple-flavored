package mapleflavored.subspaces;

import java.util.function.Consumer;
import mapleflavored.gamerules.MapleGameRules;
import mapleflavored.gamerules.SyncedGameRule;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Wither's Subspace: every Wither opens one where it appears, as far as a game rule sets, and another game rule sets
 * the light level clients draw inside it. Withered Hearts can't return inside it, and the Wither never moves further from
 * its centre than a third game rule allows, 50 blocks by default, so it can't be lured away.
 */
public final class WitherSubspaces {
	private static final SyncedGameRule LIGHT_LEVEL = SyncedGameRule.of(MapleGameRules.WITHER_SUBSPACE_LIGHT_LEVEL);

	// Rounding in a move's arithmetic, so a move ending on the leash's edge isn't undone.
	private static final double LEASH_TOLERANCE = 1.0E-6;

	private WitherSubspaces() {
	}

	static void register() {
		ServerEntityEvents.ENTITY_LOAD.register(WitherSubspaces::openOrLeash);
	}

	/**
	 * The light level everything inside a Wither's Subspace in the level is drawn at, from 0 to 15, as the game rule sets
	 * it. Works on both the server and the client.
	 */
	public static int lightLevel(Level level) {
		return LIGHT_LEVEL.get(level);
	}

	/** Whether the position is inside any Wither's Subspace in the level. Works on both the server and the client. */
	public static boolean contains(Level level, Vec3 position) {
		return !Subspaces.containing(level, position, Subspace.Kind.WITHER).isEmpty();
	}

	/**
	 * Makes the Wither's move of {@code delta} with {@code move}, vanilla's call, kept on its leash: it never ends further
	 * from its Subspace's centre than the Wither leash radius game rule allows, or than the Wither already is. A Wither
	 * without a Subspace in its level moves freely. A piston's push past the leash is refused rather than turned along its edge.
	 */
	public static void moveLeashed(WitherBoss wither, MoverType type, Vec3 delta, Consumer<Vec3> move) {
		ServerLevel level = wither.level() instanceof ServerLevel serverLevel ? serverLevel : null;
		Subspace subspace = level == null ? null : Subspaces.ownedBy(level, wither.getUUID()).orElse(null);

		if (subspace == null) {
			move.accept(delta);

			return;
		}

		int leash = level.getGameRules().get(MapleGameRules.WITHER_LEASH_RADIUS);
		Vec3 start = wither.position();
		double limit = Math.max(leash, start.distanceTo(subspace.centre()));

		// The rule can change at any time, so the home its wandering flight stays in follows it.
		leashHome(wither, subspace, leash);

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

	// Every way a Wither appears adds it to a level, so this catches both building one and /summon.
	private static void openOrLeash(Entity entity, ServerLevel level) {
		if (!(entity instanceof WitherBoss wither) || !wither.isAlive()) return;

		Subspaces.openIfNew(level, wither, Subspace.Kind.WITHER, level.getGameRules().get(MapleGameRules.WITHER_SUBSPACE_RADIUS));

		int leash = level.getGameRules().get(MapleGameRules.WITHER_LEASH_RADIUS);

		Subspaces.ownedBy(level, wither.getUUID()).ifPresent(subspace -> leashHome(wither, subspace, leash));
	}

	// The home keeps the Wither's wandering flight picking spots within its leash.
	private static void leashHome(WitherBoss wither, Subspace subspace, int leash) {
		wither.setHomeTo(BlockPos.containing(subspace.centre()), leash);
	}
}
