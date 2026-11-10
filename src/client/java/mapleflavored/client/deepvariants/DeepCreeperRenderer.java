package mapleflavored.client.deepvariants;

import mapleflavored.MapleFlavored;
import mapleflavored.deepvariants.DeepVariants;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;

/** Draws a Deep Creeper as a creeper, charged glow included, with its own deepslate texture. */
public class DeepCreeperRenderer extends CreeperRenderer {
	private static final Identifier TEXTURE = MapleFlavored.id("textures/entity/deep_creeper.png");

	private DeepCreeperRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	/** Registers the renderer. Call once from client init. */
	static void register() {
		EntityRenderers.register(DeepVariants.DEEP_CREEPER, DeepCreeperRenderer::new);
	}

	@Override
	public Identifier getTextureLocation(CreeperRenderState state) {
		return TEXTURE;
	}
}
