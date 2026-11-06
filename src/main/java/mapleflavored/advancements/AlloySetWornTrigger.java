package mapleflavored.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import mapleflavored.alloys.Alloy;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** Fires when a player puts on the last piece of a full Alloy set; a criterion names the Alloy it waits for. */
public final class AlloySetWornTrigger extends SimpleCriterionTrigger<AlloySetWornTrigger.TriggerInstance> {
	@Override
	public Codec<TriggerInstance> codec() {
		return TriggerInstance.CODEC;
	}

	/** Fires for every criterion waiting on the Alloy whose full set the player now wears. */
	public void trigger(ServerPlayer player, Alloy alloy) {
		trigger(player, instance -> instance.alloy() == alloy);
	}

	/** A criterion met by wearing a full set of {@code alloy}. */
	public record TriggerInstance(Optional<Holder<LootItemCondition>> player, Alloy alloy) implements SimpleCriterionTrigger.SimpleInstance {
		static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
			Alloy.CODEC.fieldOf("alloy").forGetter(TriggerInstance::alloy)
		).apply(instance, TriggerInstance::new));
	}
}
