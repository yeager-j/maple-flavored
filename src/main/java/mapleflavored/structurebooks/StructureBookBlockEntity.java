package mapleflavored.structurebooks;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Holds the enchantment of a structure book. A book without one can't be taken. Clients are sent only whether the book
 * is locked; {@link BookLocks} decides that on the server.
 */
public class StructureBookBlockEntity extends BlockEntity {
	private static final String ENCHANTMENT = "enchantment";
	private static final String LOCKED = "locked";

	private @Nullable Holder<Enchantment> enchantment;
	// The client's copy of the lock, as last sent. Never saved.
	private boolean shownLocked;

	public StructureBookBlockEntity(BlockPos pos, BlockState state) {
		super(StructureBooks.BLOCK_ENTITY, pos, state);
	}

	/** The enchantment the book gives at level I, if it holds one. */
	public Optional<Holder<Enchantment>> enchantment() {
		return Optional.ofNullable(enchantment);
	}

	/** Sets the enchantment the book gives at level I. */
	public void setEnchantment(Holder<Enchantment> enchantment) {
		this.enchantment = enchantment;
		setChanged();
	}

	/** Whether the book was locked when the server last sent it. For drawing it on the client. */
	public boolean isShownLocked() {
		return shownLocked;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		enchantment = input.read(ENCHANTMENT, Enchantment.CODEC).orElse(null);
		shownLocked = input.getBooleanOr(LOCKED, false);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);

		if (enchantment != null) output.store(ENCHANTMENT, Enchantment.CODEC, enchantment);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();

		if (level instanceof ServerLevel serverLevel) tag.putBoolean(LOCKED, BookLocks.isLocked(serverLevel, worldPosition));

		return tag;
	}
}
