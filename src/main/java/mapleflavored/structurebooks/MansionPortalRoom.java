package mapleflavored.structurebooks;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * The Woodland Mansion's fake End portal room: a first-floor secret room with a ring of green wool over orange wool,
 * where the mansion's book floats.
 */
public final class MansionPortalRoom {
	private static final String PORTAL_ROOM = "1x2_s2";
	// Vanilla fills each first-floor 1x2 secret room with one of these two, the clean chest room or the portal room.
	private static final List<String> SECRET_ROOMS = List.of("1x2_s1", PORTAL_ROOM);

	/** The fake End portal room's template. */
	static final Identifier TEMPLATE = location(PORTAL_ROOM);

	private MansionPortalRoom() {
	}

	/**
	 * Makes the planned mansion's first first-floor 1x2 secret room its fake End portal room, replacing that piece in
	 * {@code pieces}. Both rooms are the same size and placed the same way, so the room takes the same spot.
	 *
	 * @return whether the mansion has the room, false when its layout has no such secret room
	 */
	public static boolean make(List<WoodlandMansionPiece> pieces, StructureTemplateManager templates) {
		List<StructureTemplate> secretRooms = SECRET_ROOMS.stream().map(room -> templates.getOrCreate(location(room))).toList();
		OptionalInt secretRoom = IntStream.range(0, pieces.size())
			.filter(index -> secretRooms.contains(pieces.get(index).template()))
			.findFirst();

		if (secretRoom.isEmpty()) return false;

		WoodlandMansionPiece room = pieces.get(secretRoom.getAsInt());
		StructurePlaceSettings settings = room.placeSettings();

		pieces.set(
			secretRoom.getAsInt(),
			new WoodlandMansionPiece(templates, PORTAL_ROOM, room.templatePosition(), settings.getRotation(), settings.getMirror())
		);

		return true;
	}

	private static Identifier location(String room) {
		return Identifier.withDefaultNamespace("woodland_mansion/" + room);
	}
}
