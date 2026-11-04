# 10: Copper replaces stone tools

**What to build:** The tool ladder becomes wood, copper, iron, diamond, Netherite, then Alloys. Stone tools are removed and copper fills their place.

**Blocked by:** 01 (GameTest harness)

**Status:** done

Spec: the Maple Flavored spec in this folder.

- [x] Stone tool recipes are removed, including the stone spear; the stone tool materials tag stays so Stone Age still works
- [x] A wooden pickaxe mines copper ore, deepslate copper ore and raw copper blocks; everything else at stone tier needs copper or better
- [x] Chest loot and the Bonus Chest give copper tools wherever they gave stone ones
- [x] Getting an Upgrade asks for a copper pickaxe
- [x] GameTests cover mining drops by tool and the loot replacements
