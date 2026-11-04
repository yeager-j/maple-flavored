package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.Map;
import mapleflavored.enchantments.RemovedEnchantments;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceManagerRegistryLoadTask;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Leaves the removed enchantments' files out when a registry loads from data packs, so no pack can register them.
 * Network loads aren't touched: a client takes whatever registry its server sends.
 */
@Mixin(ResourceManagerRegistryLoadTask.class)
public abstract class ResourceManagerRegistryLoadTaskMixin {
	// The listing runs in load's first lambda. A renumbered lambda fails this injection at startup, not silently.
	@ModifyExpressionValue(
		method = "lambda$load$0",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/resources/FileToIdConverter;listMatchingResources(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/Map;"
		)
	)
	private Map<Identifier, Resource> leaveOutRemovedEnchantments(Map<Identifier, Resource> files) {
		return RemovedEnchantments.withoutRemovedFiles(files);
	}
}
