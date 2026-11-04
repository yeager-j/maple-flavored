package mapleflavored.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wither Skeletons spawn holding a Netherite sword that never drops, in place of vanilla's stone sword, since stone
 * tools are removed. Their base attack drops from 4 to 3, so with the sword's 7 they hit for 10 on Normal.
 */
@Mixin(WitherSkeleton.class)
public abstract class WitherSkeletonMixin {
	private static final double BASE_ATTACK_DAMAGE = 3.0;

	@Inject(method = "populateDefaultEquipmentSlots", at = @At("TAIL"))
	private void holdNetheriteSword(RandomSource random, DifficultyInstance difficulty, CallbackInfo callback) {
		WitherSkeleton skeleton = (WitherSkeleton) (Object) this;

		skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
		// A chance of exactly 0 skips the drop roll, so Looting can't raise it. Picking up gear resets the slot's chance.
		skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
	}

	@ModifyArg(
		method = "finalizeSpawn",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;setBaseValue(D)V")
	)
	private double lowerBaseAttackDamage(double vanillaBaseAttackDamage) {
		return BASE_ATTACK_DAMAGE;
	}
}
