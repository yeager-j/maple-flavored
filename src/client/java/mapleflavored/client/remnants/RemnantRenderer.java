package mapleflavored.client.remnants;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mapleflavored.remnants.Remnant;
import mapleflavored.remnants.Remnants;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Draws a Remnant as a large, softly pulsing XP orb that glows at full brightness.
 *
 * <p>The server sends a Remnant only to its owner, so this is only ever drawn on its owner's client.
 */
public final class RemnantRenderer extends EntityRenderer<Remnant, EntityRenderState> {
	// Vanilla's own texture, used in place rather than copied into the mod.
	private static final Identifier EXPERIENCE_ORB_TEXTURE = Identifier.withDefaultNamespace("textures/entity/experience/experience_orb.png");
	private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(EXPERIENCE_ORB_TEXTURE);
	// The largest of the orb sheet's 16 icons, in its 4 × 4 grid.
	private static final int ICON = 10;
	private static final float SCALE = 0.6F;
	private static final int FULL_LIGHT = 15;

	private RemnantRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	/** Registers the renderer for the Remnant entity type. Call once from client init. */
	public static void register() {
		EntityRenderers.register(Remnants.REMNANT, RemnantRenderer::new);
	}

	@Override
	protected int getBlockLightLevel(Remnant entity, BlockPos blockPos) {
		return FULL_LIGHT;
	}

	@Override
	public EntityRenderState createRenderState() {
		return new EntityRenderState();
	}

	@Override
	public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		float u0 = ICON % 4 * 16 / 64.0F;
		float u1 = (ICON % 4 * 16 + 16) / 64.0F;
		float v0 = ICON / 4 * 16 / 64.0F;
		float v1 = (ICON / 4 * 16 + 16) / 64.0F;
		float pulse = (Mth.sin(state.ageInTicks / 10.0F) + 1.0F) * 0.5F;
		float scale = SCALE * (0.9F + 0.1F * pulse);
		int alpha = (int) (160 + 95 * pulse);

		poseStack.pushPose();
		poseStack.translate(0.0F, 0.25F, 0.0F);
		poseStack.rotate(camera.orientation);
		poseStack.scale(scale, scale, scale);
		submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
			vertex(buffer, pose, -0.5F, -0.5F, u0, v1, alpha, state.lightCoords);
			vertex(buffer, pose, 0.5F, -0.5F, u1, v1, alpha, state.lightCoords);
			vertex(buffer, pose, 0.5F, 0.5F, u1, v0, alpha, state.lightCoords);
			vertex(buffer, pose, -0.5F, 0.5F, u0, v0, alpha, state.lightCoords);
		});
		poseStack.popPose();

		super.submit(state, poseStack, submitNodeCollector, camera);
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float u, float v, int alpha, int lightCoords) {
		buffer.addVertex(pose, x, y, 0.0F)
			.setColor(160, 255, 90, alpha)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(lightCoords)
			.setNormal(pose, 0.0F, 1.0F, 0.0F);
	}
}
