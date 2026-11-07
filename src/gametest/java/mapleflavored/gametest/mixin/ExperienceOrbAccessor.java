package mapleflavored.gametest.mixin;

import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** How many orbs of its value an experience orb stands for, which vanilla keeps private. */
@Mixin(ExperienceOrb.class)
public interface ExperienceOrbAccessor {
	/** One for a lone orb, more once others of the same value have merged into it. */
	@Accessor("count")
	int maple$count();
}
