package mapleflavored.gametest.mixin;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The running test behind a helper, so a test can listen for its own pass or fail and clean up either way. */
@Mixin(GameTestHelper.class)
public interface GameTestHelperAccessor {
	@Accessor("testInfo")
	GameTestInfo maple$testInfo();
}
