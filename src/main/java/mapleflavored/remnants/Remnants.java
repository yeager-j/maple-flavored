package mapleflavored.remnants;

import java.util.Optional;
import mapleflavored.MapleFlavored;
import mapleflavored.gamerules.MapleGameRules;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * The Remnants module: death drops all of a player's XP as a {@link Remnant}, unless Keep XP is on.
 *
 * <p>A Remnant stays where its owner died, unless nothing could be collected there, such as in the void or lava. Then
 * it goes to their last solid ground in that dimension, or else to their respawn point, and the Recovery Compass
 * points to it.
 */
public final class Remnants {
	// Reaching level 21,864 takes more than Integer.MAX_VALUE points.
	private static final int HIGHEST_LEVEL_IN_POINTS = 21_863;
	private static final ResourceKey<EntityType<?>> REMNANT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, MapleFlavored.id("remnant"));

	/** The Remnant entity type. */
	public static final EntityType<Remnant> REMNANT = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		REMNANT_KEY,
		EntityType.Builder.<Remnant>of(Remnant::new, MobCategory.MISC)
			.noLootTable()
			.fireImmune()
			.sized(0.5F, 0.5F)
			.clientTrackingRange(8)
			.updateInterval(20)
			.build(REMNANT_KEY)
	);

	private Remnants() {
	}

	/** Registers the Remnant entity and the death, respawn and solid ground hooks. Call once from mod init. */
	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) onDeath(player);
		});
		// Death takes the XP away when Keep XP is off, so whatever the dead player still holds is theirs to keep.
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			if (!alive) copyXp(oldPlayer, newPlayer);
		});
		SolidGround.register();
		PendingRemnants.register();
	}

	/** The player's deaths, counted by vanilla's statistic, including the one being handled during a death. */
	static int deaths(ServerPlayer player) {
		return player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
	}

	/** Whether a Remnant at the player's feet could be collected: not in the void and not in lava. */
	static boolean canCollectAt(ServerPlayer player) {
		return !player.isInLava() && canCollectAt(player.level(), player.blockPosition());
	}

	private static boolean canCollectAt(ServerLevel level, BlockPos pos) {
		return pos.getY() >= level.getMinY() && !level.getFluidState(pos).is(FluidTags.LAVA);
	}

	/**
	 * All the player's XP in points, from their level and progress.
	 *
	 * <p>Vanilla's {@code totalExperience} isn't used, since adding levels by command doesn't update it.
	 */
	static int xpPoints(Player player) {
		// Commands allow levels whose points don't fit in an int, or whose square doesn't fit in a long.
		if (player.experienceLevel > HIGHEST_LEVEL_IN_POINTS) return Integer.MAX_VALUE;

		long pointsInLevel = Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
		long points = pointsToReachLevel(player.experienceLevel) + pointsInLevel;

		return (int) Math.min(points, Integer.MAX_VALUE);
	}

	// Closed forms of summing Player.getXpNeededForNextLevel over every lower level, halved last to stay in integers.
	private static long pointsToReachLevel(long level) {
		if (level <= 16) return level * level + 6 * level;
		if (level <= 31) return (5 * level * level - 81 * level + 720) / 2;

		return (9 * level * level - 325 * level + 4440) / 2;
	}

	private static void onDeath(ServerPlayer player) {
		if (player.isSpectator()) return;
		if (player.level().getGameRules().get(MapleGameRules.KEEP_XP)) return;

		int points = xpPoints(player);

		takeAllXp(player);

		if (points == 0) return;

		Placement placement = placementFor(player);

		PendingRemnants.add(player, placement.level(), placement.position(), points);

		if (placement.moved()) {
			BlockPos remnantPos = BlockPos.containing(placement.position());

			player.setLastDeathLocation(Optional.of(GlobalPos.of(placement.level().dimension(), remnantPos)));
		}
	}

	private static Placement placementFor(ServerPlayer player) {
		ServerLevel level = player.level();

		if (canCollectAt(player)) return new Placement(level, player.position(), false);

		// Recorded ground may have filled with lava since.
		Optional<Vec3> solidGround = SolidGround.lastIn(player, level.dimension())
			.filter(position -> canCollectAt(level, BlockPos.containing(position)));

		if (solidGround.isPresent()) return new Placement(level, solidGround.get(), true);

		// Not consuming the spawn block leaves a respawn anchor's charge for the respawn itself.
		TeleportTransition respawn = player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);

		return new Placement(respawn.newLevel(), respawn.position(), true);
	}

	private static void takeAllXp(Player player) {
		player.experienceLevel = 0;
		player.experienceProgress = 0.0F;
		player.totalExperience = 0;
	}

	private static void copyXp(Player from, Player to) {
		to.experienceLevel = from.experienceLevel;
		to.experienceProgress = from.experienceProgress;
		to.totalExperience = from.totalExperience;
	}

	/** Where a Remnant goes, and whether that is somewhere other than its owner's death spot. */
	private record Placement(ServerLevel level, Vec3 position, boolean moved) {
	}
}
