package mapleflavored.bosses;

import java.util.function.Predicate;
import mapleflavored.access.EnderDragonAccessor;
import mapleflavored.access.EnderDragonFightAccessor;
import mapleflavored.advancements.MapleTriggers;
import mapleflavored.gamerules.MapleGameRules;
import mapleflavored.subspaces.Subspace;
import mapleflavored.subspaces.Subspaces;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.dimension.end.EnderDragonFight;

/**
 * Shared credit for the boss advancements. An Elder Guardian's death credits every player within 64 blocks of it. A
 * Warden dying or digging back down credits every player within the Warden survival range game rule of it. An Ender
 * Dragon's death credits every player its fight counts as taking part. A Wither's death, and whatever else it does that
 * earns an advancement, credits every player inside its Subspace. Spectators are never credited.
 */
public final class BossKillCredit {
	/** How far from a dying Elder Guardian a player is credited with its kill, in blocks. */
	public static final double ELDER_GUARDIAN_CREDIT_RANGE = 64.0;

	private BossKillCredit() {
	}

	/** Registers the death hooks. Call once from mod init. */
	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register(BossKillCredit::creditDeath);
		// A Wither's Subspace has ended by the time its death is announced here, so its kill is credited as its Subspace
		// ends, which says whether the Wither died.
		Subspaces.whenEnded(Subspace.Kind.WITHER, BossKillCredit::creditWitherKill);
	}

	/** Credits every player near the Warden with surviving it. Call as the Warden digs back down, before it's removed. */
	public static void creditWardenSurvivors(Warden warden) {
		if (!(warden.level() instanceof ServerLevel level)) return;

		double range = level.getGameRules().get(MapleGameRules.WARDEN_SURVIVAL_RANGE);

		trigger(level, within(warden, range), MapleTriggers.WARDEN_SURVIVED);
	}

	/** Triggers {@code trigger} for every player inside the Wither's Subspace. Server only. */
	public static void creditPlayersInside(WitherBoss wither, PlayerTrigger trigger) {
		if (!(wither.level() instanceof ServerLevel level)) return;

		Subspaces.ownedBy(level, wither.getUUID()).ifPresent(subspace -> trigger(level, inside(subspace), trigger));
	}

	// A Wither removed without dying, such as by Peaceful, credits nobody.
	private static void creditWitherKill(ServerLevel level, Subspace subspace, Subspaces.EndCause cause) {
		if (cause == Subspaces.EndCause.BOSS_DIED) trigger(level, inside(subspace), MapleTriggers.WITHER_KILLED);
	}

	private static void creditDeath(LivingEntity entity, DamageSource source) {
		if (!(entity.level() instanceof ServerLevel level)) return;

		switch (entity) {
			case ElderGuardian guardian -> trigger(level, within(guardian, ELDER_GUARDIAN_CREDIT_RANGE), MapleTriggers.ELDER_GUARDIAN_KILLED);
			case Warden warden -> creditWardenSurvivors(warden);
			case EnderDragon dragon -> creditDragonFight(level, dragon);
			default -> {
			}
		}
	}

	// A dragon outside a fight, such as one summoned in the Overworld, credits only its killer, through vanilla's own
	// criterion.
	private static void creditDragonFight(ServerLevel level, EnderDragon dragon) {
		EnderDragonFight fight = ((EnderDragonAccessor) dragon).maple$dragonFight();

		if (fight == null) return;

		trigger(level, ((EnderDragonFightAccessor) fight).maple$validPlayer(), MapleTriggers.ENDER_DRAGON_KILLED);
	}

	private static Predicate<Entity> within(Entity boss, double range) {
		return player -> player.distanceToSqr(boss) <= range * range;
	}

	private static Predicate<Entity> inside(Subspace subspace) {
		return player -> subspace.contains(player.position());
	}

	private static void trigger(ServerLevel level, Predicate<Entity> credited, PlayerTrigger trigger) {
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && credited.test(player)) trigger.trigger(player);
		}
	}
}
