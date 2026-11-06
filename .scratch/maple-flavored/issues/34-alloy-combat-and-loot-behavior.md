# 34: Alloy combat and loot behavior

**What to build:** The Alloy traits that need game logic: damage reductions, Piglin and Illager behavior, and Orichalcum's drop bonus.

**Blocked by:** 33 (Alloy gear and numeric traits)

**Status:** done

Spec: the Maple Flavored spec in this folder.

- [x] Steel armor: 10% less sonic boom and explosion damage per piece
- [x] Electrum armor: 15% less fire damage per piece; counts as gold to Piglins
- [x] Orichalcum armor: 10% less magic and indirect magic damage per piece; Illager neutrality with one piece, ending in raids, after attacking an Illager (that Illager and those nearby that see it) or on a book alert. Anger lasts the playtest game rule `orichalcum_illager_anger_time` (30 s, like a Piglin's), and covers an Evoker's Vexes
- [x] Orichalcum tools: +50% to drops that Fortune and Looting affect, additive, skipping Embers, Pith and enchanted books. Counts gain half their count before Fortune or Looting; Looting chances gain half their unenchanted chance
- [x] GameTests cover each reduction, Piglin and Illager behavior and the drop bonus
- [x] Reductions add up per piece (Jackson): a full Steel set takes 40% off, a full Electrum set 60% off fire
- [ ] Hand check: Illagers in a Woodland Mansion with one Orichalcum piece; hitting one with others around, and an Evoker's Vexes; Piglins around a player in Electrum; creeper and Warden damage in full Steel
