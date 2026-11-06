# Plan: ticket 33, Alloy gear and numeric traits

Repo `C:\Users\jackson\Developer\maple-flavored` (Fabric, MC 26.3). Ticket `.scratch/maple-flavored/issues/33-alloy-gear-and-numeric-traits.md` (with Jackson's uncommitted 2026-11-06 edits). Spec: "Materials and Alloys", "Material units", "Anvil, grindstone and smithing rules", "Milestones and advancements". Design: `.scratch/maple-flavored-design/issues/06-material-roles-and-alloys.md`, `17-advancements.md`. Branch `33-alloy-gear-and-numeric-traits`. Jackson's doc edits (spec, design 06 and 17, ticket, `docs/project-overview.md`) go into this PR.

## Context / end goal

Ticket 32 made Alloy ingots; right now they do nothing. This ticket turns them into gear: every Netherite item gets an Alloy version per Alloy, made at a Smithing Table, each beating Netherite in its own way, so iron, gold, copper and diamond get an endgame use and no material is strictly best. Jackson's change today gives the two miners distinct jobs: Shakudo is the everyday fast miner (Efficiency V insta-mines stone), Adamant the brute-force one (hard blocks mine like deepslate, and the pickaxe can break bedrock under a full beacon's Haste II). Closing it means a player can take Netherite gear to a Smithing Table with an Alloy ingot, feel the trait, repair and enchant it with that ingot, and switch Alloys later. Tickets 34–37 hang off the items and the `AlloyGear` lookup made here.

Decisions from Jackson (this session): Fabric datagen for the bulk JSON; horse and nautilus armor get the attribute traits; Alloy smithing keeps the **share** of durability left; Shakudo's bonus multiplies the **final** break speed; textures reuse **Matcha** art where it exists, with **Orichalcum a recolour of Electrum**. From the doc edits: Shakudo +30%, Adamant hardness cap and bedrock breaking, three playtest game rules, the challenge **Breaking the Rules**.

## Items

12 Netherite items in 26.3 × 5 Alloys = 60 items, ids `maple_flavored:<alloy>_<kind>`: sword, shovel, pickaxe, axe, hoe, spear, helmet, chestplate, leggings, boots, horse_armor, nautilus_armor. Each is built with **vanilla's own `Item.Properties` helper and Netherite's baselines** (from `Items.java`: sword 3/-2.4, shovel 1.5/-3, pickaxe 1/-2.8, axe 5/-3, hoe -4/0, spear's nine numbers, `humanoidArmor`/`horseArmor`/`nautilusArmor`), `.fireResistant()`, then:

- `.repairable(alloy.ingot())`, so material units, repairs and the table deposit use the own ingot with no extra code.
- A per-Alloy `ToolMaterial` = Netherite's, with Steel durability ×1.5 and Adamant `attackDamageBonus` +1.
- A per-Alloy `ArmorMaterial` = Netherite's, with asset `maple_flavored:<alloy>`, Adamant toughness 4 (+1), Steel knockback resistance 0.2 (+0.1).
- Steel humanoid armor `.durability(netherite × 3 / 2)` (helmet 610, chest 888, legs 832, boots 721; tools 3046). Mob armor has no durability.
- Extra item attribute modifiers (id `maple_flavored:<alloy>_<trait>`), appended to the material's attributes:

| Alloy | Tools and weapons | Armor, per piece (incl. horse/nautilus, body slot) |
|---|---|---|
| Steel | durability ×1.5; `attack_knockback` +0.5 (main hand) | durability ×1.5; knockback resistance 0.2 (folded) |
| Electrum | +1 Enchantment Slot (tag) | +1 Enchantment Slot (tag; humanoid armor only, mob armor isn't enchantable) |
| Shakudo | +30% mining (code, below); `attack_speed` +10% (ADD_MULTIPLIED_TOTAL, main hand) | `movement_speed` +2.5% (ADD_MULTIPLIED_BASE) |
| Adamant | +1 attack damage (folded into base); hardness cap and bedrock (code, below) | toughness 4 (folded) |
| Orichalcum | Netherite numbers (its traits are ticket 34) | Netherite numbers |

Vanilla's tool attribute builder is private, so `AlloyGear` rebuilds the two base modifiers with vanilla's formula (sword/tool: baseline + bonus, speed baseline; spear: `1/duration − 4`). A GameTest pins every Alloy item to its Netherite item so this can't drift.

## Alloy mining (code, both sides)

Mining is predicted on the client and checked on the server (`ServerPlayerGameMode` recomputes `getDestroyProgress`), so every rule below runs in common code and reads the game rules through a synced copy.

- **Game rules** in `MapleGameRules` (category PLAYER, apply at once):
  - `shakudo_mining_bonus`, percent, default 30, range 0–1000.
  - `adamant_hardness_cap`, default 3 (deepslate), range 1–100.
  - `adamant_bedrock_break_time`, ticks, default 300 (15 s), range 1–72000.
- **Sync**: `AlloyMining` keeps a level attachment holding the three values, synced to all players and refreshed each level tick, exactly like `WitherSubspaces.LIGHT_LEVEL` (`src/main/java/mapleflavored/subspaces/WitherSubspaces.java`). `AlloyMining.rules(Level)` reads it on either side, defaulting to the rules' defaults.
- **Shakudo**: `PlayerMixin` `@ModifyReturnValue` on `Player.getDestroySpeed(BlockState)`: holding a Shakudo tool or weapon in the main hand multiplies the result by `1 + bonus/100`. That's the final break speed, after Efficiency, Haste and `block_break_speed` (vanilla multiplies that attribute last too), so Efficiency V: 35 × 1.3 = 45.5 ≥ stone's insta-mine 45. A live game rule can't sit in a static item attribute, hence code; the effect is the same as a final ×1.3 on the attribute.
- **Adamant hardness cap**: new `BlockBehaviourMixin` wraps the `state.getDestroySpeed(level, pos)` read in `BlockBehaviour.getDestroyProgress`: when the main hand holds an Adamant tool whose kind has a mineable tag (pickaxe, axe, shovel, hoe; sword and spear have none) and the block is in that tag and breakable (hardness ≥ 0), hardness becomes `min(hardness, cap)`. Verified in 26.3 data: trial spawner, vault and reinforced deepslate are in no mineable tag; obsidian, crying obsidian, ancient debris, ender chest, respawn anchor, Netherite/Alloy blocks and spawners are in `mineable/pickaxe`. Uncleared spawners stay unbreakable (`SpawnerBlockMixin` returns 0 before reaching vanilla); a Cleared one caps 5 → 3.
- **Bedrock**: the same mixin, at HEAD of `getDestroyProgress`: for `Blocks.BEDROCK`, when the main hand holds an Adamant pickaxe with Efficiency ≥ V and the player has Haste with amplifier ≥ 1, return `1 / breakTime` per tick; otherwise vanilla (0). Nothing else changes the time. Bedrock has no loot table, so it drops nothing. Adventure mode is already blocked by vanilla's `blockActionRestricted`.
- **Breaking the Rules**: `MapleTriggers.BEDROCK_BROKEN` (PlayerTrigger), fired from Fabric's `PlayerBlockBreakEvents.AFTER` when the state is bedrock and the player is in Survival. Advancement `maple_flavored:nether/break_bedrock`: parent `minecraft:nether/create_full_beacon`, challenge, visible, `rewards.experience 100`, icon bedrock, title "Breaking the Rules" (fixed), description "Break bedrock".

## Seams and API (package `mapleflavored.alloys`)

```java
/** The 12 kinds of Netherite gear, each with Netherite's baselines, the vanilla helper that builds it, and its mineable tag. */
public enum GearKind { SWORD, SHOVEL, PICKAXE, AXE, HOE, SPEAR, HELMET, CHESTPLATE, LEGGINGS, BOOTS, HORSE_ARMOR, NAUTILUS_ARMOR;
    Item netheriteItem(); Optional<TagKey<Block>> mineable(); }

/** The Alloy versions of Netherite gear. */
public final class AlloyGear {
    public static Item of(Alloy alloy, GearKind kind);
    public static Optional<Alloy> alloyOf(ItemStack stack);   // also for 34-36
    public static Optional<GearKind> kindOf(ItemStack stack);
    static void register();                                   // from Alloys.register(); creative tabs after each Netherite item
    /** The base's share of durability left, applied to the smithing result. */
    public static ItemStack keepWear(ItemStack base, ItemStack result);
}

/** Shakudo's mining bonus, Adamant's hardness cap and bedrock breaking, with their synced game rules. */
public final class AlloyMining {
    public record Rules(int shakudoBonusPercent, int adamantHardnessCap, int bedrockBreakTicks) {}
    public static Rules rules(Level level);
    public static float destroySpeed(Player player, float vanillaSpeed);                 // Shakudo
    public static float hardness(Player player, BlockState state, float vanillaHardness); // Adamant cap
    public static OptionalDouble bedrockProgress(Player player, BlockState state);        // empty = vanilla
    static void register();
}
```

- `Alloy` gains `toolMaterial()`, `armorMaterial()` and its item trait modifiers (one place per number).
- **Smithing**: plain vanilla `minecraft:smithing_transform` data, no template (26.3's `template` is optional, so the template slot must be empty). One recipe per (Alloy, kind): base = the Netherite item **plus the other four Alloys'** item of that kind, addition = the Alloy ingot, result = that Alloy's item. 60 recipes. Same Alloy + own ingot matches nothing.
  - The existing `SmithingTransformRecipeMixin` already refuses enchanted bases for every smithing transform, and `SmithingMenuMixin`'s refusal flag shows the message, so "enchanted gear refused" needs no new code.
  - New injection in `SmithingTransformRecipeMixin` at `RETURN` of `assemble`: when the result is Alloy gear, `AlloyGear.keepWear(base, result)`: `damage = min(round(baseDamage × newMax / oldMax), newMax − 1)`. Vanilla upgrades (diamond → Netherite) keep vanilla's rule.
  - No salvage: nothing turns Alloy gear back into Netherite gear or an ingot (no recipe exists; tested).
- **Material units**: `MaterialUnits.fullRepairUnits` returns 1 when the material is a Netherite Ingot **or** in `#maple_flavored:alloy_ingots` (today it reaches 1 only because Alloy gear has no crafting recipe).
- **Electrum slot**: `maple_flavored:extra_enchantment_slot` gains the Electrum tools, weapons and humanoid armor.

## Data and assets

- **Fabric datagen** (new): `fabricApi { configureDataGeneration { client = true } }` in `build.gradle`; entrypoint `fabric-datagen` → `mapleflavored.client.datagen.MapleDataGenerator` in the client source set; output committed under `src/main/generated`. Providers, all driven by `AlloyGear`'s list:
  - Item models: `generateFlatItem(HANDHELD)` for tools, `generateSpear`, `generateTrimmableArmorSet` (trim icons for every trim material), `generateFlatItem` for horse/nautilus armor.
  - Recipes: the 60 smithing transforms, written as `SmithingTransformRecipe` objects with an empty template.
  - Item tags: Alloy gear joins `minecraft:swords`, `shovels`, `pickaxes`, `axes`, `hoes`, `spears`, `head_armor`, `chest_armor`, `leg_armor`, `foot_armor`, `cluster_max_harvestables` and the convention tags `c:tools/melee_weapon`, `c:tools/mining_tool`, `c:armors/horse`, `c:armors/nautilus` (the only tags naming Netherite gear; `enchantable/*` and `trimmable_armor` follow from them). Electrum's ten enchantable items are listed in the hand-written `extra_enchantment_slot` tag.
  - Generated paths never overlap hand-written ones (checked: no hand `data/minecraft/tags/item/*`).
- **Hand-written**: `assets/maple_flavored/equipment/<alloy>.json` (layers humanoid, humanoid_leggings, humanoid_baby, horse_body, nautilus_body, like vanilla `netherite.json` minus its trim override); the Breaking the Rules advancement; lang: 60 item names, the advancement, the three game rules.
- **Textures** (download from `kleiwright/matcha-flavoured` at commit `71c6e65`, as ticket 32 did; recolour script in the scratchpad):
  - Steel, Electrum, Shakudo: Matcha's `MF_resourcepack/assets/matcha/textures/item/<alloy>_{sword,shovel,pickaxe,axe,hoe,spear,spear_in_hand,helmet,chestplate,leggings,boots}.png` and `entity/equipment/{humanoid,humanoid_leggings}/<alloy>.png`; Electrum also `electrum_horse_armor.png` and `horse_body/electrum.png`. Unchanged.
  - Adamant: Matcha's Netherite retextures (the same art its Adamant ingot came from): `MF_resourcepack/assets/minecraft/textures/item/netherite_*.png`, `entity/equipment/humanoid/netherite.png`, `humanoid_baby/netherite.png`, `matcha/.../humanoid_leggings/netherite.png`. Unchanged.
  - Orichalcum: every Electrum texture above recoloured emerald green, with the same recolour as `orichalcum_ingot.png`.
  - Gaps Matcha doesn't cover, recoloured into each Alloy's palette: horse armor (item + `horse_body`) for Steel, Shakudo and Adamant from Matcha's Electrum horse armor; nautilus armor (item + `nautilus_body`) for all five from vanilla's Netherite nautilus armor; `humanoid_baby` for the four non-Adamant Alloys from Matcha's Netherite baby layer.
  - Every file listed in `CREDITS.md` (path, author, changed or not). Send Jackson a contact sheet of all 60 icons plus the equipment layers.

## Steps

1. Branch; commit Jackson's doc edits first. `GearKind`, `AlloyGear`, Alloy materials and trait modifiers; register items; creative tabs; `MaterialUnits` rule; `SmithingTransformRecipeMixin` wear injection.
2. `AlloyMining`, its game rules and sync, `PlayerMixin` and `BlockBehaviourMixin` injections, `BEDROCK_BROKEN` trigger and the advancement.
3. Datagen wiring and providers; run `gradlew runDatagen`; commit `src/main/generated`.
4. Equipment JSON, lang, `extra_enchantment_slot`.
5. Textures + credits + contact sheet.
6. GameTests (`AlloyGearGameTests`, `AlloyMiningGameTests`, registered in `src/gametest/resources/fabric.mod.json`).
7. `GLOSSARY.md`: add **Alloy gear** (Netherite gear made into an Alloy at a Smithing Table). Tick ticket boxes, leaving the hand check open.

## Acceptance matrix (GameTests)

Pinning (all 60 items):
- Each Alloy item has its Netherite item's components except: name/model, `repairable` (own ingot), equippable asset, Steel's max damage, and attribute modifiers that differ by exactly that Alloy's item traits.
- Each Alloy item is in every item tag its Netherite item is in.

Smithing, through a real `SmithingMenu` on a placed table with a survival mock player:
| Template | Base | Addition | Expected |
|---|---|---|---|
| empty | Netherite sword | Steel ingot | Steel sword; taking it uses the sword and 1 ingot |
| empty | each Netherite kind × each Alloy (recipe manager) | that ingot | that Alloy's item (60) |
| empty | Steel chestplate | Electrum ingot | Electrum chestplate |
| empty | Steel sword | Steel ingot | nothing |
| upgrade template | Netherite sword | Steel ingot | nothing |
| empty | Sharpness Netherite sword | Steel ingot | nothing; menu reports the enchanted refusal |
| empty | trimmed, renamed Netherite helmet | Adamant ingot | Adamant helmet, trim and name kept |
| empty | Netherite sword, 1015/2031 damage | Steel ingot | Steel sword, 1522 damage |
| empty | Steel sword, 3000/3046 damage | Electrum ingot | Electrum sword, 2000 damage |
| empty | Steel sword | Netherite Ingot | nothing (no salvage); no smelting/blasting recipe takes Alloy gear |

Item traits, observed on a mock player holding or wearing the item (stated literally):
- Steel sword: max damage 3046; player `attack_knockback` 0.5. Steel helmet 610, chestplate 888, leggings 832, boots 721; full set knockback resistance 0.8 (Netherite 0.4).
- Electrum: a level-30 player's table saves 2 enchantments on an Electrum sword and on an Electrum chestplate; 1 on Netherite (through `EnchantingTables`).
- Shakudo: sword attack speed 1.76 (Netherite 1.6); full set movement speed 0.11 (base 0.1); a horse wearing Shakudo horse armor 1.025 × its base.
- Adamant: sword attack damage 9 (Netherite 8); full set toughness 16 (Netherite 12); Adamant horse armor toughness 4.
- Orichalcum sword and chestplate match Netherite's numbers.

Mining (`AlloyMiningGameTests`, a grounded survival mock player, read through `BlockState.getDestroyProgress` and real `handleBlockBreakAction` breaks):
- Shakudo pickaxe, Efficiency V, on stone: progress ≥ 1 (insta-mine); Netherite Efficiency V: < 1. Without Efficiency, Shakudo's progress is 1.3 × Netherite's.
- Game rule `shakudo_mining_bonus` 0: Shakudo equals Netherite, applied without a reload.
- Adamant pickaxe on obsidian: same progress as Netherite on deepslate (hardness 3); on stone and deepslate: equal to Netherite; on reinforced deepslate, a trial spawner and a vault: equal to Netherite; an uncleared spawner: 0. Adamant shovel on obsidian: unchanged (not in its tag). Game rule `adamant_hardness_cap` 50: obsidian back to Netherite speed.
- Bedrock: Adamant pickaxe, Efficiency V, Haste II → progress 1/300 per tick; a real break with STOP_DESTROY at 300 ticks removes the bedrock, drops nothing and grants Breaking the Rules; STOP at 100 ticks is refused. Haste I, Efficiency IV, a Netherite pickaxe, or Adamant shovel → 0. Game rule `adamant_bedrock_break_time` 100 → 1/100.
- Breaking the Rules: a Creative player breaking bedrock doesn't get it; its parent, frame, visibility and 100 XP checked (pattern `CrystalHeartGameTests.assertAdvancement`).

Repairs and enchanting:
- Anvil: Steel sword at max damage + 1 Steel ingot → full repair, 1 ingot used, 0 XP; + Netherite Ingot → nothing; + Electrum ingot → nothing.
- Table: first save on a plain Steel sword takes 1 Steel ingot as its deposit; grindstone returns it.

Then the full suite.

## Checks

`gradlew runDatagen` (output unchanged on a second run), `gradlew build`, `gradlew runGameTest` (JAVA_HOME from `gradle.properties`), full suite green. `larp review --message "Review the diff"`, fix verified findings. PR closes ticket 33. Then `/watch-prs` for Codex.

## Hand check (left open in the ticket)

Shakudo movement speed (with and without sprinting); Shakudo Efficiency V insta-mining stone and an Adamant pickaxe on obsidian and bedrock under a real beacon, with no client/server flicker; changing a mining game rule in game; icons in inventory, trimmed armor icons, spear in hand; worn armor on a player, horse and nautilus; the creative tab order.

## Out of scope

Damage reductions, Piglin gold, Illager neutrality, Orichalcum drops (34); set bonuses (35); Armored Elytra (36); Reinforcement (37); tooltip lines for the coded mining traits; Alloy ingots as trim materials or beacon payment; mobs spawning with Alloy gear.

## Least confident

- Fabric datagen with `client = true` under `splitEnvironmentSourceSets()` on Loom 1.19 for 26.3 (new to this repo); fallback is a datagen source set of its own.
- Whether `SmithingMenu`'s template slot and recipe-property sets behave with template-less recipes in the real menu (data says yes; the menu test proves it).
- The synced rules reaching the client before its first mining tick after a rule change (one tick of disagreement at most; the server's 0.7 tolerance absorbs it except at insta-mine edges).
