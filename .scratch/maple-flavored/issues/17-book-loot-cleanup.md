# 17: Book loot cleanup

**What to build:** Enchantments come only from the Library, so found books are always level I, most book loot is replaced, and found or dropped gear is plain.

**Blocked by:** 01 (GameTest harness)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [x] Every enchanted book from any source is one enchantment at level I
- [x] Enchanted books leave every chest table; each entry becomes 1 diamond
- [x] Fishing's book entry gives only Luck of the Sea and Lure books; ominous vaults keep only Breach, Density and Wind Burst books
- [x] Bartering loses Soul Speed books and iron boots with no replacement
- [x] Gear in chests, fishing and bartering is plain
- [x] Mobs remember gear they spawned with; it keeps vanilla's drop chance and drops without enchantments; picked-up gear drops unchanged
- [x] GameTests roll each changed loot table with a fixed seed, and kill mobs holding spawned and picked-up gear
