package mapleflavored.client.deepvariants;

import mapleflavored.MapleFlavored;
import mapleflavored.deepvariants.DeepSpider;
import mapleflavored.deepvariants.DeepVariants;
import net.minecraft.client.model.monster.spider.SpiderModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.SpiderEyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Draws a Deep Spider as a spider with its own deepslate texture and glowing teal eyes. */
public class DeepSpiderRenderer extends SpiderRenderer<DeepSpider> {
	private static final Identifier TEXTURE = MapleFlavored.id("textures/entity/deep_spider.png");
	private static final RenderType EYES = RenderTypes.eyes(MapleFlavored.id("textures/entity/deep_spider_eyes.png"));

	private DeepSpiderRenderer(EntityRendererProvider.Context context) {
		super(context);
		// The spider's own eyes glow red over the teal ones.
		this.layers.removeIf(SpiderEyesLayer.class::isInstance);
		this.addLayer(new DeepSpiderEyesLayer(this));
	}

	/** Registers the renderer. Call once from client init. */
	public static void register() {
		EntityRenderers.register(DeepVariants.DEEP_SPIDER, DeepSpiderRenderer::new);
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return TEXTURE;
	}

	private static class DeepSpiderEyesLayer extends EyesLayer<LivingEntityRenderState, SpiderModel> {
		DeepSpiderEyesLayer(RenderLayerParent<LivingEntityRenderState, SpiderModel> renderer) {
			super(renderer);
		}

		@Override
		public RenderType renderType() {
			return EYES;
		}
	}
}
