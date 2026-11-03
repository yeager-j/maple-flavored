# 07: Crystal Heart, Pith and Netherite recipe

**What to build:** Crystal Hearts raise Max Hearts or save a player from a lethal hit. Pith is the rare material behind Crystal Hearts and Netherite, and drops from Evokers and Piglin Brutes.

**Blocked by:** 05 (Max Hearts and death cost)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [ ] Crystal Heart and Pith are registered items
- [ ] Consuming a Crystal Heart takes about 1.6 seconds and adds 1 Max Heart; at 30 it is refused and not used up
- [ ] Held in either hand, it saves from a lethal hit with the Totem's effects, following vanilla's hand order against a Totem, and is used up without adding a heart
- [ ] A save is not a death: no heart or XP cost
- [ ] Recipes: 8 diamonds around 1 Pith makes a Crystal Heart; a Nether Star makes 9 Pith, with no reverse recipe; a Netherite Ingot is 4 Netherite Scrap and 4 Pith
- [ ] Pith drops: Evokers 1 at 25%, Piglin Brutes 1 at 10%
- [ ] Postmortal still counts only a Totem. The Crystal Heart advancements come with the Spawner Clearing ticket, as their parent lives there
- [ ] Credit Matcha Flavoured for the Crystal Heart recipe and Nether Star recipe
- [ ] GameTests cover consumption, the cap, the save, both hands, the recipes and the drops
