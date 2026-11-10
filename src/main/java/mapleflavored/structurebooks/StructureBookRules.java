package mapleflavored.structurebooks;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import mapleflavored.strongholds.StrongholdIllagers;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/** Which structures hold a book, and where each one's book goes. */
public final class StructureBookRules {
	// Seeds the book's choices apart from the structure's own randomness, so adding a book changes no layout.
	private static final int BOOK_SALT = 0x5B00C;

	private static final StructureBookRule MINESHAFT = new StructureBookRule(
		List.of(Enchantments.EFFICIENCY),
		(pieces, planning) -> MineshaftBookSpots.choose(pieces, planning.terrain(), planning.random()),
		StructureBookRule.Placement.POCKET
	);
	private static final StructureBookRule DESERT_PYRAMID = ordinary(List.of(Enchantments.BLAST_PROTECTION), StructureBookSpots::desertPyramid);
	private static final StructureBookRule JUNGLE_TEMPLE = ordinary(List.of(Enchantments.LOOTING), StructureBookSpots::jungleTemple);
	private static final StructureBookRule IGLOO = ordinary(List.of(Enchantments.FROST_WALKER), StructureBookSpots::igloo);
	private static final StructureBookRule SHIPWRECK = ordinary(List.of(Enchantments.DEPTH_STRIDER), StructureBookSpots::shipwreck);
	private static final StructureBookRule OCEAN_RUIN = ordinary(
		List.of(Enchantments.CHANNELING, Enchantments.IMPALING, Enchantments.LOYALTY, Enchantments.RIPTIDE),
		StructureBookSpots::oceanRuin
	);
	private static final StructureBookRule PILLAGER_OUTPOST = ordinary(
		List.of(Enchantments.MULTISHOT, Enchantments.PIERCING, Enchantments.PROJECTILE_PROTECTION, Enchantments.QUICK_CHARGE),
		StructureBookSpots::pillagerOutpost
	);
	private static final StructureBookRule ABANDONED_CAMP = ordinary(List.of(Enchantments.LUNGE), StructureBookSpots::abandonedCamp);
	private static final StructureBookRule NETHER_FORTRESS = new StructureBookRule(
		List.of(Enchantments.FIRE_PROTECTION),
		StructureBookSpots::netherFortress,
		StructureBookRule.Placement.OPEN,
		(pieces, book, random) -> new BookLocks.Keys(BookLocks.Kind.SPAWNERS, StructureBookSpots.fortressBlazeSpawners(pieces))
	);
	private static final StructureBookRule END_CITY = ordinary(List.of(Enchantments.FEATHER_FALLING), StructureBookSpots::endCity);
	private static final StructureBookRule ANCIENT_CITY = new StructureBookRule(
		List.of(Enchantments.SWIFT_SNEAK),
		StructureBookSpots::ancientCity,
		StructureBookRule.Placement.OPEN,
		(pieces, book, random) -> new BookLocks.Keys(BookLocks.Kind.SHRIEKERS, ShriekerKeepers.choose(book, ShriekerKeepers.origins(pieces), random))
	);
	private static final StructureBookRule WOODLAND_MANSION = ordinary(List.of(Enchantments.FORTUNE), StructureBookSpots::woodlandMansion);
	private static final StructureBookRule STRONGHOLD = new StructureBookRule(
		List.of(Enchantments.UNBREAKING),
		StructureBookSpots::stronghold,
		StructureBookRule.Placement.OPEN,
		(pieces, book, random) -> new BookLocks.Keys(BookLocks.Kind.EVOKERS, StrongholdIllagers.keeperKeys(pieces, random))
	);
	private static final StructureBookRule BURIED_TREASURE = new StructureBookRule(
		List.of(Enchantments.AQUA_AFFINITY, Enchantments.RESPIRATION),
		StructureBookSpots::buriedTreasure,
		StructureBookRule.Placement.IN_CHEST
	);
	private static final StructureBookRule TRAIL_RUINS = new StructureBookRule(
		List.of(Enchantments.SILK_TOUCH),
		StructureBookSpots::trailRuins,
		StructureBookRule.Placement.IN_SUSPICIOUS_GRAVEL
	);

	// Each Bastion layout, named by the template its start piece places, holds its own book.
	private static final Map<Identifier, StructureBookRule> BASTION_LAYOUTS = bastionLayouts();

	/** Every Abandoned Camp, one per biome variant. */
	public static final List<ResourceKey<Structure>> ABANDONED_CAMPS = List.of(
		BuiltinStructures.ABANDONED_CAMP_BAMBOO_JUNGLE,
		BuiltinStructures.ABANDONED_CAMP_BIRCH_FOREST,
		BuiltinStructures.ABANDONED_CAMP_CHERRY_GROVE,
		BuiltinStructures.ABANDONED_CAMP_DAPPLED_FOREST,
		BuiltinStructures.ABANDONED_CAMP_FLOWER_FOREST,
		BuiltinStructures.ABANDONED_CAMP_FOREST,
		BuiltinStructures.ABANDONED_CAMP_MEADOW,
		BuiltinStructures.ABANDONED_CAMP_OLD_GROWTH_BIRCH_FOREST,
		BuiltinStructures.ABANDONED_CAMP_OLD_GROWTH_PINE_TAIGA,
		BuiltinStructures.ABANDONED_CAMP_OLD_GROWTH_SPRUCE_TAIGA,
		BuiltinStructures.ABANDONED_CAMP_PALE_GARDEN,
		BuiltinStructures.ABANDONED_CAMP_SAVANNA,
		BuiltinStructures.ABANDONED_CAMP_SNOWY_TAIGA,
		BuiltinStructures.ABANDONED_CAMP_SPARSE_JUNGLE,
		BuiltinStructures.ABANDONED_CAMP_SWAMP,
		BuiltinStructures.ABANDONED_CAMP_TAIGA,
		BuiltinStructures.ABANDONED_CAMP_WINDSWEPT_FOREST,
		BuiltinStructures.ABANDONED_CAMP_WOODED_BADLANDS
	);

	private static final Map<ResourceKey<Structure>, StructureBookRule> RULES = rules();

	private StructureBookRules() {
	}

	/** The enchantments a structure's book can hold, or none for a structure without a book. */
	public static List<ResourceKey<Enchantment>> enchantmentsFor(ResourceKey<Structure> structure) {
		if (structure.equals(BuiltinStructures.BASTION_REMNANT)) {
			return BASTION_LAYOUTS.values().stream().flatMap(layout -> layout.enchantments().stream()).toList();
		}

		return Optional.ofNullable(RULES.get(structure)).map(StructureBookRule::enchantments).orElse(List.of());
	}

	/**
	 * The structure's planned pieces with its book piece added last, or the same pieces when the structure holds no
	 * book, its rule finds no spot, or the pieces are empty (a structure that failed to generate). A book its rule locks
	 * is planned into {@link BookLocks} too.
	 *
	 * @param seed the world seed
	 * @param chunkPos the chunk the structure starts in
	 * @param dimension the dimension the structure is planned in
	 * @param terrain the terrain the structure is planned into
	 * @param templates the templates the structure's pieces are built from
	 */
	public static PiecesContainer withBook(
		Holder<Structure> structure,
		PiecesContainer pieces,
		long seed,
		ChunkPos chunkPos,
		ResourceKey<Level> dimension,
		TerrainPreview terrain,
		StructureTemplateManager templates
	) {
		if (pieces.isEmpty()) return pieces;

		Optional<StructureBookRule> rule = structure.unwrapKey().flatMap(key -> ruleFor(key, pieces.pieces()));

		if (rule.isEmpty()) return pieces;

		WorldgenRandom random = bookRandom(seed, chunkPos);
		StructureBookRule.Planning planning = new StructureBookRule.Planning(terrain, templates, random);
		Optional<BookSpot> spot = rule.get().spot().spot(pieces.pieces(), planning);

		if (spot.isEmpty()) return pieces;

		ResourceKey<Enchantment> enchantment = rule.get().chooseEnchantment(random);
		BookLocks.Keys keys = rule.get().lock().keys(pieces.pieces(), spot.get().pos(), random);
		int anchorIndex = pieces.pieces().indexOf(spot.get().anchor());
		StructureBookPiece book = new StructureBookPiece(
			spot.get().pos(),
			enchantment,
			anchorIndex,
			spot.get().anchor(),
			rule.get().placement(),
			keys.kind().isKeeper() ? keys.positions() : List.of()
		);

		BookLocks.plan(dimension, spot.get().pos(), keys);

		return new PiecesContainer(Stream.concat(pieces.pieces().stream(), Stream.of(book)).toList());
	}

	// A Bastion's rule is its layout's; a layout this mod doesn't know, such as a datapack's, holds no book.
	private static Optional<StructureBookRule> ruleFor(ResourceKey<Structure> structure, List<StructurePiece> pieces) {
		if (structure.equals(BuiltinStructures.BASTION_REMNANT)) {
			return StructureBookSpots.startTemplate(pieces).map(BASTION_LAYOUTS::get);
		}

		return Optional.ofNullable(RULES.get(structure));
	}

	private static StructureBookRule ordinary(List<ResourceKey<Enchantment>> enchantments, StructureBookRule.SpotRule spot) {
		return new StructureBookRule(enchantments, spot, StructureBookRule.Placement.OPEN);
	}

	private static Map<ResourceKey<Structure>, StructureBookRule> rules() {
		Map<ResourceKey<Structure>, StructureBookRule> rules = new HashMap<>(Map.of(
			BuiltinStructures.MINESHAFT, MINESHAFT,
			BuiltinStructures.MINESHAFT_MESA, MINESHAFT,
			BuiltinStructures.DESERT_PYRAMID, DESERT_PYRAMID,
			BuiltinStructures.JUNGLE_TEMPLE, JUNGLE_TEMPLE,
			BuiltinStructures.IGLOO, IGLOO,
			BuiltinStructures.SHIPWRECK, SHIPWRECK,
			BuiltinStructures.SHIPWRECK_BEACHED, SHIPWRECK,
			BuiltinStructures.OCEAN_RUIN_COLD, OCEAN_RUIN,
			BuiltinStructures.OCEAN_RUIN_WARM, OCEAN_RUIN,
			BuiltinStructures.PILLAGER_OUTPOST, PILLAGER_OUTPOST
		));

		rules.put(BuiltinStructures.FORTRESS, NETHER_FORTRESS);
		rules.put(BuiltinStructures.END_CITY, END_CITY);
		rules.put(BuiltinStructures.ANCIENT_CITY, ANCIENT_CITY);
		rules.put(BuiltinStructures.BURIED_TREASURE, BURIED_TREASURE);
		rules.put(BuiltinStructures.TRAIL_RUINS, TRAIL_RUINS);
		rules.put(BuiltinStructures.WOODLAND_MANSION, WOODLAND_MANSION);
		rules.put(BuiltinStructures.STRONGHOLD, STRONGHOLD);
		ABANDONED_CAMPS.forEach(camp -> rules.put(camp, ABANDONED_CAMP));

		return Map.copyOf(rules);
	}

	private static Map<Identifier, StructureBookRule> bastionLayouts() {
		Map<Identifier, StructureBookRule> layouts = new LinkedHashMap<>();

		layouts.put(bastionStart("treasure/big_air_full"), bastionLayout(Enchantments.FIRE_ASPECT, StructureBookSpots::bastionTreasureRoom));
		layouts.put(bastionStart("bridge/starting_pieces/entrance_base"), bastionLayout(Enchantments.SOUL_SPEED, StructureBookSpots::bastionBridge));
		layouts.put(bastionStart("units/air_base"), bastionLayout(Enchantments.FLAME, StructureBookSpots::bastionHousingUnits));
		layouts.put(bastionStart("hoglin_stable/air_base"), bastionLayout(Enchantments.THORNS, StructureBookSpots::bastionHoglinStables));

		return Collections.unmodifiableMap(layouts);
	}

	// Every layout's book is locked by its keeper Brutes.
	private static StructureBookRule bastionLayout(ResourceKey<Enchantment> enchantment, StructureBookRule.SpotRule spot) {
		return new StructureBookRule(
			List.of(enchantment),
			spot,
			StructureBookRule.Placement.OPEN,
			(pieces, book, random) -> new BookLocks.Keys(BookLocks.Kind.BRUTES, StructureBookSpots.bastionKeepers(pieces, random))
		);
	}

	private static Identifier bastionStart(String path) {
		return Identifier.withDefaultNamespace("bastion/" + path);
	}

	private static WorldgenRandom bookRandom(long seed, ChunkPos chunkPos) {
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));

		random.setLargeFeatureWithSalt(seed, chunkPos.x(), chunkPos.z(), BOOK_SALT);

		return random;
	}
}
