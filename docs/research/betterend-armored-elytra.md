# BetterEnd Armored Elytra: flight, armor and crafting

Research for Maple Flavored's elytra design. It reports what the code does and which approaches would port to 26.3. It does not choose a penalty value. That decision belongs to the user.

## Summary

- **BetterEnd has a flight penalty value that doesn't change gliding or rockets on 26.3.** The Armored Elytra declares `movementFactor = 0.97`. BCLib multiplies the X and Z of the `input` argument of `LivingEntity.travel` by it.
- **Vanilla gliding ignores that argument.** `travelFallFlying` only reads `input` in its climbable branch, and `updateFallFlyingMovement` never receives it. Glide speed, glide distance and rocket boosts therefore match a vanilla Elytra. The factor only reaches the fluid and climbable paths.
- **Firework Rockets never read the factor at all.** They set the player's velocity directly in a separate method.
- **The declared durability and armor values are also overwritten.** `BaseArmorItem` calls `humanoidArmor` after they are set. My reading is that the Armored Elytra really has 240 durability, 9 armor, 3.5 toughness and 0.2 knockback resistance, rather than 900, about 7.8, about 3.0 and 0.5.
- **A real penalty on 26.3 needs a different hook.** See [What would port to 26.3](#what-would-port-to-263).

All of this comes from reading decompiled code and calculating. I did not run the mod or measure anything in-game.

## Sources and access

| Source | Used for | Access |
|---|---|---|
| `better-end-26.3.8-fabric.jar` in the user's `BetterEnd` Modrinth profile | Items, recipes, license | Decompiled, not run |
| `bclib-26.3.3-fabric.jar`, same profile | Flight mixin, `BCLElytraItem`, `BaseArmorItem` | Decompiled, not run |
| `wover-item-api-26.3.4.jar`, nested in `worldweaver-26.3.4-fabric.jar` | `CustomArmorMaterial` defaults | Decompiled, not run |
| `minecraft-merged-deobf-26.3.jar` in Loom's cache | `LivingEntity`, `FireworkRocketEntity`, `Item`, `Items`, `ArmorMaterial` | Decompiled; `travelFallFlying` also spot-checked with `javap` |

**Decompiler:** Vineflower 1.12.0 (a Fernflower fork), found in `~/.gradle/caches/modules-2/files-2.1/org.vineflower/vineflower/1.12.0/`. Nothing was downloaded. Output is in the session scratchpad and isn't part of the repo.

Line numbers refer to Vineflower's output, not to any upstream source file.

I searched the class files of all four mod jars for `BCLElytraItem`, `getMovementFactor`, `isFallFlying`, `updateFallFlying` and similar names, including the nested Wover jars for the first group. The only flight-related hits are the classes covered here, plus the Hydrothermal Vent described under [Other glider interactions](#other-glider-interactions). Those four jars are the profile's whole `mods` folder.

## The flight code

### Classes involved

| Class | Jar | Role |
|---|---|---|
| `org.betterx.bclib.mixin.common.elytra.LivingEntityMixin` | BCLib | The only code that reads the factor |
| `org.betterx.bclib.items.elytra.BCLElytraItem` | BCLib | Interface declaring `getMovementFactor()` |
| `org.betterx.betterend.item.ArmoredElytra` | BetterEnd | Stores and returns `movementFactor` |
| `org.betterx.betterend.item.CrystaliteElytra` | BetterEnd | Subclass of `ArmoredElytra` |
| `org.betterx.betterend.registry.EndItems` | BetterEnd | Supplies the values |

BetterEnd's own `LivingEntityMixin` and `PlayerMixin` contain nothing about flight.

### The mixin

- **Target:** `LivingEntity.travel(Vec3)`
- **Injection:** `@ModifyVariable` at `@At("HEAD")`, with `argsOnly = true`, `remap = false` and `require = 0`
- **Listed in:** `bclib.mixins.common.json`, so it isn't client-only

```java
double movementFactor = elytra.getMovementFactor();
return moveDelta.multiply(movementFactor, 1.0, movementFactor);
```

It only applies when `isFallFlying()` is true and the chest item is a `BCLElytraItem`.

The variable modified is `travel`'s `input` parameter, which the mixin calls `moveDelta`. It is the movement key input (strafe, vertical, forward), not velocity. The formula is:

```text
input' = (input.x * factor, input.y, input.z * factor)
```

`require = 0` means a failed injection would be silent. **Inference:** it does apply, because `LivingEntity.travel(Vec3)` exists under that name in 26.3. The flight result is the same either way.

### Values by tier

| Item | Registry id | `movementFactor` | Effect on `input` |
|---|---|---|---|
| Vanilla Elytra | `minecraft:elytra` | none | Mixin returns it unchanged |
| Crystalite Elytra | `betterend:elytra_crystalite` | 1.0 | X and Z multiplied by 1.0, so no change |
| Armored Elytra | `betterend:elytra_armored` | 0.97 | X and Z multiplied by 0.97 |

The values come from `EndItems` lines 126 and 128.

## Effect on gliding

### Facts from vanilla 26.3

`LivingEntity.travel` picks one of three paths. The fluid check comes first:

```java
if (this.shouldTravelInFluid(...)) this.travelInFluid(input);
else if (this.isFallFlying()) this.travelFallFlying(input);
else this.travelInAir(input);
```

`travelFallFlying` uses `input` only on a climbable block, and it stops gliding there:

```java
if (this.onClimbable()) { this.travelInAir(input); this.stopFallFlying(); }
else { ... this.setDeltaMovement(this.updateFallFlyingMovement(lastMovement)); ... }
```

`javap` confirms this: `aload_1` feeds `travelInAir` in the climbable branch. `updateFallFlyingMovement(Vec3 movement)` takes the current velocity, not `input`. It depends only on look direction, velocity and gravity, and ends with a fixed drag:

```java
return movement.multiply(0.99F, 0.98F, 0.99F);
```

### What follows

- **Fact:** during a normal glide, `movementFactor` can't reach the velocity calculation. For any tier, glide speed and distance per height lost match a vanilla Elytra.
- **Fact:** the factor can only matter on two paths:
  - One tick in `travelInAir` when gliding reaches a climbable block.
  - `travelInFluid`, if the gliding flag is still set in a fluid. `updateFallFlying` doesn't check for fluids, but I didn't trace every other place that clears the flag.
- **Inference:** the name and the 0.97 suggest a flight penalty was intended. The jar can't show whether it ever worked, so I make no claim about older versions.

The Armored Elytra adds no gravity or drag attribute, so nothing else in it alters the glide.

### Vanilla reference numbers (calculated)

These come from my own restatement of `updateFallFlyingMovement`, not from running the game. It assumes default gravity of 0.08, a fixed pitch, no collisions and steady state. Positive pitch is looking down. Distance covered is about height lost times the ratio.

| Pitch | Horizontal speed (blocks/s) | Sink rate (blocks/s) | Glide ratio |
|---|---|---|---|
| 0° | 30.2 | 3.0 | 10.1 |
| 10° | 32.8 | 3.4 | 9.8 |
| 20° | 39.9 | 4.5 | 8.8 |
| 30° | 50.0 | 6.8 | 7.4 |
| 45° | 64.6 | 13.5 | 4.8 |

These apply unchanged to both BetterEnd tiers during a normal glide.

## Effect on Firework Rocket boosts

### Facts from vanilla 26.3

The boost is in `FireworkRocketEntity.tick`, lines 131 to 143. When the attached entity is gliding, each tick does this on each axis:

```java
movement.add(lookAngle.x * 0.1 + (lookAngle.x * 1.5 - movement.x) * 0.5, ...)
```

- It reads the look direction and current velocity, and writes the velocity with `setDeltaMovement`.
- It never touches `travel` or `input`, so **`movementFactor` has no effect on rocket boosts**.
- Rocket lifetime is `10 * (1 + flightDuration) + nextInt(6) + nextInt(7)` ticks, and no BetterEnd flight code changes it.
- The rocket's explosion damage (`fireworks`) isn't in the `bypasses_armor` tag, so **inference:** the Armored Elytra's armor reduces it. `fly_into_wall` and `fall` are in that tag, so armor doesn't reduce them.

### Vanilla reference numbers (calculated)

Same model as above, applying the rocket then the glide step each tick. Real entity tick order may differ slightly.

| Pitch while boosting | Steady horizontal (blocks/s) | Steady vertical (blocks/s) | Total speed (blocks/s) |
|---|---|---|---|
| 0° | 33.5 | -0.6 | 33.5 |
| -20° (looking up) | 30.6 | +12.8 | 33.1 |
| -45° (looking up) | 22.4 | +25.2 | 33.7 |

## How the item stores armor

### Facts

The armor is a fixed part of the item type, held in its default components. No chestplate is stored inside the item, and combining doesn't copy a chestplate's stats.

`ArmoredElytra.defaultSettings` builds `Item.Properties` with `DataComponents.GLIDER`, a durability and divided armor values. Then `BaseArmorItem.applyArmorSettings` runs later, inside the `super(...)` call:

```java
settings.humanoidArmor(material, type);
if (customAttributes != null) settings.attributes(customAttributes);
```

`ArmoredElytra` passes no `customAttributes`. In vanilla, `humanoidArmor` sets `MAX_DAMAGE`, `ATTRIBUTE_MODIFIERS`, `ENCHANTABLE`, `EQUIPPABLE` and `REPAIRABLE`. Component initializers run in order, each calling `components.set`, so the later call wins.

Inputs to the table below:

- Neither material calls `.durability(...)`, so `CustomArmorMaterial`'s default of 15 applies.
- `ArmorType.CHESTPLATE` has a unit durability of 16.

### Declared and effective values

The effective column is **inference from reading the order of calls**. It wasn't confirmed in-game, and it's worth checking with an item tooltip before relying on it.

| Property | Armored declared | Armored effective | Crystalite declared | Crystalite effective | Vanilla Elytra |
|---|---|---|---|---|---|
| Durability | 900 | 240 (16 × 15) | 650 | 240 (16 × 15) | 432 |
| Armor | 9 / 1.15 ≈ 7.83 | 9 | 8 / 1.2 ≈ 6.67 | 8 | none |
| Toughness | 3.5 / 1.15 ≈ 3.04 | 3.5 | 1.2 / 1.25 = 0.96 | 1.2 | none |
| Knockback resistance | 0.5 | 0.2 | 0.5 | 0.1 | none |
| Repair item | Phantom Membrane | Aeternium Ingot | Enchanted Membrane | Terminite Ingot | Phantom Membrane |
| Damaged when hit | not declared | Yes | not declared | Yes | No |
| Enchantment value | not declared | 40 | not declared | 30 | not checked |
| Fire resistant | Yes | Yes | No | No | not checked |

- **Repair:** `isValidRepairItem` looks unused, because 26.3's `Item` has no such method to override. `REPAIRABLE` comes from the material's tag instead. I didn't read BCLib's `AnvilMenuMixin`, which could alter anvil behaviour.
- **Damaged when hit:** `Equippable`'s `damageOnHurt` defaults to true, and the vanilla Elytra sets it to false.
- **Glide wear:** vanilla's `updateFallFlying` applies the usual 1 durability per 20 ticks. `BCLElytraItem`'s own tick methods aren't called from anywhere in the jars.
- **Survives the overwrite:** `GLIDER`, rarity and fire resistance aren't touched by `humanoidArmor`. `GLIDER` is what makes the item glide, through `LivingEntity.canGlideUsing`.
- **Display:** the equipment asset `assets/betterend/equipment/elytra_armored.json` has both `humanoid` and `wings` layers. BetterEnd also ships render mixins for it, which I did not audit.

## Crafting

| Item | Recipe type | Inputs |
|---|---|---|
| Armored Elytra | `minecraft:smithing_transform` | Base `minecraft:elytra`, addition `betterend:aeternium_ingot`, template `betterend:aeternium_upgrade_smithing_template` |
| Crystalite Elytra | `betterend:infusion`, time 500 | Input `minecraft:elytra`. North Amber Gem; east, south and west Enchanted Membrane; each diagonal Crystal Shards |

Facts from `data/betterend/recipe/`. The smithing recipe uses `TransmuteRecipe.createWithOriginalComponents`. **Inference from that name:** enchantments on the base Elytra carry over. I didn't read the method.

## Other glider interactions

The Hydrothermal Vent's block entity applies `moveRelative(force, POSITIVE_Y)` to gliding entities in its area. It checks `DataComponents.GLIDER`, so it treats every tier the same, including vanilla.

## What would port to 26.3

These are facts about the code. No option here is a recommendation, and the sample multipliers are only there to show scale.

### The item

- **No mixin is needed to make it glide.** `canGlideUsing` only needs `GLIDER`, an `EQUIPPABLE` slot matching the worn slot, and an item that won't break on its next damage.
- **Armor can use `humanoidArmor`, or explicit `ATTRIBUTE_MODIFIERS`.** Set any overrides after `humanoidArmor`, or they are lost as they appear to be in BetterEnd.
- **The vanilla `smithing_transform` recipe type does the upgrade.**

### The flight behaviour

| Approach | Ports? | Glide | Rocket boost | Notes |
|---|---|---|---|---|
| BetterEnd's `input` multiplier on `travel` | Yes, it compiles against 26.3 | No change | No change | Only reaches fluid and climbable paths |
| Scale the return value of private `updateFallFlyingMovement`, or its `0.99F` drag | Yes, by mixin | Large change | Small change | Rockets are affected because this runs after the rocket each tick |
| Item modifier on `Attributes.GRAVITY` | Yes, data component with no mixin | Speed changes, ratio doesn't | Almost no change | Applies whenever worn, including walking, jumping and falling |
| Item modifier on `Attributes.AIR_DRAG_MODIFIER` | Not for gliding | No change | No change | `updateFallFlyingMovement` uses fixed constants, not this attribute |
| Change `FireworkRocketEntity.tick`'s `1.5` or `0.1`, or its `setDeltaMovement` call | Yes, by mixin | No change | Direct change | Needs a chest-item check on the attached entity |

### Sensitivity, calculated with the same model

The multipliers are illustrative. Horizontal drag scales X and Z on each glide step.

| Change | Glide ratio at 0° | Glide speed at 0° (blocks/s) | Held rocket speed at 0° (blocks/s) |
|---|---|---|---|
| Vanilla | 10.1 | 30.2 | 33.5 |
| Horizontal drag × 0.99 | 5.0 | 15.0 | 32.8 |
| Horizontal drag × 0.97 | 2.5 | 7.4 | 31.5 |
| Gravity × 1.25 | 10.1 | 37.8 | 33.5 |
| Rocket power 1.5 → 1.2 | 10.1 | 30.2 | 27.6 |

What the code and these numbers show:

- **The same number means very different things on different hooks.** On BetterEnd's hook, 0.97 changes nothing in a glide. Applied per tick to horizontal velocity, it cuts the glide ratio by about three quarters, because it compounds every tick.
- **More gravity makes a glide faster, not shorter.** The glide step is linear in velocity and gravity, so scaling gravity scales steady speed and leaves the ratio alone. An item modifier on gravity alone won't shorten glide distance per height lost at a given pitch.
- **Glide and rocket penalties are separate.** Horizontal drag barely touches held-rocket speed, and a rocket-only change leaves gliding alone. Getting a chosen effect on both likely means two hooks.

**Inference, for any mixin:** it should go in the common source set and read the same equipped item on both sides. Glide movement and the rocket tick both run on client and server, so applying it on one side only risks rubber-banding.

**Tooling, not confirmed:** `updateFallFlyingMovement` can be hooked with plain Mixin (`@Inject` at `RETURN` with `cancellable = true`, or `@ModifyConstant`). MixinExtras jars are in the Gradle cache, but `fabric-loader-0.20.1.jar` has no top-level match for `ModifyReturnValue`. Check how it's provided before relying on `@ModifyReturnValue`.

## License

- **BetterEnd and BCLib code:** MIT, per each jar's `LICENSE` and `fabric.mod.json`. The notice reads "Copyright (c) 2020 paulevsGitch".
- **Some BetterEnd assets:** CC BY-NC-SA 4.0, with the requested attribution "Team BetterX". `LICENSE.ASSETS` names `textures/gui/infusion.png`, `icon_updater.png` and `lang/de_de.json`.
- **Elytra textures:** not on that list. **Inference:** they fall under the jar's MIT license. Check the upstream repo before reusing them.
- **For Maple Flavored**, which is CC BY-NC-SA 4.0: any reused asset must go in `CREDITS.md`, and any substantial copied code must keep the MIT notice. This document holds only short excerpts, and re-implementing the vanilla-based behaviour needs no BetterEnd code.

## Not verified

- Nothing was run, so the effective item stats and all speeds are from reading code and calculating. The calculated figures assume steady state, a fixed pitch and no collisions.
- Whether the gliding flag survives in fluids, so whether the fluid path is reachable in play.
- Older BetterEnd or BCLib versions, and upstream source. Only these jars were read.
- BetterEnd's render mixins, BCLib's `AnvilMenuMixin`, and how the smithing template is obtained.
