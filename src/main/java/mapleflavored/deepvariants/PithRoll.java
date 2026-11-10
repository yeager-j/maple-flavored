package mapleflavored.deepvariants;

import com.mojang.serialization.MapCodec;
import mapleflavored.gamerules.MapleGameRules;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * The {@code maple_flavored:deep_variant_pith_roll} loot condition, which passes at the chance the Deep variant Pith
 * drop game rule sets. Loot tables can't read a game rule themselves, and the chance is a playtest value.
 */
final class PithRoll implements LootItemCondition {
	static final MapCodec<PithRoll> MAP_CODEC = MapCodec.unit(new PithRoll());
	private static final double WHOLE_PERCENT = 100.0;

	private PithRoll() {
	}

	@Override
	public MapCodec<PithRoll> codec() {
		return MAP_CODEC;
	}

	@Override
	public boolean test(LootContext context) {
		double percent = context.getLevel().getGameRules().get(MapleGameRules.DEEP_VARIANT_PITH_DROP_PERCENT);

		return context.getRandom().nextDouble() * WHOLE_PERCENT < percent;
	}
}
