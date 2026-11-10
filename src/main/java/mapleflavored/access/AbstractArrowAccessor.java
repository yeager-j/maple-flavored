package mapleflavored.access;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads an arrow's base damage, which vanilla only lets callers set, so a Deep Skeleton can scale its arrows. */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
	/** The damage the arrow deals per block per tick of speed, before enchantments. */
	@Accessor("baseDamage")
	double maple$getBaseDamage();
}
