# Matcha Flavoured structures and structure loot

Research notes for Maple Flavored. They describe what Matcha Flavoured does and make no design recommendations.

## Source and confidence

Primary source: the Matcha Flavoured 1.12.9 data pack and resource pack, the same download used in `matcha-heart-system.md`. Paths starting `dp/` or `rp/` are inside the extracted download. English names come from `rp/assets/minecraft/lang/en_us.json`.

Vanilla comparisons come from the Minecraft 26.3 `minecraft-common.jar` in this machine's Fabric Loom cache (`~/.gradle/caches/fabric-loom/26.3/`). I read the vanilla JSON files directly, so vanilla numbers below are from 26.3 data, not from memory, unless marked otherwise.

Structure `.nbt` files were parsed with a small read-only script that listed each template's blocks, block entities and entities. Loot odds were computed with a second script, then checked by hand for every table that has a non-zero result.

Labels:

- **Fact**: directly stated by a cited file. Unlabelled statements in tables and lists are facts.
- **Inference**: my reading of how pack data or vanilla behaves. Vanilla Java code was not read, only vanilla data files.
- **Unconfirmed**: could not be established from what I read.

Nothing was run in game.

Names: an emerald is an **Obol**. `turtle_scute` is **Pith**. A **Divine Fragment** is a `turtle_scute` with a glint and rare rarity (`dp/data/matcha/loot_table/treasure/divine_fragment.json`). A **Crystal Heart** is described in `matcha-heart-system.md`; this note only adds sources.

Scope: the Abbey is covered briefly, as requested. Villages are covered in `matcha-villagers.md` and are only summarised here.

## Summary

Matcha adds one structure, the **Overgrown Abbey**. It restyles four vanilla structures: pillager outposts (now "Papal Outposts"), villages (now abandoned), the woodland mansion's spider room and the igloo basement. It also makes dungeons place a second spawner template. It replaces the loot of 61 vanilla chest, barrel, pot and archaeology tables and adds 15 tables of its own.

Crystal Hearts come from 7 chest tables plus the normal vault, which reaches one of them. Divine Fragments come from 4 chest tables plus the ominous vault, and from **every broken spawner**: Matcha's `blocks/spawner.json` always drops one. Plain, unglinted Pith is in no chest table.

The Papal Outpost is the most heart-rich structure players can find through trades. Its tower holds two Abbey "barracks" chests (1 in 3 heart chance each), a zombie spawner (one guaranteed fragment), and suspicious gravel that can give Clay Statues. A Papal Outpost map costs 1 Obol from a level 1 Archaeologist.

The chicken roost and seagull roost templates are not referenced anywhere in the pack, so they appear to be unused.

## Crystal Heart and Divine Fragment odds by table

Chance per chest at Luck 0, computed from the pool weights and roll counts (**inference**: my arithmetic). A uniform 0 to 1 roll count is treated as 50% for one roll. Quality and bonus rolls only matter with Luck, so they are ignored. All tables are under `dp/data/minecraft/loot_table/chests/`.

| Table | Where used | Crystal Heart | Divine Fragment | Pool detail |
| --- | --- | --- | --- | --- |
| `abbey/tower.json` | Abbey tower, 1 chest | 50% | none | 0 to 1 rolls of `treasure/crystal_heart` |
| `abbey/barracks.json` | Abbey tower, 3 chests; Papal Outpost tower, 2 chests | 33.3% | none | 1 roll: empty 66, heart 33 |
| `stronghold_corridor.json` | Strongholds | 50% | 12.2% | Heart: 0 to 1 rolls. Fragment: weight 1 of 39 in a 4 to 6 roll pool |
| `abandoned_mineshaft.json` | Mineshafts | 25% | none | 0 to 1 rolls: Enoch 1, heart 1 |
| `simple_dungeon.json` | Dungeons | 25% | none | 0 to 1 rolls: Enoch 1, heart 1 |
| `trial_chambers/reward_unique.json` | Vault sub-table | 22.2% | none | 1 roll: heart 4 of 18 |
| `ancient_city.json` | Ancient cities | 18.2% | 18.2% | 1 roll, bonus 1: Topaz 5, heart 2 (quality 1), fragment 2 (quality 2), of 11 |
| `trial_chambers/reward_ominous_rare.json` | Vault sub-table | none | 21.1% | 1 roll: fragment 8 of 38 |
| `trial_chambers/reward_ominous_unique.json` | Vault sub-table | none | 33.3% | 1 roll: fragment 6 of 18. Also Elegy of the Abbey disc, 4 of 18 |

Each table gives at most one of each item per chest, so the chance is also the expected count. The exception is `stronghold_corridor`, which averages 0.128 fragments per chest.

### Vaults

Matcha does not override the vault parents `trial_chambers/reward.json` or `reward_ominous.json`, so the vanilla 26.3 versions apply. Each has three pools: 1 roll of rare (weight 8) or common (weight 2), 1 to 3 rolls of common, and one roll of the unique table with a random chance of 0.25 (normal) or 0.75 (ominous). Combining these with Matcha's sub-tables (**inference**):

| Vault | Crystal Heart per opening | Divine Fragment per opening |
| --- | --- | --- |
| Normal (`reward`) | 5.6% | none |
| Ominous (`reward_ominous`) | none | 37.6%, about 0.42 expected |

Pith odds were also computed: no chest, barrel, pot or archaeology table in the pack contains unglinted `turtle_scute`.

### Other tables with no heart or fragment

The pack also replaces these vanilla tables. None of them contains a Crystal Heart, Pith or Divine Fragment, directly or through nested tables:

- `chests/`: `abandoned_camp_common_chest`, `abandoned_camp_secret_chest`, `ancient_city_ice_box`, the four `bastion_*` tables, `buried_treasure`, `desert_pyramid`, `end_city_treasure`, `igloo_chest`, `jungle_temple`, `jungle_temple_dispenser`, `nether_bridge`, `pillager_outpost`, `ruined_portal`, the three `shipwreck_*` tables, `spawn_bonus_chest`, `stronghold_crossing`, `stronghold_library`, `trial_chambers/` `corridor`, `entrance`, `intersection`, `intersection_barrel`, `reward_common`, `reward_ominous_common`, `reward_rare` and `supply`, both `underwater_ruin_*` tables, `woodland_mansion`, and 14 of vanilla's 16 `village/*` tables
- `barrels/abandoned_camp_barrel`, `pots/trial_chambers/corridor`, and the `archaeology/` tables `desert_well`, `ocean_ruin_cold`, `ocean_ruin_warm`, `trail_ruins_common` and `trail_ruins_rare`

`buried_treasure` and `shipwreck_treasure` hold the Divine Comedy and Paradise Lost books, which trade for hearts (see `matcha-heart-system.md`).

Vanilla's `village/village_fletcher` and `village_shepherd` are not overridden. `matcha-villagers.md` notes the abandoned villages do not use the village chest tables at all.

Tables Matcha adds: `chests/abbey/barracks`, `chests/abbey/tower`, `pots/abbey/common`, `archaeology/abbey/grave`, `archaeology/village_plains`, and ten `chests/equipment/*` tables.

### Indirect heart sources in structures

- `archaeology/abbey/grave.json`: 1 roll from emerald 1, bone meal 4, bone 7, skeleton skull 2, Wooden Cross 2 and `matcha:archaeology/statues` 1, total 17. So 5.9% of brushes give a Clay Statue (**inference**, arithmetic), which a level 5 Archaeologist trades for a Crystal Heart (`matcha-villagers.md`). The Abbey templates contain no suspicious blocks. This table is only used by Papal Outposts.
- `pots/abbey/common.json`: 1 roll, total weight 495. A Trial Key is 10 of 495, about 2%. Other entries are arrows, iron, copper, leather, rabbit hide, gunpowder, blaze powder and an emerald (5).

## Spawners and Divine Fragments

`dp/data/minecraft/loot_table/blocks/spawner.json` has one pool, 1 roll, of `matcha:treasure/divine_fragment`, with no conditions. **Inference:** every spawner a player breaks drops exactly one Divine Fragment, with or without Silk Touch. Trial spawners are a different block and are not covered by this table.

Spawners found in structure templates:

| Structure | Spawners per piece | Mob |
| --- | --- | --- |
| Dungeon (`spawner/*` templates) | 1 | zombie, skeleton or spider |
| Papal Outpost watchtower | 1 | zombie |
| Abbey `waterfall` room | 8 | skeleton 6, husk 2 |
| Abbey `spider_triple_walkway` room | 1 | cave spider |
| Mansion `1x1_as2` room | 1 | spider |

Every spawner above tags its mobs `NoDrops`. The pack's own spawner mobs therefore drop no loot (**inference**: from the tag name; I did not trace the code that reads it).

## Overgrown Abbey (brief)

What it is: a new jigsaw structure, `dp/data/minecraft/worldgen/structure/abbey_overgrown.json`. A ruined stone tower sits on the surface above a dungeon of copper-trap puzzle rooms.

- **Where:** `forest` and `dark_forest` only (`tags/worldgen/biome/has_structure/abbey_overgrown.json`).
- **How often:** `structure_set/abbey.json` uses random spread, spacing 60, separation 40, and must be at least 16 chunks from a village placement.
- **Size:** jigsaw size 20, within 116 blocks of the start.
- **Finding it:** a level 3 Archaeologist (cartographer) sells an "Overgrown Abbey Map" for Divine Comedy or Paradise Lost (`dp/data/matcha/villager_trade/cartographer/3/abbey_*.json`, search radius 100).

What it gives:

- **Crystal Hearts:** one Crystal Heart lies as a never-despawning item entity in the entrance stairwell `entrances/entrance_0.nbt`, reached through copper-eye doors and grates. The boon room `rooms/boon_heart.nbt` holds another, and is one of three equal-weight boon rooms. Tower chests add 3 × 33.3% + 50% = 1.5 expected hearts (**inference**, arithmetic). The exact number of boon rooms per Abbey is **unconfirmed**.
- **Other loot:** the tower and barracks chests also give a Compound Bow (Power I) or an Anemos iron sword, plus a holy book (Tanakh or Quran) from the tower chest. About 20 decorated pots use `pots/abbey/common`.
- **Trial spawners and vaults:** the dungeon has trial spawners for spiders, cave spiders, zombies, skeletons, a blaze, and named "Buff Copper Skeletons" or "Buff Stray" in copper armour. Normal and ominous vaults sit in some rooms, and the `boon_vault` room has an ominous vault.
- **Spawners:** 9 regular spawners in two rooms, each worth a Divine Fragment when broken.
- **Copper eyes:** marker entities turn into target blocks. Hitting one with a projectile (`advancement/mechanics/hit_copper_eye.json`) opens iron-bar doors, slides copper grates, launches players with wind charges, or drops a lantern (`dp/data/matcha/function/abbey/`).
- **Music:** "Elegy of the Abbey" is a disc at weight 4 of 18 in `reward_ominous_unique`. A second disc, "Abbey", has a loot table (`matcha:music_disc/abbey`) that nothing references.
- **Protection:** the `matcha:in_dungeon` predicate covers Abbeys and trial chambers. Warding stones cannot be set up there (`function/mechanics/warding_stone/setup.mcfunction`).

Data faults (**fact**, but the in-game effect is an **inference**): several jigsaws point to the pool `abbey/default/dungeon`, which does not exist, and one points to `abbey/default/entrace_rooms`, a misspelling. Those connectors probably generate nothing. `boon_rooms.json` and `rooms.json` carry wrong `name` fields, which I believe the game ignores.

## Papal Outposts (pillager outposts)

Files: `dp/data/minecraft/worldgen/structure/pillager_outpost.json`, `template_pool/pillager_outpost/`, `processor_list/outpost_rot.json`, `processor_list/cobwebs.json`, `structure/pillager_outpost/*.nbt`. Placement and biomes are vanilla, since the structure set and biome tag are not overridden.

Changes from vanilla 26.3:

- `spawn_overrides` is `{}`. Vanilla 26.3 has a monster override that spawns only pillagers inside the full bounding box. **Inference:** outposts no longer spawn pillagers, and normal monster spawning applies.
- The features pool re-weights pieces: cage1 1, cage2 3, cage with allays 3, logs 3, tent1 3, tent2 5, targets 3, empty 6. Vanilla gives every piece weight 1 with empty 6. Matcha's cage with allays template contains no entities at all. I did not check the vanilla template.
- Both processor lists turn magenta glazed terracotta into cobwebs (30%) or air.
- The pack replaces every template except the base plate.

What a player gets:

| Piece | Contents |
| --- | --- |
| `watchtower.nbt` | 2 trapped chests on `chests/abbey/barracks`, 1 on `chests/pillager_outpost`, 7 pots on `pots/abbey/common`, 12 suspicious gravel on `archaeology/abbey/grave`, 1 zombie spawner, 6 beds |
| `watchtower_overgrown.nbt` | The same layout, but its 12 suspicious gravel have no loot table. It is placed over the watchtower with `block_rot` integrity 0.05 |
| `feature_plate.nbt` | 17 suspicious gravel on `archaeology/abbey/grave` |
| `feature_cage1.nbt` | Trapped chest with fixed items: planks, sticks, 4 copper ingots, 12 raw copper, leather, glowstone, torches, an arrow, rabbit hide, bones |
| `feature_cage2.nbt` | An empty barrel |

**Inference:** from the two barracks chests alone, one outpost gives at least one Crystal Heart 1 − (2/3)² ≈ 55.6% of the time. The overgrown overlay keeps only about 5% of its blocks, so its extra chests and spawner rarely place. **Unconfirmed:** whether `block_rot` treats block entities differently.

`chests/pillager_outpost.json` itself is summarised in `matcha-villagers.md`. It has no heart or fragment.

Finding them: the level 1 Archaeologist trade `dp/data/matcha/villager_trade/cartographer/1/papal_outpost.json` sells a "Papal Outpost Map" for 1 Obol, with `max_uses` 999 and search radius 100.

## Villages and `village_beta`

See `matcha-villagers.md`. In short: all five vanilla village structures start from `village_beta/town_centers`, placed much more sparsely (spacing 80, separation 50). The buildings are empty and abandoned, with suspicious gravel on `archaeology/village_plains` and horror effects. No village piece references a heart or fragment table.

## Abandoned camp

The abandoned camp is a vanilla 26.3 structure: 18 biome variants, one structure set, spacing 37 and separation 8. Matcha does not override its structure, set or templates. It only replaces the three loot tables:

- `chests/abandoned_camp_common_chest.json`: 4 to 6 rolls of camping odds and ends, 2 rolls of copper equipment, bow, bucket, spyglass or shears, and 1 roll of `abandoned_camp_map`.
- `chests/abandoned_camp_secret_chest.json`: 2 rolls of raw diamond, Pickled Carrots, Canned Apples or Mead, and 4 to 6 rolls of a map or raw copper, gold or iron. It also has 0 to 1 rolls of iron helmet, chestplate, leggings or spear, and 1 explorer map. Vanilla's version gives a diamond or potions, ingots instead of raw metal, and iron axe or boots in place of helmet and chestplate.
- `barrels/abandoned_camp_barrel.json`: 4 to 8 rolls of supplies, plus a bundle, wooden axe or fishing rod.

None of them gives a heart, Pith or fragment.

## Chicken roosts and seagull roost

Templates: `dp/data/matcha/structure/chicken_roosts/plains/` (bush 1 to 3, log 1 and 2, tree 1 to 4) and `dp/data/matcha/structure/seagull_roost/small_1.nbt`.

- Chicken roosts are 8 to 9 block clumps of oak log and leaves, grass and dry grass. They hold 3 to 9 chickens of variant `matcha:bobwhite`: one tagged `mama`, the rest tagged `wild`, some as age-locked chicks. `log_2` adds mushrooms and an item entity.
- The seagull roost is basalt, diorite, concrete powder and dead coral. It holds 12 chickens of variant `matcha:seagull`.
- No loot.

**Fact:** a full-text search of the data pack finds no reference to `chicken_roosts` or `seagull_roost` in any JSON or function file. The biome tags `matcha:is_bobwhite_habitat`, `is_seagull_habitat` and `habitat/plains` are also unreferenced. **Inference:** the roosts do not generate in 1.12.9 and are unfinished or placed by hand. **Unconfirmed:** whether something outside the data pack places them.

## `spawner` structures (dungeons)

`dp/data/minecraft/worldgen/feature/monster_room.json` becomes a `minecraft:sequence`. It runs the vanilla `monster_room`, then a `minecraft:template` feature that picks one of `matcha:spawner/zombie_spawner`, `skeleton_spawner` or `spider_spawner` (weight 1 each, no rotation). Each template is a single spawner block: `NoDrops` mobs, 4 per spawn, delay 200 to 800, player range 16.

Placed features (`placed_feature/monster_room.json`, `monster_room_deep.json`):

| Feature | Matcha count per chunk | Vanilla 26.3 count |
| --- | --- | --- |
| `monster_room` (Y 0 to top) | 15 | 10 |
| `monster_room_deep` (bottom + 6 to Y −1) | 8 | 4 |

**Inference:** the template lands on the room origin, where vanilla puts its own spawner. That replaces the vanilla spawner with a `NoDrops` one, and dungeons become about 1.5 to 2 times as common. **Unconfirmed:** whether `sequence` still places the template when `monster_room` fails to find a valid spot. 26.3 itself uses both feature types (`desert_well.json`, `sulfur_spring.json`).

Each dungeon thus offers a `simple_dungeon` chest (25% heart) and one spawner (one Divine Fragment when broken).

## Other restyled templates

- `structure/woodland_mansion/1x1_as2.nbt`: the mansion spider room, filled with 95 cobwebs. Its spider spawner's spawn potentials tag spiders `NoDrops`. I did not compare it with the vanilla template.
- `structure/igloo/bottom.nbt`: the igloo basement, with a villager, a zombie villager and a cure kit. See `matcha-villagers.md`.

## Not checked

- Nothing was tested in game, and vanilla Java code was not read. Behaviour of `NoDrops`, missing jigsaw pools, `sequence` failure and `block_rot` on block entities is inferred.
- Real-world frequency of Abbeys, boon rooms and outposts was not measured.
- Trial spawner rewards inside the Abbey were not traced. The Abbey trial spawners set only `spawn_potentials`, so **inference:** they use vanilla's default reward tables.
- The resource pack in the download has no `sounds.json`, and no Abbey map or disc models. **Unconfirmed:** where the Abbey music audio and its game-music hook live.
- Loot tables outside chests, barrels, pots, archaeology and `blocks/spawner` were not tabulated. Mob drops of fragments, from the Evoker, Elder Guardian and Piglin Brute, are out of scope.
