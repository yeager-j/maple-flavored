# Matcha Flavoured villages and villagers

Research notes for Maple Flavored. They describe what Matcha Flavoured does and make no design recommendations.

## Source and confidence

Primary source: [kleiwright/matcha-flavoured](https://github.com/kleiwright/matcha-flavoured), branch `26.3`, commit `71c6e65` (2026-10-30). Paths starting `dp/` are under `MF_datapack/` and paths starting `rp/` are under `MF_resourcepack/`. `dp/data/matcha/function/setup/scoreboard/create_scoreboards.mcfunction` sets `current_version` to 1129, the same version as the 1.12.9 release used in `matcha-heart-system.md`. I did not diff this branch against the release download.

Matcha Flavoured is a data pack and resource pack, not a Fabric mod. The README says "Matcha Flavoured is a datapack", and the Modrinth link it gives is `modrinth.com/datapack/matcha-flavoured`. Modrinth could not be reached from this session, so nothing here comes from the Modrinth page.

Secondary context comes from the repo's `README.md` and `changelog.md`, plus these GitHub pull requests and issues:

- [PR #463](https://github.com/kleiwright/matcha-flavoured/pull/463), "Beacon Kindling rework", merged 2026-10-16
- [PR #509](https://github.com/kleiwright/matcha-flavoured/pull/509), "Optimisations and Multiplayer Compat for Alpha Villages", merged 2026-10-20
- [PR #545](https://github.com/kleiwright/matcha-flavoured/pull/545), "Update villager gifts"
- [Issue #371](https://github.com/kleiwright/matcha-flavoured/issues/371), "Abbey-Style Villages"
- [Issue #320](https://github.com/kleiwright/matcha-flavoured/issues/320), "Wandering Trader Beacon Fails if Game Closed"

Labels:

- **Fact**: directly stated by a cited pack file. Unlabelled statements in tables and lists are facts.
- **Inference**: my reading of how pack code or vanilla Minecraft behaves. I did not check vanilla behaviour against 26.3 code. Vanilla numbers I quote come from memory and are marked as such.
- **Unconfirmed**: could not be established from what I read.

Nothing was run in game. All behaviour below comes from reading files. Structure `.nbt` files were parsed with a small script, which listed their blocks, entities and block entities.

Names: the pack renames things through `rp/assets/minecraft/lang/en_us.json`. An emerald is an **Obol**, wheat is **Straw**, beetroot is **Tomatoes**, an ender pearl is **Stable Void**, a fermented spider eye is **Baked Apple**, and an endermite spawn egg is **Benzene**. The professions renamed are listed under "Professions and trades".

Time: the pack's `dp/data/minecraft/timeline/day.json` and `villager_schedule.json` both have `period_ticks` 72000. A Matcha day is 60 real minutes, so 1 day = 72000 ticks. **Inference:** vanilla's day is 24000 ticks.

## Summary

Every village is replaced by an abandoned "beta" village. These villages have no villagers, beds, workstations or bell, and play horror effects. The main source of villagers is the **Asylum Seeker**, a villager spawn egg the Wandering Trader sells for 1 Obol each, up to 4 per visit. Players call the trader with a craftable **Beacon Kindling**, made from 1 Obol, a campfire and a hay bale. The trader arrives after 10 minutes, stays 5, and each player can light one kindling every 15 minutes. Both steps give Obol advancement rewards, so a single Obol is enough for the first two villagers. Two other villager sources remain: igloo basements, which hold a villager and a zombie villager, and curing natural zombie villagers. Natural brewing is disabled, so Weakness for curing comes from a Nether-crafted splash item or from structures.

Villager trades are fully replaced. Every trade costs or pays Obols, and nothing sells random enchanted books. The Butcher becomes the **Cook**, selling foods and recipes. The Librarian becomes the **Mouthpiece**, trading holy books and Crystal Hearts for fixed-enchantment Ofuda. Employed villagers also get one random favourite-food buy trade. I found no changes to breeding or iron golems. Raids still work: there is no custom raid code, Ominous Bottles are craftable, and patrols are held off for the first 5 Matcha days.

## Villages

### Do villages generate?

Yes, but every vanilla village structure is replaced by an abandoned one. In-game, the maps call them "Abandoned Plains Village Map", "Abandoned Desert Village Map" and so on (`rp/.../en_us.json`).

| Setting | Value | Source |
| --- | --- | --- |
| Structures in the set | plains, desert, savanna, snowy, taiga, weight 1 each | `dp/data/minecraft/worldgen/structure_set/villages.json` |
| Placement | `random_spread`, spacing 80, separation 50, salt 10387312 | same |
| Start pool, all 5 types | `minecraft:village_beta/town_centers` | `worldgen/structure/village_*.json` |
| Jigsaw size | 5 | same |
| `max_distance_from_center` | 116 | same |
| `spawn_overrides` | `{}` | same |

Biomes for each type:

| Type | Biomes |
| --- | --- |
| Plains | `plains`, `sunflower_plains` |
| Desert | `desert` |
| Savanna | `savanna` |
| Snowy | `snowy_plains` |
| Taiga | `taiga`, `old_growth_pine_taiga`, `old_growth_spruce_taiga` |

**Inference:** vanilla villages use spacing 34 and separation 8 (from memory). If so, Matcha has about (80/34)² ≈ 5.5 times fewer placement cells per area, so villages are much rarer. Because all five types share one start pool, a village looks the same in every biome.

### What a village contains

`template_pool/village_beta/`: the town centre is a single well (weight 4). Roads are invisible jigsaw-only pieces (`roads/*.nbt` contain only jigsaw blocks). Buildings are chosen from this pool, total weight 24:

| Building | Weight | Suspicious gravel | Notable contents |
| --- | --- | --- | --- |
| `small_1` | 1 | 3 | |
| `medium_1` | 2 | 4 | |
| `medium_2` | 3 | 3 | |
| `large_1` | 2 | 6 | |
| `extra_large_1` | 4 | 8 | Chest holding one "Cyan Rose Classic" item |
| `extra_large_2` | 5 | 8 | |
| `extra_large_3` | 2 | 8 | |
| `extra_large_4` | 1 | 1 | Haunted house, see below |
| `extra_large_5` | 4 | 8 | |

The buildings are oak planks, cobblestone and mossy cobblestone, with cobwebs and pale hanging moss. They contain no villagers, beds, workstations or bell. None of the parsed `village_beta` files contains a bed block or a villager entity. **Inference:** with no beds or job sites, an abandoned village has no POIs for villagers to claim until the player adds them.

`extra_large_4`, chosen 1 time in 24 building picks (about 4.2%), contains:

- A `minecraft:mannequin` tagged `haunted`, with the skin texture `minecraft:entity/player/wide/him`.
- A chest of 3 diamonds, 6 iron ore, 11 coal, 14 cobblestone and a damaged wooden pickaxe.
- A chest of "porkchop_classic" items.
- A jukebox fed by a hopper that holds the "Dry Hands" disc.

These come from `dp/data/minecraft/structure/village_beta/buildings/extra_large_4.nbt`.

### Horror effects

`dp/data/matcha/function/timers/tick.mcfunction` runs `environmental/village/tick` every tick for each player matching `predicate/in_village.json`, which checks for a location inside `#minecraft:village`. Each tick it:

1. Stops the player's music.
2. Plays eerie sounds that depend on the block underfoot (`eerie_sound.mcfunction`).
3. Turns the nearest haunted mannequin to face the player. The mannequin vanishes if a survival player comes within 5 blocks or reaches the jukebox (`entity.mcfunction`), or opens an oak door (`advancement/mechanics/opened_oak_door.json`).
4. Plays Dry Hands from the jukebox once a survival player is within 30 blocks (`jukebox.mcfunction`).

PR #509, "Alpha Villages", added these effects. Its commit titles say `village_eerie_sound` runs only for players inside villages.

### Loot

Village suspicious gravel uses `dp/data/minecraft/loot_table/archaeology/village_plains.json`, which makes 1 roll from a total weight of 19:

| Entry | Weight | Chance per brush |
| --- | --- | --- |
| Emerald (Obol) | 3 | 15.8% |
| Iron ingot | 3 | 15.8% |
| Poppy | 3 | 15.8% |
| Wheat (Straw) | 3 | 15.8% |
| Heart, howl and friend pottery sherds | 2 each | 10.5% each |
| `matcha:archaeology/statues` | 1 | 5.3% |

The chance column is **inference**, my arithmetic at Luck 0.

`matcha:archaeology/statues` gives one Cheerful or one Mournful Clay Statue, as goat horns with weight 1 each. The Archaeologist (cartographer) at level 5 trades either statue for a Crystal Heart. This answers a question `matcha-heart-system.md` left unconfirmed. That note searched for references to `treasure/cheerful_clay_statue`, but the gravel loot points to a different table, `archaeology/statues`. That table is also referenced by `archaeology/desert_well.json`, `trail_ruins_rare.json` and `abbey/grave.json`.

`dp/data/minecraft/loot_table/chests/village/` still holds 14 `village_*` chest tables. **Inference:** none of the `village_beta` structures reference them, since their chests hold fixed items and no `LootTable`. **Unconfirmed:** whether anything else uses them.

## Getting villagers

| Route | Requirements | When it becomes reachable |
| --- | --- | --- |
| Asylum Seeker from a summoned Wandering Trader | 1 Obol, 1 campfire, 1 hay bale (4 Straw), then 1 Obol per seeker | As soon as the player holds an Obol |
| Igloo basement | Find an igloo with a basement | Exploring snowy biomes. **Inference:** the igloo layout is vanilla's |
| Curing a zombie villager | A splash Weakness item, a golden apple, a zombie villager | Weakness needs crimson fungus from the Nether, an igloo, or trial chamber dispensers |
| Wandering Trader that spawns naturally | **Inference:** sells the same Asylum Seekers | Random. **Unconfirmed** |

I found no other route. No loot table, recipe or advancement gives a villager spawn egg. Villagers and zombie villagers appear in no structure except the igloo basement.

### Route 1: Asylum Seekers from a summoned trader

#### Beacon Kindling

- Recipe `dp/data/matcha/recipe/crafting/beacon_kindling.json` is shaped: emerald over campfire over hay block, making 1. The result is a chicken spawn egg that places a `marker` tagged `beacon_kindling`.
- `advancement/recipe_unlocks/obol.json` unlocks the recipe when the player first holds an emerald.
- Campfire recipe: 2 sticks over 2 logs (`recipe/crafting/campfire.json`). Hay block recipe: 4 wheat (`recipe/crafting/hay_block.json`).

Timings come from `function/mechanics/beacon_kindling/` and are checked once every 10 seconds by `10s_loop.mcfunction`:

| Step | Score and value | Real time |
| --- | --- | --- |
| Kindling placed | Marker `Lifetime` = 90, and a signal campfire is set | 0 min |
| Player cooldown | `BeaconKindlingCooldown` += 90. A kindling placed during cooldown is removed and the player is told to wait | 15 min |
| Trader arrives | When marker `Lifetime` ≤ 30, `summon wandering_trader ^1 ^ ^ {Invulnerable:1b,Tags:["summoned_by_beacon"]}` | 10 min |
| Trader leaves | Trader `Lifetime` = 30, then teleported away and killed | 5 min after arrival |
| Kindling goes out | Marker `Lifetime` reaches 0, and the campfire is set to `lit=false` | 15 min |
| Kindling broken or unlit | Marker removed, and its trader is removed if one has already come | Checked every 10 s |

PR #463 states the same 15, 10 and 5 minute timings. A comment in `summon_wandering_trader.mcfunction` says the scoreboard only updates while the marker is loaded. **Inference:** the player must stay near the kindling for the timer to advance.

#### Trader offers

These are set by `dp/data/minecraft/trade_set/wandering_trader/`. **Unconfirmed:** whether vanilla adds other wandering-trader trade sets that this pack does not override.

| Set | Offers | Pool | Trades |
| --- | --- | --- | --- |
| `buying.json` | 4, `allow_duplicates` true | `asylum_seeker` only | 1 Obol for 1 Asylum Seeker, `max_uses` 1 |
| `common.json` | 6 | 21 music-disc trades | The trader buys a disc for 1 to 8 Obols, for example Pigstep 8, Creator 5, Lava Chicken 3, most 2, and 11, 13, 5 and Tears 1 |
| `uncommon.json` | 3 | 6 map trades | 1 Obol each for an Abandoned Village map (5 types) or a witch hut map |

**Inference:** each visit offers 4 Asylum Seekers, so up to 4 villagers per kindling, for 4 Obols.

#### The Asylum Seeker item

`dp/data/matcha/villager_trade/wandering_trader/asylum_seeker.json` gives a `minecraft:villager_spawn_egg` named "Asylum Seeker" (`item_model` `matcha:application`, lore "Click the ground to summon"). Its `given_item_modifier` uses `set_count` rolls to choose attributes, then sets the count back to 1:

| Attribute | Roll | Outcomes | Chance (inference) |
| --- | --- | --- | --- |
| Cause of emigration (lore only) | uniform 1 to 8 | Political Persecution, Religious Persecution, War, Ethnic Persecution, Natural Disaster, Refused to Disclose, The Dead God, Famine | 12.5% each |
| Age | uniform 1 to 9 | 1 to 7 adult, 8 to 9 child | Adult 77.8%, child 22.2% |
| Orphaned (child only) | uniform 1 to 9, on a 1 | Replaces the cause line with "Orphaned" | 1 in 9 children, 2.5% overall |
| Culture | uniform 1 to 6 | None (empty `VillagerData`), desert, swamp, plains, taiga, jungle | 16.7% each |

Further details:

- A child has `Age: -216000`, which is 3 Matcha days or 3 real hours. **Inference:** vanilla babies use -24000.
- Every egg applies Invisibility for 1 tick.
- No culture rolls snowy or savanna.
- **Unconfirmed:** which biome type an egg with "Culture: None" spawns as.
- Using an egg triggers `advancement/mechanics/application.json`, and `application.mcfunction` shows a poof of particles at the nearest villager.

#### Obol cost at the start

Emerald ore no longer drops Obols. It drops prismarine crystals or shards (`dp/data/minecraft/loot_table/blocks/emerald_ore.json`). Early Obols come from:

- the village gravel above, at 15.8% per brush
- zombie villagers, which drop an emerald 1 time in 11 when killed by a player (`entities/zombie_villager.json`, weight 1 against 10 for blaze powder)
- the "Two in One" advancement for crafting a mattock, which gives 1 Obol
- chests such as the desert pyramid, buried treasure, ruins and igloo

Advancement rewards (`dp/data/matcha/advancement/tutorial/`):

| Advancement | Parent | Criterion | Reward |
| --- | --- | --- | --- |
| `obtain_obol` "You're Rich!" | `root` | Hold an emerald | none |
| `light_beacon` "Bonfire Lit" | `obtain_obol` | Use a Beacon Kindling on a block | 1 Obol |
| `trade_for_asylum_seeker` "A Good Samaritan" | `light_beacon` | Trade for a villager spawn egg | 1 Obol |
| `trade_fish` "Fishmonger" | `trade_for_asylum_seeker` | Sell fish to a Fisherman | 1 Obol |
| `trade_everything` "A True Community" | `trade_fish` | Trade with 12 professions (every one except weaponsmith) | 5 Obols |
| `kill_villager` "God help you..." (hidden) | `trade_for_asylum_seeker` | Kill a villager or a wandering trader | none |

**Inference:** one found Obol is enough for two villagers. It crafts the kindling, lighting the kindling refunds 1 Obol, that Obol buys a seeker, and that trade refunds 1 more Obol for a second seeker. The summoned trader is invulnerable, so only a naturally spawned trader can be killed for "God help you...". I found no penalty for that advancement.

### Route 2: Igloo basement

`dp/data/minecraft/structure/igloo/bottom.nbt` is overridden. It contains:

- an unemployed plains `villager` with `PersistenceRequired`
- a `zombie_villager` with cleric profession, already tagged `SpawnChecked` so the spawn filter below does not remove it
- a brewing stand holding 1 splash "Pickled Crimson Fungus", which is Weakness II for 60 s
- a chest marker that uses `chests/igloo_chest.json`

That chest table makes 5 to 8 rolls of leather, tallow, gold nuggets, wheat and emerald, with the emerald at weight 1 of 51. A second pool rolls once from `matcha:food/minecraft/golden_apple`.

**Inference:** a basement therefore gives one free villager plus exactly one cure kit for the zombie villager. Only `bottom.nbt` is overridden, so the igloo structure, its biomes and the basement chance are vanilla. **Unconfirmed:** those vanilla values.

### Route 3: Curing zombie villagers

The data pack contains no curing code. **Inference:** curing works as in vanilla: Weakness plus a golden apple.

Zombie villager spawning:

- Natural monster spawn weight is 5 in most overworld biomes, against a monster total of about 590 to 600. It is 1 in `desert` and 25 in `old_growth_pine_taiga` (`dp/data/minecraft/worldgen/biome/*.json`). **Inference:** 5 of about 600 is about 0.8% of monster spawn picks.
- `function/mechanics/spawn_mechanic/check_mob_spawn.mcfunction` removes non-undead mobs that spawn under open sky. Zombie villagers are undead, so before the dragon dies they can spawn on the surface at night.
- After the first Ender Dragon kill, `first_dragon_killed_reward.mcfunction` sets `gamerule_safe_surface` to 1. From then on, `safe_surface.mcfunction` removes every `#matcha:mundane_hostiles` mob, which includes `zombie_villager`, that spawns where it can see the sky or at y 63 to 350 outside a dungeon.
- Baby zombies, including zombie villagers through `#minecraft:zombies`, get `max_health` 4 on Normal and Easy (`normal_and_easy_modifications/modify_baby_zombies.mcfunction`).

Weakness sources:

- `minecraft:brewing_fuel` is replaced with an empty list (`dp/data/minecraft/tags/item/brewing_fuel.json`). **Inference:** brewing stands, called "Kettle" and crafted from iron, copper and stone, cannot be fuelled, so vanilla Weakness potions cannot be brewed.
- The pack's own splash Weakness is Pickled Crimson Fungus, crafted shapeless from crimson fungus, brown mushroom and glass bottle, giving Weakness II for 60 s (`recipe/food/crafting/pickled_crimson_fungus.json`). Outside the Nether, crimson fungus only appears in `chests/bastion_hoglin_stable.json` among the loot tables I searched.
- Trial chamber dispensers hold 2 to 5 splash or lingering Weakness potions (`loot_table/dispensers/trial_chambers/chamber.json`).
- The igloo basement holds one.
- The Chemist (cleric) sells splash potions, but none are Weakness.

Golden apple: `recipe/food/crafting/golden_apple.json` makes it from 8 gold nuggets around an apple. **Inference:** vanilla needs 8 gold ingots. Its effects are Regeneration and Absorption, and it has no food component.

**Inference, not tested:** a villager that zombies convert after the dragon dies, on the surface, has no `SpawnChecked` tag. It would then be removed by `safe_surface` on its first check.

## Breeding and iron golems

- I found no data-pack change to villager breeding: no function, tag or predicate touches villager willingness, beds or food. **Inference:** vanilla rules apply, but Asylum Seekers and igloo villagers must be given beds, since abandoned villages have none.
- Crop items: the `beetroots` loot table has a villager-only branch that gives 2 tomatoes and 2 tomato seeds when a villager harvests. The changelog says "Villagers can now replant tomatoes", and issue #565 was "farmer villagers can't plant tomatoes". The `wheat` table still drops wheat and seeds. **Unconfirmed:** whether the custom components on carrots and bread affect villagers picking them up or sharing them.
- Iron golems: no function, loot table or tag changes their spawning or drops. `iron_golem` appears only in `tags/entity_type/villager_friends.json` and in sound calls in Abbey door functions. The vanilla iron golem loot table is not overridden.
- The Warding Stone gives Regeneration I for 3 s to every `#matcha:villager_friends` entity within 16 blocks, every 0.5 s. That tag holds villager, wandering trader, iron golem, snow golem, wolf, cat, player and happy ghast. It also slows undead within 24 blocks and deals 7 damage to the nearest one (`function/mechanics/warding_stone/effects.mcfunction`). The recipe is 4 blaze spawn eggs, 4 chiseled stone bricks and 1 blaze rod (`recipe/crafting/warding_stone.json`). "Holy Turret" sits in the Hell advancement tab.

## Professions and trades

### Renamed professions

| Vanilla | Matcha |
| --- | --- |
| Armorer | Engineer |
| Butcher | Cook |
| Cartographer | Archaeologist |
| Cleric | Chemist |
| Leatherworker | Lumberjack |
| Librarian | Mouthpiece |
| Toolsmith | Jeweler |

Farmer, Fisherman, Fletcher, Mason, Shepherd and Weaponsmith keep their names (`rp/.../en_us.json`). No profession is added or removed, and `trade_everything` lists the same 13 vanilla professions minus weaponsmith.

### How trades are defined

Each `dp/data/minecraft/trade_set/<profession>/level_N.json` points to a `#matcha:<profession>/...` tag of files in `dp/data/matcha/villager_trade/`. No vanilla trade remains in those sets.

- Almost every trade costs or pays Obols and has `max_uses` 999.
- Trades named `filler` (18 black wool for 1 Obol) have `given_item_modifier: discard`. **Inference:** they are never offered, so those levels have no trades.
- Several sets have `amount` larger than their pool, for example Cook level 1 with `amount` 3 and 2 trades. **Inference:** the villager offers each trade once.

| Profession | Level 1 | Level 2 | Level 3 | Level 4 | Level 5 |
| --- | --- | --- | --- | --- | --- |
| Cook | 8 Sweet Berry Toast, or its recipe | 4 Warped Stroganoff, or its recipe | 16 Chorus Mochi, or its recipe | Cleaver | none |
| Mouthpiece | Paradise Lost or Divine Comedy for 1 Crystal Heart; 16 Stable Void | Avesta, Quran or Tanakh plus 1 Crystal Heart for an Ofuda | Book of Enoch plus 1 heart for an Ofuda | Lesser Key of Solomon plus 2 hearts for an Ofuda | 2 hearts plus 1 echo shard for Pith |
| Archaeologist | Explorer maps and a sherd | Maps, sherd, brush | Maps; Paradise Lost or Divine Comedy for an Overgrown Abbey map | Ancient city map, sherd | Mansion map, sherd; either Clay Statue for 1 Crystal Heart |
| Engineer | 64 of each of 5 redstone parts | 64 lamps, targets or copper bulbs | 32 to 64 dispensers, droppers, hoppers or observers | 32 crafters or pistons | Geothermal Bundle |
| Chemist | 16 splash potions from 7 types (infested, oozing, pitch, slowness, weaving, wind charged, wither) | none | none | none | none |
| Farmer | 64 bone meal and 4 seed bundles | Fertilized eggs | Calves and piglets | none | none |
| Fisherman | Angler's Almanac | Buys 12 fish of a species for 1 Obol (16 species) | 8 fish for 1 (10 species) | 4 fish for 1 (10 species) | 1 or 2 fish for 1 (11 species) |
| Fletcher | 64 arrows of 3 kinds | 64 spectral arrows | Compound Bow | none | none |
| Lumberjack | 64 logs | 64 logs, which also costs a sapling | 64 stems, some also cost a fungus | Hatchet | none |
| Mason | Bulk stone blocks | Bulk blocks | Bulk blocks | Bulk blocks | none |
| Shepherd | 64 wool of any colour (16 offers) | Shepherd's Shears for 3 Obols; lambs | Crook | none | none |
| Jeweler | Earrings: Opal, Diamond, Amethyst or Lapis | Ruby Circlet | Amber Earrings | Hepatizon Laurel, Topaz Earrings | Buys gems: Topaz 10, Ruby 8, Opal 4, Amber 4 Obols |
| Weaponsmith | Buys 4 diamonds, 1 Hepatizon or 24 lapis blocks for Obols; Electrum for 2 Silver | Recycles 49 gear items into alloy, the grindstone role in the README | none | none | none |

Points relevant to the research question:

- **Cook**: food trades are at levels 1 to 3, at 1 Obol each. Each recipe trade gives a "Cooking Recipe" item. Level 4 sells the Cleaver (`butcher/4/butcher_knife.json`).
- **Crystal Hearts**: the Mouthpiece and Archaeologist trades match `matcha-heart-system.md`. Divine Comedy and Paradise Lost each also buy an Overgrown Abbey map from the Archaeologist at level 3 (`cartographer/3/abbey_*.json`).
- **Diamonds**: the Weaponsmith buys 4 diamonds for 1 Obol (`weaponsmith/1/obol_from_diamond.json`). The Jeweler takes 2 diamonds plus 1 Obol for Diamond Earrings.
- **Enchanted books**: no trade uses `enchant_randomly` or `enchant_with_levels`. The only enchanted books are the five Ofuda, whose enchantments are fixed:

| Ofuda trade | Blessing in lore | Stored enchantments |
| --- | --- | --- |
| `librarian/2/avesta` | Ahura Mazda | Efficiency II, Unbreaking I |
| `librarian/2/quran` | Muhammad | Protection II |
| `librarian/2/tanakh` | Moshe | Loyalty III, Traversal III, Power II |
| `librarian/3/enoch` | Samyaza | Sharpness III |
| `librarian/4/solomon` | Belial | Mending I |

### Favourite food

Every 30 s, `function/favorite_food/trader_update_init.mcfunction` checks employed villagers that are not fishermen, have XP above 0 and lack the `foodChecked` tag. Each one gets one extra trade, chosen at random from 42 foods (`random value 0..41`). In that trade the villager buys the food for 1 Obol, with `maxUses` 1.

**Inference:** this happens after the villager's first trade, since XP must be above 0. PR #449, "Fix Favourite Food Maximum Uses", and issue #444 concern its restock. **Unconfirmed:** the restock behaviour in 1.12.9.

### Amnestic

The Amnestic is crafted shapeless from azure bluet, sugar, glowstone dust and paper (`recipe/crafting/amnestic.json`). Used on a villager, `mechanics/amnestic/villager_consumed.mcfunction`:

- sets the villager to unemployed, level 1, XP 0
- clears its job site
- removes `foodChecked`, so it rolls a new favourite food

This lets players re-roll professions. PR #297 was the "26.3 Amnestic Rework".

## Wandering Trader, igloo villagers and raids

### Wandering Trader

The summoning mechanic is covered under "Route 1". Natural spawning:

- `setup/gamerules.mcfunction` does not change any trader-spawning gamerule.
- **Inference:** natural traders still spawn as in vanilla and draw from the same overridden trade sets, so they also sell Asylum Seekers.
- **Unconfirmed:** that natural traders still spawn in 26.3 with this pack, and whether vanilla wandering-trader trade sets exist beyond `buying`, `common` and `uncommon`.

The summoned trader is spawned without llamas and is invulnerable. Issue #320 reported that the beacon failed if the game was closed. PR #463 says the rework "does not break when tracked entities are unloaded".

### Igloo villagers

Covered under "Route 2".

### Raids

- No raid code was found. The only raid-related setting is `"minecraft:gameplay/can_start_raid": false` in `dp/data/minecraft/dimension_type/the_nether.json`. **Inference:** this matches vanilla.
- Patrols: `dp/data/minecraft/timeline/early_game.json` keeps `can_pillager_patrol_spawn` false until tick 360000, which is 5 Matcha days or 5 real hours. `mushroom_fields.json` keeps patrols off. **Inference:** vanilla uses 120000 ticks, which is also 5 of its own days.
- `recipe/crafting/ominous_bottle.json` is shapeless: endermite spawn egg (Benzene, a Hell-tab item), echo shard and 2 sculk veins, making 1 Ominous Bottle. **Inference:** a player can therefore start a raid at will, once they have built a village with beds for Asylum Seekers.
- Pillager outposts are restyled as overgrown stone ruins, called "Papal Outpost" on maps (`tags/worldgen/structure/on_papal_outpost_explorer_maps.json`). The outpost structure has `spawn_overrides` `{}`. **Inference:** vanilla outposts keep spawning pillagers through a spawn override, so this removes that. The outpost chest table has 3 rolls of bone, glowstone dust, emerald (weight 2) and rabbit hide, plus 1 arrow, 1 goat horn, and a 1 in 4 roll for a Sentry trim.
- Hero of the Village gifts are replaced by pack items in all 15 `loot_table/gameplay/hero_of_the_village/*_gift.json` files, updated in PR #545. Examples: the Cook (butcher) gives a random Matcha meal, the Jeweler earrings, the Archaeologist a Nazar or Wooden Cross, and the Engineer a redstone part.

## Conflicts and open points

- The task described Matcha Flavoured as a Fabric mod. The repo and README describe a data pack plus resource pack.
- The "Culture" roll covers 6 options, but villages spawn in snowy and savanna biomes and no seeker rolls either type. I found no comment explaining this.
- `matcha-heart-system.md` marked the Clay Statue source as unconfirmed. It is the `archaeology/statues` table, rolled from village, desert well, trail ruin and Abbey grave gravel.
- Issue #371 and the "Builders wanted!" issue (#375, open) discuss future Abbey-style villages with tavern and barracks themes such as "Famine" and "War". These match the Asylum Seeker causes, but nothing from them exists in the data I read.

## Not checked

- No behaviour was tested in game, and vanilla 26.3 code and data were not read. Every vanilla comparison is from memory.
- Modrinth and CurseForge pages could not be fetched from this session.
- Village and igloo frequency in a real world, and how often igloos have basements, were not measured.
- The 42 favourite foods' recipes, the Cooking Recipe items, and the contents of Farmer bundles were not traced.
- I did not check whether villagers can use the pack's custom bread and carrots for breeding.
- Remaining `village_beta` NBT details beyond blocks, entities and block entities, such as exact positions, were not examined.
