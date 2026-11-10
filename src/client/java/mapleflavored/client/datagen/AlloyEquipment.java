package mapleflavored.client.datagen;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import mapleflavored.MapleFlavored;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.model.EquipmentClientInfo.Layer;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

/**
 * The Alloys' equipment assets, which name the textures worn gear is drawn with: one per Alloy for its armor and mob
 * armor, and one for its Armored Elytra, which draws the chestplate and the wings.
 */
class AlloyEquipment extends FabricCodecDataProvider<EquipmentClientInfo> {
	AlloyEquipment(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.RESOURCE_PACK, "equipment", EquipmentClientInfo.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, EquipmentClientInfo> assets, HolderLookup.Provider registries) {
		for (Alloy alloy : Alloy.values()) {
			Identifier texture = MapleFlavored.id(alloy.path());

			assets.accept(texture, armor(texture));
			assets.accept(BuiltInRegistries.ITEM.getKey(AlloyGear.armoredElytra(alloy)), armoredElytra(texture));
		}
	}

	@Override
	public String getName() {
		return "Alloy equipment assets";
	}

	private static EquipmentClientInfo armor(Identifier texture) {
		return EquipmentClientInfo.builder()
			.addHumanoidLayers(texture)
			.addLayers(LayerType.HORSE_BODY, new Layer(texture))
			.addLayers(LayerType.NAUTILUS_BODY, new Layer(texture))
			.build();
	}

	// The wings are drawn over the player's own texture where the wing texture is clear, as the Elytra's are.
	private static EquipmentClientInfo armoredElytra(Identifier texture) {
		return EquipmentClientInfo.builder()
			.addLayers(LayerType.HUMANOID, new Layer(texture))
			.addLayers(LayerType.HUMANOID_BABY, new Layer(texture))
			.addLayers(LayerType.WINGS, new Layer(texture, Optional.empty(), true))
			.build();
	}
}
