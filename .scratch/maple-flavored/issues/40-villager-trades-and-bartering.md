# 40: Villager trades and bartering

**What to build:** Trades stop skipping the Library and support Reinforcement.

**Blocked by:** 04 (Design: Librarian book trades), 10 (Copper replaces stone tools), 37 (Reinforcement)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [ ] Villagers sell plain gear wherever vanilla sells enchanted gear
- [ ] Toolsmiths sell copper tools instead of stone
- [ ] Master Armorers sell the Reinforcement template for 32 emeralds
- [ ] Every Novice Librarian offers all 3 Novice trades, one of which buys any enchanted book for 4 emeralds, 10 villager XP and 50 player XP points per find (2^(level−1)), with 12 uses and a price multiplier of 0; the list shows the level I price, and the result slot shows the real payout
- [ ] Librarian levels 2 to 4 lose their book slot, and no villager sells an enchanted book
- [ ] No villager sells Pith, Crystal Hearts or raw diamonds
- [ ] GameTests check trade lists by profession and level
