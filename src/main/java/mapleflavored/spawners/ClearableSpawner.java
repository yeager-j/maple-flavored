package mapleflavored.spawners;

/** A spawner block entity that carries its {@link SpawnerClearing}. Every {@code SpawnerBlockEntity} is one. */
public interface ClearableSpawner {
	/** This spawner's Clearing, which it saves. Only the server's is real: a client's copy never leaves unstarted. */
	SpawnerClearing maple_flavored$clearing();

	/** What this spawner shows of its Clearing: derived from it on the server, and as last sent on a client. */
	SpawnerView maple_flavored$view();
}
