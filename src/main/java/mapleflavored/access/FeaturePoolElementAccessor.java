package mapleflavored.access;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.pools.FeaturePoolElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads which feature a jigsaw pool element places, as an Ancient City's sculk patches are placed. */
@Mixin(FeaturePoolElement.class)
public interface FeaturePoolElementAccessor {
	/** The feature placed at the element's position. */
	@Accessor("feature")
	Holder<PlacedFeature> feature();
}
