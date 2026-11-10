# 61: Diamond scarcity

**What to build:** Diamond ore drops a piece of a diamond, and 4 pieces craft 1 diamond, the way Netherite scrap makes an ingot. A full diamond set takes about four times the mining, by every route: big caves, small tunnels and branch mines.

**Blocked by:** None

**Status:** needs-triage

Spec: not in the Maple Flavored spec yet. Add its user stories and a bullet under Loot and worldgen data once the open decisions below are settled. The reasons are in the last comment of `.scratch/maple-flavored-design/issues/21-deep-caves.md`.

## Open decisions

Jackson settles these before the ticket is ready to build.

- **The piece's name and look.** "Diamond Shard" is a placeholder here. It needs its own texture.
- **The recipe.** 4 shards in a 2 × 2 square, as Tattered Leather does, or shapeless.
- **Which diamond costs change.** Every diamond cost is four times the mining unless it's repriced:
  - a Crystal Heart is 8 diamonds, so 32 ores, and crafting from 10 to 30 Max Hearts would be 640 ores
  - an Adamant ingot uses a block of diamond
  - copying a Reinforcement template takes 7 diamonds
  - enchanting and repairing diamond gear costs diamonds through Material units
  - vanilla recipes: the enchanting table, jukebox and the rest
- **Chest diamonds.** They stay whole in this ticket, so a chest diamond is worth 4 ores. That favours exploring structures. Confirm it, including the 1 diamond that replaces each enchanted-book chest entry.
- **Fortune.** It's back in Woodland Mansions and is now worth much more. Fortune III averages 2.2 times the shards. Judge in playtesting whether four mansions is still the right price.
- **Tuning.** The 4-to-1 ratio is a recipe, so it can't be a game rule. Shards per ore could be a playtest game rule in `MapleGameRules` if Jackson wants to tune it in game.

## Checklist

- [ ] A registered `maple_flavored:diamond_shard` item with its own texture, in the creative menu's ingredients next to diamond
- [ ] A recipe: 4 Diamond Shards craft 1 diamond. It unlocks when a player first holds a Diamond Shard
- [ ] Diamond ore and deepslate diamond ore drop 1 Diamond Shard where vanilla drops 1 diamond, with vanilla's Fortune bonus, explosion decay and XP. Silk Touch still drops the ore block
- [ ] Smelting or blasting a diamond ore block gives 1 Diamond Shard, not a diamond
- [ ] Diamonds in chest loot stay whole diamonds
- [ ] The diamond Milestone and Diamonds! still count when a diamond enters the inventory, so they come after the first craft, not the first ore. Shards don't count
- [ ] The Orichalcum drop bonus applies to shards as it did to diamonds
- [ ] The spec, `docs/project-overview.md` and the glossary describe the Diamond Shard
- [ ] GameTests:
  - both ores drop shards and no diamonds, with and without Fortune, over seeded rolls
  - Silk Touch drops the ore
  - smelting an ore gives a shard
  - the recipe crafts
  - a chest table that held diamonds still does
  - the Milestone waits for a diamond
- [ ] Hand check: the shard texture in the inventory and on the ground

## Notes

- `LootSubstitutions` swaps items in every loot table, chests included, so a blanket diamond swap there would also change chests. Override the two ore block loot tables in data instead.
- Cutting diamond ore generation to a quarter was considered and set aside. A big cave would show about 3 exposed ores, not 13, and feel empty, and chest diamonds wouldn't gain value.
- This relies on the Deep variants for danger. If playtesting shows they aren't a threat behind a torch line, the extra mining is only grind. Intense Darkness, recorded in the same design comment, is the idea to revisit then.
