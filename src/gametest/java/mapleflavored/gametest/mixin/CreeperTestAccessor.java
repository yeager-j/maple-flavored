package mapleflavored.gametest.mixin;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reads a creeper's explosion power and sets it off at once, without waiting out its fuse. */
@Mixin(Creeper.class)
public interface CreeperTestAccessor {
	/** The explosion power before a charged creeper's doubling. */
	@Accessor("explosionRadius")
	int maple$getExplosionRadius();

	/** Explodes the creeper now, as its fuse running out would. */
	@Invoker("explodeCreeper")
	void maple$explode();
}
