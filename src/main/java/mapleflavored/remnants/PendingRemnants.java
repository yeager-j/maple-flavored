package mapleflavored.remnants;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Adds new Remnants to their level once the entities of the chunk they go in have loaded.
 *
 * <p>A Remnant can go far from where its owner died, to their last solid ground or respawn point. Adding an entity to
 * a chunk whose saved entities haven't loaded would later save that chunk with only the new entity, losing the rest.
 * Until then the Remnant waits on its owner, saved with them, so a shutdown or logout doesn't lose it.
 */
final class PendingRemnants {
	private static final int LOADING_TICKET_RADIUS = 1;
	private static final TicketType LOADING_TICKET = Registry.register(
		BuiltInRegistries.TICKET_TYPE,
		MapleFlavored.id("remnant"),
		new TicketType(20L, TicketType.FLAG_LOADING)
	);

	private static final AttachmentType<List<Pending>> PENDING = AttachmentRegistry.create(
		MapleFlavored.id("pending_remnants"),
		builder -> builder
			.persistent(Pending.CODEC.listOf())
			.copyOnDeath()
	);

	private PendingRemnants() {
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.hasAttached(PENDING)) placeWaiting(player);
			}
		});
	}

	/** Adds a Remnant of the owner's points to the level now if its chunk's entities are loaded, or else once they are. */
	static void add(ServerPlayer owner, ServerLevel level, Vec3 position, int points) {
		List<Pending> pending = new ArrayList<>(owner.getAttachedOrElse(PENDING, List.of()));

		pending.add(new Pending(level.dimension(), position, points, Remnants.deaths(owner)));
		owner.setAttached(PENDING, List.copyOf(pending));
		placeWaiting(owner);
	}

	private static void placeWaiting(ServerPlayer owner) {
		List<Pending> stillWaiting = owner.getAttachedOrElse(PENDING, List.<Pending>of())
			.stream()
			.filter(pending -> !tryPlace(owner, pending))
			.toList();

		if (stillWaiting.isEmpty()) {
			owner.removeAttached(PENDING);
		} else {
			owner.setAttached(PENDING, stillWaiting);
		}
	}

	/** Adds the Remnant if its chunk's entities are loaded, or else keeps the chunk loading; true once it's settled. */
	private static boolean tryPlace(ServerPlayer owner, Pending pending) {
		ServerLevel level = owner.level().getServer().getLevel(pending.dimension());

		// A dimension that no longer exists, such as one from a removed datapack, can never take the Remnant.
		if (level == null) return true;

		ChunkPos chunk = ChunkPos.containing(BlockPos.containing(pending.position()));

		if (!level.areEntitiesLoaded(chunk.pack())) {
			level.getChunkSource().addTicketWithRadius(LOADING_TICKET, chunk, LOADING_TICKET_RADIUS);

			return false;
		}

		level.addFreshEntity(Remnant.create(level, pending.position(), owner.getUUID(), pending.points(), pending.ownerDeaths()));

		return true;
	}

	/** A Remnant not yet in its level, with its owner's death count when it was made. */
	private record Pending(ResourceKey<Level> dimension, Vec3 position, int points, int ownerDeaths) {
		static final Codec<Pending> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Pending::dimension),
			Vec3.CODEC.fieldOf("position").forGetter(Pending::position),
			Codec.INT.fieldOf("points").forGetter(Pending::points),
			Codec.INT.fieldOf("owner_deaths").forGetter(Pending::ownerDeaths)
		).apply(instance, Pending::new));
	}
}
