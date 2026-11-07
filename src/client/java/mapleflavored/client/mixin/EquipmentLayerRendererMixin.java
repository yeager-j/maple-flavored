package mapleflavored.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import mapleflavored.client.reinforcement.ReinforcedChainmail;
import mapleflavored.client.alloys.PortalTrim;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws no trim on wings, a Reinforced piece's chainmail beneath its layers, and (PROTOTYPE) {@link PortalTrim}'s
 * starfield over the worn layers, after vanilla's layers, trim and glint.
 */
@Mixin(EquipmentLayerRenderer.class)
public abstract class EquipmentLayerRendererMixin {
	// Past the orders vanilla used: one per layer, then the trim and its glint.
	private static final int ORDER_AFTER_VANILLA = 8;

	// Vanilla trims every layer but baby armor, though no vanilla wings can be trimmed. An Armored Elytra keeps its
	// chestplate's trim, which belongs on the body; wings have no trim textures, so they'd draw missing ones.
	@ModifyVariable(
		method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
		at = @At("STORE")
	)
	private ArmorTrim noTrimOnWings(ArmorTrim trim, EquipmentClientInfo.LayerType layerType) {
		return layerType == EquipmentClientInfo.LayerType.WINGS ? null : trim;
	}

	// Hooked where vanilla starts counting its layers' orders, and takes the first one, so every layer draws over the
	// chainmail and it shows only where the armor leaves gaps.
	@ModifyVariable(
		method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
		at = @At("STORE"),
		name = "nextOrder"
	)
	@SuppressWarnings({"unchecked", "rawtypes"})
	private int submitReinforcedChainmail(
		int nextOrder,
		@Local(argsOnly = true) EquipmentClientInfo.LayerType layerType,
		@Local(argsOnly = true) Model model,
		@Local(argsOnly = true) Object state,
		@Local(argsOnly = true) ItemStack stack,
		@Local(argsOnly = true) PoseStack poseStack,
		@Local(argsOnly = true) SubmitNodeCollector collector,
		@Local(argsOnly = true, ordinal = 0) int light,
		@Local(argsOnly = true, ordinal = 1) int outlineColor
	) {
		RenderType renderType = ReinforcedChainmail.renderType(stack, layerType);

		if (renderType == null) return nextOrder;

		collector.order(nextOrder).submitModel(model, state, poseStack, renderType, light, OverlayTexture.NO_OVERLAY, -1, null, outlineColor);

		return nextOrder + 1;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Inject(
		method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
		at = @At("TAIL")
	)
	private void submitPortalTrim(
		EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> asset, Model model, Object state, ItemStack stack,
		PoseStack poseStack, SubmitNodeCollector collector, int light, Identifier playerTexture, int outlineColor, int order,
		CallbackInfo ci
	) {
		RenderType renderType = PortalTrim.renderType(asset, layerType);
		if (renderType != null) {
			collector.order(order + ORDER_AFTER_VANILLA)
				.submitModel(model, state, poseStack, renderType, light, OverlayTexture.NO_OVERLAY, -1, null, outlineColor);
		}
	}
}
