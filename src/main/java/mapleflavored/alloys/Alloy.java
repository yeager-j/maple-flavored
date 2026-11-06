package mapleflavored.alloys;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * The five Alloys. Each is made in an Alloy Furnace from a Netherite Ingot and one storage block, and has its own ingot
 * and a block that stores 9 of them.
 */
public enum Alloy {
	/** Made from a block of iron. */
	STEEL("steel", MapColor.METAL),
	/** Made from a block of gold. */
	ELECTRUM("electrum", MapColor.GOLD),
	/** Made from a block of copper. */
	SHAKUDO("shakudo", MapColor.COLOR_PURPLE),
	/** Made from a block of diamond. */
	ADAMANT("adamant", MapColor.DIAMOND),
	/** Made from a block of emerald. */
	ORICHALCUM("orichalcum", MapColor.EMERALD);

	// A block of Netherite's hardness, blast resistance and sound.
	private static final float BLOCK_HARDNESS = 50.0F;
	private static final float BLOCK_BLAST_RESISTANCE = 1200.0F;

	private final Item ingot;
	private final Block block;

	Alloy(String name, MapColor color) {
		ingot = Alloys.registerItem(name + "_ingot", Item::new, new Item.Properties().fireResistant());
		block = Alloys.registerBlock(
			name + "_block",
			Block::new,
			BlockBehaviour.Properties.of()
				.mapColor(color)
				.requiresCorrectToolForDrops()
				.strength(BLOCK_HARDNESS, BLOCK_BLAST_RESISTANCE)
				.sound(SoundType.NETHERITE_BLOCK),
			new Item.Properties().fireResistant()
		);
	}

	/** This Alloy's ingot. */
	public Item ingot() {
		return ingot;
	}

	/** This Alloy's storage block, which works as a beacon base. */
	public Block block() {
		return block;
	}
}
