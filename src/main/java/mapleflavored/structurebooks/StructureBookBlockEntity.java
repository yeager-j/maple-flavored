package mapleflavored.structurebooks;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** Holds the enchantment of a structure book. A book without one can't be taken. */
public class StructureBookBlockEntity extends BlockEntity {
	private static final String ENCHANTMENT = "enchantment";

	private @Nullable Holder<Enchantment> enchantment;

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

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		enchantment = input.read(ENCHANTMENT, Enchantment.CODEC).orElse(null);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);

		if (enchantment != null) output.store(ENCHANTMENT, Enchantment.CODEC, enchantment);
	}
}
