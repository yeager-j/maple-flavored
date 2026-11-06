package mapleflavored.mixin;

import mapleflavored.subspaces.WitherSubspaces;
import mapleflavored.wither.WitherPassage;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Keeps every move the Wither makes within its leash, whatever drives it: its chase, its flight, knockback or a piston,
 * and notices it passing through bedrock or obsidian. Vanilla's Wither doesn't override {@code move}, so this adds the
 * override.
 */
@Mixin(WitherBoss.class)
public abstract class WitherBossMixin extends Monster {
	private WitherBossMixin(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	@Override
	public void move(MoverType type, Vec3 delta) {
		WitherBoss wither = (WitherBoss) (Object) this;
		AABB before = getBoundingBox();

		WitherSubspaces.moveLeashed(wither, type, delta, leashed -> super.move(type, leashed));
		WitherPassage.afterMove(wither, before);
	}
}
