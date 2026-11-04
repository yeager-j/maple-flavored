package mapleflavored.spawners;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.Nullable;

/**
 * One normal spawner's Clearing: the deaths of mobs it spawned, the players who entered its activation range, whether
 * it is Cleared, and the book it drops when broken, if any.
 *
 * <p>The requirement grows with each new player and never drops. Once Cleared, nothing changes.
 */
public final class SpawnerClearing {
	/** How many bars the progress cage has while no mob has died. */
	public static final int CAGE_BARS = 12;

	private static final int BASE_REQUIREMENT = 12;
	private static final int PER_ADDITIONAL_PLAYER = 2;

	/** Saves and syncs a spawner's Clearing. */
	public static final Codec<SpawnerClearing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.optionalFieldOf("deaths", 0).forGetter(clearing -> clearing.deaths),
		UUIDUtil.CODEC_LINKED_SET.optionalFieldOf("players", Set.of()).forGetter(clearing -> clearing.players),
		Codec.BOOL.optionalFieldOf("cleared", false).forGetter(clearing -> clearing.cleared),
		Enchantment.CODEC.optionalFieldOf("book").forGetter(SpawnerClearing::heldBook)
	).apply(instance, SpawnerClearing::new));

	private int deaths;
	private final Set<UUID> players;
	private boolean cleared;
	private @Nullable Holder<Enchantment> heldBook;

	/** A spawner's Clearing before anything has counted. */
	public static SpawnerClearing unstarted() {
		return new SpawnerClearing(0, Set.of(), false, Optional.empty());
	}

	private SpawnerClearing(int deaths, Set<UUID> players, boolean cleared, Optional<Holder<Enchantment>> heldBook) {
		this.deaths = deaths;
		this.players = new LinkedHashSet<>(players);
		this.cleared = cleared;
		this.heldBook = heldBook.orElse(null);
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

	/** The enchantment of the level I book this spawner drops with its Pith, if it holds one. */
	public Optional<Holder<Enchantment>> heldBook() {
		return Optional.ofNullable(heldBook);
	}

	/**
	 * How many of the cage's 12 bars are still standing: the share of the requirement still to go, rounded up, so the
	 * cage only empties when the spawner is Cleared.
	 */
	public int cageBars() {
		if (cleared) return 0;

		int remaining = requirement() - deaths;

		return Math.ceilDiv(CAGE_BARS * remaining, requirement());
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

	void holdBook(Holder<Enchantment> enchantment) {
		heldBook = enchantment;
	}
}
