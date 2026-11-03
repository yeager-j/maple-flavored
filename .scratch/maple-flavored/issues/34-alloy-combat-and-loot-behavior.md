# 34: Alloy combat and loot behavior

**What to build:** The Alloy traits that need game logic: damage reductions, Piglin and Illager behavior, and Orichalcum's drop bonus.

**Blocked by:** 33 (Alloy gear and numeric traits)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [ ] Steel armor: 10% less sonic boom and explosion damage per piece
- [ ] Electrum armor: 15% less fire damage per piece; counts as gold to Piglins
- [ ] Orichalcum armor: 10% less magic and indirect magic damage per piece; Illager neutrality with one piece, ending in raids, after attacking an Illager (that Illager and those nearby that see it) or on a book alert
- [ ] Orichalcum tools: +50% to drops that Fortune and Looting affect, additive, skipping Embers, Pith and enchanted books
- [ ] GameTests cover each reduction, Piglin and Illager behavior and the drop bonus
