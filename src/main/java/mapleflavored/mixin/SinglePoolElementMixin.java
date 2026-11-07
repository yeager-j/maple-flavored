package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.datafixers.util.Either;
import mapleflavored.villages.AbandonedVillages;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Places every village piece without its hay bales and brewing stands. */
@Mixin(SinglePoolElement.class)
abstract class SinglePoolElementMixin {
	@Shadow
	@Final
	protected Either<Identifier, StructureTemplate> template;

	// LegacySinglePoolElement, which every vanilla village piece is, builds its settings on these.
	@ModifyReturnValue(method = "getSettings", at = @At("RETURN"))
	private StructurePlaceSettings stripAbandonedVillagePieces(StructurePlaceSettings settings) {
		if (template.left().filter(AbandonedVillages::isVillageTemplate).isPresent()) settings.addProcessor(AbandonedVillages.STRIPPED_BLOCKS);

		return settings;
	}
}
