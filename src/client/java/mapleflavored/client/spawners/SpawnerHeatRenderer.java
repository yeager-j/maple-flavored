package mapleflavored.client.spawners;

import com.mojang.blaze3d.vertex.PoseStack;
import mapleflavored.MapleFlavored;
import mapleflavored.client.StagedBox;
import mapleflavored.spawners.ClearableSpawner;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a spawner as vanilla does and, until it is Cleared, glowing hot over its own texture. The glow cools back to
 * the plain spawner in patches as its mobs die, its eyes last, and is gone once Cleared.
 */
public final class SpawnerHeatRenderer implements BlockEntityRenderer<SpawnerBlockEntity, SpawnerHeatRenderer.State> {
	// The glow, painted over vanilla's spawner texture pixel for pixel. A cooled pixel is left clear.
	private static final Identifier HEAT = MapleFlavored.id("textures/entity/spawner_heat.png");
	// How far the glow sits off the block's own faces, outside and in, so the two never fight for depth.
	private static final float CLEARANCE = 0.004F;
	// The far faces' glow, seen through the bars, is duller so the near bars stand out against it.
	private static final int INSIDE_TINT = ARGB.color(153, 90, 77);

	private final SpawnerRenderer vanilla;

	private SpawnerHeatRenderer(BlockEntityRendererProvider.Context context) {
		this.vanilla = new SpawnerRenderer(context);
	}

	/** Replaces vanilla's spawner renderer. Call once from client init. */
	public static void register() {
		BlockEntityRenderers.register(BlockEntityTypes.MOB_SPAWNER, SpawnerHeatRenderer::new);
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
		state.remainingShare = ((ClearableSpawner) spawner).maple_flavored$view().remainingShare();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		vanilla.submit(state, poseStack, submitNodeCollector, camera);

		if (state.remainingShare <= 0.0F) return;

		RenderType heat = RenderTypes.entityCutoutCull(HEAT);
		int stage = StagedBox.stageAt(1.0F - state.remainingShare);

		StagedBox.submit(
			poseStack, submitNodeCollector, heat, stage, -CLEARANCE, 1.0F + CLEARANCE, StagedBox.Side.OUTSIDE, LightCoordsUtil.FULL_BRIGHT, StagedBox.UNTINTED
		);
		StagedBox.submit(
			poseStack, submitNodeCollector, heat, stage, CLEARANCE, 1.0F - CLEARANCE, StagedBox.Side.INSIDE, LightCoordsUtil.FULL_BRIGHT, INSIDE_TINT
		);
	}

	/** Vanilla's spawner render state, plus how much of the Clearing is still to go. */
	public static final class State extends SpawnerRenderState {
		float remainingShare;
	}
}
