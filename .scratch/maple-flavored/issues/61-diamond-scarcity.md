# 61: Diamond scarcity

**What to build:** Diamond ore drops a Diamond Shard, and 4 shards craft 1 diamond, the way Netherite scrap makes an ingot. A full diamond set takes about four times the mining, by every route: big caves, small tunnels and branch mines. A Crystal Heart costs 5 diamonds, down from 8.

**Blocked by:** None

**Status:** ready-for-human (hand check left)

Spec: the Maple Flavored spec in this folder, user stories 20, 68–68c and 136, and the Diamond Shard bullets under Loot and worldgen data. The reasons are in the last comment of `.scratch/maple-flavored-design/issues/21-deep-caves.md`.

## Open decisions

- **Fortune.** It's back in Woodland Mansions and is now worth much more. Fortune III averages 2.2 times the shards. Judge in playtesting whether four mansions is still the right price.

## Settled

Decided by Jackson on 2026-11-10.

- The item is the Diamond Shard.
- Loot is split by danger. Safe, early sources give shards in place of their diamonds, book diamonds included: buried treasure, shipwreck treasure, the desert pyramid's chests and suspicious sand, the abandoned camp's secret chest, abandoned mineshafts, and the trial chambers' intersection chests, barrels and corridor pots. Dangerous structures keep whole diamonds: Bastions, End Cities, Nether Fortresses, Strongholds, Ancient Cities, Woodland Mansions, Jungle Temples, trial chamber vaults, dungeons, pillager outposts and big ocean ruins. A shard replaces a diamond one for one, so a stack of 3 diamonds becomes 3 shards. Jackson's pick also named the village toolsmith and weaponsmith, but their chests hold no diamonds in Maple, so they're left as they are.
- Copying a Reinforcement template takes 7 Diamond Shards, not 7 diamonds. The other diamond costs aren't repriced: an Adamant ingot's block of diamond, Material units and vanilla recipes all cost four times the mining.
- Shards per ore stays fixed at 1, in data. No game rule.
- 4 Diamond Shards craft 1 diamond, shapeless.
- A Crystal Heart is 5 diamonds and 1 Pith, in the shield recipe's shape with the Pith in the centre, so it vaguely looks like a heart:

  ```
  D . D
  D P D
  . D .
  ```

## Survey: loot that gives diamonds

From the 26.3 jar and the mod's overrides, on 2026-11-09. "Per open" is the average number of diamonds one chest, vault opening, brush or pot gives. No entity, gift, fishing, bartering or mob equipment table gives a diamond. Vanilla's village toolsmith and weaponsmith chests give diamonds too, but the mod's overrides already take them out.

Diamond entries:

| Table | Rolls | Diamonds | Weight | Per open |
| --- | --- | --- | --- | --- |
| `chests/buried_treasure` | 1–3 | 1–2 | 5/15 | 1.00 |
| `chests/end_city_treasure` | 2–6 | 2–7 | 5/89 | 1.01 |
| `chests/bastion_treasure` (mod override) | 3 | 2–6 | 5/126 | 0.48 |
| `chests/abandoned_camp_secret_chest` | 2 | 1 | 1/5 | 0.40 |
| `chests/nether_bridge` | 2–4 | 1–3 | 5/78 | 0.38 |
| `chests/trial_chambers/intersection` | 1–3 | 1–2 | 10/86 | 0.35 |
| `chests/trial_chambers/reward_ominous_common` (vault) | 1 | 2–3 | 2/15 | 0.33 |
| `chests/jungle_temple` | 2–6 | 1–3 | 3/89 | 0.27 |
| `chests/shipwreck_treasure` | 3–6 | 1 | 5/150 | 0.15 |
| `chests/stronghold_corridor` | 2–3 | 1–3 | 3/101 | 0.15 |
| `chests/abandoned_mineshaft` | 2–4 | 1–2 | 3/98 | 0.14 |
| `archaeology/desert_pyramid` (suspicious sand) | 1 | 1 | 1/8 | 0.12 |
| `chests/desert_pyramid` | 2–4 | 1–3 | 5/247 | 0.12 |
| `chests/trial_chambers/intersection_barrel` | 1–3 | 1–3 | 1/33 | 0.12 |
| `chests/trial_chambers/reward_ominous_rare` (vault, mod override) | 1 | 1 | 2/29 | 0.07 |
| `chests/trial_chambers/reward_common` (vault) | 1 | 1–2 | 1/25 | 0.06 |
| `pots/trial_chambers/corridor` | 1 | 1–2 | 5/351 | 0.02 |

Enchanted books that `LootEnchantments` turns into 1 diamond, in every chest-type table, vaults and pots included:

| Table | Per open |
| --- | --- |
| `chests/stronghold_library` | 1.15 |
| `chests/ancient_city` | 0.72 |
| `chests/desert_pyramid` | 0.24 |
| `chests/underwater_ruin_big` | 0.22 |
| `chests/woodland_mansion` | 0.19 |
| `chests/trial_chambers/reward_rare` (vault) | 0.17 |
| `chests/abandoned_mineshaft` | 0.14 |
| `chests/simple_dungeon` | 0.14 |
| `chests/trial_chambers/reward_ominous_rare` (vault, mod override) | 0.14 |
| `chests/pillager_outpost` | 0.11 |
| `chests/bastion_other` (mod override) | 0.10 |
| `chests/jungle_temple` | 0.04 |
| `chests/stronghold_crossing` | 0.04 |
| `chests/stronghold_corridor` | 0.02 |

Books in entity, fishing and bartering tables become level I books, not diamonds, so they're out of scope.

## Checklist

- [x] First, a survey of every loot table that gives diamonds, written into this ticket for Jackson to decide which become shards. Nothing in loot changes before he decides. It covers:
  - vanilla chest, archaeology, vault, gift and entity tables in the 26.3 jar
  - the tables the mod overrides or changes in code, such as the 1 diamond that replaces each enchanted-book chest entry
  - each table's count and weight
- [x] A registered `maple_flavored:diamond_shard` item with its own texture, in the creative menu's ingredients next to diamond
- [x] A shapeless recipe: 4 Diamond Shards craft 1 diamond. It unlocks when a player first holds a Diamond Shard
- [x] The Crystal Heart recipe is the 5-diamond shape above, in `src/main/resources/data/maple_flavored/recipe/crystal_heart.json`. The 8-diamond recipe no longer crafts. Spec story 20, `docs/project-overview.md` and `CREDITS.md` say 5 diamonds, and `CREDITS.md` still credits Matcha Flavoured for the idea
- [x] Diamond ore and deepslate diamond ore drop 1 Diamond Shard where vanilla drops 1 diamond, with vanilla's Fortune bonus, explosion decay and XP. Silk Touch still drops the ore block
- [x] Smelting or blasting a diamond ore block gives 1 Diamond Shard, not a diamond
- [x] Loot diamonds follow Jackson's decisions from the survey
- [x] The diamond Milestone and Diamonds! still count when a diamond enters the inventory, so they come after the first craft, not the first ore. Shards don't count
- [x] The Orichalcum drop bonus applies to shards as it did to diamonds
- [x] The spec, `docs/project-overview.md` and the glossary describe the Diamond Shard
- [x] GameTests:
  - both ores drop shards and no diamonds, with and without Fortune, over seeded rolls
  - Silk Touch drops the ore
  - smelting an ore gives a shard
  - both recipes craft, and 8 diamonds around a Pith craft nothing
  - each surveyed table gives what Jackson decided
  - the Milestone waits for a diamond
- [ ] Hand check: the shard texture in the inventory and on the ground

## Notes

- `LootSubstitutions` swaps items in every loot table, chests included, so a blanket diamond swap there would also change chests. Override the two ore block loot tables in data, and change other tables one by one after the survey.
- Whole diamonds in loot are worth 4 ores each, which favours exploring structures. Shards in loot keep loot at its old value against mining. That's the trade-off behind the survey.
- Cutting diamond ore generation to a quarter was considered and set aside. A big cave would show about 3 exposed ores, not 13, and feel empty, and chest diamonds wouldn't gain value.
- This relies on the Deep variants for danger. If playtesting shows they aren't a threat behind a torch line, the extra mining is only grind. Intense Darkness, recorded in the same design comment, is the idea to revisit then.
- Jackson asked for Deep Cores to be removed with this ticket. Nothing of them was built, so that meant deleting the unused `deep_core.nbt` template and the notes that said it was kept.
