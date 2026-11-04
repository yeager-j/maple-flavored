package mapleflavored.remnants;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Each player's last solid ground per dimension: where they last stood on something solid, outside lava. */
final class SolidGround {
	// The exact standing position rather than the block, since a Remnant at the bottom of a slab's block would sit
	// inside the slab, out of reach.
	private static final AttachmentType<Map<ResourceKey<Level>, Vec3>> LAST_SOLID_GROUND = AttachmentRegistry.create(
		MapleFlavored.id("last_solid_ground"),
		builder -> builder
			.persistent(Codec.unboundedMap(ResourceKey.codec(Registries.DIMENSION), Vec3.CODEC))
			.copyOnDeath()
	);

	private SolidGround() {
	}

	static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			for (ServerPlayer player : level.players()) {
				track(player);
			}
		});
	}

	/** Where the player last stood on solid ground in that dimension, if they ever have. */
	static Optional<Vec3> lastIn(ServerPlayer player, ResourceKey<Level> dimension) {
		return Optional.ofNullable(player.getAttachedOrElse(LAST_SOLID_GROUND, Map.of()).get(dimension));
	}

	private static void track(ServerPlayer player) {
		if (!isOnSolidGround(player)) return;

		ResourceKey<Level> dimension = player.level().dimension();
		Map<ResourceKey<Level>, Vec3> lastSolidGround = player.getAttachedOrElse(LAST_SOLID_GROUND, Map.of());
		Vec3 recorded = lastSolidGround.get(dimension);

		// Recording once per block keeps a walking player from rewriting the attachment every tick.
		if (recorded != null && BlockPos.containing(recorded).equals(player.blockPosition())) return;

		Map<ResourceKey<Level>, Vec3> updated = new HashMap<>(lastSolidGround);

		updated.put(dimension, player.position());
		player.setAttached(LAST_SOLID_GROUND, Map.copyOf(updated));
	}

	// The server's on-ground flag comes from the client, so the block below is checked as well. That also leaves out
	// standing on an entity, such as a boat, which can drift away.
	private static boolean isOnSolidGround(ServerPlayer player) {
		if (!player.isAlive() || player.isSpectator() || !player.onGround()) return false;
		if (!Remnants.canCollectAt(player)) return false;

		BlockPos below = player.getOnPos();

		return !player.level().getBlockState(below).getCollisionShape(player.level(), below).isEmpty();
	}
}
