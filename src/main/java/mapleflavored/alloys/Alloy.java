package mapleflavored.alloys;

import com.mojang.serialization.Codec;
import java.util.Objects;
import mapleflavored.items.MapleItems;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.jspecify.annotations.Nullable;

/**
 * The five Alloys. Each is made in an Alloy Furnace from a Netherite Ingot and one storage block, and has its own ingot,
 * a block that stores 9 of them, and a version of every Netherite item ({@link AlloyGear}).
 */
public enum Alloy implements StringRepresentable {
	/** Made from a block of iron. */
	STEEL("steel", MapColor.METAL, AlloyTraits.STEEL),
	/** Made from a block of gold. */
	ELECTRUM("electrum", MapColor.GOLD, AlloyTraits.ELECTRUM),
	/** Made from a block of copper. */
	SHAKUDO("shakudo", MapColor.COLOR_ORANGE, AlloyTraits.SHAKUDO),
	/** Made from a block of diamond. */
	ADAMANT("adamant", MapColor.DIAMOND, AlloyTraits.ADAMANT),
	/** Made from a block of emerald. */
	ORICHALCUM("orichalcum", MapColor.EMERALD, AlloyTraits.ORICHALCUM);

	/** Reads and writes an Alloy by its {@link #path()}. */
	public static final Codec<Alloy> CODEC = StringRepresentable.fromEnum(Alloy::values);

	// A block of Netherite's hardness, blast resistance and sound.
	private static final float BLOCK_HARDNESS = 50.0F;
	private static final float BLOCK_BLAST_RESISTANCE = 1200.0F;

	private final String path;
	private final MapColor color;
	private final AlloyTraits traits;
	private @Nullable Item ingot;
	private @Nullable Block block;

	Alloy(String name, MapColor color, AlloyTraits traits) {
		this.path = name;
		this.color = color;
		this.traits = traits;
	}

	/** Registers this Alloy's ingot and block. Call once from {@link Alloys#register()}. */
	void register() {
		ingot = MapleItems.register(path + "_ingot", Item::new, new Item.Properties().fireResistant());
		block = Alloys.registerBlock(
			path + "_block",
			Block::new,
			BlockBehaviour.Properties.of()
				.mapColor(color)
				.requiresCorrectToolForDrops()
				.strength(BLOCK_HARDNESS, BLOCK_BLAST_RESISTANCE)
				.sound(SoundType.NETHERITE_BLOCK),
			new Item.Properties().fireResistant()
		);
	}

	/** The Alloy's name in ids, such as {@code steel}. */
	public String path() {
		return path;
	}

	@Override
	public String getSerializedName() {
		return path;
	}

	/** The plain-number traits this Alloy's gear has on top of Netherite's. */
	public AlloyTraits traits() {
		return traits;
	}

	/** This Alloy's ingot. Throws before {@link Alloys#register()} has run. */
	public Item ingot() {
		return Objects.requireNonNull(ingot, "Alloys.register() hasn't run");
	}

	/** This Alloy's storage block, which works as a beacon base. Throws before {@link Alloys#register()} has run. */
	public Block block() {
		return Objects.requireNonNull(block, "Alloys.register() hasn't run");
	}
}
