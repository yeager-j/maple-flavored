package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mapleflavored.structurebooks.FortressBlazeSpawners;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Plans every new Nether Fortress with at least one Blaze spawner, so its Fire Protection book always has a spawner to
 * Clear. A layout without a spawner room is planned again, drawing on from the same random, so a world seed always
 * gives the same fortress.
 */
@Mixin(NetherFortressStructure.class)
public abstract class NetherFortressStructureMixin {
	// About 1 in 2,000 vanilla layouts has no spawner room, so a second try almost always has one. The last is kept.
	@Unique
	private static final int ATTEMPTS = 16;

	@WrapMethod(method = "generatePieces")
	private static void planWithBlazeSpawner(StructurePiecesBuilder builder, Structure.GenerationContext context, Operation<Void> original) {
		for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
			if (attempt > 1) builder.clear();

			original.call(builder, context);

			if (FortressBlazeSpawners.hasRoom(builder.build().pieces())) return;
		}
	}
}
