package mapleflavored.access;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Sets a creeper's explosion power, so a Deep Creeper can explode as hard as TNT. */
@Mixin(Creeper.class)
public interface CreeperAccessor {
	/** Sets the explosion power before a charged creeper's doubling. */
	@Accessor("explosionRadius")
	void maple$setExplosionRadius(int radius);
}
