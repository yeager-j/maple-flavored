package mapleflavored.spawners;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What a client is told of a spawner's Clearing, and all it needs to draw the glow and predict mining.
 *
 * @param remainingShare the share of the requirement still to go, from 1 before any death down toward 0, and exactly 0
 *                       once Cleared
 * @param cleared        whether the spawner is Cleared, so it can be broken
 */
public record SpawnerView(float remainingShare, boolean cleared) {
	/** Sends a view to clients. */
	public static final Codec<SpawnerView> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.FLOAT.fieldOf("remaining_share").forGetter(SpawnerView::remainingShare),
		Codec.BOOL.fieldOf("cleared").forGetter(SpawnerView::cleared)
	).apply(instance, SpawnerView::new));
}
