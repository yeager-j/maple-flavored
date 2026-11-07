package mapleflavored.strongholds;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import mapleflavored.structurebooks.IllagerAlert;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Walks an Illager hunting the player room by room through a Stronghold, while it has no path to them. Vanilla's path
 * search reaches only so far, and without a full path an attacker heads for the nearest point in a straight line, which in
 * a Stronghold is often a dead end in the room beneath the player. This goal heads instead for the next doorway on
 * {@link StrongholdRoutes#route the fewest doorways} to the player's room, and hands back to the Illager's own attack once
 * it can reach them. A crossbowman that can see the player shoots instead.
 */
public final class StrongholdRouteGoal extends Goal {
	// How often the hunter checks for a path to its quarry, and its next doorway, in ticks, on average. Each wait varies
	// by up to half that either way, so a whole garrison alerted on one tick doesn't search on the same ticks ever after.
	private static final int CHECK_INTERVAL = 20;
	private static final double SPEED = 1.0;

	private final AbstractIllager illager;
	private long nextCheck;
	private Optional<BlockPos> waypoint = Optional.empty();

	/** The goal for an Illager that closes in to attack. Add it ahead of the Illager's attack goals. */
	public StrongholdRouteGoal(AbstractIllager illager) {
		this.illager = illager;
		setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (illager.level().getGameTime() < nextCheck) return false;

		nextCheck = nextCheckAfter(illager.level().getGameTime());
		waypoint = nextWaypoint();

		return waypoint.isPresent();
	}

	@Override
	public void start() {
		waypoint.ifPresent(this::walkTo);
	}

	@Override
	public boolean canContinueToUse() {
		if (illager.getNavigation().isDone()) return false;

		if (illager.level().getGameTime() < nextCheck) return true;

		nextCheck = nextCheckAfter(illager.level().getGameTime());

		Optional<BlockPos> next = nextWaypoint();

		if (next.isEmpty()) return false;

		// Always, even to the same doorway: looking for a path to the quarry left the navigation aimed at the quarry, and
		// the next look would take the path to the doorway for one to the quarry.
		walkTo(next.get());
		waypoint = next;

		return true;
	}

	@Override
	public void stop() {
		illager.getNavigation().stop();
		waypoint = Optional.empty();
		// At a doorway, look for the next one straight away, before the attack goal sets off the wrong way.
		nextCheck = 0L;
	}

	private long nextCheckAfter(long now) {
		return now + CHECK_INTERVAL / 2 + illager.getRandom().nextInt(CHECK_INTERVAL + 1);
	}

	private void walkTo(BlockPos spot) {
		illager.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, SPEED);
	}

	// The doorway to head for, while the Illager hunts a player it has no path to.
	private Optional<BlockPos> nextWaypoint() {
		LivingEntity target = illager.getTarget();

		if (!(target instanceof Player quarry) || !IllagerAlert.isHunting(illager)) return Optional.empty();

		if (illager instanceof CrossbowAttackMob && illager.getSensing().hasLineOfSight(quarry)) return Optional.empty();

		Path direct = illager.getNavigation().createPath(quarry, 0);

		if (direct != null && direct.canReach()) return Optional.empty();

		List<BlockPos> route = StrongholdRoutes.route((ServerLevel) illager.level(), illager.blockPosition(), quarry.blockPosition());

		return route.stream().findFirst();
	}
}
