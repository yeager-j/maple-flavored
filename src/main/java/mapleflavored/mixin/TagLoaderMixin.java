package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.List;
import java.util.Map;
import mapleflavored.enchantments.RemovedEnchantments;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Leaves the removed enchantments out of every enchantment tag as it loads from data packs. A tag that requires an
 * unregistered enchantment fails to load, so without this each vanilla tag naming a removed one would need a replaced
 * copy.
 */
@Mixin(TagLoader.class)
public abstract class TagLoaderMixin {
	@Shadow
	@Final
	private String directory;

	@ModifyReturnValue(method = "load", at = @At("RETURN"))
	private Map<Identifier, List<TagLoader.EntryWithSource>> leaveOutRemovedEnchantments(Map<Identifier, List<TagLoader.EntryWithSource>> tags) {
		return RemovedEnchantments.isTagDirectory(directory) ? RemovedEnchantments.withoutRemovedEntries(tags) : tags;
	}
}
