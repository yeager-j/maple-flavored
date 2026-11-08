# 60: Tattered Leather

**What to build:** Zombies and every other mob that drops rotten flesh drop Tattered Leather instead, and 4 Tattered Leather craft into 1 leather. Leather for armor and books comes from fighting, not only from cows. Clerics buy spider eyes in place of the rotten flesh mobs no longer drop.

**Blocked by:** None

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder, user stories 124a and 165i, the Tattered Leather bullet under Loot and worldgen data, the Cleric line under Trades and bartering, and the vanilla changes under Milestones and advancements.

- [ ] A registered `maple_flavored:tattered_leather` item with its own texture, in the creative menu's ingredients next to leather
- [ ] A recipe: 4 Tattered Leather in a 2 × 2 square craft 1 leather, as vanilla does with rabbit hide. Its recipe unlocks when a player first holds Tattered Leather
- [ ] Every mob whose vanilla loot table drops rotten flesh drops Tattered Leather instead, with the same count, Looting bonus and conditions: zombie, husk, drowned, zombie villager, zombified piglin, zoglin, zombie horse, camel husk and zombie nautilus. Deep Zombies follow through the zombie's table
- [ ] Rotten flesh stays everywhere else: chest loot, fishing junk and cat gifts
- [ ] The Novice Cleric's trade buys 4 spider eyes for 1 emerald in place of 32 rotten flesh, keeping vanilla's 16 uses, 2 villager XP and 0.05 reputation discount
- [ ] A Balanced Diet no longer asks for rotten flesh. Its other foods, position and reward stay vanilla
- [ ] `CREDITS.md` credits Matcha Flavoured for zombies dropping Tattered Leather, and lists the texture's source
- [ ] GameTests: each listed mob's loot table, rolled with fixed seeds, gives Tattered Leather and no rotten flesh, in the counts rotten flesh had; a chest table that held rotten flesh still does; the recipe crafts; a Novice Cleric offers the spider eye trade and not the rotten flesh one; A Balanced Diet is granted for every other food and has no rotten flesh criterion
- [ ] Hand check: the Tattered Leather texture in the inventory and on the ground

## Notes

- In Matcha Flavoured, Tattered Leather is vanilla rabbit hide renamed, with its own texture (`MF_resourcepack/assets/minecraft/textures/item/rabbit_hide.png` by Klei, at commit `71c6e65`), dropped 1–2 per zombie only on player kills. Jackson chose a new item instead, so rabbit hide stays vanilla, and rotten flesh's counts and conditions instead of Matcha's. The texture can start from Matcha's, credited, or Jackson can draw one.
- `LootSubstitutions` swaps items in every loot table, chests included, so a blanket rotten flesh swap there would also change chests. Limit the swap to entity loot tables, or override those tables in data.
- A Balanced Diet is `data/minecraft/advancement/husbandry/balanced_diet.json`; drop its `rotten_flesh` criterion and its entry in `requirements`.
- The Cleric trade is data in 26.3: `data/minecraft/villager_trade/cleric/1/rotten_flesh_emerald.json`, listed by the `minecraft:villager_trade/cleric/level_1` tag. The repo already overrides trades for other professions under `src/main/resources/data/minecraft/villager_trade/`.
