package mapleflavored.gametest.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import java.lang.reflect.Method;
import mapleflavored.gametest.RunsAlone;
import mapleflavored.gametest.TestBatches;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Gives a {@link RunsAlone} test an environment of its own as Fabric turns its {@code @GameTest} into a test instance.
 * Fabric looks the environment up by the name in the annotation, and has no hook for choosing another.
 */
@Mixin(targets = "net.fabricmc.fabric.impl.gametest.TestAnnotationLocator$TestMethod", remap = false)
public abstract class TestMethodMixin {
	@Shadow
	public abstract Method method();

	@Shadow
	abstract Identifier identifier();

	@ModifyExpressionValue(
		method = "testData",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;getOrThrow(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/core/Holder$Reference;")
	)
	private Holder.Reference<TestEnvironmentDefinition<?>> runAloneWhenMarked(
		Holder.Reference<TestEnvironmentDefinition<?>> declared, @Local(argsOnly = true) Registry<TestEnvironmentDefinition<?>> environments
	) {
		if (!method().isAnnotationPresent(RunsAlone.class)) return declared;

		return TestBatches.environmentOfItsOwn(environments, identifier(), declared);
	}
}
