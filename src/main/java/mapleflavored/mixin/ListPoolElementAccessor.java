package mapleflavored.mixin;

import java.util.List;
import net.minecraft.world.level.levelgen.structure.pools.ListPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the elements a jigsaw list element places together, such as a Pillager Outpost's watchtower and its overgrowth. */
@Mixin(ListPoolElement.class)
public interface ListPoolElementAccessor {
	/** The elements, placed in order at the same spot. */
	@Accessor("elements")
	List<StructurePoolElement> elements();
}
