package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A zombie villager under Weakness is cured with an enchanted golden apple, whatever its effects, instead of a plain
 * golden apple, so the first villager waits for the Enchanting Table.
 */
@Mixin(ZombieVillager.class)
abstract class ZombieVillagerMixin {
	@ModifyExpressionValue(
		method = "mobInteract",
		at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/Items;GOLDEN_APPLE:Lnet/minecraft/world/item/Item;")
	)
	private Item cureWithEnchantedGoldenApple(Item cure) {
		return Items.ENCHANTED_GOLDEN_APPLE;
	}
}
