# 46: Illager book drops

**What to build:** Illagers drop some books, but raid Illagers don't, except raid-only rewards.

**Blocked by:** 02 (Design: Stronghold Illagers), 03 (Design: Illager book drops), 17 (Book loot cleanup)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [ ] Book drops per the design task, each its own roll on player kills: Vindicator Sharpness 10% and Sweeping Edge 2.5%; Pillager Captain Power 20%, Punch 5% and Infinity 5%; Evoker Protection 15%, one roll; Ravager Knockback 10%
- [ ] Each Looting level adds 20% of the base chance; the Orichalcum bonus skips these books, as long as each book's chance sits on a pool of only books or on the book's own entry (see `OrichalcumDrops`)
- [ ] Illagers spawned by a raid wave drop no books; every Ravager can drop Knockback
- [ ] GameTests roll drops with a fixed seed for normal and raid Illagers
