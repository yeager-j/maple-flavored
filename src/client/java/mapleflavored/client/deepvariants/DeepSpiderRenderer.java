package mapleflavored.client.deepvariants;

import mapleflavored.MapleFlavored;
import mapleflavored.deepvariants.DeepSpider;
import mapleflavored.deepvariants.DeepVariants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.layers.SpiderEyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Draws a Deep Spider as a spider with its own deepslate texture and glowing pale eyes. */
public class DeepSpiderRenderer extends SpiderRenderer<DeepSpider> {
	private static final Identifier TEXTURE = MapleFlavored.id("textures/entity/deep_spider.png");
	private static final Identifier EYES = MapleFlavored.id("textures/entity/deep_spider_eyes.png");

	private DeepSpiderRenderer(EntityRendererProvider.Context context) {
		super(context);
		// The spider's own eyes would glow red over the pale ones.
		this.layers.removeIf(SpiderEyesLayer.class::isInstance);
		this.addLayer(new DeepEyesLayer<>(this, EYES));
	}

	/** Registers the renderer. Call once from client init. */
	static void register() {
		EntityRenderers.register(DeepVariants.DEEP_SPIDER, DeepSpiderRenderer::new);
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return TEXTURE;
	}
}
