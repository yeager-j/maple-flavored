package mapleflavored.client.deepvariants;

import mapleflavored.MapleFlavored;
import mapleflavored.deepvariants.DeepVariants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

/** Draws a Deep Zombie as a zombie with its own deepslate textures and glowing pale eyes. */
public class DeepZombieRenderer extends ZombieRenderer {
	private static final Identifier ADULT_TEXTURE = MapleFlavored.id("textures/entity/deep_zombie.png");
	// Deep Zombies only spawn as adults, but a spawn egg used on one makes a baby.
	private static final Identifier BABY_TEXTURE = MapleFlavored.id("textures/entity/deep_zombie_baby.png");
	private static final Identifier ADULT_EYES = MapleFlavored.id("textures/entity/deep_zombie_eyes.png");
	private static final Identifier BABY_EYES = MapleFlavored.id("textures/entity/deep_zombie_baby_eyes.png");

	private DeepZombieRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.addLayer(new DeepEyesLayer<>(this, ADULT_EYES, BABY_EYES));
	}

	/** Registers the renderer. Call once from client init. */
	static void register() {
		EntityRenderers.register(DeepVariants.DEEP_ZOMBIE, DeepZombieRenderer::new);
	}

	@Override
	public Identifier getTextureLocation(ZombieRenderState state) {
		return state.isBaby ? BABY_TEXTURE : ADULT_TEXTURE;
	}
}
