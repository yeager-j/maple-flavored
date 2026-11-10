package mapleflavored.mobs;

import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;

/**
 * The one rule for when an entity is gone for good: it died, it was removed in a way that destroys it, as converting,
 * despawning and Peaceful's removal of monsters do, or its save failed to load, as Peaceful refuses to load a hostile
 * mob from its chunk. An entity that only unloads or changes dimension isn't gone.
 *
 * <p>Whatever must hear that a mob is gone, such as its Restore spot, its book's lock or its Subspace, listens to
 * {@link #EVENT}. An entity can be reported more than once: one that dies is reported as it dies and again when its
 * death animation ends and it's removed. So a listener must change nothing the second time.
 */
public final class GoneForGood {
	/** Fires on the server thread for each entity that is gone for good. */
	public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, listeners -> (server, gone) -> {
		for (Listener listener : listeners) {
			listener.onGone(server, gone);
		}
	});

	// Saved entities that failed to load, on whichever thread loaded them, until their server's next tick ends.
	private static final Queue<FailedLoad> FAILED_LOADS = new ConcurrentLinkedQueue<>();

	private GoneForGood() {
	}

	/** Registers the hooks that notice entities going. Call once from mod init. */
	static void register() {
		// A dying entity is gone the moment it dies; it's only removed once its death animation ends.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity.level() instanceof ServerLevel level) report(level.getServer(), entity, Cause.DIED);
		});
		// Unloading to a chunk or to another dimension has no reason, or one that doesn't destroy.
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			Entity.RemovalReason reason = entity.getRemovalReason();

			if (reason != null && reason.shouldDestroy()) report(level.getServer(), entity, Cause.REMOVED);
		});
		ServerTickEvents.END_SERVER_TICK.register(GoneForGood::reportFailedLoads);
	}

	/**
	 * Reports a saved entity that failed to load: it's dropped from its save, so it's gone for good. Call for every saved
	 * entity that fails to load. Safe to call from any thread; listeners hear of it by the end of the server's tick.
	 *
	 * @param level the level it failed to load into
	 * @param saved the entity as it was saved
	 */
	public static void failedToLoad(ServerLevel level, ValueInput saved) {
		saved.read("UUID", UUIDUtil.CODEC).ifPresent(uuid -> FAILED_LOADS.add(new FailedLoad(level.getServer(), uuid, saved)));
	}

	private static void report(MinecraftServer server, Entity entity, Cause cause) {
		EVENT.invoker().onGone(server, new Gone(entity.getUUID(), cause, new Attachments() {
			@Override
			public <A> Optional<A> read(AttachmentType<A> type) {
				return Optional.ofNullable(entity.getAttached(type));
			}
		}));
	}

	private static void reportFailedLoads(MinecraftServer server) {
		for (FailedLoad failed = FAILED_LOADS.poll(); failed != null; failed = FAILED_LOADS.poll()) {
			// A load that failed in a world closed since then is no news to this one.
			if (failed.server() != server) continue;

			ValueInput saved = failed.saved();

			EVENT.invoker().onGone(server, new Gone(failed.uuid(), Cause.FAILED_TO_LOAD, new Attachments() {
				@Override
				public <A> Optional<A> read(AttachmentType<A> type) {
					return savedAttachment(saved, type);
				}
			}));
		}
	}

	// The attachment as the entity saved it. One that isn't saved with its entity is never found.
	private static <A> Optional<A> savedAttachment(ValueInput saved, AttachmentType<A> type) {
		if (!type.isPersistent()) return Optional.empty();

		return saved.child(AttachmentTarget.NBT_ATTACHMENT_KEY)
			.flatMap(attachments -> attachments.read(type.identifier().toString(), type.persistenceCodec()));
	}

	/** Listens for entities that are gone for good. */
	@FunctionalInterface
	public interface Listener {
		void onGone(MinecraftServer server, Gone gone);
	}

	/** How an entity went. */
	public enum Cause {
		/** It died. It is still in its level, dying. */
		DIED,

		/** It was removed for good, with or without dying first. */
		REMOVED,

		/** Its save failed to load, so it never came back into a level. */
		FAILED_TO_LOAD
	}

	/**
	 * An entity that is gone for good.
	 *
	 * @param uuid the entity's UUID
	 * @param attachments what was attached to it as it went, or as it was last saved if it failed to load
	 */
	public record Gone(UUID uuid, Cause cause, Attachments attachments) {
		/** What the entity had attached, if it had it. A failed load only has what was saved with the entity. */
		public <A> Optional<A> attached(AttachmentType<A> type) {
			return attachments.read(type);
		}
	}

	/** Reads what an entity had attached. */
	public interface Attachments {
		<A> Optional<A> read(AttachmentType<A> type);
	}

	private record FailedLoad(MinecraftServer server, UUID uuid, ValueInput saved) {
	}
}
