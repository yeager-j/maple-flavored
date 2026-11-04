package mapleflavored.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps a chest's items while its loot is still unrolled. Vanilla saves only the loot table then, which would lose a
 * structure book put in a Buried Treasure chest at generation. The loot is still rolled when the chest is first opened,
 * into the empty slots.
 */
@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin extends RandomizableContainerBlockEntity {
	@Shadow
	private NonNullList<ItemStack> items;

	private ChestBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Inject(method = "saveAdditional", at = @At("TAIL"))
	private void saveItemsBesideLoot(ValueOutput output, CallbackInfo ci) {
		if (getLootTable() != null) ContainerHelper.saveAllItems(output, items, false);
	}

	@Inject(method = "loadAdditional", at = @At("TAIL"))
	private void loadItemsBesideLoot(ValueInput input, CallbackInfo ci) {
		if (getLootTable() != null) ContainerHelper.loadAllItems(input, items);
	}
}
