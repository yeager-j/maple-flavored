package mapleflavored.bosses;

import java.util.function.Predicate;
import mapleflavored.advancements.MapleTriggers;
import mapleflavored.mixin.EnderDragonAccessor;
import mapleflavored.mixin.EnderDragonFightAccessor;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.level.dimension.end.EnderDragonFight;

/**
 * Shared kill credit for the bosses without a Subspace. An Elder Guardian's death credits every player within 64 blocks
 * of it, and an Ender Dragon's death every player its fight counts as taking part. Spectators are never credited.
 */
public final class BossKillCredit {
	/** How far from a dying Elder Guardian a player is credited with its kill, in blocks. */
	public static final double ELDER_GUARDIAN_CREDIT_RANGE = 64.0;

	private BossKillCredit() {
	}

	/** Registers the death hook. Call once from mod init. */
	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register(BossKillCredit::credit);
	}

	private static void credit(LivingEntity entity, DamageSource source) {
		if (!(entity.level() instanceof ServerLevel level)) return;

		if (entity instanceof ElderGuardian guardian) {
			Predicate<Entity> nearby = player -> player.distanceToSqr(guardian) <= ELDER_GUARDIAN_CREDIT_RANGE * ELDER_GUARDIAN_CREDIT_RANGE;

			trigger(level, nearby, MapleTriggers.ELDER_GUARDIAN_KILLED);
		} else if (entity instanceof EnderDragon dragon) {
			EnderDragonFight fight = ((EnderDragonAccessor) dragon).maple$dragonFight();

			// A dragon outside a fight credits only its killer, through vanilla's own criterion.
			if (fight != null) trigger(level, ((EnderDragonFightAccessor) fight).maple$validPlayer(), MapleTriggers.ENDER_DRAGON_KILLED);
		}
	}

	private static void trigger(ServerLevel level, Predicate<Entity> credited, PlayerTrigger trigger) {
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && credited.test(player)) trigger.trigger(player);
		}
	}
}
