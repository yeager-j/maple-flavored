package mapleflavored.remnants;

import java.util.Optional;
import mapleflavored.MapleFlavored;
import mapleflavored.experience.ExperiencePoints;
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
			if (!alive) ExperiencePoints.copy(oldPlayer, newPlayer);
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

	private static void onDeath(ServerPlayer player) {
		if (player.isSpectator()) return;
		if (player.level().getGameRules().get(MapleGameRules.KEEP_XP)) return;

		int points = ExperiencePoints.total(player);

		ExperiencePoints.clear(player);

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

	/** Where a Remnant goes, and whether that is somewhere other than its owner's death spot. */
	private record Placement(ServerLevel level, Vec3 position, boolean moved) {
	}
}
