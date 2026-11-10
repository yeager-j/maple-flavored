package mapleflavored.structurebooks;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

/**
 * Where a structure's book goes, as planned: a world position inside one of its pieces, and how the book is put there.
 * An unlocked book moves with that piece when the piece settles onto the terrain as it's built.
 *
 * @param anchor the planned piece the book sits in
 * @param pos the book's world position while the pieces are planned
 * @param placement how the book is put at the spot as the chunk is built
 */
public record BookSpot(StructurePiece anchor, BlockPos pos, Placement placement) {
	/** A spot for a floating book that takes the spot as it is, as most structures' books do. */
	public BookSpot(StructurePiece anchor, BlockPos pos) {
		this(anchor, pos, Placement.OPEN);
	}

	/** How a book is put at its spot as the chunk is built. */
	public enum Placement implements StringRepresentable {
		/** A floating book in an open pocket carved around it, sealing any liquid touching it. */
		POCKET("pocket"),
		/** A floating book that takes the spot as it is: it replaces what's there and is waterlogged in water. */
		OPEN("open"),
		/** A book item in the container at the spot, left there by the container's loot when it's rolled. */
		IN_CHEST("in_chest"),
		/** Suspicious gravel at the spot that gives the book when brushed. */
		IN_SUSPICIOUS_GRAVEL("in_suspicious_gravel");

		/** Saves a placement by its {@link #getSerializedName}, so renaming a constant changes no save. */
		public static final Codec<Placement> CODEC = StringRepresentable.fromEnum(Placement::values);

		private final String savedName;

		Placement(String savedName) {
			this.savedName = savedName;
		}

		@Override
		public String getSerializedName() {
			return savedName;
		}
	}
}
