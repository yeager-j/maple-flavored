package mapleflavored.structurebooks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import mapleflavored.mixin.ListPoolElementAccessor;
import mapleflavored.mixin.SinglePoolElementAccessor;
import mapleflavored.mixin.StructurePieceInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.ListPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.structures.BuriedTreasurePieces;
import net.minecraft.world.level.levelgen.structure.structures.DesertPyramidPiece;
import net.minecraft.world.level.levelgen.structure.structures.JungleTemplePiece;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Where structures' books go: each in the structure's most important spot, where a player exploring it properly will
 * find it. Buried Treasure and Trail Ruins have no open room, so theirs is in the treasure chest and in one of the ruin's
 * gravel blocks. The Nether Fortress's book is also locked by the Blaze spawners found here, and each Bastion layout's
 * book floats above that layout's own chest. Local coordinates come from vanilla's code-built pieces and templates.
 */
final class StructureBookSpots {
	// In the shaft of the hidden treasure room, one block above the TNT pressure plate.
	private static final BlockPos DESERT_PYRAMID_TREASURE = new BlockPos(10, -10, 10);
	// Above the hidden chest behind the lever puzzle, seen once the puzzle is solved.
	private static final BlockPos JUNGLE_TEMPLE_HIDDEN_CHEST = new BlockPos(9, -2, 10);

	private static final Identifier IGLOO_BASEMENT = Identifier.withDefaultNamespace("igloo/bottom");
	private static final Identifier IGLOO = Identifier.withDefaultNamespace("igloo/top");
	// Both at head height in the room's centre.
	private static final BlockPos IGLOO_BASEMENT_CENTRE = new BlockPos(3, 2, 5);
	private static final BlockPos IGLOO_CENTRE = new BlockPos(3, 2, 4);

	// Most valuable first. A wreck's front half holds only the supply chest.
	private static final List<String> SHIPWRECK_CHESTS = List.of("treasure_chest", "map_chest", "supply_chest");
	private static final String OCEAN_RUIN_CHEST = "chest";

	private static final List<Identifier> WATCHTOWERS = List.of(
		Identifier.withDefaultNamespace("pillager_outpost/watchtower"),
		Identifier.withDefaultNamespace("pillager_outpost/watchtower_overgrown")
	);
	// The top deck's centre at head height, the captain's floor beside the chest.
	private static final BlockPos WATCHTOWER_TOP = new BlockPos(7, 15, 7);

	private static final String CAMPSITE_DIRECTORY = "abandoned_camp/camp/";
	private static final String TENT_ENTRANCE = "minecraft:connector";

	private static final Identifier END_CITY_LOOT_ROOM = Identifier.withDefaultNamespace("end_city/fat_tower_top");
	private static final Identifier END_CITY_TOWER_TOP = Identifier.withDefaultNamespace("end_city/tower_top");
	private static final BlockPos END_CITY_LOOT_ROOM_CENTRE = new BlockPos(8, 2, 8);
	// At head height over the top of the tower's spiral stairs, under the Shulker that guards the cap room.
	private static final BlockPos END_CITY_TOWER_TOP_CENTRE = new BlockPos(4, 2, 4);

	// Two above the courtyard's lava well, at head height for a player standing on the well's rim.
	private static final BlockPos FORTRESS_LAVA_WELL = new BlockPos(6, 7, 6);
	// The centre of the bridge crossing every fortress starts from, at head height.
	private static final BlockPos FORTRESS_START_CROSSING = new BlockPos(9, 6, 9);
	// Where a fortress's spawner room places its Blaze spawner.
	private static final BlockPos FORTRESS_BLAZE_SPAWNER = new BlockPos(3, 5, 5);

	private static final String BASTION_HOUSING_COURTYARDS = "bastion/units/center_pieces/";
	private static final Identifier BASTION_STABLE_WALL = Identifier.withDefaultNamespace("bastion/hoglin_stable/walls/wall_base");
	private static final String LOOT_TABLE = "LootTable";
	// The only templates that place a Piglin Brute, one each.
	private static final List<Identifier> BASTION_BRUTES = List.of(
		Identifier.withDefaultNamespace("bastion/mobs/melee_piglin"),
		Identifier.withDefaultNamespace("bastion/mobs/melee_piglin_always")
	);
	private static final int BASTION_KEEPERS = 3;

	private static final List<Identifier> TRAIL_RUINS_TOWERS = IntStream.rangeClosed(1, 5)
		.mapToObj(n -> Identifier.withDefaultNamespace("trail_ruins/tower/tower_" + n))
		.toList();

	private StructureBookSpots() {
	}

	static Optional<BookSpot> desertPyramid(List<StructurePiece> pieces, StructureBookRule.Planning planning) {
		return firstOfType(pieces, DesertPyramidPiece.class).map(piece -> inCodeBuiltPiece(piece, DESERT_PYRAMID_TREASURE));
	}

	static Optional<BookSpot> jungleTemple(List<StructurePiece> pieces, StructureBookRule.Planning planning) {
		return firstOfType(pieces, JungleTemplePiece.class).map(piece -> inCodeBuiltPiece(piece, JUNGLE_TEMPLE_HIDDEN_CHEST));
	}

	/** In the basement lab when the igloo has one, else in the igloo. */
	static Optional<BookSpot> igloo(List<StructurePiece> pieces, StructureBookRule.Planning planning) {
		StructureTemplateManager templates = planning.templates();
		Optional<BookSpot> basement = templatePiece(pieces, templates, IGLOO_BASEMENT).map(piece -> inTemplate(piece, IGLOO_BASEMENT_CENTRE));

		return basement.or(() -> templatePiece(pieces, templates, IGLOO).map(piece -> inTemplate(piece, IGLOO_CENTRE)));
	}

	/** Just above the wreck's most valuable chest. */
	static Optional<BookSpot> shipwreck(List<StructurePiece> pieces, StructureBookRule.Planning planning) {
		Optional<TemplateStructurePiece> wreck = firstOfType(pieces, TemplateStructurePiece.class);

		return wreck.flatMap(piece -> SHIPWRECK_CHESTS.stream()
			.flatMap(chest -> dataMarkers(piece, chest))
			.findFirst()
			.map(marker -> new BookSpot(piece, marker))
		);
	}

	/** Just above the chest of the first ruin that has one, which is the big ruin in a cluster. */
	static Optional<BookSpot> oceanRuin(List<StructurePiece> pieces, StructureBookRule.Planning planning) {
		return templatePieces(pieces)
			.flatMap(ruin -> dataMarkers(ruin, OCEAN_RUIN_CHEST).map(chest -> new BookSpot(ruin, chest.above())))
			.findFirst();
	}

	static Optional<BookSpot> pillagerOutpost(List<StructurePiece> pieces, StructureBookRule.Planning planning) {
		return pieces.stream()
			.filter(PoolElementStructurePiece.class::isInstance)
			.map(PoolElementStructurePiece.class::cast)
			.filter(piece -> placesTemplate(piece, WATCHTOWERS::contains))
			.findFirst()
			.map(watchtower -> inPoolElement(watchtower, WATCHTOWER_TOP));
	}

	/** Floating above the campfire, or at the tent's entrance when the campsite didn't generate. */
	static Optional<BookSpot> abandonedCamp(List<StructurePiece> pieces, StructureBookRule.Planning planning) {
		List<PoolElementStructurePiece> poolPieces = pieces.stream()
			.filter(PoolElementStructurePiece.class::isInstance)
			.map(PoolElementStructurePiece.class::cast)
			.toList();
		Optional<BookSpot> campfire = poolPieces.stream()
			.filter(piece -> placesTemplate(piece, id -> id.getPath().startsWith(CAMPSITE_DIRECTORY)))
			.flatMap(campsite -> blocksIn(campsite, planning.templates(), Blocks.CAMPFIRE).map(fire -> new BookSpot(campsite, fire.above())))
			.findFirst();

		return campfire.or(() -> poolPieces.stream()
			.findFirst()
			.flatMap(tent -> tentEntrance(tent, planning.templates()).map(entrance -> new BookSpot(tent, entrance.above())))
		);
	}

	/**
	 * At the top of a tower: in the highest fat tower's loot room, or the highest thin tower's cap room when the city has
	 * no fat tower. Never on the ship.
	 */
	static Optional<BookSpot> endCity(List<StructurePiece> pieces, StructureBookRule.Planning planning) {
		StructureTemplateManager templates = planning.templates();
		Optional<BookSpot> lootRoom = highestTemplatePiece(pieces, templates, END_CITY_LOOT_ROOM)
			.map(piece -> inTemplate(piece, END_CITY_LOOT_ROOM_CENTRE));

		return lootRoom.or(() -> highestTemplatePiece(pieces, templates, END_CITY_TOWER_TOP)
			.map(piece -> inTemplate(piece, END_CITY_TOWER_TOP_CENTRE))
		);
	}

	/** In the treasure chest, which the treasure places where it comes to rest on stone. */
	static Optional<BookSpot> buriedTreasure(List<StructurePiece> pieces, StructureBookRule.Planning planning) {
		return firstOfType(pieces, BuriedTreasurePieces.BuriedTreasurePiece.class).map(treasure -> {
			BoundingBox box = treasure.getBoundingBox();

			return new BookSpot(treasure, new BlockPos(box.minX(), box.minY(), box.minZ()));
		});
	}

	/** One of the gravel blocks of the ruin's tower, the one piece every Trail Ruin has, chosen at random. */
	static Optional<BookSpot> trailRuins(List<StructurePiece> pieces, StructureBookRule.Planning planning) {
		Optional<PoolElementStructurePiece> tower = pieces.stream()
			.filter(PoolElementStructurePiece.class::isInstance)
			.map(PoolElementStructurePiece.class::cast)
			.filter(piece -> placesTemplate(piece, TRAIL_RUINS_TOWERS::contains))
			.findFirst();

		return tower.flatMap(piece -> {
			List<BlockPos> gravel = blocksIn(piece, planning.templates(), Blocks.GRAVEL).toList();

			if (gravel.isEmpty()) return Optional.empty();

			return Optional.of(new BookSpot(piece, gravel.get(planning.random().nextInt(gravel.size()))));
		});
	}

	static Optional<BookSpot> netherFortress(List<StructurePiece> pieces, StructureBookRule.Planning planning) {
		Optional<BookSpot> lavaWell = firstOfType(pieces, NetherFortressPieces.CastleEntrance.class)
			.map(entrance -> inCodeBuiltPiece(entrance, FORTRESS_LAVA_WELL));

		return lavaWell.or(() -> firstOfType(pieces, NetherFortressPieces.StartPiece.class)
			.map(start -> inCodeBuiltPiece(start, FORTRESS_START_CROSSING)));
	}

	/** Where each of the fortress's spawner rooms will place its Blaze spawner. */
	static List<BlockPos> fortressBlazeSpawners(List<StructurePiece> pieces) {
		return pieces.stream()
			.filter(NetherFortressPieces.MonsterThrone.class::isInstance)
			.map(throne -> inCodeBuiltPiece(throne, FORTRESS_BLAZE_SPAWNER).pos())
			.toList();
	}

	/** The template a jigsaw structure's start piece places, which names its layout. */
	static Optional<Identifier> startTemplate(List<StructurePiece> pieces) {
		if (pieces.isEmpty() || !(pieces.getFirst() instanceof PoolElementStructurePiece start)) return Optional.empty();

		return singleElements(start.getElement())
			.findFirst()
			.flatMap(single -> ((SinglePoolElementAccessor) single).template().left());
	}

	/** Above the treasure chest in the gold pile at the treasure room's heart. */
	static Optional<BookSpot> bastionTreasureRoom(List<StructurePiece> pieces, StructureBookRule.Planning planning) {
		return aboveChest(pieces, planning.templates(), template -> true, BuiltInLootTables.BASTION_TREASURE);
	}

	/** Above the bridge layout's own chest, in the gatehouse at the bridge's end. */
	static Optional<BookSpot> bastionBridge(List<StructurePiece> pieces, StructureBookRule.Planning planning) {
		return aboveChest(pieces, planning.templates(), template -> true, BuiltInLootTables.BASTION_BRIDGE);
	}

	/** Above the chest in the courtyard every housing unit opens onto. */
	static Optional<BookSpot> bastionHousingUnits(List<StructurePiece> pieces, StructureBookRule.Planning planning) {
		return aboveChest(
			pieces,
			planning.templates(),
			template -> template.getPath().startsWith(BASTION_HOUSING_COURTYARDS),
			BuiltInLootTables.BASTION_OTHER
		);
	}

	/** Above a stable's own chest, or the chest in the stables' base wall when no stable with one generated. */
	static Optional<BookSpot> bastionHoglinStables(List<StructurePiece> pieces, StructureBookRule.Planning planning) {
		StructureTemplateManager templates = planning.templates();

		return aboveChest(pieces, templates, template -> true, BuiltInLootTables.BASTION_HOGLIN_STABLE)
			.or(() -> aboveChest(pieces, templates, BASTION_STABLE_WALL::equals, BuiltInLootTables.BASTION_OTHER));
	}

	/**
	 * Where three of the Bastion's Piglin Brutes, its keepers, spawn, chosen at random: the origins of the pieces that
	 * place them. Every Brute when there are fewer.
	 */
	static List<BlockPos> bastionKeepers(List<StructurePiece> pieces, RandomSource random) {
		List<BlockPos> brutes = new ArrayList<>(pieces.stream()
			.filter(PoolElementStructurePiece.class::isInstance)
			.map(PoolElementStructurePiece.class::cast)
			.filter(piece -> placesTemplate(piece, BASTION_BRUTES::contains))
			.map(PoolElementStructurePiece::getPosition)
			.toList());

		Util.shuffle(brutes, random);

		return List.copyOf(brutes.subList(0, Math.min(BASTION_KEEPERS, brutes.size())));
	}

	/** The key of the keeper Brute the piece places, if it places one of the {@code keepers} its book was planned with. */
	static Optional<BlockPos> keeperKey(StructurePiece piece, List<BlockPos> keepers) {
		if (!(piece instanceof PoolElementStructurePiece pool) || !placesTemplate(pool, BASTION_BRUTES::contains)) return Optional.empty();

		return Optional.of(pool.getPosition()).filter(keepers::contains);
	}

	private static Optional<BookSpot> aboveChest(
		List<StructurePiece> pieces,
		StructureTemplateManager templates,
		Predicate<Identifier> template,
		ResourceKey<LootTable> lootTable
	) {
		String table = lootTable.identifier().toString();

		return pieces.stream()
			.filter(PoolElementStructurePiece.class::isInstance)
			.map(PoolElementStructurePiece.class::cast)
			.filter(piece -> placesTemplate(piece, template))
			.flatMap(piece -> blockInfosIn(piece, templates, Blocks.CHEST)
				.filter(chest -> chest.nbt() != null && table.equals(chest.nbt().getStringOr(LOOT_TABLE, "")))
				.map(chest -> new BookSpot(piece, chest.pos().above()))
			)
			.findFirst();
	}

	private static Optional<TemplateStructurePiece> highestTemplatePiece(
		List<StructurePiece> pieces,
		StructureTemplateManager templates,
		Identifier location
	) {
		StructureTemplate template = templates.getOrCreate(location);

		return templatePieces(pieces)
			.filter(piece -> piece.template() == template)
			.max(Comparator.comparingInt(piece -> piece.getBoundingBox().maxY()));
	}

	private static <T extends StructurePiece> Optional<T> firstOfType(List<StructurePiece> pieces, Class<T> type) {
		return pieces.stream().filter(type::isInstance).map(type::cast).findFirst();
	}

	private static Stream<TemplateStructurePiece> templatePieces(List<StructurePiece> pieces) {
		return pieces.stream().filter(TemplateStructurePiece.class::isInstance).map(TemplateStructurePiece.class::cast);
	}

	// The template manager caches templates, so a piece built from a template holds that same instance.
	private static Optional<TemplateStructurePiece> templatePiece(List<StructurePiece> pieces, StructureTemplateManager templates, Identifier location) {
		StructureTemplate template = templates.getOrCreate(location);

		return templatePieces(pieces).filter(piece -> piece.template() == template).findFirst();
	}

	// A list element places several templates at one spot, as an outpost's watchtower does with its overgrowth.
	private static Stream<SinglePoolElement> singleElements(StructurePoolElement element) {
		return switch (element) {
			case SinglePoolElement single -> Stream.of(single);
			case ListPoolElement list -> ((ListPoolElementAccessor) list).elements().stream().flatMap(StructureBookSpots::singleElements);
			default -> Stream.empty();
		};
	}

	private static boolean placesTemplate(PoolElementStructurePiece piece, Predicate<Identifier> location) {
		return singleElements(piece.getElement())
			.anyMatch(single -> ((SinglePoolElementAccessor) single).template().left().filter(location).isPresent());
	}

	private static BookSpot inCodeBuiltPiece(StructurePiece piece, BlockPos local) {
		StructurePieceInvoker world = (StructurePieceInvoker) piece;
		BlockPos pos = new BlockPos(world.worldX(local.getX(), local.getZ()), world.worldY(local.getY()), world.worldZ(local.getX(), local.getZ()));

		return new BookSpot(piece, pos);
	}

	private static BookSpot inTemplate(TemplateStructurePiece piece, BlockPos local) {
		BlockPos pos = piece.templatePosition().offset(StructureTemplate.calculateRelativePosition(piece.placeSettings(), local));

		return new BookSpot(piece, pos);
	}

	// Pool elements place their template rotated about its corner, with no mirror.
	private static BookSpot inPoolElement(PoolElementStructurePiece piece, BlockPos local) {
		BlockPos pos = piece.getPosition().offset(StructureTemplate.transform(local, Mirror.NONE, piece.getRotation(), BlockPos.ZERO));

		return new BookSpot(piece, pos);
	}

	// Data markers sit just above the chest they fill, in the template's open space.
	private static Stream<BlockPos> dataMarkers(TemplateStructurePiece piece, String id) {
		return piece.template()
			.filterBlocks(piece.templatePosition(), piece.placeSettings(), Blocks.STRUCTURE_BLOCK)
			.stream()
			.filter(marker -> marker.nbt() != null && id.equals(marker.nbt().getStringOr("metadata", "")))
			.map(StructureTemplate.StructureBlockInfo::pos);
	}

	private static Stream<BlockPos> blocksIn(PoolElementStructurePiece piece, StructureTemplateManager templates, Block block) {
		return blockInfosIn(piece, templates, block).map(StructureTemplate.StructureBlockInfo::pos);
	}

	private static Stream<StructureTemplate.StructureBlockInfo> blockInfosIn(
		PoolElementStructurePiece piece,
		StructureTemplateManager templates,
		Block block
	) {
		return poolTemplate(piece, templates).stream()
			.flatMap(template -> template.filterBlocks(piece.getPosition(), poolSettings(piece), block).stream());
	}

	private static Optional<BlockPos> tentEntrance(PoolElementStructurePiece tent, StructureTemplateManager templates) {
		return blockInfosIn(tent, templates, Blocks.JIGSAW)
			.filter(jigsaw -> jigsaw.nbt() != null && TENT_ENTRANCE.equals(jigsaw.nbt().getStringOr("name", "")))
			.map(StructureTemplate.StructureBlockInfo::pos)
			.findFirst();
	}

	private static Optional<StructureTemplate> poolTemplate(PoolElementStructurePiece piece, StructureTemplateManager templates) {
		return singleElements(piece.getElement())
			.findFirst()
			.map(single -> ((SinglePoolElementAccessor) single).template().map(templates::getOrCreate, template -> template));
	}

	private static StructurePlaceSettings poolSettings(PoolElementStructurePiece piece) {
		return new StructurePlaceSettings().setRotation(piece.getRotation());
	}
}
