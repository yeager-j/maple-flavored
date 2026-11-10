package mapleflavored.access;

import com.mojang.datafixers.util.Either;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads which template a jigsaw pool element places. */
@Mixin(SinglePoolElement.class)
public interface SinglePoolElementAccessor {
	/** The template's location, or the template itself for an element built in code. */
	@Accessor("template")
	Either<Identifier, StructureTemplate> template();
}
