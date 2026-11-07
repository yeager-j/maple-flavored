package mapleflavored.client.reinforcement;

import java.util.Map;
import mapleflavored.MapleFlavored;
import mapleflavored.reinforcement.Reinforcement;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;

/**
 * A worn Reinforced piece's chainmail, drawn beneath its own layers so it shows wherever the armor leaves gaps. It
 * skips wings and baby armor, which have no texture for it.
 */
public final class ReinforcedChainmail {
	private static final Map<EquipmentClientInfo.LayerType, RenderType> RENDER_TYPES = Map.of(
		EquipmentClientInfo.LayerType.HUMANOID, renderType(EquipmentClientInfo.LayerType.HUMANOID),
		EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS, renderType(EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS)
	);

	private ReinforcedChainmail() {
	}

	/** The chainmail pass for this layer of the worn stack, or null if the stack isn't Reinforced or the layer takes none. */
	public static RenderType renderType(ItemStack stack, EquipmentClientInfo.LayerType layer) {
		return Reinforcement.isReinforced(stack) ? RENDER_TYPES.get(layer) : null;
	}

	private static RenderType renderType(EquipmentClientInfo.LayerType layer) {
		return RenderTypes.armorCutoutNoCull(MapleFlavored.id("textures/entity/equipment/" + layer.getSerializedName() + "/reinforced.png"));
	}
}
