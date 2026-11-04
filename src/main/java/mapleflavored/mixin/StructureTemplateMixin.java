package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import mapleflavored.structurebooks.StructureBooks;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Keeps a placed template from replacing a structure book, so a template structure placed later, such as Trial
 * Chambers, can't erase another structure's book.
 */
@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin {
	@ModifyExpressionValue(
		method = "placeInWorld",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;processBlockInfos(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Ljava/util/List;)Ljava/util/List;"
		)
	)
	private List<StructureTemplate.StructureBlockInfo> skipStructureBooks(
		List<StructureTemplate.StructureBlockInfo> blocks,
		@Local(argsOnly = true) ServerLevelAccessor level
	) {
		return blocks.stream().filter(block -> !level.getBlockState(block.pos()).is(StructureBooks.BLOCK)).toList();
	}
}
