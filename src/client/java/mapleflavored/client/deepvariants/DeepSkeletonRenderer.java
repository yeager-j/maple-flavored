package mapleflavored.client.deepvariants;

import mapleflavored.MapleFlavored;
import mapleflavored.deepvariants.DeepVariants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;

/** Draws a Deep Skeleton as a skeleton with its own deepslate texture. */
public class DeepSkeletonRenderer extends SkeletonRenderer {
	private static final Identifier TEXTURE = MapleFlavored.id("textures/entity/deep_skeleton.png");

	private DeepSkeletonRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	/** Registers the renderer. Call once from client init. */
	static void register() {
		EntityRenderers.register(DeepVariants.DEEP_SKELETON, DeepSkeletonRenderer::new);
	}

	@Override
	public Identifier getTextureLocation(SkeletonRenderState state) {
		return TEXTURE;
	}
}
