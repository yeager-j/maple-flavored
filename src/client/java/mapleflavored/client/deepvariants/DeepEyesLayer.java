package mapleflavored.client.deepvariants;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Draws a Deep variant's pale eyes over its body at full brightness, so they show in the dark as a spider's do.
 * Vanilla's eyes layer has one texture, and a baby zombie's face sits elsewhere on its own, so this picks by age.
 */
final class DeepEyesLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
	private final RenderType adultEyes;
	private final RenderType babyEyes;

	/** A layer for a mob with no baby form. */
	DeepEyesLayer(RenderLayerParent<S, M> renderer, Identifier texture) {
		this(renderer, texture, texture);
	}

	DeepEyesLayer(RenderLayerParent<S, M> renderer, Identifier adultTexture, Identifier babyTexture) {
		super(renderer);
		this.adultEyes = RenderTypes.eyes(adultTexture);
		this.babyEyes = RenderTypes.eyes(babyTexture);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, S state, float yRot, float xRot) {
		RenderType eyes = state.isBaby ? this.babyEyes : this.adultEyes;

		collector.order(1).submitModel(this.getParentModel(), state, poseStack, eyes, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
	}
}
