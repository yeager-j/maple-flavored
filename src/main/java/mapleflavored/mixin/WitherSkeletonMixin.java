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
 * Wither Skeletons spawn holding a Netherite sword that never drops, in place of vanilla's stone sword, since stone
 * tools are removed. Their lower base attack is a row in {@code CommonMobs}.
 */
@Mixin(WitherSkeleton.class)
public abstract class WitherSkeletonMixin {
	@Inject(method = "populateDefaultEquipmentSlots", at = @At("TAIL"))
	private void holdNetheriteSword(RandomSource random, DifficultyInstance difficulty, CallbackInfo callback) {
		WitherSkeleton skeleton = (WitherSkeleton) (Object) this;

		skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
		// A chance of exactly 0 skips the drop roll, so Looting can't raise it. Picking up gear resets the slot's chance.
		skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
	}
}
