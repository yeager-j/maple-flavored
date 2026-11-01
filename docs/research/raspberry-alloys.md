# Raspberry Flavoured: alloys, materials and progression

Research for Maple Flavored's alloy design. It lists options only. The decision belongs to a later conversation.

## Summary

- **Divine Fragments are not a Raspberry Flavoured item.** The name appears nowhere in the pack's current files or its 168-commit history. Matcha's author credits Raspberry as the inspiration, so the link is real, but the item itself is Matcha's.
- **The closest Raspberry items are Stardust and Soul Beads.** That is my inference. Stardust comes from crushing a Nether Star, and Soul Beads come from destroying spawners. Neither is an alloy ingredient.
- **Raspberry's alloys sit on hosts too, as Matcha's do.** Rose Gold and Steel go on iron, and Electrum, Necromium and Netherite appear to go on diamond. The host idea alone won't separate Maple from either pack.
- **Raspberry differs in what surrounds the alloys:**
  - A heat ladder gates alloying.
  - Enchantments that would compete with material traits are removed.
  - Alloys are used outside gear.
  - Each top alloy has its own special weapon.

## Sources and access

| Source | Used for | Access |
|---|---|---|
| [raspmary/Raspberry-Flavoured](https://github.com/raspmary/Raspberry-Flavoured/tree/4912b305f5fe08e3e5a4c13712e8eb8425be6b52) at `4912b30` ("3.3.1 changes", 2026-08-23) | Recipes, stats, advancement and item text | Cloned, read as data, not run |
| [Modded Minecraft Wiki: Gear](https://moddedmc.wiki/en/project/raspberry-core/latest/docs/rf/gear), [Vanilla Changes](https://moddedmc.wiki/en/project/raspberry-core/latest/docs/rf/vanilla-changes), [FAQ](https://moddedmc.wiki/en/project/raspberry-core/latest/docs/rf/faq) | Stated design intent | Read through a page-summarising fetch tool, so treat wording as paraphrase |
| [cassiancc/Raspberry-Core](https://github.com/cassiancc/Raspberry-Core/tree/f9a33796560abb27b400d2f0fa2b60076a8212e6) at `f9a3379` and [its Modrinth page](https://modrinth.com/mod/raspberry-core) | Companion mod's role | Cloned and searched by keyword, not read in full |
| [Matcha Flavoured on Modrinth](https://modrinth.com/project/matcha-flavoured) | Author's credit to Raspberry | Modrinth API |
| Matcha data pack, extracted to the session scratchpad at `dl/dp` | Matcha recipes | Local files |
| [Matcha wiki: Divine Fragment](https://matchaflavou.red/w/Divine_Fragment) | Item's current behaviour | Read through the same fetch tool |
| [ModDex reviews](https://moddex.gg/modpack/raspberry-flavoured) | Player reception only | Secondary, opinion |

CurseForge refused a direct request with HTTP 403. A fetch tool did return the [pack page](https://www.curseforge.com/minecraft/modpacks/raspberry-flavoured), but it holds only a tagline, links and the file list, and I used it only for those. The [Matcha Fandom alloys page](https://matcha-flavoured-datapack.fandom.com/wiki/Alloys) returned HTTP 402 and I did not use it.

In the links below, `recipes.js` means [`kubejs/server_scripts/recipes.js`](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/server_scripts/recipes.js).

## Divine Fragments

### Facts

- Matcha's author [writes](https://modrinth.com/project/matcha-flavoured) of Raspberry that "Electrum, Shakudo, Hepatizon, and Divine Fragments were directly inspired by this mod". They also praise its "resource economy and progression".
- A case-insensitive search of the pack finds no Divine Fragment:
  - The only item-related "divine" hits are Mining Master's Divine Beryl gem, its ores and its block.
  - That gem's ore feature is [removed from world generation](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/startup_scripts/worldgen.js#L85), and the gem and its blocks are also listed in `config/item_obliterator.json5`.
- `git log -G` over `kubejs/` finds no `divine fragment` or `divine_fragment` in any commit, back to the first on 2023-07-05.
- Raspberry Core's source has no match for "divine", "shakudo" or "hepatizon".
- In Matcha, a [recipe](#matcha-recipes) turns one Nether Star into nine fragments, implemented as glinting `turtle_scute`.
- The Matcha wiki now calls the item **Pith** ("formerly Divine Fragment"). It lists spawners, chests, mob drops and trading as sources.

### Inference

Matcha's fragment looks like a merger of two Raspberry items:

| Raspberry item | Source | Uses |
|---|---|---|
| **Stardust** | [Crushing a Nether Star](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/server_scripts/recipes.js#L3276) | Universal repair material and the [Everlasting Tablet](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/server_scripts/altar_recipes.js#L22-L24) (Unbreaking) |
| **Soul Bead** (from Quark) | Destroying spawners, Vexes, Wraiths, Piglin Alchemist barter | Terrabins, Spirited Exopearls and others |

The shared pattern is a rare material from a Nether Star or a spawner. The key difference is that **Raspberry's are not alloy ingredients**, while Matcha's fragment goes into Electrum and Adamant. I found no statement from either author confirming this mapping.

### Discrepancy

The wiki says a Nether Star yields seven Stardust. The 3.3.1 script yields ten. Prefer the script, though the wiki may describe another version.

## Materials and progression

### Advancement path

These come from the pack's own [advancement files](https://github.com/raspmary/Raspberry-Flavoured/tree/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/config/openloader/data/ZZZ_Advancements/data/raspberry_flavoured/advancements), with text from [its lang file](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/config/openloader/resources/99_RaspberryResources/assets/raspberry/lang/en_us.json). The pack has no quest book that I could find: there are no `.snbt` files, and the advancements fill that role.

- **Main:** wooden pickaxe, furnace, copper pickaxe, iron, Forge, Bronze, Rose Gold.
- **From the Forge:** iron pickaxe upgrade, then the Smithing Table, then the Rose Gold upgrade, then the Steel upgrade, then "all five special modified weapons".
- **Gems and top armour:** polishing, diamond pickaxe and armour, then full sets of Electrum, Necromium and Netherite. Their titles are the gifts of Hermes, Hades and Hephaestus.
- **Machines:** Sturdy Alloy, mixer, Blaze Burner, superheating, Steel, crushing wheels.
- **Nether:** ancient debris, Necromium, Netherite. Separately, Wither, then Stardust.

### Metal and alloy recipes

| Output | Ingredients | Yield | Heat | Forge recipe? |
|---|---|---|---|---|
| Rose Gold | 3 copper, 2 gold, 1 zinc | 1, plus 3 nuggets at 50% | Heated | [Yes](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/server_scripts/recipes.js#L1455-L1459) |
| Bronze | 3 copper, 2 zinc, 1 lead | 6, plus 3 at 50% | Heated | [Yes](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/server_scripts/recipes.js#L1460-L1464) |
| Sturdy Alloy (renamed Andesite Alloy) | Sturdy stone, zinc, lead | 6, plus 3 at 50% | Heated | Not found |
| Steel | 3 iron, 1 lead, 1 coke | 4 | Heated | None found |
| Electrum | 4 gold, 5 silver | 1 | Superheated | None found |
| Necromium | 2 netherite scrap, 3 silver, 4 echo shards | 1 | Superheated | None found |
| Netherite | 4 scrap, 4 gold, 1 magma cream | 1 | Superheated | None found |

Mixing recipes are at [`recipes.js` lines 2987-2994](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/server_scripts/recipes.js#L2987-L2994). "None found" means a search of `recipes.js` found no Forge recipe producing that ingot. Steel's only Forge recipe converts sheets back to ingots.

Two sources conflict with this table:

- A ModDex reviewer says all alloys require a mixer, but Rose Gold and Bronze have Forge recipes in 3.3.1.
- The `machines/steel` advancement's parent is superheating, while the Steel recipe needs only `.heated()`. The advancement order may be guidance rather than a true requirement.

### Tool tiers

Text comes from [`en_us.json`](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/assets/kubejs/lang/en_us.json#L290-L312), and durability from [`durability_modify.js`](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/startup_scripts/durability_modify.js#L60-L145).

| Tier | Material | Durability | Tool trait | Armour trait |
|---|---|---|---|---|
| Pre-iron | Copper (replaces stone) | 350 | Visual oxidation only | Lightning immunity, Aftershock |
| Iron | Iron | 700 | None | Slight knockback resistance |
| Iron | Gold | 150 | Fast, more drops | Slight fire resistance |
| Iron | **Rose Gold** | 1000 | Fast, Silk Touch behaviour, armour-piercing magic damage | Magic resistance |
| Iron | **Steel** | 3000 | Knockback | Knockback resistance (the wiki adds projectile protection) |
| Diamond | Diamond | 2200 | None | Projectile resistance |
| Netherite | **Electrum** | 4000 | Fastest mining, more damage while sprinting | Movement speed |
| Netherite | **Necromium** | 6000 | Slows targets, mining occasionally heals | Weakens attackers |
| Netherite | **Netherite** | 8000 | Ignites targets, auto-smelts | Knockback and fire resistance, fireproof |

Steel's durability exceeds diamond's even though its mining level is only iron's. Durability is not a strict function of tier, and that looks deliberate (inference).

### Hosts

- **Stated:** "Use Rose Gold in a Smithing Table on your Iron equipment", and "Use Steel in a Smithing Table on your Iron tools and Chainmail armor". Diamond is also an upgrade: Diamond Plating on [iron gear](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/server_scripts/recipes.js#L1797-L1806).
- **Inferred for Electrum, Necromium and Netherite on diamond:**
  - Their [horse armour](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/server_scripts/recipes.js#L1789-L1791) upgrades from diamond.
  - [Milling their tools](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/server_scripts/recycling.js#L196-L212) returns diamond shards plus alloy nuggets.
- **Could not confirm:** I did not find the smithing recipes for these tools or armour in the repository. They probably come from the upstream mods' jars, which the repository doesn't contain.

### Enchanting

- The [script](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/server_scripts/forging_enchanting.js#L130-L146) removes Efficiency, Fortune, Silk Touch, Looting, Sharpness, Protection, Mending, Power and others.
- The wiki says experience is gone and that remaining enchantments should be chosen around each material's innate abilities.
- Enchantments come from Enchanted Tablets, applied at a Smithing Table, with [one tablet adding one level](https://moddedmc.wiki/en/project/raspberry-core/latest/docs/rf/vanilla-changes).
- Blank Tablets [are made from Rose Gold and amethyst](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/server_scripts/recipes.js#L2999), so one alloy feeds the enchanting system.

### Other design features

- **Special weapons:** Each of the five top materials has [its own weapon](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/server_scripts/forging_enchanting.js#L5-L60): Rose Gold rapier, Steel mace, Electrum spear, Necromium claymore and Netherite broad axe. Each upgrades from a matching tool using more alloy, withered bone and rose quartz, and enchantments are lost.
- **Alloys outside gear:** Steel makes [hoppers](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/server_scripts/recipes.js#L793), [cauldrons](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/server_scripts/recipes.js#L796), [anvils](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/server_scripts/recipes.js#L799), [minecarts](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/server_scripts/recipes.js#L861) and [machines](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/server_scripts/recipes.js#L758-L761). Rose Gold makes [bombs](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/kubejs/server_scripts/recipes.js#L250) and arrows.
- **Lead as a connecting input:** Lead goes into Bronze, Steel and Sturdy Alloy. Its ore is reachable with a copper pickaxe, so an early metal feeds later recipes.
- **Repair:** One unit of material fully repairs gear in an anvil with [no XP](https://moddedmc.wiki/en/project/raspberry-core/latest/docs/rf/gear).
- **Recycling:** Milling returns host materials and alloy nuggets, which makes upgrades partly reversible.
- **Stated intent:** The wiki says tools and armour have unique properties rather than being incremental upgrades. It names Rose Gold's Silk Touch and Golden armour's fire protection as traits that stay useful. Author-level rationale is otherwise thin. I did not read the pack's Discord.
- **Player reception (opinion, ModDex):** Reviewers praise the intrinsic material system. One wishes Bronze were more useful and finds the jump to Steel large.

## Comparison with Matcha

The Matcha column uses the brief's advancement summary and the recipes below.

| Trait | Raspberry | Matcha |
|---|---|---|
| Knockback | Steel, on iron | Steel, on iron |
| Silk Touch | Rose Gold, on iron | Shakudo, on copper |
| Speed | Electrum, on diamond | Hepatizon, on copper |
| Fortune and Looting | Gold, base material | Electrum, on diamond (stronger) |
| Top-tier identity | Three parallel themes | Adamant ("Divine Constitution") |
| Alloy made in | Forge or heated mixer | Crafting grid or blast furnace (from file paths) |
| Rare material in alloys | No | Yes, in Electrum and Adamant |

### Matcha recipes

These are from `dl/dp/data/matcha/recipe`. Matcha reuses vanilla item IDs, so names are mapped by file name:

- **`crafting/divine_fragment.json`:** 1 Nether Star makes 9 glinting `turtle_scute`.
- **`crafting/electrum_alloy.json`:** 4 gold, 1 scute, 4 prismarine crystals make `heart_of_the_sea`.
- **`crafting/adamant.json`:** 4 netherite scrap, 1 scute, 4 gold make `netherite_ingot`.
- **`crafting/shakudo_alloy.json`:** 4 copper blocks, 2 gold blocks make `shulker_shell`.
- **`smithing/electrum_pickaxe.json`:** Diamond pickaxe plus `heart_of_the_sea` gives an Electrum-named `netherite_pickaxe`.

### Inference

Matcha seems to have rearranged Raspberry's trait palette rather than copied it. Three parallels stand out:

- Shakudo resembles Rose Gold in composition (copper-rich with gold) and in its Silk Touch role, and sits on an earlier host.
- Adamant's recipe matches Raspberry's Netherite with the magma cream replaced by a fragment. The four scrap and four gold also match vanilla Netherite, so this is weak evidence.
- Hepatizon takes Electrum's speed role.

Raspberry has no Shakudo. Hepatizon appears only as a [Design Decor chain item](https://github.com/raspmary/Raspberry-Flavoured/blob/4912b305f5fe08e3e5a4c13712e8eb8425be6b52/config/item_obliterator.json5#L3607) on the obliterator list. So I could not confirm what the credit for those two names refers to, beyond the general idea of themed alloys.

## Options to consider

These are inferences about how the ideas might transfer. None has been tested, and none is a recommendation. Maple has no Create dependency, so anything using mixers, Blaze Burners, milling or sequenced assembly needs a different mechanism.

| # | Option | Idea from Raspberry | Difference from Matcha | Without Create | Risk or open question |
|---|---|---|---|---|---|
| 1 | **Heat ladder** | Heated, then superheated, gates alloy tiers | Station progression instead of grid crafting | A new furnace block or fuel tiers | New blocks and UI, and more work than data-driven recipes |
| 2 | **Parallel families per tier** | Gold, Rose Gold and Steel at iron; three at the top | Choice within a tier rather than a sequence of hosts | Works with smithing recipes alone | Traits need balancing so no choice dominates |
| 3 | **Remove competing enchantments** | Silk Touch, Fortune and similar exist only on materials | Partly shared: Matcha has intrinsic enchantments but its hosts differ | Fabric code or data changes | Large effect on the whole game, so it depends on Maple's enchanting plan |
| 4 | **Alloys with a non-gear economy** | Steel in machines and storage, Rose Gold in tablets | Gives each alloy a reason to exist beyond armour | Crafting recipes for vanilla blocks | Raising vanilla block costs may annoy players |
| 5 | **Early metal as connecting input** | Lead feeds Bronze, Steel and Sturdy Alloy | Adds a resource-economy link | Needs a lead-like ore, or a vanilla substitute | New ore means new world generation and textures |
| 6 | **Exclusive weapon per alloy** | One special form per top material | Matcha has shared forms (claymore, spear, dolabra) | Smithing transforms | More items and models per alloy |
| 7 | **Keep the rare catalyst out of alloying** | Stardust and Soul Beads gate repair, tablets and storage | Direct contrast with Matcha's fragment | Works as plain data | Alloys then need another scarcity source |
| 8 | **Non-linear durability** | Steel beats diamond on durability, not on mining level | Makes durability a trade-off, not a tier | Item components | Harder for players to read at a glance |
| 9 | **Reversible upgrades** | Milling returns host and alloy nuggets | Lowers the cost of trying a family | Grindstone, furnace or custom salvage | Possible duplication exploits |

### Names

Steel and Electrum are in both packs. Rose Gold, Bronze and Necromium are Raspberry's, and Shakudo, Hepatizon and Adamant are Matcha's. Other historical alloys remain open if distinct naming matters.

### Licensing

Per its [CurseForge page](https://www.curseforge.com/minecraft/modpacks/raspberry-flavoured), the pack is All Rights Reserved. Per its [Modrinth page](https://modrinth.com/mod/raspberry-core), Raspberry Core's code is MIT and its assets are All Rights Reserved. These options reuse ideas only. Copying Raspberry assets or scripts into a CC BY-NC-SA mod would need separate permission.

## Not confirmed

- Exact smithing recipes for Rose Gold, Steel, Electrum, Necromium and Netherite gear.
- Which mods supply those recipes. The repository has no mod list or jars, and I did not inspect any.
- In-game behaviour. Nothing was run, so all of this comes from files and documentation.
- The authors' reasoning beyond what the wiki states.
- What Raspberry had before the repository's first commit on 2023-07-05, or in CurseForge builds that differ from the repository.
- The mapping of Matcha's vanilla IDs to its alloy names, beyond file names and the recipes above.
- The current Matcha version's Pith recipes, which may differ from the extracted data pack.
