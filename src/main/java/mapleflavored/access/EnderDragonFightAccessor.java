package mapleflavored.access;

import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Which players a dragon fight counts as taking part, which vanilla keeps private. */
@Mixin(EnderDragonFight.class)
public interface EnderDragonFightAccessor {
	/** Accepts the players the fight shows its boss bar to: alive and within 192 blocks of the arena's centre. */
	@Accessor("validPlayer")
	Predicate<Entity> maple$validPlayer();
}
