# 61: Diamond scarcity

**What to build:** Diamond ore drops a Diamond Shard, and 4 shards craft 1 diamond, the way Netherite scrap makes an ingot. A full diamond set takes about four times the mining, by every route: big caves, small tunnels and branch mines. A Crystal Heart costs 5 diamonds, down from 8.

**Blocked by:** None

**Status:** needs-triage

Spec: not in the Maple Flavored spec yet. Add its user stories and a bullet under Loot and worldgen data once the open decisions below are settled. The reasons are in the last comment of `.scratch/maple-flavored-design/issues/21-deep-caves.md`.

## Open decisions

Jackson settles these before the ticket is ready to build.

- **Which loot diamonds become shards.** The survey in the checklist comes first. Jackson decides from its list.
- **Which other diamond costs change.** The Crystal Heart is settled at 5 diamonds, so 20 ores. The rest are four times the mining unless they're repriced:
  - an Adamant ingot uses a block of diamond
  - copying a Reinforcement template takes 7 diamonds
  - enchanting and repairing diamond gear costs diamonds through Material units
  - vanilla recipes: the enchanting table, jukebox and the rest
- **Fortune.** It's back in Woodland Mansions and is now worth much more. Fortune III averages 2.2 times the shards. Judge in playtesting whether four mansions is still the right price.
- **Tuning.** The 4-to-1 ratio is a recipe, so it can't be a game rule. Shards per ore could be a playtest game rule in `MapleGameRules` if Jackson wants to tune it in game.

## Settled

Decided by Jackson on 2026-11-10.

- The item is the Diamond Shard.
- 4 Diamond Shards craft 1 diamond, shapeless.
- A Crystal Heart is 5 diamonds and 1 Pith, in the shield recipe's shape with the Pith in the centre, so it vaguely looks like a heart:

  ```
  D . D
  D P D
  . D .
  ```

## Checklist

- [ ] First, a survey of every loot table that gives diamonds, written into this ticket for Jackson to decide which become shards. Nothing in loot changes before he decides. It covers:
  - vanilla chest, archaeology, vault, gift and entity tables in the 26.3 jar
  - the tables the mod overrides or changes in code, such as the 1 diamond that replaces each enchanted-book chest entry
  - each table's count and weight
- [ ] A registered `maple_flavored:diamond_shard` item with its own texture, in the creative menu's ingredients next to diamond
- [ ] A shapeless recipe: 4 Diamond Shards craft 1 diamond. It unlocks when a player first holds a Diamond Shard
- [ ] The Crystal Heart recipe is the 5-diamond shape above, in `src/main/resources/data/maple_flavored/recipe/crystal_heart.json`. The 8-diamond recipe no longer crafts. Spec story 20, `docs/project-overview.md` and `CREDITS.md` say 5 diamonds, and `CREDITS.md` still credits Matcha Flavoured for the idea
- [ ] Diamond ore and deepslate diamond ore drop 1 Diamond Shard where vanilla drops 1 diamond, with vanilla's Fortune bonus, explosion decay and XP. Silk Touch still drops the ore block
- [ ] Smelting or blasting a diamond ore block gives 1 Diamond Shard, not a diamond
- [ ] Loot diamonds follow Jackson's decisions from the survey
- [ ] The diamond Milestone and Diamonds! still count when a diamond enters the inventory, so they come after the first craft, not the first ore. Shards don't count
- [ ] The Orichalcum drop bonus applies to shards as it did to diamonds
- [ ] The spec, `docs/project-overview.md` and the glossary describe the Diamond Shard
- [ ] GameTests:
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
