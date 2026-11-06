package mapleflavored.subspaces;

import java.util.Objects;
import java.util.function.Consumer;
import mapleflavored.MapleFlavored;
import mapleflavored.advancements.MapleTriggers;
import mapleflavored.gamerules.MapleGameRules;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.network.codec.ByteBufCodecs;
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
 * its centre than a third game rule allows, 50 blocks by default, so it can't be lured away. Every player inside when the
 * Wither dies is credited with the kill.
 */
public final class WitherSubspaces {
	/**
	 * The light level the Wither Subspace light level game rule sets, synced to every player in the level so their client
	 * can draw it. Read it through {@link #lightLevel}; only this module writes it.
	 */
	private static final AttachmentType<Integer> LIGHT_LEVEL = AttachmentRegistry.create(
		MapleFlavored.id("wither_subspace_light_level"),
		builder -> builder.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all())
	);

	// Rounding in a move's arithmetic, so a move ending on the leash's edge isn't undone.
	private static final double LEASH_TOLERANCE = 1.0E-6;

	private WitherSubspaces() {
	}

	static void register() {
		ServerEntityEvents.ENTITY_LOAD.register(WitherSubspaces::openOrLeash);
		Subspaces.ENDED.register(WitherSubspaces::creditKill);
		ServerTickEvents.END_LEVEL_TICK.register(WitherSubspaces::syncLightLevel);
	}

	/**
	 * The light level everything inside a Wither's Subspace in the level is drawn at, from 0 to 15, as the game rule sets
	 * it. Works on both the server and the client.
	 */
	public static int lightLevel(Level level) {
		return level.getAttachedOrElse(LIGHT_LEVEL, MapleGameRules.WITHER_SUBSPACE_LIGHT_LEVEL.defaultValue());
	}

	/** Whether the position is inside any Wither's Subspace in the level. Works on both the server and the client. */
	public static boolean contains(Level level, Vec3 position) {
		return Subspaces.containing(level, position).stream().anyMatch(subspace -> subspace.kind() == Subspace.Kind.WITHER);
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

	// Game rules stay on the server, so the light level reaches clients as level data, and a change shows at once.
	private static void syncLightLevel(ServerLevel level) {
		int rule = level.getGameRules().get(MapleGameRules.WITHER_SUBSPACE_LIGHT_LEVEL);

		if (!Objects.equals(level.getAttached(LIGHT_LEVEL), rule)) level.setAttached(LIGHT_LEVEL, rule);
	}

	/** Triggers {@code trigger} for every player inside the Wither's Subspace, but spectators. Server only. */
	public static void triggerForPlayersInside(WitherBoss wither, PlayerTrigger trigger) {
		if (!(wither.level() instanceof ServerLevel level)) return;

		Subspaces.ownedBy(level, wither.getUUID()).ifPresent(subspace -> Subspaces.triggerForPlayersInside(level, subspace, trigger));
	}

	// Every way a Wither appears adds it to a level, so this catches both building one and /summon. One that already
	// has a Subspace is only loading again.
	private static void openOrLeash(Entity entity, ServerLevel level) {
		if (!(entity instanceof WitherBoss wither) || !wither.isAlive()) return;

		if (!Subspaces.ownsAny(level.getServer(), wither.getUUID())) {
			Subspaces.open(level, wither, Subspace.Kind.WITHER, level.getGameRules().get(MapleGameRules.WITHER_SUBSPACE_RADIUS));
		}

		int leash = level.getGameRules().get(MapleGameRules.WITHER_LEASH_RADIUS);

		Subspaces.ownedBy(level, wither.getUUID()).ifPresent(subspace -> leashHome(wither, subspace, leash));
	}

	// The home keeps the Wither's wandering flight picking spots within its leash.
	private static void leashHome(WitherBoss wither, Subspace subspace, int leash) {
		wither.setHomeTo(BlockPos.containing(subspace.centre()), leash);
	}

	// A Wither discarded, such as by Peaceful, credits nobody.
	private static void creditKill(ServerLevel level, Subspace subspace) {
		if (subspace.kind() != Subspace.Kind.WITHER || !Subspaces.bossIsDying(level.getServer(), subspace)) return;

		Subspaces.triggerForPlayersInside(level, subspace, MapleTriggers.WITHER_KILLED);
	}
}
