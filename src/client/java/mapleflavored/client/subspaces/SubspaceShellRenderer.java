package mapleflavored.client.subspaces;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mapleflavored.MapleFlavored;
import mapleflavored.subspaces.Subspace;
import mapleflavored.subspaces.Subspaces;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;

/**
 * Draws each Warden Subspace's edge as a faint, slowly drifting sculk shell, seen from inside and outside wherever the
 * sphere passes through air: a dome over the ground and a sheet through caves.
 */
public final class SubspaceShellRenderer {
	private static final Identifier TEXTURE = MapleFlavored.id("textures/environment/subspace_shell.png");
	private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentEmissive(TEXTURE);

	// Hand-check values. The texture brings the colour; the vertices only make it faint.
	private static final int COLOR = ARGB.color(96, 255, 255, 255);
	private static final float BLOCKS_PER_TEXTURE = 16.0F;
	private static final long DRIFT_MILLIS = 120_000L;

	private static final int LONGITUDES = 64;
	private static final int LATITUDES = 32;
	private static final int FULL_BRIGHT = 0xF000F0;

	private SubspaceShellRenderer() {
	}

	/** Draws the shells from now on. Call once from client init. */
	static void register() {
		LevelRenderEvents.COLLECT_SUBMITS.register(SubspaceShellRenderer::submitAll);
	}

	private static void submitAll(LevelRenderContext context) {
		ClientLevel level = Minecraft.getInstance().level;

		if (level == null) return;

		Vec3 camera = context.levelState().cameraRenderState.pos;
		double renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0;
		float drift = (float) (Util.getMillis() % DRIFT_MILLIS) / DRIFT_MILLIS;

		for (Subspace subspace : Subspaces.inLevel(level, Subspace.Kind.WARDEN)) {
			boolean isInRange = subspace.centre().distanceTo(camera) - subspace.radius() <= renderDistance;

			if (isInRange) submit(context, subspace, camera, drift);
		}
	}

	private static void submit(LevelRenderContext context, Subspace subspace, Vec3 camera, float drift) {
		PoseStack poseStack = context.poseStack();
		Vec3 offset = subspace.centre().subtract(camera);
		Sphere sphere = new Sphere((float) subspace.radius(), drift);

		poseStack.pushPose();
		poseStack.translate(offset.x, offset.y, offset.z);
		context.submitNodeCollector().submitCustomGeometry(poseStack, RENDER_TYPE, sphere::addTo);
		poseStack.popPose();
	}

	/**
	 * A latitude and longitude mesh of the sphere. The texture repeats a whole number of times around it, so it meets
	 * itself without a seam, and drifts around it over time.
	 */
	private record Sphere(float radius, float drift) {
		void addTo(PoseStack.Pose pose, VertexConsumer buffer) {
			for (int latitude = 0; latitude < LATITUDES; latitude++) {
				for (int longitude = 0; longitude < LONGITUDES; longitude++) {
					addVertex(pose, buffer, latitude, longitude);
					addVertex(pose, buffer, latitude + 1, longitude);
					addVertex(pose, buffer, latitude + 1, longitude + 1);
					addVertex(pose, buffer, latitude, longitude + 1);
				}
			}
		}

		private void addVertex(PoseStack.Pose pose, VertexConsumer buffer, int latitude, int longitude) {
			float polar = Mth.PI * latitude / LATITUDES;
			float azimuth = Mth.TWO_PI * longitude / LONGITUDES;
			float x = Mth.sin(polar) * Mth.cos(azimuth);
			float y = Mth.cos(polar);
			float z = Mth.sin(polar) * Mth.sin(azimuth);
			float u = (float) longitude / LONGITUDES * repeatsAround() + drift * repeatsAround();
			float v = (float) latitude / LATITUDES * Mth.PI * radius / BLOCKS_PER_TEXTURE;

			buffer.addVertex(pose, x * radius, y * radius, z * radius)
				.setColor(COLOR)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(FULL_BRIGHT)
				.setNormal(pose, x, y, z);
		}

		private int repeatsAround() {
			return Math.max(1, Math.round(Mth.TWO_PI * radius / BLOCKS_PER_TEXTURE));
		}
	}
}
