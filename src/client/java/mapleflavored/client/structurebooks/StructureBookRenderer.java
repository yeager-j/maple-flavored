package mapleflavored.client.structurebooks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mapleflavored.MapleFlavored;
import mapleflavored.client.StagedBox;
import mapleflavored.structurebooks.StructureBookBlockEntity;
import mapleflavored.structurebooks.StructureBooks;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a structure book as the enchanting table's open book, floating, bobbing and slowly turning. A locked book is
 * drawn closed and still at the middle of a glowing gold cage, which breaks away in patches as the keys of its lock
 * are done.
 */
public final class StructureBookRenderer implements BlockEntityRenderer<StructureBookBlockEntity, StructureBookRenderer.State> {
	// Wraps the game time so it stays precise as a float; the animation skips once per wrap, about every 14 hours.
	private static final long TIME_WRAP = 1L << 20;
	private static final float TURN_PER_TICK = 0.025F;
	private static final float BOB_HEIGHT = 0.05F;
	private static final float BOB_SPEED = 0.1F;
	// The enchanting table tips its book this far to face up.
	private static final float TILT_DEGREES = 80.0F;
	private static final float OPEN = 1.0F;
	private static final float CLOSED = 0.0F;
	// A closed book's covers stand 6 pixels up from its spine, which sits at the block's centre. Half of that back down
	// puts the middle of the book there instead.
	private static final float CLOSED_BOOK_CENTRING = -3.0F / 16.0F;
	private static final Identifier CAGE = MapleFlavored.id("textures/entity/structure_book_cage.png");
	// The cage's box within the block: the 14 pixels its atlas draws for each face, centred.
	private static final float CAGE_MIN = 1.0F / 16.0F;
	private static final float CAGE_MAX = 15.0F / 16.0F;
	private static final float LEFT_PAGE = 0.1F;
	private static final float RIGHT_PAGE = 0.9F;

	private final SpriteGetter sprites;
	private final BookModel bookModel;

	private StructureBookRenderer(BlockEntityRendererProvider.Context context) {
		this.sprites = context.sprites();
		this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
	}

	/** Registers the renderer for the structure book's block entity. Call once from client init. */
	public static void register() {
		BlockEntityRenderers.register(StructureBooks.BLOCK_ENTITY, StructureBookRenderer::new);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(
		StructureBookBlockEntity book,
		State state,
		float partialTicks,
		Vec3 cameraPosition,
		ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
	) {
		BlockEntityRenderer.super.extractRenderState(book, state, partialTicks, cameraPosition, breakProgress);

		long gameTime = book.getLevel() == null ? 0L : book.getLevel().getGameTime();

		state.time = gameTime % TIME_WRAP + partialTicks;
		state.lockRemaining = book.shownLockRemaining();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		boolean locked = state.lockRemaining > 0.0F;
		BookPose pose = locked ? BookPose.CAGED : BookPose.free(state.time);
		BookModel.State bookState = BookModel.State.forAnimation(pose.time, LEFT_PAGE, RIGHT_PAGE, pose.openness);

		poseStack.pushPose();
		poseStack.translate(0.5F, 0.5F + Mth.sin(pose.time * BOB_SPEED) * BOB_HEIGHT, 0.5F);
		poseStack.rotate(Axis.YP, -pose.time * TURN_PER_TICK);
		poseStack.rotateDegrees(Axis.ZP, TILT_DEGREES);
		poseStack.translate(pose.alongCovers, 0.0F, 0.0F);
		submitNodeCollector.submitModel(
			bookModel,
			bookState,
			poseStack,
			state.lightCoords,
			OverlayTexture.NO_OVERLAY,
			-1,
			EnchantTableRenderer.BOOK_TEXTURE,
			sprites,
			0
		);
		poseStack.popPose();

		if (!locked) return;

		StagedBox.submit(
			poseStack,
			submitNodeCollector,
			RenderTypes.entityCutout(CAGE),
			StagedBox.stageAt(1.0F - state.lockRemaining),
			CAGE_MIN,
			CAGE_MAX,
			StagedBox.Side.OUTSIDE,
			LightCoordsUtil.FULL_BRIGHT,
			StagedBox.UNTINTED
		);
	}

	/**
	 * How the book itself is posed.
	 *
	 * @param time the animation time it bobs and turns by, in ticks
	 * @param openness how far open it is, from {@link #CLOSED} to {@link #OPEN}
	 * @param alongCovers how far it is moved along its covers, away from its spine, in blocks
	 */
	private record BookPose(float time, float openness, float alongCovers) {
		// Closed, level and facing one way, as an unlocked book is at time 0, with its middle at the cage's.
		static final BookPose CAGED = new BookPose(0.0F, CLOSED, CLOSED_BOOK_CENTRING);

		static BookPose free(float time) {
			return new BookPose(time, OPEN, 0.0F);
		}
	}

	/** What one frame of a structure book needs: the animation time, in ticks, and the share of its lock still on. */
	public static final class State extends BlockEntityRenderState {
		float time;
		float lockRemaining;
	}
}
