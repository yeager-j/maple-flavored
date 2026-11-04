package mapleflavored.structurebooks;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import mapleflavored.mixin.ListPoolElementAccessor;
import mapleflavored.mixin.SinglePoolElementAccessor;
import mapleflavored.mixin.StructurePieceInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.ListPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.structures.DesertPyramidPiece;
import net.minecraft.world.level.levelgen.structure.structures.JungleTemplePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Where the books of structures without a wake event go: each in the structure's most important spot, where a player
 * exploring it properly will find it. Local coordinates come from vanilla's code-built pieces and templates.
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
	private static final Identifier END_CITY_START = Identifier.withDefaultNamespace("end_city/base_floor");
	private static final BlockPos END_CITY_LOOT_ROOM_CENTRE = new BlockPos(8, 2, 8);
	private static final BlockPos END_CITY_START_CENTRE = new BlockPos(4, 2, 4);

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
		return pieces.stream()
			.filter(TemplateStructurePiece.class::isInstance)
			.map(TemplateStructurePiece.class::cast)
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

	/** In the first fat tower's loot room, or the start house when the city has no fat tower. Never on the ship. */
	static Optional<BookSpot> endCity(List<StructurePiece> pieces, StructureBookRule.Planning planning) {
		StructureTemplateManager templates = planning.templates();
		Optional<BookSpot> lootRoom = templatePiece(pieces, templates, END_CITY_LOOT_ROOM).map(piece -> inTemplate(piece, END_CITY_LOOT_ROOM_CENTRE));

		return lootRoom.or(() -> templatePiece(pieces, templates, END_CITY_START).map(piece -> inTemplate(piece, END_CITY_START_CENTRE)));
	}

	private static <T extends StructurePiece> Optional<T> firstOfType(List<StructurePiece> pieces, Class<T> type) {
		return pieces.stream().filter(type::isInstance).map(type::cast).findFirst();
	}

	// The template manager caches templates, so a piece built from a template holds that same instance.
	private static Optional<TemplateStructurePiece> templatePiece(List<StructurePiece> pieces, StructureTemplateManager templates, Identifier location) {
		StructureTemplate template = templates.getOrCreate(location);

		return pieces.stream()
			.filter(TemplateStructurePiece.class::isInstance)
			.map(TemplateStructurePiece.class::cast)
			.filter(piece -> piece.template() == template)
			.findFirst();
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
		return poolTemplate(piece, templates).stream()
			.flatMap(template -> template.filterBlocks(piece.getPosition(), poolSettings(piece), block).stream())
			.map(StructureTemplate.StructureBlockInfo::pos);
	}

	private static Optional<BlockPos> tentEntrance(PoolElementStructurePiece tent, StructureTemplateManager templates) {
		return poolTemplate(tent, templates).stream()
			.flatMap(template -> template.filterBlocks(tent.getPosition(), poolSettings(tent), Blocks.JIGSAW).stream())
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
