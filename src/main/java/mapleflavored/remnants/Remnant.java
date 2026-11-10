package mapleflavored.remnants;

import java.util.UUID;
import mapleflavored.advancements.MapleTriggers;
import mapleflavored.experience.ExperiencePoints;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The XP a player dropped on death, which only they can see and collect by walking into it.
 *
 * <p>It can't be hurt, never despawns and stays saved with its chunk. Each death its owner has after the one that made
 * it cuts its points to 80%, rounded down, and it is removed once none are left.
 */
public final class Remnant extends Entity {
	private @Nullable UUID owner;
	private int startingPoints;
	// The owner's death count just after the death that made this Remnant, so that death doesn't cut it.
	private int ownerDeathsAtCreation;

	/** The constructor the entity type uses, for loading a saved Remnant. */
	public Remnant(EntityType<? extends Remnant> type, Level level) {
		super(type, level);
	}

	/**
	 * A new Remnant at {@code position}, not yet added to the level.
	 *
	 * @param ownerDeaths the owner's death count including the death that made it
	 */
	static Remnant create(ServerLevel level, Vec3 position, UUID owner, int points, int ownerDeaths) {
		Remnant remnant = new Remnant(Remnants.REMNANT, level);

		remnant.owner = owner;
		remnant.startingPoints = points;
		remnant.ownerDeathsAtCreation = ownerDeaths;
		remnant.setPos(position);

		return remnant;
	}

	/** The points left after {@code laterDeaths} deaths, each keeping 80% of the points, rounded down. */
	static int decayedPoints(int startingPoints, int laterDeaths) {
		int points = startingPoints;

		for (int death = 0; death < laterDeaths && points > 0; death++) {
			points = (int) (points * 4L / 5);
		}

		return points;
	}

	@Override
	public void tick() {
		super.tick();

		if (!(level() instanceof ServerLevel level)) return;

		// A Remnant made by a command has no owner and nobody could ever collect it.
		if (owner == null) {
			discard();

			return;
		}

		// Decay needs the owner's death count, which is known only while they are online. An offline owner's Remnant is
		// seen by no one, so it waits unchanged until they return.
		ServerPlayer onlineOwner = level.getServer().getPlayerList().getPlayer(owner);

		if (onlineOwner == null) return;

		int points = pointsFor(onlineOwner);

		if (points == 0) {
			discard();

			return;
		}

		if (isTouchedBy(onlineOwner)) collect(onlineOwner, points);
	}

	/** Only the owner's client is sent this entity, so nobody else sees it. */
	@Override
	public boolean broadcastToPlayer(ServerPlayer player) {
		return player.getUUID().equals(owner);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	public boolean ignoreExplosion(Explosion explosion) {
		return true;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE_ENTITY;
	}

	@Override
	public boolean canUsePortal(boolean ignorePassenger) {
		return false;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		startingPoints = input.getIntOr("starting_points", 0);
		ownerDeathsAtCreation = input.getIntOr("owner_deaths", 0);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.storeNullable("owner", UUIDUtil.CODEC, owner);
		output.putInt("starting_points", startingPoints);
		output.putInt("owner_deaths", ownerDeathsAtCreation);
	}

	private int pointsFor(ServerPlayer owner) {
		// Saves of player stats and of chunks can drift apart after a crash; never treat that as negative deaths.
		int laterDeaths = Math.max(0, Remnants.deaths(owner) - ownerDeathsAtCreation);

		return decayedPoints(startingPoints, laterDeaths);
	}

	private boolean isTouchedBy(ServerPlayer owner) {
		boolean canCollect = owner.level() == level() && owner.isAlive() && !owner.isSpectator();

		return canCollect && owner.getBoundingBox().intersects(getBoundingBox());
	}

	private void collect(ServerPlayer owner, int points) {
		ExperiencePoints.add(owner, points);
		MapleTriggers.REMNANT_COLLECTED.trigger(owner);
		discard();
	}
}
