package mapleflavored.wardensummoning;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import mapleflavored.MapleFlavored;
import mapleflavored.access.WardenSpawnTrackerAccessor;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Warden summoning, replacing vanilla's rule. A shriek counts only for players in {@link WardenTerritory}, and its
 * Warden spawns near one of them rather than at the shrieker, so a distant or spawn-proofed shrieker can't keep a
 * player's cooldown running. A search that finds no room clears the cooldown instead.
 */
public final class WardenSummoning {
	// Vanilla's: the players a shriek counts for, and the box in which a Warden stops shriekers warning.
	private static final double COUNTED_RADIUS = 16.0;
	private static final double NEARBY_WARDEN_BOX = 48.0;

	// Vanilla searches 20 times, 6 blocks up and down. The wide ring reaches farther, so it tries more and scans deeper.
	private static final SearchRing NEAR = new SearchRing(5, 15, 6, 20);
	private static final SearchRing WIDE = new SearchRing(5, 50, 12, 60);

	/**
	 * Whom the shrieker's current shriek counts for, kept with the shrieker so a world saved mid-shriek still knows whom
	 * to summon on.
	 */
	private static final AttachmentType<Shriek> SHRIEK = AttachmentRegistry.create(
		MapleFlavored.id("shriek"),
		builder -> builder.persistent(Shriek.CODEC)
	);

	private WardenSummoning() {
	}

	/** Registers the shrieker's saved shriek. Call once from mod init. */
	public static void register() {
	}

	/**
	 * Warns for a shriek the player caused, in place of {@link WardenSpawnTracker#tryWarn}: raises the warning level
	 * shared by the players it counts for, the cause and those within 16 blocks of the shrieker who are in Warden
	 * Territory.
	 *
	 * @return the new warning level, or empty when the shriek warns nobody: no one it would count for is in Warden
	 * Territory, one of them is on cooldown, or a Warden is near the shrieker
	 */
	public static OptionalInt warn(ServerLevel level, SculkShriekerBlockEntity shrieker, ServerPlayer cause) {
		shrieker.removeAttached(SHRIEK);

		BlockPos position = shrieker.getBlockPos();

		if (hasNearbyWarden(level, position)) return OptionalInt.empty();

		List<ServerPlayer> counted = countedPlayers(level, position, cause);

		if (counted.isEmpty()) return OptionalInt.empty();

		OptionalInt warningLevel = raiseSharedWarningLevel(counted);

		if (warningLevel.isPresent()) {
			shrieker.setAttached(SHRIEK, new Shriek(cause.getUUID(), counted.stream().map(ServerPlayer::getUUID).toList()));
		}

		return warningLevel;
	}

	/**
	 * Ends the shrieker's shriek, in place of vanilla's summon. At the max warning level it summons a Warden near the
	 * cause, if they're in Warden Territory, or else the player in Warden Territory nearest the shrieker. With nobody
	 * there, it's cancelled. If no spot has room, every player the shriek counted for comes off cooldown.
	 *
	 * @return whether a Warden appeared
	 */
	public static boolean summon(ServerLevel level, SculkShriekerBlockEntity shrieker, int warningLevel) {
		Shriek shriek = shrieker.removeAttached(SHRIEK);

		if (shriek == null || warningLevel < WardenSpawnTracker.MAX_WARNING_LEVEL) return false;

		ServerPlayer target = target(level, shrieker.getBlockPos(), shriek);

		if (target == null) return false;
		if (spawnNear(level, target).isPresent()) return true;

		clearCooldowns(level, shriek);

		return false;
	}

	/**
	 * Spawns a Warden 5 to 15 blocks across from the player, or failing that 5 to 50, on top of a block with room for it.
	 * Only spots in loaded chunks count. It emerges from the ground, as a shrieker's Warden does, and ignores warning
	 * levels and other Wardens nearby.
	 *
	 * @return the Warden, or empty if no spot had room
	 */
	public static Optional<Warden> spawnNear(ServerLevel level, ServerPlayer player) {
		Vec3 feet = player.position();

		return NEAR.trySpawn(level, feet).or(() -> WIDE.trySpawn(level, feet));
	}

	private static boolean hasNearbyWarden(ServerLevel level, BlockPos shrieker) {
		AABB box = AABB.ofSize(Vec3.atCenterOf(shrieker), NEARBY_WARDEN_BOX, NEARBY_WARDEN_BOX, NEARBY_WARDEN_BOX);

		return !level.getEntitiesOfClass(Warden.class, box).isEmpty();
	}

	// Vanilla's players, the cause wherever they are and the living non-spectators near the shrieker, less everyone
	// outside Warden Territory.
	private static List<ServerPlayer> countedPlayers(ServerLevel level, BlockPos shrieker, ServerPlayer cause) {
		Vec3 origin = Vec3.atCenterOf(shrieker);

		return level.getPlayers(player -> player == cause || isNear(player, origin)).stream()
			.filter(player -> WardenTerritory.contains(level, player.blockPosition()))
			.toList();
	}

	private static boolean isNear(ServerPlayer player, Vec3 origin) {
		return !player.isSpectator() && player.isAlive() && player.position().closerThan(origin, COUNTED_RADIUS);
	}

	// Vanilla's: the highest warning level goes up by one and every counted player takes it, unless any is on cooldown.
	private static OptionalInt raiseSharedWarningLevel(List<ServerPlayer> counted) {
		List<WardenSpawnTracker> trackers = counted.stream().map(ServerPlayer::getWardenSpawnTracker).toList();

		if (trackers.stream().anyMatch(tracker -> accessor(tracker).maple$onCooldown())) return OptionalInt.empty();

		WardenSpawnTracker highest = trackers.stream().max(Comparator.comparingInt(WardenSpawnTracker::getWarningLevel)).orElseThrow();

		accessor(highest).maple$increaseWarningLevel();
		trackers.forEach(tracker -> accessor(tracker).maple$copyData(highest));

		return OptionalInt.of(highest.getWarningLevel());
	}

	private static @Nullable ServerPlayer target(ServerLevel level, BlockPos shrieker, Shriek shriek) {
		if (level.getPlayerByUUID(shriek.cause()) instanceof ServerPlayer cause && isHuntable(level, cause)) return cause;

		Vec3 origin = Vec3.atCenterOf(shrieker);

		return level.getPlayers(player -> isHuntable(level, player)).stream()
			.min(Comparator.comparingDouble(player -> player.distanceToSqr(origin)))
			.orElse(null);
	}

	// A player a summoned Warden can come for: alive, playing and in Warden Territory.
	private static boolean isHuntable(ServerLevel level, ServerPlayer player) {
		return player.isAlive() && !player.isSpectator() && WardenTerritory.contains(level, player.blockPosition());
	}

	// Their warning level stays, so the next shriek that warns them summons again.
	private static void clearCooldowns(ServerLevel level, Shriek shriek) {
		for (UUID uuid : shriek.counted()) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);

			if (player != null) accessor(player.getWardenSpawnTracker()).maple$setCooldownTicks(0);
		}
	}

	private static WardenSpawnTrackerAccessor accessor(WardenSpawnTracker tracker) {
		return (WardenSpawnTrackerAccessor) tracker;
	}

	/** The player who caused a shriek, and every player it counted for, whose cooldowns a failed summon clears. */
	private record Shriek(UUID cause, List<UUID> counted) {
		static final Codec<Shriek> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("cause").forGetter(Shriek::cause),
			UUIDUtil.CODEC.listOf().fieldOf("counted").forGetter(Shriek::counted)
		).apply(instance, Shriek::new));
	}

	/**
	 * A ring of spots around a player, from {@code minDistance} to {@code maxDistance} blocks across, each scanned
	 * {@code verticalReach} blocks up and down for a block to stand on.
	 */
	private record SearchRing(double minDistance, double maxDistance, int verticalReach, int attempts) {
		Optional<Warden> trySpawn(ServerLevel level, Vec3 centre) {
			RandomSource random = level.getRandom();

			for (int attempt = 0; attempt < attempts; attempt++) {
				BlockPos spot = randomSpot(centre, random);

				// Reading a spot whose chunk isn't ready would wait for it to load or generate, on the server thread.
				// Level.isLoaded doesn't tell: it passes a chunk that's still loading.
				if (level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(spot.getX()), SectionPos.blockToSectionCoord(spot.getZ())) == null) {
					continue;
				}

				// One attempt with no spread, so vanilla's scan, border check and room check run at exactly this spot.
				Optional<Warden> warden = SpawnUtil.trySpawnMob(
					EntityTypes.WARDEN,
					EntitySpawnReason.TRIGGERED,
					level,
					spot,
					1,
					0,
					verticalReach,
					SpawnUtil.Strategy.ON_TOP_OF_COLLIDER,
					false
				);

				if (warden.isPresent()) return warden;
			}

			return Optional.empty();
		}

		// Uniform over the ring's area, as vanilla's square search is over its square. A Warden spawns at the middle of its
		// block, so that's where the distance is measured to.
		private BlockPos randomSpot(Vec3 centre, RandomSource random) {
			BlockPos centreBlock = BlockPos.containing(centre);
			int reach = Mth.ceil(maxDistance) + 1;
			BlockPos spot;
			double distance;

			do {
				spot = centreBlock.offset(random.nextIntBetweenInclusive(-reach, reach), 0, random.nextIntBetweenInclusive(-reach, reach));
				distance = Math.hypot(spot.getX() + 0.5 - centre.x, spot.getZ() + 0.5 - centre.z);
			} while (distance < minDistance || distance > maxDistance);

			return spot;
		}
	}
}
