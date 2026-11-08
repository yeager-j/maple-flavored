package mapleflavored.deepvariants;

import mapleflavored.mixin.AbstractArrowAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A skeleton that takes the place of natural skeletons below Y 0. It spots players from twice as far, and its arrows hit
 * harder and carry a stray's Slowness.
 */
public class DeepSkeleton extends Skeleton {
	private static final double FOLLOW_RANGE = 32.0;
	// The effect a stray adds to its arrows.
	private static final int STRAY_SLOWNESS_TICKS = 600;

	/** Creates a Deep Skeleton of {@code type}; used as the entity type's factory. */
	public DeepSkeleton(EntityType<? extends DeepSkeleton> type, Level level) {
		super(type, level);
	}

	/** A skeleton's attributes with the Deep Skeleton's follow range. */
	public static AttributeSupplier.Builder createAttributes() {
		return AbstractSkeleton.createAttributes().add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
	}

	@Override
	protected int getBaseExperienceReward(ServerLevel level) {
		return DeepVariants.XP_MULTIPLIER * super.getBaseExperienceReward(level);
	}

	@Override
	protected AbstractArrow getArrow(ItemStack projectile, float power, @Nullable ItemStack firingWeapon) {
		AbstractArrow arrow = super.getArrow(projectile, power, firingWeapon);
		double vanillaDamage = ((AbstractArrowAccessor) arrow).maple$getBaseDamage();

		arrow.setBaseDamage(vanillaDamage * DeepVariants.damageMultiplier(this));

		if (arrow instanceof Arrow plainArrow) {
			plainArrow.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, STRAY_SLOWNESS_TICKS));
		}

		return arrow;
	}
}
