package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mapleflavored.structurebooks.DungeonBooks;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.levelgen.feature.MonsterRoomFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Gives each dungeon's spawner the structure book that fights the mob the dungeon chose for it. The book drops once the
 * spawner is Cleared and broken.
 */
@Mixin(MonsterRoomFeature.class)
public abstract class MonsterRoomFeatureMixin {
	@WrapOperation(
		method = "place",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/entity/SpawnerBlockEntity;setEntityId(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/util/RandomSource;)V"
		)
	)
	private void holdDungeonBook(
		SpawnerBlockEntity spawner, EntityType<?> type, RandomSource random, Operation<Void> original, @Local(argsOnly = true) WorldGenLevel level
	) {
		original.call(spawner, type, random);
		DungeonBooks.give(level, spawner, type, random);
	}
}
