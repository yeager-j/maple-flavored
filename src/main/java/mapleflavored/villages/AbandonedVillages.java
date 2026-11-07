package mapleflavored.villages;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosAlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.Clear;

/**
 * Every village is an Abandoned Village, vanilla's zombie village, so finding one doesn't skip progression. Its pieces
 * are placed without hay bales and brewing stands. The rest of the rules are data: the village structure set's spacing,
 * the town centre pools that hold only zombie town centres, the zombie villager pools, the empty hay pile feature and
 * the picked-over village chest tables.
 */
public final class AbandonedVillages {
	private static final String VILLAGE_TEMPLATES = "village/";

	/** Replaces hay bales and brewing stands with air. Added to the placement of every village piece. */
	public static final StructureProcessor STRIPPED_BLOCKS = new RuleProcessor(List.of(
		toAir(Blocks.HAY_BLOCK),
		toAir(Blocks.BREWING_STAND)
	));

	private AbandonedVillages() {
	}

	/** Whether the template is one of vanilla's village pieces. */
	public static boolean isVillageTemplate(Identifier template) {
		return template.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) && template.getPath().startsWith(VILLAGE_TEMPLATES);
	}

	private static ProcessorRule toAir(Block block) {
		return new ProcessorRule(
			new BlockMatchTest(block),
			AlwaysTrueTest.INSTANCE,
			PosAlwaysTrueTest.INSTANCE,
			Blocks.AIR.defaultBlockState(),
			new Clear()
		);
	}
}
