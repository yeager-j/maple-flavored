package mapleflavored.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import mapleflavored.loot.SpawnedGear;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Gear a mob spawned with drops plain, both on death and when the mob swaps it for something it picked up. */
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
}
