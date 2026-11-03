# 16: Enchanted golden apples

**What to build:** Golden apples become the base for enchanted golden apples, made at the table with effects that unlock with Library Progress. Any enchanted golden apple cures a zombie villager.

**Blocked by:** 15 (Library Progress and Mending)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [ ] A plain golden apple keeps its food value and has no effects
- [ ] The table accepts golden apples and offers effects: Regeneration II 20 s at 15%, Fire Resistance 5 min at 30%, Absorption IV 2 min at 50%, Resistance 5 min at 75% Library Progress
- [ ] Each effect takes 1 Enchantment Slot (no gold extra slot) and 1 lapis; the first save takes a block of gold; at least one effect
- [ ] One apple per operation at the table and grindstone; each apple carries its own deposit; apples stack only when their effects match
- [ ] A saved apple becomes the vanilla enchanted golden apple item with the chosen effects; the grindstone returns a golden apple and the block of gold
- [ ] Every enchanted golden apple in loot becomes a golden apple
- [ ] Any enchanted golden apple cures a zombie villager under Weakness
- [ ] Advancement: Make an enchanted golden apple (Story, under Enchanter); Zombie Doctor moves under it. A Balanced Diet still works
- [ ] GameTests cover thresholds, slots, the grindstone, stacking and a cure
