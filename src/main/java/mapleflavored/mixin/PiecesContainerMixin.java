package mapleflavored.mixin;

import java.util.List;
import mapleflavored.structurebooks.StructureBookPiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Links each structure book to the piece it follows whenever a structure's pieces are gathered, both as they're
 * planned and as a saved structure loads. Pieces are saved one by one, so the link can't be saved with the book.
 */
@Mixin(PiecesContainer.class)
public abstract class PiecesContainerMixin {
	@Shadow
	public abstract List<StructurePiece> pieces();

	@Inject(method = "<init>", at = @At("TAIL"))
	private void linkStructureBooks(List<StructurePiece> pieces, CallbackInfo ci) {
		StructureBookPiece.linkAnchors(pieces());
	}
}
