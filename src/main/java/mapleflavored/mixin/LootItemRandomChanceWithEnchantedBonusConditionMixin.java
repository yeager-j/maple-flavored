package mapleflavored.mixin;

import mapleflavored.alloys.OrichalcumDrops;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Looting's chance bonus also gets Orichalcum's drop bonus, read from the killer's main hand. */
@Mixin(LootItemRandomChanceWithEnchantedBonusCondition.class)
public abstract class LootItemRandomChanceWithEnchantedBonusConditionMixin {
	@Shadow
	@Final
	private float unenchantedChance;

	@Shadow
	@Final
	private Holder<Enchantment> enchantment;

	@ModifyVariable(method = "test(Lnet/minecraft/world/level/storage/loot/LootContext;)Z", at = @At("STORE"), ordinal = 0)
	private float addOrichalcumBonus(float chance, LootContext context) {
		return OrichalcumDrops.withChanceBonus(chance, unenchantedChance, enchantment, OrichalcumDrops.killerWeapon(context));
	}
}
