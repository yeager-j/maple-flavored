package mapleflavored.client.spawners;

import com.mojang.blaze3d.vertex.PoseStack;
import mapleflavored.client.Forcefield;
import mapleflavored.spawners.ClearableSpawner;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a spawner as vanilla does, wrapped in the world border's forcefield until it is Cleared. The forcefield starts
 * half a block out and closes in on the spawner as its mobs die, and is gone once Cleared.
 */
public final class SpawnerForcefieldRenderer implements BlockEntityRenderer<SpawnerBlockEntity, SpawnerForcefieldRenderer.State> {
	// Tuning values for the hand check.
	private static final float FRESH_OUTSET = 0.5F;
	private static final float CLOSING_OUTSET = 0.02F;

	private final SpawnerRenderer vanilla;

	private SpawnerForcefieldRenderer(BlockEntityRendererProvider.Context context) {
		this.vanilla = new SpawnerRenderer(context);
	}

	/** Replaces vanilla's spawner renderer. Call once from client init. */
	public static void register() {
		BlockEntityRenderers.register(BlockEntityTypes.MOB_SPAWNER, SpawnerForcefieldRenderer::new);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(
		SpawnerBlockEntity spawner,
		State state,
		float partialTicks,
		Vec3 cameraPosition,
		ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
	) {
		vanilla.extractRenderState(spawner, state, partialTicks, cameraPosition, breakProgress);
		state.remainingShare = ((ClearableSpawner) spawner).maple_flavored$clearing().remainingShare();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		vanilla.submit(state, poseStack, submitNodeCollector, camera);

		if (state.remainingShare <= 0.0F) return;

		float outset = Mth.lerp(state.remainingShare, CLOSING_OUTSET, FRESH_OUTSET);

		Forcefield.submit(poseStack, submitNodeCollector, -outset, 1.0F + outset);
	}

	/** Vanilla's spawner render state, plus how much of the Clearing is still to go. */
	public static final class State extends SpawnerRenderState {
		float remainingShare;
	}
}
