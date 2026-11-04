package mapleflavored.client.structurebooks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Draws a structure book as the enchanting table's open book, floating, bobbing and slowly turning. */
public final class StructureBookRenderer implements BlockEntityRenderer<StructureBookBlockEntity, StructureBookRenderer.State> {
	// Wraps the game time so it stays precise as a float; the animation skips once per wrap, about every 14 hours.
	private static final long TIME_WRAP = 1L << 20;
	private static final float TURN_PER_TICK = 0.025F;
	private static final float BOB_HEIGHT = 0.05F;
	private static final float BOB_SPEED = 0.1F;
	// The enchanting table tips its book this far to face up.
	private static final float TILT_DEGREES = 80.0F;
	private static final float OPEN = 1.0F;
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
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		BookModel.State bookState = BookModel.State.forAnimation(state.time, LEFT_PAGE, RIGHT_PAGE, OPEN);

		poseStack.pushPose();
		poseStack.translate(0.5F, 0.5F + Mth.sin(state.time * BOB_SPEED) * BOB_HEIGHT, 0.5F);
		poseStack.rotate(Axis.YP, -state.time * TURN_PER_TICK);
		poseStack.rotateDegrees(Axis.ZP, TILT_DEGREES);
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
	}

	/** What one frame of a structure book needs: the animation time, in ticks. */
	public static final class State extends BlockEntityRenderState {
		float time;
	}
}
