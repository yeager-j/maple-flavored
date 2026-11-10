package mapleflavored.gametest;

import java.util.List;
import mapleflavored.gametest.mixin.GameTestHelperAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Which batch a GameTest runs in. Vanilla runs the tests that name the same environment in the same dimension together
 * as one batch, so a test runs alone only when no other test names its environment.
 */
public final class TestBatches {
	private TestBatches() {
	}

	/**
	 * Registers an environment only {@code test} uses, which sets up what {@code declared} does, and returns it. This is
	 * what puts a {@link RunsAlone} test in its own batch.
	 */
	public static Holder.Reference<TestEnvironmentDefinition<?>> environmentOfItsOwn(
		Registry<TestEnvironmentDefinition<?>> environments, Identifier test, Holder<TestEnvironmentDefinition<?>> declared
	) {
		ResourceKey<TestEnvironmentDefinition<?>> key = ResourceKey.create(Registries.TEST_ENVIRONMENT, test.withPrefix("alone/"));

		return ((WritableRegistry<TestEnvironmentDefinition<?>>) environments)
			.register(key, new TestEnvironmentDefinition.AllOf(List.of(declared)), RegistrationInfo.BUILT_IN);
	}

	/** Fails the test unless it is the only one in its batch. */
	static void assertRunsAlone(GameTestHelper helper, String why) {
		GameTestInstance test = ((GameTestHelperAccessor) helper).maple$testInfo().getTest();
		long testsInBatch = helper.getLevel().registryAccess().lookupOrThrow(Registries.TEST_INSTANCE).stream()
			.filter(other -> other.batch().equals(test.batch()) && other.dimension().equals(test.dimension()))
			.count();

		helper.assertTrue(testsInBatch == 1, why + ", so the test must be marked @RunsAlone; it shares its batch with " + (testsInBatch - 1) + " others");
	}
}
