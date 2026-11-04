package mapleflavored.mixin;

import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Turns a code-built piece's local coordinates into world coordinates, by the piece's orientation and box. */
@Mixin(StructurePiece.class)
public interface StructurePieceInvoker {
	/** The world X of the piece-local x and z. */
	@Invoker("getWorldX")
	int worldX(int x, int z);

	/** The world Y of the piece-local y. */
	@Invoker("getWorldY")
	int worldY(int y);

	/** The world Z of the piece-local x and z. */
	@Invoker("getWorldZ")
	int worldZ(int x, int z);
}
