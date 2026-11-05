package mapleflavored.client.spawners;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mapleflavored.spawners.ClearableSpawner;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.WorldBorderRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.border.BorderStatus;
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
	private static final int COLOR = ARGB.opaque(BorderStatus.STATIONARY.getColor());

	// As the world border draws it: the texture repeats every 2 blocks and scrolls once every 3 seconds.
	private static final float BLOCKS_PER_TEXTURE = 2.0F;
	private static final long SCROLL_MILLIS = 3000L;
	private static final int FULL_BRIGHT = 0xF000F0;

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
		float scroll = (float) (Util.getMillis() % SCROLL_MILLIS) / SCROLL_MILLIS;

		submitNodeCollector.submitCustomGeometry(
			poseStack,
			RenderTypes.energySwirl(WorldBorderRenderer.FORCEFIELD_LOCATION, scroll, scroll),
			(pose, buffer) -> addForcefield(pose, buffer, -outset, 1.0F + outset)
		);
	}

	// Every face of the box from min to max, drawn both ways so the far side shows through the near one.
	private static void addForcefield(PoseStack.Pose pose, VertexConsumer buffer, float min, float max) {
		for (Direction face : Direction.values()) {
			Vec3[] corners = faceCorners(face, min, max);

			for (int i = 0; i < corners.length; i++) {
				addVertex(pose, buffer, corners[i], face, face);
			}

			for (int i = corners.length - 1; i >= 0; i--) {
				addVertex(pose, buffer, corners[i], face, face.getOpposite());
			}
		}
	}

	// The face's corners, counter-clockwise seen from outside the box.
	private static Vec3[] faceCorners(Direction face, float min, float max) {
		return switch (face) {
			case DOWN -> new Vec3[] {new Vec3(min, min, min), new Vec3(max, min, min), new Vec3(max, min, max), new Vec3(min, min, max)};
			case UP -> new Vec3[] {new Vec3(min, max, min), new Vec3(min, max, max), new Vec3(max, max, max), new Vec3(max, max, min)};
			case NORTH -> new Vec3[] {new Vec3(min, min, min), new Vec3(min, max, min), new Vec3(max, max, min), new Vec3(max, min, min)};
			case SOUTH -> new Vec3[] {new Vec3(min, min, max), new Vec3(max, min, max), new Vec3(max, max, max), new Vec3(min, max, max)};
			case WEST -> new Vec3[] {new Vec3(min, min, min), new Vec3(min, min, max), new Vec3(min, max, max), new Vec3(min, max, min)};
			case EAST -> new Vec3[] {new Vec3(max, min, min), new Vec3(max, max, min), new Vec3(max, max, max), new Vec3(max, min, max)};
		};
	}

	// The texture runs along the face's two in-plane axes, so it lines up across the box's edges.
	private static void addVertex(PoseStack.Pose pose, VertexConsumer buffer, Vec3 corner, Direction face, Direction normal) {
		double u = face.getAxis() == Direction.Axis.X ? corner.z : corner.x;
		double v = face.getAxis() == Direction.Axis.Y ? corner.z : corner.y;

		buffer.addVertex(pose, (float) corner.x, (float) corner.y, (float) corner.z)
			.setColor(COLOR)
			.setUv((float) u / BLOCKS_PER_TEXTURE, (float) v / BLOCKS_PER_TEXTURE)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(FULL_BRIGHT)
			.setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
	}

	/** Vanilla's spawner render state, plus how much of the Clearing is still to go. */
	public static final class State extends SpawnerRenderState {
		float remainingShare;
	}
}
