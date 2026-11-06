package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import mapleflavored.alloys.IllagerNeutrality;
import mapleflavored.loot.SpawnedGear;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Gear a mob spawned with drops plain, both on death and when the mob swaps it for something it picked up. Illagers and
 * their Vexes leave Orichalcum wearers alone: vanilla asks this both when a mob picks a target and whenever it reads or
 * keeps one, so a neutral mob neither picks the player nor keeps chasing them.
 */
@Mixin(Mob.class)
public abstract class MobMixin {
	@ModifyArg(
		method = {"dropCustomDeathLoot", "equipItemIfPossible"},
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Mob;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"
		),
		index = 1,
		require = 2
	)
	private ItemStack dropSpawnedGearPlain(ItemStack gear, @Local EquipmentSlot slot) {
		return SpawnedGear.asDropped((Mob) (Object) this, slot, gear);
	}

	@ModifyReturnValue(method = "canAttack", at = @At("RETURN"))
	private boolean leaveOrichalcumWearersAlone(boolean canAttack, LivingEntity target) {
		return canAttack && !IllagerNeutrality.isNeutralTo((Mob) (Object) this, target);
	}
}
