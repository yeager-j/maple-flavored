package mapleflavored.brewing;

import java.util.Optional;
import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.UseCooldown;

/**
 * Brewing changes: potions, splash potions, lingering potions and water bottles stack, and a player's splash and
 * lingering throws share one cooldown. Weakness from Strength is recipe data.
 */
public final class Brewing {
	private static final int POTION_STACK_SIZE = 16;

	// Both throwable potions share this group, so throwing either puts both on cooldown. Dispensers never apply it.
	private static final UseCooldown THROW_COOLDOWN = new UseCooldown(1.0F, Optional.of(MapleFlavored.id("thrown_potion")));

	private Brewing() {
	}

	/** Registers potion stacking and the throw cooldown. Call once from mod init. */
	public static void register() {
		DefaultItemComponentEvents.MODIFY.register(context -> {
			context.modify(Items.POTION, (components, lookup, item) -> components.set(DataComponents.MAX_STACK_SIZE, POTION_STACK_SIZE));
			context.modify(Items.SPLASH_POTION, (components, lookup, item) -> {
				components.set(DataComponents.MAX_STACK_SIZE, POTION_STACK_SIZE);
				components.set(DataComponents.USE_COOLDOWN, THROW_COOLDOWN);
			});
			context.modify(Items.LINGERING_POTION, (components, lookup, item) -> {
				components.set(DataComponents.MAX_STACK_SIZE, POTION_STACK_SIZE);
				components.set(DataComponents.USE_COOLDOWN, THROW_COOLDOWN);
			});
		});
	}
}
