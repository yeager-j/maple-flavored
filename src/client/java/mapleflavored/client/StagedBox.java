package mapleflavored.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A box drawn from a stage atlas: a texture with a column for each face of the box, in the order of
 * {@link Direction#get3DDataValue()}, and a row for each stage of something wearing away, the untouched stage on top.
 * Each face reads its cell as a block model's face reads its texture, so an atlas painted over a block's own texture
 * lines up with it.
 */
public final class StagedBox {
	/** How many stages every stage atlas has. */
	public static final int STAGES = 12;
	/** The tint that leaves the atlas's own colours as they are. */
	public static final int UNTINTED = -1;

	private static final int FACES = Direction.values().length;
	// A share that reaches a stage only within float error still counts as reaching it.
	private static final float ROUNDING = 1.0E-4F;
	// Where each of a face's corners sits in its cell: top left, bottom left, bottom right, top right.
	private static final float[] CELL_U = {0.0F, 0.0F, 1.0F, 1.0F};
	private static final float[] CELL_V = {0.0F, 1.0F, 1.0F, 0.0F};

	private StagedBox() {
	}

	/** The stage to draw once {@code goneShare} of the whole has worn away, from 0 for none of it. */
	public static int stageAt(float goneShare) {
		return Mth.clamp(Mth.floor(goneShare * STAGES + ROUNDING), 0, STAGES - 1);
	}

	/**
	 * Submits a box spanning {@code min} to {@code max} on every axis, in the block's own coordinates, where 0 to 1 is
	 * the block itself.
	 *
	 * @param renderType the render type of the stage atlas to draw from
	 * @param stage the atlas row to show, from {@link #stageAt}
	 * @param side which way the faces look
	 * @param light the packed light every face is lit with
	 * @param tint the colour every face is multiplied by, or {@link #UNTINTED}
	 */
	public static void submit(
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		RenderType renderType,
		int stage,
		float min,
		float max,
		Side side,
		int light,
		int tint
	) {
		submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> addFaces(pose, buffer, stage, min, max, side, light, tint));
	}

	private static void addFaces(PoseStack.Pose pose, VertexConsumer buffer, int stage, float min, float max, Side side, int light, int tint) {
		for (Direction face : Direction.values()) {
			Vec3[] corners = faceCorners(face, min, max);
			Direction normal = side.normalOf(face);

			for (int corner : side.cornerOrder) {
				Vec3 position = corners[corner];
				float u = (face.get3DDataValue() + CELL_U[corner]) / FACES;
				float v = (stage + CELL_V[corner]) / STAGES;

				buffer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z)
					.setColor(tint)
					.setUv(u, v)
					.setOverlay(OverlayTexture.NO_OVERLAY)
					.setLight(light)
					.setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
			}
		}
	}

	// The face's corners in cell order, which runs counter-clockwise seen from outside the box.
	private static Vec3[] faceCorners(Direction face, float min, float max) {
		return switch (face) {
			case DOWN -> new Vec3[] {new Vec3(min, min, max), new Vec3(min, min, min), new Vec3(max, min, min), new Vec3(max, min, max)};
			case UP -> new Vec3[] {new Vec3(min, max, min), new Vec3(min, max, max), new Vec3(max, max, max), new Vec3(max, max, min)};
			case NORTH -> new Vec3[] {new Vec3(max, max, min), new Vec3(max, min, min), new Vec3(min, min, min), new Vec3(min, max, min)};
			case SOUTH -> new Vec3[] {new Vec3(min, max, max), new Vec3(min, min, max), new Vec3(max, min, max), new Vec3(max, max, max)};
			case WEST -> new Vec3[] {new Vec3(min, max, min), new Vec3(min, min, min), new Vec3(min, min, max), new Vec3(min, max, max)};
			case EAST -> new Vec3[] {new Vec3(max, max, max), new Vec3(max, min, max), new Vec3(max, min, min), new Vec3(max, max, min)};
		};
	}

	/** Which way a box's faces look. A render type that culls back faces shows them from that side only. */
	public enum Side {
		/** Out of the box, as its faces are seen from around it. */
		OUTSIDE(new int[] {0, 1, 2, 3}, false),
		/** Into the box, as the far faces are seen through gaps in the near ones. */
		INSIDE(new int[] {3, 2, 1, 0}, true);

		private final int[] cornerOrder;
		private final boolean facesInward;

		Side(int[] cornerOrder, boolean facesInward) {
			this.cornerOrder = cornerOrder;
			this.facesInward = facesInward;
		}

		private Direction normalOf(Direction face) {
			return facesInward ? face.getOpposite() : face;
		}
	}
}
