package mapleflavored.subspaces;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import mapleflavored.MapleFlavored;
import mapleflavored.gamerules.MapleGameRules;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SculkSpreader;

/**
 * The sculk a Warden's Subspace spreads: one weak patch where the Warden appeared, spread by vanilla's sculk rules as if
 * a mob had died there next to a catalyst. It spreads only while its Warden is loaded and its Subspace lasts, and stays
 * within a few blocks of the Warden's spawn point. Game rules set its size and strength.
 */
public final class WardenSculk {
	/**
	 * Each Subspace's patch that is still spreading, by its id. Lost on a reload, which leaves that patch as far as it
	 * spread.
	 */
	private static final AttachmentType<Map<UUID, SubspaceSculkSpreader>> PATCHES = AttachmentRegistry.create(
		MapleFlavored.id("sculk_patches"),
		builder -> builder.initializer(HashMap::new)
	);

	// Further than anything a cursor writes reaches past it: veins wrap around a converted block, up to √5 blocks out.
	private static final double WRITE_REACH = 3.0;

	private WardenSculk() {
	}

	static void register() {
		Subspaces.STARTED.register(WardenSculk::seed);
		ServerTickEvents.END_LEVEL_TICK.register(WardenSculk::spreadAll);
	}

	/**
	 * Whether a sculk cursor may move to the target. A Subspace's cursors stay within its patch; vanilla's own cursors are
	 * left to {@code vanilla}, vanilla's rule.
	 */
	public static boolean allowsCursorMove(LevelAccessor level, SculkSpreader spreader, BlockPos target, BooleanSupplier vanilla) {
		if (!(spreader instanceof SubspaceSculkSpreader patch)) return vanilla.getAsBoolean();

		return vanilla.getAsBoolean() && patch.canSpreadTo(level, target);
	}

	// Where a catalyst would start spreading from a mob dying where the Warden stands.
	private static void seed(ServerLevel level, Subspace subspace) {
		int charge = level.getGameRules().get(MapleGameRules.WARDEN_SCULK_PATCH_CHARGE);

		if (subspace.kind() != Subspace.Kind.WARDEN || charge == 0) return;

		// The patch keeps inside its Subspace, however the two radii are set, so its shriekers stay where the Subspace's
		// cleanup looks for them.
		int patchRadius = level.getGameRules().get(MapleGameRules.WARDEN_SCULK_PATCH_RADIUS);
		double radius = Math.min(patchRadius, subspace.radius() - WRITE_REACH);

		if (radius < 0.0) return;

		BlockPos feet = BlockPos.containing(subspace.centre());

		level.getAttachedOrCreate(PATCHES).put(subspace.id(), new SubspaceSculkSpreader(subspace, feet, radius, charge));
	}

	private static void spreadAll(ServerLevel level) {
		Map<UUID, SubspaceSculkSpreader> patches = level.getAttached(PATCHES);

		if (patches == null || patches.isEmpty()) return;

		Set<UUID> open = Subspaces.inLevel(level).stream().map(Subspace::id).collect(Collectors.toSet());

		patches.keySet().retainAll(open);
		patches.values().removeIf(patch -> isWardenHere(level, patch.subspace()) && !patch.spread(level));
	}

	// The spread needs its Warden, so it pauses while the Warden is unloaded or has carried its UUID to another level.
	private static boolean isWardenHere(ServerLevel level, Subspace subspace) {
		return level.getEntity(subspace.boss()) instanceof Warden warden && warden.isAlive();
	}
}
