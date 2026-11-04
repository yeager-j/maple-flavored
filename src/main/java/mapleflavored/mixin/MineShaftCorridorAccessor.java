package mapleflavored.mixin;

import net.minecraft.world.level.levelgen.structure.structures.MineshaftPieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads whether a Mineshaft corridor is a cobwebbed spider corridor. */
@Mixin(MineshaftPieces.MineShaftCorridor.class)
public interface MineShaftCorridorAccessor {
	/** Whether the corridor is full of cobwebs and holds a cave spider spawner. */
	@Accessor("spiderCorridor")
	boolean isSpiderCorridor();
}
