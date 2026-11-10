package mapleflavored.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import mapleflavored.client.reinforcement.ReinforcedChainmail;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Draws no trim on wings, and a Reinforced piece's chainmail beneath its layers. */
@Mixin(EquipmentLayerRenderer.class)
public abstract class EquipmentLayerRendererMixin {
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
}
