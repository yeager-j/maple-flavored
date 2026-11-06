# 33: Alloy gear and numeric traits

**What to build:** Every Netherite item has an Alloy version for each Alloy, made at a Smithing Table, with the traits that are plain numbers.

**Blocked by:** 11 (Material units, repairs and anvil rules), 14 (Library and checklist table), 32 (Alloy Furnace and Alloy materials)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [ ] Alloy versions of every item with a Netherite version in 26.3
- [ ] Smithing: Netherite gear + Alloy ingot, no template; Alloy gear + different ingot changes Alloy; enchanted gear refused; no salvage
- [ ] Tool traits: Steel +50% durability and 0.5 attack knockback; Electrum +1 Enchantment Slot; Shakudo +30% mining (on the final break speed, so Efficiency V insta-mines stone) and +10% attack speed; Adamant +1 damage
- [ ] Adamant hardness cap: blocks in the tool's mineable tag mine as if hardness were at most 3; trial spawners, vaults, reinforced deepslate and unbreakable blocks unaffected
- [ ] Adamant pickaxe + Efficiency V + Haste II breaks bedrock in a flat 15 seconds, no drop; the advancement Breaking the Rules (Nether, under Beaconator, challenge, 100 XP)
- [ ] Shakudo mining bonus, hardness cap and bedrock time as playtest game rules in `MapleGameRules`
- [ ] Armor traits per piece: Steel +50% durability and +0.1 knockback resistance over Netherite; Electrum +1 slot; Shakudo +2.5% speed; Adamant +1 toughness
- [ ] Repairs and enchanting use the own Alloy ingot
- [ ] GameTests cover smithing and each numeric trait, Shakudo Efficiency V insta-mining stone (Netherite doesn't), the hardness cap on obsidian but not on trial spawners or reinforced deepslate, and bedrock breaking with and without Haste II
- [ ] Hand check: Shakudo movement speed
