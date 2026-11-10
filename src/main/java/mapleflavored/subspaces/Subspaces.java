package mapleflavored.subspaces;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;
import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Subspaces module: the areas bosses open around where they appear. Each {@link Subspace} lasts until its boss dies
 * or is removed for good, however long its boss stays unloaded, and its rules come from its record alone.
 *
 * <p>Every level keeps its own Subspaces, saved with the level and sent to the players in it.
 */
public final class Subspaces {
	// Each level's Subspaces, synced to the players in that level. Nothing outside this class reads or writes it.
	private static final AttachmentType<List<Subspace>> ATTACHMENT = AttachmentRegistry.create(
		MapleFlavored.id("subspaces"),
		builder -> builder
			.persistent(Subspace.CODEC.listOf())
			.syncWith(Subspace.STREAM_CODEC.apply(ByteBufCodecs.list()), AttachmentSyncPredicate.all())
	);

	/** Fires once for each Subspace, as its boss opens it, with the Subspace already among its level's. */
	public static final Event<Started> STARTED = EventFactory.createArrayBacked(Started.class, listeners -> (level, subspace) -> {
		for (Started listener : listeners) {
			listener.onStarted(level, subspace);
		}
	});

	/** Fires once for each Subspace, as it ends, with what ended it. The Subspace is already gone from its level's. */
	public static final Event<Ended> ENDED = EventFactory.createArrayBacked(Ended.class, listeners -> (level, subspace, cause) -> {
		for (Ended listener : listeners) {
			listener.onEnded(level, subspace, cause);
		}
	});

	private Subspaces() {
	}

	/** Registers the hooks that open and end Subspaces and apply their rules. Call once from mod init. */
	public static void register() {
		// A dying boss ends its Subspace the moment it dies; it's only removed once its death animation ends.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity.level() instanceof ServerLevel level) endOwnedBy(level.getServer(), entity.getUUID(), EndCause.BOSS_DIED);
		});
		// Removed without dying, such as a Warden digging down. Unloading has no reason, or one that doesn't destroy.
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			Entity.RemovalReason reason = entity.getRemovalReason();

			if (reason != null && reason.shouldDestroy()) endOwnedBy(level.getServer(), entity.getUUID(), EndCause.BOSS_REMOVED);
		});
		ServerTickEvents.END_SERVER_TICK.register(Subspaces::endAllWhilePeaceful);
		WardenSubspaces.register();
		WitherSubspaces.register();
	}

	/** The Subspaces in the level that contain the position. Works on both the server and the client. */
	public static List<Subspace> containing(Level level, Vec3 position) {
		return inLevel(level).stream().filter(subspace -> subspace.contains(position)).toList();
	}

	/** The Subspaces of one kind in the level that contain the position. Works on both the server and the client. */
	public static List<Subspace> containing(Level level, Vec3 position, Subspace.Kind kind) {
		return inLevel(level, kind).stream().filter(subspace -> subspace.contains(position)).toList();
	}

	/** Every Subspace in the level. Works on both the server and the client. */
	public static List<Subspace> inLevel(Level level) {
		return level.getAttachedOrElse(ATTACHMENT, List.of());
	}

	/** Every Subspace of one kind in the level. Works on both the server and the client. */
	public static List<Subspace> inLevel(Level level, Subspace.Kind kind) {
		return inLevel(level).stream().filter(subspace -> subspace.kind() == kind).toList();
	}

	/** The Subspace the boss opened in the level, if it opened one there. */
	public static Optional<Subspace> ownedBy(Level level, UUID boss) {
		return inLevel(level).stream().filter(subspace -> subspace.boss().equals(boss)).findFirst();
	}

	/** Calls the listener as each Subspace of one kind starts, as {@link #STARTED} does. Register once, at mod init. */
	public static void whenStarted(Subspace.Kind kind, Started listener) {
		STARTED.register((level, subspace) -> {
			if (subspace.kind() == kind) listener.onStarted(level, subspace);
		});
	}

	/** Calls the listener as each Subspace of one kind ends, as {@link #ENDED} does. Register once, at mod init. */
	public static void whenEnded(Subspace.Kind kind, Ended listener) {
		ENDED.register((level, subspace, cause) -> {
			if (subspace.kind() == kind) listener.onEnded(level, subspace, cause);
		});
	}

	/**
	 * Ends every Subspace in the level whose boss isn't loaded in any level right now. Nothing tells a boss that will load
	 * again from one that never will, so nothing ends these on its own: this is for whoever knows they were abandoned.
	 */
	public static void endAbandoned(ServerLevel level) {
		end(level, subspace -> !isBossLoaded(level.getServer(), subspace), EndCause.ABANDONED);
	}

	/** Whether the Subspace with this id is still open in the level. */
	static boolean isActive(Level level, UUID id) {
		return inLevel(level).stream().anyMatch(subspace -> subspace.id().equals(id));
	}

	/**
	 * Opens a Subspace centred on where the boss stands, unless it has one in any level already: then it's only loading
	 * again, perhaps after carrying its UUID through a portal.
	 */
	static void openIfNew(ServerLevel level, Entity boss, Subspace.Kind kind, double radius) {
		if (ownsAny(level.getServer(), boss.getUUID())) return;

		Subspace subspace = new Subspace(UUID.randomUUID(), boss.getUUID(), kind, boss.position(), radius);

		level.setAttached(ATTACHMENT, Stream.concat(inLevel(level).stream(), Stream.of(subspace)).toList());
		STARTED.invoker().onStarted(level, subspace);
	}

	private static boolean ownsAny(MinecraftServer server, UUID boss) {
		for (ServerLevel level : server.getAllLevels()) {
			if (ownedBy(level, boss).isPresent()) return true;
		}

		return false;
	}

	private static boolean isBossLoaded(MinecraftServer server, Subspace subspace) {
		for (ServerLevel level : server.getAllLevels()) {
			if (level.getEntity(subspace.boss()) != null) return true;
		}

		return false;
	}

	// A boss can carry its UUID to another dimension, while its Subspace stays where it was opened.
	private static void endOwnedBy(MinecraftServer server, UUID boss, EndCause cause) {
		for (ServerLevel level : server.getAllLevels()) {
			end(level, subspace -> subspace.boss().equals(boss), cause);
		}
	}

	// Every boss is a monster, which Peaceful removes as it loads and never loads again, so no death or removal would
	// ever end its Subspace.
	private static void endAllWhilePeaceful(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			if (level.getDifficulty() == Difficulty.PEACEFUL) end(level, subspace -> true, EndCause.PEACEFUL);
		}
	}

	private static void end(ServerLevel level, Predicate<Subspace> ending, EndCause cause) {
		List<Subspace> subspaces = inLevel(level);
		List<Subspace> ended = subspaces.stream().filter(ending).toList();

		if (ended.isEmpty()) return;

		level.setAttached(ATTACHMENT, subspaces.stream().filter(ending.negate()).toList());
		ended.forEach(subspace -> ENDED.invoker().onEnded(level, subspace, cause));
	}

	/** Called when a Subspace starts. */
	@FunctionalInterface
	public interface Started {
		void onStarted(ServerLevel level, Subspace subspace);
	}

	/** Called when a Subspace ends. */
	@FunctionalInterface
	public interface Ended {
		void onEnded(ServerLevel level, Subspace subspace, EndCause cause);
	}

	/** What ended a Subspace. */
	public enum EndCause {
		/** Its boss died. The boss is still in its level, dying. */
		BOSS_DIED,

		/** Its boss was removed for good without dying, such as a Warden digging down. */
		BOSS_REMOVED,

		/** Its level turned Peaceful, which never loads its boss again. */
		PEACEFUL,

		/** It was {@linkplain #endAbandoned ended as abandoned}, with its boss not loaded. */
		ABANDONED
	}
}
