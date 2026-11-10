package mapleflavored.access;

import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Vanilla's private warning steps, which Warden summoning runs for the players in Warden Territory. */
@Mixin(WardenSpawnTracker.class)
public interface WardenSpawnTrackerAccessor {
	/** Whether a recent warning still blocks the next. */
	@Invoker("onCooldown")
	boolean maple$onCooldown();

	/** Raises the warning level by one and starts the cooldown, unless it's already running. */
	@Invoker("increaseWarningLevel")
	void maple$increaseWarningLevel();

	/** Takes the other tracker's warning level, cooldown and time since the last warning. */
	@Invoker("copyData")
	void maple$copyData(WardenSpawnTracker from);

	/** Sets how many ticks remain before the next warning can count. */
	@Accessor("cooldownTicks")
	void maple$setCooldownTicks(int ticks);
}
