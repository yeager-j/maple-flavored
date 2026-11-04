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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wither Skeletons spawn holding a copper sword instead of a stone one, since stone tools are removed. Both swords
 * have the same attack damage, so the mob hits as hard as in vanilla.
 */
@Mixin(WitherSkeleton.class)
public abstract class WitherSkeletonMixin {
	@Inject(method = "populateDefaultEquipmentSlots", at = @At("TAIL"))
	private void holdCopperSword(RandomSource random, DifficultyInstance difficulty, CallbackInfo callback) {
		((WitherSkeleton) (Object) this).setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.COPPER_SWORD));
	}
}
