package mapleflavored.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.WorldBorderRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.world.level.border.BorderStatus;
import net.minecraft.world.phys.Vec3;

/** The world border's forcefield drawn as a box, as uncleared spawners and locked structure books wear it. */
public final class Forcefield {
	private static final int COLOR = ARGB.opaque(BorderStatus.STATIONARY.getColor());

	// As the world border draws it: the texture repeats every 2 blocks and scrolls once every 3 seconds.
	private static final float BLOCKS_PER_TEXTURE = 2.0F;
	private static final long SCROLL_MILLIS = 3000L;
	private static final int FULL_BRIGHT = 0xF000F0;

	private Forcefield() {
	}

	/**
	 * Submits a forcefield box spanning {@code min} to {@code max} on every axis, in the block's own coordinates, where
	 * 0 to 1 is the block itself.
	 */
	public static void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, float min, float max) {
		float scroll = (float) (Util.getMillis() % SCROLL_MILLIS) / SCROLL_MILLIS;

		submitNodeCollector.submitCustomGeometry(
			poseStack,
			RenderTypes.energySwirl(WorldBorderRenderer.FORCEFIELD_LOCATION, scroll, scroll),
			(pose, buffer) -> addBox(pose, buffer, min, max)
		);
	}

	// Every face of the box from min to max, drawn both ways so the far side shows through the near one.
	private static void addBox(PoseStack.Pose pose, VertexConsumer buffer, float min, float max) {
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
}
