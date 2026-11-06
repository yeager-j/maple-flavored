package mapleflavored.client.alloys;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.Set;
import java.util.function.Function;
import mapleflavored.MapleFlavored;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.equipment.EquipmentAsset;

/**
 * PROTOTYPE: draws the End portal's starfield over an Alloy's worn trim. A second pass over the armor model, like the
 * enchantment glint, that keeps only the pixels in that layer's {@code alloy_trim_mask.png}.
 */
public final class PortalTrim {
	/** The Alloys whose worn trim is a starfield. None yet; it's meant for a future End Alloy. */
	private static final Set<Identifier> ALLOYS = Set.of();

	private static final RenderPipeline PIPELINE = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
			.withLocation(MapleFlavored.id("pipeline/alloy_portal_trim"))
			.withBindGroupLayout(BindGroupLayouts.PROJECTION)
			.withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
			.withBindGroupLayout(BindGroupLayouts.FOG)
			.withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER1_SAMPLER2)
			.withVertexShader(MapleFlavored.id("core/alloy_portal_trim"))
			.withFragmentShader(MapleFlavored.id("core/alloy_portal_trim"))
			.withShaderDefine("PORTAL_LAYERS", 15)
			.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
			.withPrimitiveTopology(PrimitiveTopology.QUADS)
			.withCull(false)
			.withColorTargetState(ColorTargetState.DEFAULT)
			.withDepthStencilState(DepthStencilState.DEFAULT)
			.build()
	);

	private static final Function<Identifier, RenderType> RENDER_TYPES = Util.memoize(mask -> RenderType.create(
		"maple_flavored_alloy_portal_trim",
		RenderSetup.builder(PIPELINE)
			.withTexture("Sampler0", mask)
			.withTexture("Sampler1", AbstractEndPortalRenderer.END_SKY_LOCATION)
			.withTexture("Sampler2", AbstractEndPortalRenderer.END_PORTAL_LOCATION)
			.setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
			.createRenderSetup()
	));

	private PortalTrim() {
	}

	/** Loads the pipeline before resources do, so its shaders compile with vanilla's. */
	public static void register() {
	}

	/** The starfield pass for this equipment layer, or null if the asset has no starfield trim. */
	public static RenderType renderType(ResourceKey<EquipmentAsset> asset, EquipmentClientInfo.LayerType layer) {
		if (!ALLOYS.contains(asset.identifier())) {
			return null;
		}
		return RENDER_TYPES.apply(MapleFlavored.id("textures/entity/equipment/" + layer.getSerializedName() + "/alloy_trim_mask.png"));
	}
}
