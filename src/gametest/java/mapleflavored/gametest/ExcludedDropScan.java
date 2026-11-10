package mapleflavored.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Reads loot tables as data to find the shape Orichalcum's drop bonus can't handle: an item the bonus must skip, guarded
 * by a chance that grows with an enchantment, where the chance also guards something else.
 *
 * <p>The bonus is skipped on a chance in only two places: the conditions of an entry that is the excluded item itself,
 * and anywhere in a pool whose every entry is an excluded item or empty. A chance anywhere else that an excluded item
 * depends on is a leak: the item would drop more often to an Orichalcum weapon.
 */
final class ExcludedDropScan {
	private static final TagKey<Item> EXCLUDED = TagKey.create(Registries.ITEM, Identifier.parse("maple_flavored:no_orichalcum_bonus"));
	private static final String ENCHANTED_CHANCE = "minecraft:random_chance_with_enchanted_bonus";

	private final HolderLookup.Provider registries;
	private final List<String> leaks = new ArrayList<>();

	private ExcludedDropScan(HolderLookup.Provider registries) {
		this.registries = registries;
	}

	/** Every leak in the table, each described by the excluded drop and where its chance sits. Empty for a safe table. */
	static List<String> leaks(HolderLookup.Provider registries, ResourceKey<LootTable> key) {
		ExcludedDropScan scan = new ExcludedDropScan(registries);

		scan.scanTable(scan.json(key), key.identifier().toString());

		return scan.leaks;
	}

	private JsonObject json(ResourceKey<LootTable> key) {
		LootTable table = registries.lookupOrThrow(Registries.LOOT_TABLE).getOrThrow(key).value();

		return LootTable.DIRECT_CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), table).getOrThrow().getAsJsonObject();
	}

	private void scanTable(JsonObject table, String where) {
		boolean tableChance = functionsHaveChance(table);
		int index = 0;

		for (JsonElement element : array(table, "pools")) {
			JsonObject pool = element.getAsJsonObject();
			String poolWhere = where + " pool " + index++;

			if (givesOnlyExcludedItems(pool)) continue;

			boolean poolChance = tableChance || conditionsHaveChance(pool) || functionsHaveChance(pool);

			for (JsonElement entry : array(pool, "entries")) scanEntry(entry.getAsJsonObject(), poolChance, poolWhere);
		}
	}

	// chanceAbove: whether a chance on the pool or on an entry around this one already guards it.
	private void scanEntry(JsonObject entry, boolean chanceAbove, String where) {
		switch (type(entry)) {
			case "minecraft:item" -> {
				// The entry's own conditions are where the bonus is skipped; its functions' conditions aren't.
				boolean leaks = isExcluded(entry.get("name").getAsString()) && (chanceAbove || functionsHaveChance(entry));

				if (leaks) leaks(entry.get("name").getAsString(), where);
			}
			case "minecraft:alternatives", "minecraft:group", "minecraft:sequence" -> {
				boolean chance = chanceAbove || conditionsHaveChance(entry) || functionsHaveChance(entry);

				for (JsonElement child : array(entry, "children")) scanEntry(child.getAsJsonObject(), chance, where);
			}
			default -> {
				boolean chance = chanceAbove || conditionsHaveChance(entry) || functionsHaveChance(entry);

				if (chance && canGiveExcluded(entry, new HashSet<>())) leaks(entry.toString(), where);

				// A table written inside this one has pools of its own to check.
				if (entry.has("value") && entry.get("value").isJsonObject()) scanTable(entry.getAsJsonObject("value"), where + " inline table");
			}
		}
	}

	private void leaks(String drop, String where) {
		leaks.add(drop + " shares an enchanted chance in " + where);
	}

	private boolean givesOnlyExcludedItems(JsonObject pool) {
		JsonArray entries = array(pool, "entries");

		if (entries.isEmpty()) return false;

		for (JsonElement element : entries) {
			JsonObject entry = element.getAsJsonObject();
			boolean excludedItem = type(entry).equals("minecraft:item") && isExcluded(entry.get("name").getAsString());

			if (!excludedItem && !type(entry).equals("minecraft:empty")) return false;
		}

		return true;
	}

	// Whether the entry, of any type, has a way to give an excluded item. An entry type this doesn't know gives none.
	private boolean canGiveExcluded(JsonObject entry, Set<String> visitedTables) {
		return switch (type(entry)) {
			case "minecraft:item" -> isExcluded(entry.get("name").getAsString());
			case "minecraft:tag" -> tagHasExcluded(entry.get("name").getAsString());
			case "minecraft:alternatives", "minecraft:group", "minecraft:sequence" -> anyCanGiveExcluded(array(entry, "children"), visitedTables);
			case "minecraft:loot_table" -> tableCanGiveExcluded(entry.get("value"), visitedTables);
			default -> false;
		};
	}

	private boolean tableCanGiveExcluded(JsonElement value, Set<String> visitedTables) {
		JsonObject table;

		if (value.isJsonObject()) {
			table = value.getAsJsonObject();
		} else {
			if (!visitedTables.add(value.getAsString())) return false;

			table = json(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(value.getAsString())));
		}

		for (JsonElement pool : array(table, "pools")) {
			if (anyCanGiveExcluded(array(pool.getAsJsonObject(), "entries"), visitedTables)) return true;
		}

		return false;
	}

	private boolean anyCanGiveExcluded(JsonArray entries, Set<String> visitedTables) {
		for (JsonElement entry : entries) {
			if (canGiveExcluded(entry.getAsJsonObject(), visitedTables)) return true;
		}

		return false;
	}

	private static boolean isExcluded(String item) {
		return BuiltInRegistries.ITEM.getValue(Identifier.parse(item)).builtInRegistryHolder().is(EXCLUDED);
	}

	private static boolean tagHasExcluded(String tag) {
		for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, Identifier.parse(tag)))) {
			if (item.is(EXCLUDED)) return true;
		}

		return false;
	}

	// Functions are written in a holder's "modifier" list, each with a condition of its own.
	private boolean functionsHaveChance(JsonObject holder) {
		for (JsonElement function : array(holder, "modifier")) {
			if (function.isJsonObject() && conditionsHaveChance(function.getAsJsonObject())) return true;
		}

		return false;
	}

	private boolean conditionsHaveChance(JsonObject holder) {
		return holder.has("condition") && isOrHoldsEnchantedChance(holder.get("condition"));
	}

	// Whether the condition, or any condition nested in it or named by it, is a chance that grows with an enchantment.
	private boolean isOrHoldsEnchantedChance(JsonElement condition) {
		if (condition.isJsonPrimitive()) return isOrHoldsEnchantedChance(predicateJson(condition.getAsString()));

		JsonObject object = condition.getAsJsonObject();

		if (type(object).equals(ENCHANTED_CHANCE)) return true;

		if (object.has("term") && isOrHoldsEnchantedChance(object.get("term"))) return true;

		for (JsonElement term : array(object, "terms")) {
			if (isOrHoldsEnchantedChance(term)) return true;
		}

		return false;
	}

	private JsonElement predicateJson(String name) {
		ResourceKey<LootItemCondition> key = ResourceKey.create(Registries.PREDICATE, Identifier.parse(name));
		LootItemCondition predicate = registries.lookupOrThrow(Registries.PREDICATE).getOrThrow(key).value();

		return LootItemCondition.DIRECT_CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), predicate).getOrThrow();
	}

	private static String type(JsonObject entry) {
		return entry.get("type").getAsString();
	}

	// The field's elements. Loot data writes a list of one as the element alone, and leaves an empty list out.
	private static JsonArray array(JsonObject object, String field) {
		JsonArray elements = new JsonArray();

		if (!object.has(field)) return elements;

		if (object.get(field).isJsonArray()) return object.getAsJsonArray(field);

		elements.add(object.get(field));

		return elements;
	}
}
