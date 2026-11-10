package mapleflavored.access;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The fight a dragon belongs to, which vanilla keeps private. */
@Mixin(EnderDragon.class)
public interface EnderDragonAccessor {
	/** The dragon's fight, or null for a dragon outside one, such as one summoned in the Overworld. */
	@Accessor("dragonFight")
	@Nullable EnderDragonFight maple$dragonFight();
}
