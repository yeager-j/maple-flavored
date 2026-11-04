package mapleflavored.client.enchanting;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import mapleflavored.enchanting.AppleEffect;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;

/** An enchanted golden apple's tooltip lists the effects it gives, the way a potion's does. */
public final class AppleTooltip {
	private AppleTooltip() {
	}

	/** Adds the effect lines to every enchanted golden apple's tooltip. Call once from client init. */
	public static void register() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			Set<AppleEffect> effects = AppleEffect.on(stack);

			if (effects.isEmpty()) return;

			List<Component> effectLines = new ArrayList<>();

			List<MobEffectInstance> instances = effects.stream().map(AppleEffect::instance).toList();

			PotionContents.addPotionTooltip(instances, effectLines::add, 1.0F, context.tickRate());
			// Right under the name, where a potion lists its effects, and above any advanced tooltip lines.
			lines.addAll(1, effectLines);
		});
	}
}
