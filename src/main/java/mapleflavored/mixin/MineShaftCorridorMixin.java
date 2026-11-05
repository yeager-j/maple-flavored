package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mapleflavored.structurebooks.SpawnerBooks;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftPieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Gives each Mineshaft cave spider spawner a Bane of Arthropods book, which drops once the spawner is Cleared and
 * broken. This is on top of the Mineshaft's own Efficiency structure book.
 */
@Mixin(MineshaftPieces.MineShaftCorridor.class)
public abstract class MineShaftCorridorMixin {
	@WrapOperation(
		method = "postProcess",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/entity/SpawnerBlockEntity;setEntityId(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/util/RandomSource;)V"
		)
	)
	private void holdSpawnerBook(
		SpawnerBlockEntity spawner, EntityType<?> type, RandomSource random, Operation<Void> original, @Local(argsOnly = true) WorldGenLevel level
	) {
		original.call(spawner, type, random);
		SpawnerBooks.give(level, spawner, type, random);
	}
}
