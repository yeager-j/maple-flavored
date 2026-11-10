package mapleflavored.spawners;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.jspecify.annotations.Nullable;

/**
 * One normal spawner's Clearing: the deaths of mobs it spawned, the players who entered its activation range, whether
 * it is Cleared, and the extra drop it gives when broken, if any.
 *
 * <p>The requirement grows with each new player and never drops. Until Cleared, the spawner spawns twice as fast.
 * Once Cleared, nothing changes and it spawns at vanilla speed. A spawner that isn't Cleared always has deaths to go.
 */
public final class SpawnerClearing {
	private static final int BASE_REQUIREMENT = 12;
	private static final int PER_ADDITIONAL_PLAYER = 2;
	private static final int UNCLEARED_SPEEDUP = 2;

	/** Saves a spawner's Clearing. Clients are sent its {@link #view()} instead. */
	public static final Codec<SpawnerClearing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.optionalFieldOf("deaths", 0).forGetter(clearing -> clearing.deaths),
		UUIDUtil.CODEC_LINKED_SET.optionalFieldOf("players", Set.of()).forGetter(clearing -> clearing.players),
		Codec.BOOL.optionalFieldOf("cleared", false).forGetter(clearing -> clearing.cleared),
		ItemStack.CODEC.optionalFieldOf("drop").forGetter(SpawnerClearing::heldDrop),
		// Read only: saves from before the drop was any item named the enchantment of a level I book.
		Enchantment.CODEC.optionalFieldOf("book").forGetter(clearing -> Optional.empty())
	).apply(instance, SpawnerClearing::new));

	private int deaths;
	private final Set<UUID> players;
	private boolean cleared;
	private @Nullable ItemStack heldDrop;

	/** A spawner's Clearing before anything has counted. */
	public static SpawnerClearing unstarted() {
		return new SpawnerClearing(0, Set.of(), false, Optional.empty(), Optional.empty());
	}

	private SpawnerClearing(int deaths, Set<UUID> players, boolean cleared, Optional<ItemStack> heldDrop, Optional<Holder<Enchantment>> legacyBook) {
		this.deaths = deaths;
		this.players = new LinkedHashSet<>(players);
		// A save whose deaths already meet the requirement is Cleared, whatever it recorded.
		this.cleared = cleared || deaths >= requirement();
		this.heldDrop = heldDrop
			.or(() -> legacyBook.map(enchantment -> EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, 1))))
			.orElse(null);
	}

	/** The deaths of this spawner's mobs counted so far. */
	public int deaths() {
		return deaths;
	}

	/** How many players have entered this spawner's activation range since it began counting. */
	public int playerCount() {
		return players.size();
	}

	/** The deaths needed to Clear this spawner: 12, plus 2 for each player after the first. */
	public int requirement() {
		return BASE_REQUIREMENT + PER_ADDITIONAL_PLAYER * Math.max(0, players.size() - 1);
	}

	/** Whether this spawner is Cleared, so it can be broken and blown up. */
	public boolean isCleared() {
		return cleared;
	}

	/** A copy of the stack this spawner drops with its Pith, if it holds one. */
	public Optional<ItemStack> heldDrop() {
		return Optional.ofNullable(heldDrop).map(ItemStack::copy);
	}

	/** What clients are told of this Clearing. Only the server works it out. */
	public SpawnerView view() {
		return new SpawnerView(remainingShare(), cleared);
	}

	/** The wait before this spawner's next wave, given the wait vanilla chose: half as long until Cleared. */
	public int spawnDelay(int vanillaDelay) {
		return cleared ? vanillaDelay : vanillaDelay / UNCLEARED_SPEEDUP;
	}

	/** @return whether the player is new to this spawner, which raises the requirement */
	boolean recordPlayer(UUID player) {
		if (cleared) return false;

		return players.add(player);
	}

	/** Counts one death of a mob this spawner spawned, Clearing it at the requirement. Does nothing once Cleared. */
	void recordDeath() {
		if (cleared) return;

		deaths++;

		if (deaths >= requirement()) cleared = true;
	}

	void holdDrop(ItemStack drop) {
		heldDrop = drop.copy();
	}

	// The share of the requirement still to go, from 1 before any death down toward 0, and exactly 0 once Cleared.
	private float remainingShare() {
		if (cleared) return 0.0F;

		return (float) (requirement() - deaths) / requirement();
	}
}
