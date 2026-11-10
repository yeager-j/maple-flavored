package mapleflavored.gametest;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import mapleflavored.MapleFlavored;
import mapleflavored.enchantments.RemovedEnchantments;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Curse of Binding and Curse of Vanishing are never registered, and every enchantment tag loads as its data pack files
 * say, less the curses.
 */
public class RemovedCursesGameTests {
	// Vanilla's 43, less the 2 curses, plus Zephyr and Blight. A Full Library holds all of them but Mending.
	private static final int ENCHANTMENTS = 43;
	private static final FileToIdConverter TAG_FILES = FileToIdConverter.json(Registries.tagsDirPath(Registries.ENCHANTMENT));
	private static final Set<Identifier> CURSES = Set.of(
		Identifier.withDefaultNamespace("binding_curse"), Identifier.withDefaultNamespace("vanishing_curse")
	);
	// Written out, not read from the game, so the check fails if the curses' entries stop being seen at all.
	private static final Set<Identifier> TAGS_NAMING_A_CURSE = Set.of(
		Identifier.withDefaultNamespace("curse"),
		Identifier.withDefaultNamespace("on_random_loot"),
		Identifier.withDefaultNamespace("tooltip_order"),
		Identifier.withDefaultNamespace("tradeable"),
		Identifier.withDefaultNamespace("treasure")
	);

	@GameTest
	public void theCursesAreNotRegistered(GameTestHelper helper) {
		Registry<Enchantment> enchantments = enchantments(helper);

		for (ResourceKey<Enchantment> curse : RemovedEnchantments.KEYS) {
			helper.assertFalse(enchantments.containsKey(curse), curse.identifier() + " is registered");
		}

		helper.assertValueEqual(enchantments.size(), ENCHANTMENTS, "registered enchantments");
		helper.succeed();
	}

	@GameTest
	public void everyEnchantmentTagIsItsFilesWithoutTheCurses(GameTestHelper helper) {
		Registry<Enchantment> enchantments = enchantments(helper);
		Map<Identifier, List<Resource>> tagFiles = helper.getLevel().getServer().getResourceManager()
			.listResourceStacks(TAG_FILES.prefix(), file -> file.getPath().endsWith(".json"));
		Set<Identifier> tagsNamingACurse = new HashSet<>();

		for (Map.Entry<Identifier, List<Resource>> tagFile : tagFiles.entrySet()) {
			TagKey<Enchantment> tag = TagKey.create(Registries.ENCHANTMENT, TAG_FILES.fileToId(tagFile.getKey()));
			Set<Identifier> loaded = Lists.newArrayList(enchantments.getTagOrEmpty(tag)).stream()
				.map(enchantment -> enchantment.unwrapKey().orElseThrow().identifier())
				.collect(Collectors.toSet());

			helper.assertTrue(enchantments.get(tag).isPresent(), tag.location() + " loaded");

			for (Identifier element : requiredElements(tagFile.getValue())) {
				if (CURSES.contains(element)) {
					tagsNamingACurse.add(tag.location());
				} else {
					helper.assertTrue(loaded.contains(element), tag.location() + " holds " + element);
				}
			}

			CURSES.forEach(curse -> helper.assertFalse(loaded.contains(curse), tag.location() + " holds " + curse));
		}

		helper.assertValueEqual(tagsNamingACurse, TAGS_NAMING_A_CURSE, "the tags whose files name a curse");
		helper.assertValueEqual(enchantments.getTagOrEmpty(EnchantmentTags.CURSE).iterator().hasNext(), false, "the curse tag holds anything");
		helper.succeed();
	}

	@GameTest
	public void maplesEnchantmentsHaveAPlaceInTheTooltipOrder(GameTestHelper helper) {
		List<Identifier> order = Lists.newArrayList(enchantments(helper).getTagOrEmpty(EnchantmentTags.TOOLTIP_ORDER)).stream()
			.map(enchantment -> enchantment.unwrapKey().orElseThrow().identifier())
			.toList();

		helper.assertValueEqual(order.size(), ENCHANTMENTS, "enchantments in the tooltip order");
		helper.assertValueEqual(
			order.subList(order.size() - 2, order.size()), List.of(MapleFlavored.id("zephyr"), MapleFlavored.id("blight")), "the order's end"
		);
		helper.succeed();
	}

	/** The ids every data pack's copy of one tag file lists as plain required elements, not tags or optional entries. */
	private static Set<Identifier> requiredElements(List<Resource> files) {
		Set<Identifier> elements = new HashSet<>();

		for (Resource file : files) {
			try (Reader reader = file.openAsReader()) {
				for (JsonElement value : GsonHelper.parse(reader).getAsJsonArray("values")) {
					boolean isElement = value.isJsonPrimitive() && !value.getAsString().startsWith("#");

					if (isElement) elements.add(Identifier.parse(value.getAsString()));
				}
			} catch (IOException exception) {
				throw new UncheckedIOException(exception);
			}
		}

		return elements;
	}

	private static Registry<Enchantment> enchantments(GameTestHelper helper) {
		return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
	}
}
