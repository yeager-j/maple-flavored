package mapleflavored.mixin;

import mapleflavored.strongholds.StrongholdLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Opens a five-way crossing's side doorways where its side rooms really are. See {@link StrongholdLayout}. */
@Mixin(StrongholdPieces.FiveCrossing.class)
public abstract class FiveCrossingMixin {
	@Shadow
	@Final
	private boolean leftLow;

	@Shadow
	@Final
	private boolean leftHigh;

	@Shadow
	@Final
	private boolean rightLow;

	@Shadow
	@Final
	private boolean rightHigh;

	@Inject(method = "postProcess", at = @At("TAIL"))
	private void realignSideDoorways(
		WorldGenLevel level,
		StructureManager structureManager,
		ChunkGenerator generator,
		RandomSource random,
		BoundingBox chunkBB,
		ChunkPos chunkPos,
		BlockPos referencePos,
		CallbackInfo info
	) {
		StrongholdLayout.realignFiveCrossingDoorways((StructurePiece) (Object) this, level, chunkBB, leftLow, leftHigh, rightLow, rightHigh);
	}
}
