# 11: Material units, repairs and anvil rules

**What to build:** Items are repaired with their material, never by combining items, and the anvil only combines books and renames, charging XP points. This ticket also builds the shared material unit lookup that enchanting uses.

**Blocked by:** 01 (GameTest harness)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [ ] Material unit lookup: vanilla repair item where one exists, else the main crafting ingredient; overrides from the spec (phantom membrane, breeze rod, string, prismarine shard, iron ingot, block of gold for golden apples); Netherite Ingot for Netherite gear
- [ ] Full repair takes the count of that material in the recipe minus 1, at least 1; chainmail repairs like iron; Elytra and tridents take 4; Netherite gear takes 1; each unit restores an equal share
- [ ] Repairs cost no XP. No prior-work penalty
- [ ] Two books with the same single enchantment and level combine into the next level, up to max, for 50 × resulting level in XP points; renaming costs 10 points
- [ ] Items never combine with each other in the anvil, crafting grid or grindstone; books never apply to gear
- [ ] The grindstone gives no XP
- [ ] Smithing material upgrades refuse enchanted gear
- [ ] GameTests drive the anvil, grindstone, crafting and smithing menus
