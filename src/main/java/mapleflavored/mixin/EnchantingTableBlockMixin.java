package mapleflavored.mixin;

import mapleflavored.enchanting.EnchantingTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The Enchanting Table opens the Library checklist instead of vanilla's random offers, keeping the table's name. */
@Mixin(EnchantingTableBlock.class)
abstract class EnchantingTableBlockMixin {
	@Inject(method = "getMenuProvider", at = @At("HEAD"), cancellable = true)
	private void openChecklist(BlockState state, Level level, BlockPos pos, CallbackInfoReturnable<MenuProvider> callback) {
		if (!(level.getBlockEntity(pos) instanceof EnchantingTableBlockEntity table)) return;

		callback.setReturnValue(new SimpleMenuProvider(
			(containerId, inventory, player) -> new EnchantingTableMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)),
			table.getDisplayName()
		));
	}
}
