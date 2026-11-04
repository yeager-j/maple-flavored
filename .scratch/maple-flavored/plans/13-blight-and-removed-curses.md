# Plan: 13 Blight and removed curses

## End goal

Blight gives Fire Aspect's weapons a real alternative: Wither instead of fire, so it works on fire-immune mobs and in water, and against players it withers hearts. The two curses go away completely, so the enchantment list is exactly the 43 the Library reads (41 vanilla without curses, plus Zephyr and Blight; a Full Library is all but Mending), and no later ticket has to remember to skip them. Closing the ticket means Blight exists (its book comes from the Wither in ticket 29), the curses can't be obtained or applied by any means, and GameTests pin both.

## What vanilla does (26.3)

- Enchantments are data files in `data/minecraft/enchantment`. Registries load through `ResourceManagerRegistryLoadTask.load`, whose first step lists the files (`lambda$load$0`, `FileToIdConverter.listMatchingResources`).
- The curses are named in five tags: `curse`, `on_random_loot`, `tradeable`, `treasure` and `tooltip_order`. Any data naming a missing enchantment, a tag included, leaves an unbound holder that stops the world loading, so removing the curses means replacing all five.
- The GameTest server enables every available pack, Trade Rebalance included, and Fabric's mod data loads below it, so its curse trades can't be overridden.
- Java code only reads `#minecraft:curse` (grindstone, repair recipe, red tooltip names); an empty tag is harmless. `Enchantments.BINDING_CURSE` is used only by data generators.
- `MobEffectInstance.update`: a re-applied effect with the same amplifier and a longer remaining duration takes the new duration, on the same instance. The effect never ends, so the Withered Hearts timer, which checks `hasEffect` every tick, keeps running. No code is needed for the refresh.
- `Enchantment.areCompatible` checks both enchantments' exclusive sets, so Blight naming Fire Aspect is enough.

## Decisions

1. **Blight** is `data/maple_flavored/enchantment/blight.json`: Fire Aspect's definition (items, primary items, slots, weight, costs, 2 levels, direct-hit requirement), with `post_attack` → `apply_mob_effect` Wither, amplifier 0, 4 s per level, and `exclusive_set: minecraft:fire_aspect`. It is in no tags, so it comes from no table, loot or trade. Lang `enchantment.maple_flavored.blight` = "Blight".
2. **Curses are removed from the registry.** A mixin on `ResourceManagerRegistryLoadTask.lambda$load$0` drops their two files from the listing, whatever pack supplies them. Only resource loads are filtered: a client takes the server's registry over the network unchanged.
3. **Tags.** The five vanilla tags are replaced (`"replace": true`) with vanilla's values minus the curses; `curse` becomes empty.
4. **Trade Rebalance experiment is unsupported.** Its armorer trades name Binding directly, so a world with it enabled fails to load. Jackson chose this over keeping the curses registered. A GameTest-only mixin leaves it out of the test server's packs.

## API sketch

```java
package mapleflavored.enchantments;

/** Vanilla enchantments Maple removes from the game. */
public final class RemovedEnchantments {
	public static final Set<ResourceKey<Enchantment>> KEYS; // binding_curse, vanishing_curse
	/** The registry's file listing without the removed enchantments' files. */
	public static Map<Identifier, Resource> withoutRemoved(Map<Identifier, Resource> files);
}
```

Consumed by `ResourceManagerRegistryLoadTaskMixin` only. Tests read the registry.

## Tests (`EnchantmentGameTests`)

- A level I Blight sword hit gives a cow Wither I for 80 ticks; level II gives 160.
- Repeated hits refresh: hit, wait 60 ticks, hit again; the duration is back to 80 and the cow still has Wither after the first hit's would-be end.
- A player struck twice by Blight 3 s apart withers a heart 5 s after the first hit (the re-hit doesn't reset the timer).
- Exclusivity: Blight and Fire Aspect are incompatible both ways; Blight supports exactly Fire Aspect's items.
- The curses are absent from the registry, which holds 43 enchantments (a Full Library is all but Mending), and the replaced tags keep vanilla's other values.

## Out of scope

The Wither's Blight drop (29), the Library (14/15), Trade Rebalance support.
