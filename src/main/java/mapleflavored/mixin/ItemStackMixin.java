package mapleflavored.mixin;

import java.util.function.BiConsumer;
import mapleflavored.reinforcement.Reinforcement;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.apache.commons.lang3.function.TriConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A Reinforced piece adds its toughness wherever a stack lists its attribute modifiers: when worn, through the slot
 * overload, and in its tooltip, through the group overload.
 */
@Mixin(ItemStack.class)
abstract class ItemStackMixin {
	@Inject(
		method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
		at = @At("TAIL")
	)
	private void addReinforcedToughness(EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> consumer, CallbackInfo callback) {
		Reinforcement.forEachModifier((ItemStack) (Object) this, slot, consumer);
	}

	@Inject(
		method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V",
		at = @At("TAIL")
	)
	private void showReinforcedToughness(
		EquipmentSlotGroup group,
		TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> consumer,
		CallbackInfo callback
	) {
		Reinforcement.forEachModifier((ItemStack) (Object) this, group, consumer);
	}
}
