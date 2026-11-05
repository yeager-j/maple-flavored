package mapleflavored.spawners;

/** A spawner block entity that carries its {@link SpawnerClearing}. Every {@code SpawnerBlockEntity} is one. */
public interface ClearableSpawner {
	/** This spawner's Clearing, which it saves and sends to clients. */
	SpawnerClearing maple_flavored$clearing();
}
