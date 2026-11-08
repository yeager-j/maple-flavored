package mapleflavored.deepvariants;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;

/** A spider that takes the place of natural spiders below Y 0. Its bites blind, and its jockey is a Deep Skeleton. */
public class DeepSpider extends Spider {
	private static final int BLINDNESS_TICKS = 60;

	/** Creates a Deep Spider of {@code type}; used as the entity type's factory. */
	public DeepSpider(EntityType<? extends DeepSpider> type, Level level) {
		super(type, level);
	}

	@Override
	protected int getBaseExperienceReward(ServerLevel level) {
		return DeepVariants.XP_MULTIPLIER * super.getBaseExperienceReward(level);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		if (!super.doHurtTarget(level, target)) return false;

		if (target instanceof LivingEntity bitten) {
			bitten.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_TICKS), this);
		}

		return true;
	}
}
