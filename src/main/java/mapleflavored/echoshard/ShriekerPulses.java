package mapleflavored.echoshard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mapleflavored.MapleFlavored;
import mapleflavored.gamerules.MapleGameRules;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/**
 * Holding an Echo Shard lets a player hear every sculk shrieker near them: each pulses like a Warden's heartbeat from
 * where it is, faster the closer it is, so 3D sound and directional subtitles lead to it even through walls. Only the
 * holder hears it, and a pulse is no vibration.
 */
public final class ShriekerPulses {
	private static final Identifier PULSE_ID = MapleFlavored.id("item.echo_shard.shrieker_pulse");

	/** A shrieker's pulse, as an Echo Shard's holder hears it. */
	public static final Holder<SoundEvent> PULSE = Registry.registerForHolder(
		BuiltInRegistries.SOUND_EVENT,
		PULSE_ID,
		SoundEvent.createVariableRangeEvent(PULSE_ID)
	);

	// Playtest values: from a slow beat at the edge of hearing to a quick one right beside the shrieker.
	private static final int FARTHEST_BEAT_TICKS = 40;
	private static final int CLOSEST_BEAT_TICKS = 6;
	// A sound at volume 1 fades out over 16 blocks; a louder one carries further.
	private static final float BLOCKS_PER_VOLUME = 16.0F;

	// When each shrieker the player hears next pulses, by game time. Only kept while they listen.
	private static final AttachmentType<Map<BlockPos, Long>> NEXT_PULSES = AttachmentRegistry.create(
		MapleFlavored.id("next_shrieker_pulses"),
		builder -> builder.initializer(Map::of)
	);

	private ShriekerPulses() {
	}

	/** Registers the pulse sound and lets Echo Shard holders hear shriekers. Call once from mod init. */
	public static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			for (ServerPlayer player : level.players()) {
				listen(level, player);
			}
		});
	}

	/**
	 * How long a shrieker waits between pulses at {@code distance} blocks from the listener: 6 ticks beside it, slowing
	 * evenly to 40 at the edge of hearing.
	 *
	 * @param range how far the listener hears shriekers
	 */
	public static int beatTicks(double distance, int range) {
		double nearness = Mth.clamp(distance / range, 0.0, 1.0);

		return (int) Math.round(Mth.lerp(nearness, CLOSEST_BEAT_TICKS, FARTHEST_BEAT_TICKS));
	}

	private static void listen(ServerLevel level, ServerPlayer player) {
		if (!isListening(player)) {
			player.removeAttached(NEXT_PULSES);

			return;
		}

		int range = level.getGameRules().get(MapleGameRules.ECHO_SHARD_SHRIEKER_RANGE);
		long now = level.getGameTime();
		Map<BlockPos, Long> due = player.getAttachedOrCreate(NEXT_PULSES);
		Map<BlockPos, Long> next = new HashMap<>();

		for (BlockPos shrieker : shriekersNear(level, player.getEyePosition(), range)) {
			double distance = Math.sqrt(shrieker.distToCenterSqr(player.getEyePosition()));
			long pulseAt = due.getOrDefault(shrieker, now);

			if (pulseAt <= now) {
				pulse(level, player, shrieker, range);
				pulseAt = now + beatTicks(distance, range);
			}

			next.put(shrieker, pulseAt);
		}

		player.setAttached(NEXT_PULSES, Map.copyOf(next));
	}

	// In either hand, so a pickaxe can stay in the other.
	private static boolean isListening(ServerPlayer player) {
		return player.isAlive()
			&& !player.isSpectator()
			&& (player.getMainHandItem().is(Items.ECHO_SHARD) || player.getOffhandItem().is(Items.ECHO_SHARD));
	}

	// Read from loaded chunks' block entities, so nothing loads and no blocks are scanned.
	private static List<BlockPos> shriekersNear(ServerLevel level, Vec3 ear, int range) {
		double rangeSqr = (double) range * range;
		int minX = SectionPos.blockToSectionCoord(ear.x - range);
		int maxX = SectionPos.blockToSectionCoord(ear.x + range);
		int minZ = SectionPos.blockToSectionCoord(ear.z - range);
		int maxZ = SectionPos.blockToSectionCoord(ear.z + range);
		List<BlockPos> shriekers = new ArrayList<>();

		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);

				if (chunk == null) continue;

				chunk.getBlockEntities().values().stream()
					.filter(blockEntity -> blockEntity.getBlockState().is(Blocks.SCULK_SHRIEKER))
					.map(blockEntity -> blockEntity.getBlockPos().immutable())
					.filter(pos -> pos.distToCenterSqr(ear) <= rangeSqr)
					.forEach(shriekers::add);
			}
		}

		return shriekers;
	}

	// Sent to the listener alone, so nobody else hears it, and played without a game event, so nothing senses it.
	private static void pulse(ServerLevel level, ServerPlayer player, BlockPos shrieker, int range) {
		Vec3 from = Vec3.atCenterOf(shrieker);
		float volume = Math.max(1.0F, range / BLOCKS_PER_VOLUME);

		player.connection.send(new ClientboundSoundPacket(PULSE, SoundSource.BLOCKS, from.x, from.y, from.z, volume, 1.0F, level.getRandom().nextLong()));
	}
}
