package mapleflavored.enchantments;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/** Vanilla enchantments Maple removes from the game: Curse of Binding and Curse of Vanishing. */
public final class RemovedEnchantments {
	/** The removed enchantments. None of them is ever registered. */
	public static final Set<ResourceKey<Enchantment>> KEYS = Set.of(Enchantments.BINDING_CURSE, Enchantments.VANISHING_CURSE);

	private static final Set<Identifier> FILES = KEYS.stream()
		.map(key -> FileToIdConverter.registry(Registries.ENCHANTMENT).idToFile(key.identifier()))
		.collect(Collectors.toUnmodifiableSet());

	private RemovedEnchantments() {
	}

	/** A registry's data files, keyed by file path, without the removed enchantments' files. */
	public static Map<Identifier, Resource> withoutRemovedFiles(Map<Identifier, Resource> files) {
		return files.entrySet()
			.stream()
			.filter(file -> !FILES.contains(file.getKey()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}
}
