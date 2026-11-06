package mapleflavored.bosses;

import java.util.function.Predicate;
import mapleflavored.advancements.MapleTriggers;
import mapleflavored.gamerules.MapleGameRules;
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
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.dimension.end.EnderDragonFight;

/**
 * Shared credit for the boss advancements that don't come from a Subspace. An Elder Guardian's death credits every player
 * within 64 blocks of it. A Warden dying or digging back down credits every player within the Warden survival range game
 * rule of it. An Ender Dragon's death credits every player its fight counts as taking part. Spectators are never credited.
 */
public final class BossKillCredit {
	/** How far from a dying Elder Guardian a player is credited with its kill, in blocks. */
	public static final double ELDER_GUARDIAN_CREDIT_RANGE = 64.0;

	private BossKillCredit() {
	}

	/** Registers the death hook. Call once from mod init. */
	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register(BossKillCredit::creditDeath);
	}

	/** Credits every player near the Warden with surviving it. Call as the Warden digs back down, before it's removed. */
	public static void creditWardenSurvivors(Warden warden) {
		if (!(warden.level() instanceof ServerLevel level)) return;

		double range = level.getGameRules().get(MapleGameRules.WARDEN_SURVIVAL_RANGE);

		trigger(level, within(warden, range), MapleTriggers.WARDEN_SURVIVED);
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

	private static void trigger(ServerLevel level, Predicate<Entity> credited, PlayerTrigger trigger) {
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && credited.test(player)) trigger.trigger(player);
		}
	}
}
