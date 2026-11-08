package mapleflavored.deepvariants;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/**
 * A zombie that takes the place of natural adult zombies below Y 0. Its hits on a blocking player disable their shield
 * as an axe does, and its reinforcements are Deep Zombies, since vanilla calls reinforcements of the caller's own type.
 */
public class DeepZombie extends Zombie {
	// An axe's Weapon component disables blocking for 5 seconds, which a shield turns into 100 ticks of cooldown.
	private static final float AXE_SECONDS_TO_DISABLE_BLOCKING = 5.0F;

	/** Creates a Deep Zombie of {@code type}; used as the entity type's factory. */
	public DeepZombie(EntityType<? extends DeepZombie> type, Level level) {
		super(type, level);
	}

	@Override
	protected int getBaseExperienceReward(ServerLevel level) {
		return DeepVariants.XP_MULTIPLIER * super.getBaseExperienceReward(level);
	}

	@Override
	public float getSecondsToDisableBlocking() {
		return Math.max(super.getSecondsToDisableBlocking(), AXE_SECONDS_TO_DISABLE_BLOCKING);
	}
}
