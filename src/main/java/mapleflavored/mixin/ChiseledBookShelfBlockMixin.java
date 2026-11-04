package mapleflavored.mixin;

import mapleflavored.advancements.MapleTriggers;
import mapleflavored.enchanting.Library;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A player who shelves an enchanted book in a Library earns the advancement for it. */
@Mixin(ChiseledBookShelfBlock.class)
abstract class ChiseledBookShelfBlockMixin {
	@Inject(method = "addBook", at = @At("HEAD"))
	private static void creditShelving(
		Level level, BlockPos pos, Player player, ChiseledBookShelfBlockEntity shelf, ItemStack book, int slot, CallbackInfo callback
	) {
		if (!(player instanceof ServerPlayer serverPlayer) || !book.is(Items.ENCHANTED_BOOK)) return;
		if (!Library.holdsShelf(level, pos)) return;

		MapleTriggers.ENCHANTED_BOOK_SHELVED.trigger(serverPlayer);
	}
}
