package mapleflavored.gametest.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reads the movement slowdowns blocks put on an entity, which vanilla keeps protected. */
@Mixin(Entity.class)
public interface EntityAccessor {
	/** The speed multiplier a block such as a cobweb set this tick, or zero when none did. */
	@Accessor("stuckSpeedMultiplier")
	Vec3 maple$stuckSpeedMultiplier();

	/** The speed factor of the block the entity stands on, such as soul sand's 0.4. */
	@Invoker("getBlockSpeedFactor")
	float maple$blockSpeedFactor();
}
