package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mapleflavored.deepvariants.DeepVariants;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A Deep Spider's jockey is a Deep Skeleton, so a spider jockey that spawns below Y 0 is Deep top and bottom. */
@Mixin(Spider.class)
public abstract class SpiderMixin {
	@ModifyExpressionValue(
		method = "finalizeSpawn",
		at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/EntityTypes;SKELETON:Lnet/minecraft/world/entity/EntityType;")
	)
	@SuppressWarnings("unchecked")
	private EntityType<Skeleton> deepJockeyRider(EntityType<Skeleton> skeleton) {
		// The rider's type is only ever used to create a Skeleton, which a Deep Skeleton is.
		return (EntityType<Skeleton>) DeepVariants.jockeyRider((Spider) (Object) this);
	}
}
