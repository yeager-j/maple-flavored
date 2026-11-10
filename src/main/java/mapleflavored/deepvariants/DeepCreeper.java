package mapleflavored.deepvariants;

import mapleflavored.access.CreeperAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;

/**
 * A creeper that takes the place of natural creepers below Y 0. It explodes as hard as TNT, twice that when charged, with
 * a creeper's fuse and health.
 */
public class DeepCreeper extends Creeper {
	// TNT's explosion power. No game rule scales it.
	private static final int EXPLOSION_POWER = 4;
	private static final String EXPLOSION_POWER_TAG = "ExplosionRadius";

	/** Creates a Deep Creeper of {@code type}; used as the entity type's factory. */
	public DeepCreeper(EntityType<? extends DeepCreeper> type, Level level) {
		super(type, level);
		((CreeperAccessor) this).maple$setExplosionRadius(EXPLOSION_POWER);
	}

	@Override
	protected int getBaseExperienceReward(ServerLevel level) {
		return DeepVariants.XP_MULTIPLIER * super.getBaseExperienceReward(level);
	}

	// A creeper reads a missing explosion power as its own 3, which a summon without data would give a Deep Creeper.
	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		((CreeperAccessor) this).maple$setExplosionRadius(input.getByteOr(EXPLOSION_POWER_TAG, (byte) EXPLOSION_POWER));
	}
}
