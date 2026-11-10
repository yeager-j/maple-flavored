package mapleflavored.enchantments;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Vanilla enchantments Maple removes from the game: Curse of Binding and Curse of Vanishing. {@link #KEYS} is the one
 * list of them. Their registry files and their entries in enchantment tags are both left out as data packs load, so no
 * data file has to repeat the list.
 */
public final class RemovedEnchantments {
	/** The removed enchantments. None of them is ever registered. */
	public static final Set<ResourceKey<Enchantment>> KEYS = Set.of(Enchantments.BINDING_CURSE, Enchantments.VANISHING_CURSE);

	private static final Set<Identifier> FILES = KEYS.stream()
		.map(key -> FileToIdConverter.registry(Registries.ENCHANTMENT).idToFile(key.identifier()))
		.collect(Collectors.toUnmodifiableSet());

	private static final Set<Identifier> IDS = KEYS.stream().map(ResourceKey::identifier).collect(Collectors.toUnmodifiableSet());

	private static final String TAG_DIRECTORY = Registries.tagsDirPath(Registries.ENCHANTMENT);

	private RemovedEnchantments() {
	}

	/** A registry's data files, keyed by file path, without the removed enchantments' files. */
	public static Map<Identifier, Resource> withoutRemovedFiles(Map<Identifier, Resource> files) {
		return files.entrySet()
			.stream()
			.filter(file -> !FILES.contains(file.getKey()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	/** Whether a tag loader reading {@code directory} loads enchantment tags. */
	public static boolean isTagDirectory(String directory) {
		return TAG_DIRECTORY.equals(directory);
	}

	/** Enchantment tags' entries as read from data packs, keyed by tag, without those that require a removed enchantment. */
	public static Map<Identifier, List<TagLoader.EntryWithSource>> withoutRemovedEntries(Map<Identifier, List<TagLoader.EntryWithSource>> tags) {
		Map<Identifier, List<TagLoader.EntryWithSource>> kept = new HashMap<>();

		tags.forEach((tag, entries) -> kept.put(tag, entries.stream().filter(entry -> !requiresRemoved(entry.entry())).toList()));

		return kept;
	}

	// A tag entry keeps its id private. It verifies only when everything it requires is present, so one that fails
	// while just the removed enchantments are absent requires one of them.
	private static boolean requiresRemoved(TagEntry entry) {
		return !entry.verifyIfPresent(element -> !IDS.contains(element), tag -> true);
	}
}
