package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.List;
import mapleflavored.structurebooks.MansionPortalRoom;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Plans every new Woodland Mansion with its fake End portal room, where its Fortune book floats. A layout without a
 * first-floor secret room to hold it is planned again, drawing on from the same random, so a world seed always gives the
 * same mansion.
 */
@Mixin(WoodlandMansionPieces.class)
public abstract class WoodlandMansionPiecesMixin {
	// About 1 in 8 vanilla layouts has the secret room, so 256 tries leave about 1 in 10^15 mansions without one. The
	// last is kept.
	@Unique
	private static final int ATTEMPTS = 256;

	@WrapMethod(method = "generateMansion")
	private static void planWithPortalRoom(
		StructureTemplateManager templates,
		BlockPos origin,
		Rotation rotation,
		List<WoodlandMansionPieces.WoodlandMansionPiece> pieces,
		RandomSource random,
		Operation<Void> original
	) {
		for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
			if (attempt > 1) pieces.clear();

			original.call(templates, origin, rotation, pieces, random);

			if (MansionPortalRoom.make(pieces, templates)) return;
		}
	}
}
