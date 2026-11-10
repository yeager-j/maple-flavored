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
 * Holds the enchantment of a structure book. The game makes the block entity before it's given one, so a book can be
 * blank, as one a command places without data is; such a book crumbles when a player tries to take it. Clients are
 * sent only how much of the
 * book's lock is still on; {@link BookLocks} decides that on the server.
 */
public class StructureBookBlockEntity extends BlockEntity {
	private static final String ENCHANTMENT = "enchantment";
	private static final String LOCK_REMAINING = "lock_remaining";

	private @Nullable Holder<Enchantment> enchantment;
	// The client's copy of the lock, as last sent. Never saved.
	private float shownLockRemaining;

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

	/**
	 * The share of the book's lock that was still on when the server last sent it: 1 with no key done, down to 0 for a
	 * book that can be taken. For drawing it on the client.
	 */
	public float shownLockRemaining() {
		return shownLockRemaining;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		enchantment = input.read(ENCHANTMENT, Enchantment.CODEC).orElse(null);
		shownLockRemaining = input.getFloatOr(LOCK_REMAINING, 0.0F);
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

		if (level instanceof ServerLevel serverLevel) tag.putFloat(LOCK_REMAINING, BookLocks.remainingShare(serverLevel, worldPosition));

		return tag;
	}
}
