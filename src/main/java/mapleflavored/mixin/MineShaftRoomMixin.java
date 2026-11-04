package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import mapleflavored.structurebooks.MineshaftRooms;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftPieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Always carves a Mineshaft's central room, which holds its structure book, sealing liquid around it first. Vanilla
 * skips the room where it touches liquid or a {@code mineshaft_blocking} biome, which would bury the book in rock.
 */
@Mixin(MineshaftPieces.MineShaftRoom.class)
public abstract class MineShaftRoomMixin {
	@ModifyExpressionValue(
		method = "postProcess",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/structure/structures/MineshaftPieces$MineShaftRoom;isInInvalidLocation(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;)Z"
		)
	)
	private boolean carveAndSeal(
		boolean invalid,
		@Local(argsOnly = true) WorldGenLevel level,
		@Local(argsOnly = true) BoundingBox chunkBB
	) {
		MineshaftRooms.sealLiquid(level, ((StructurePiece) (Object) this).getBoundingBox(), chunkBB);

		return false;
	}
}
