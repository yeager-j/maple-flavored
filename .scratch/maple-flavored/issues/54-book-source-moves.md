# 54: Book source moves

**What to build:** Looting moves to Woodland Mansions, Thorns to Jungle Temples and Knockback to the Bastion's hoglin stables. Ravagers stop dropping Knockback. Fortune leaves Mansions for Deep Cores, which ticket 57 adds.

**Blocked by:** 19 (Ordinary structure books), 23 (Bastion books and Piglin anger), 44 (Mansion book and Illager alert), 46 (Illager book drops)

**Status:** wontfix

Spec: the Maple Flavored spec in this folder, user stories 63, 64 and 74, the Structure books source list, and the Mobs module's drops bullet.

- [ ] The Woodland Mansion's book is Looting, keeping its spot and Illager alert
- [ ] The Jungle Temple's book is Thorns
- [ ] The Bastion hoglin stables' book is Knockback
- [ ] Ravagers drop no books, including outside raids
- [ ] If the Enchanted Books tab (ticket 50) is built, its Looting, Thorns and Knockback descriptions follow, Ravager leaves the Illagers branch, and Fortune waits for ticket 57. Otherwise ticket 50 picks this up from the source table
- [ ] GameTests: each structure places its new book, and Ravager kills over many seeded rolls drop no book

## Notes

- The book sources live in `src/main/java/mapleflavored/structurebooks/StructureBookRules.java` and `StructureBookSpots.java`, and the Ravager drop in `src/main/resources/data/minecraft/loot_table/entities/ravager.json`.
- Until ticket 57 lands, Fortune has no source. That's fine during the build, since the mod is unreleased.
- The design is in `.scratch/maple-flavored-design/issues/21-deep-caves.md`, under Books.

## Comments


Built in commit `48380a6`, then reverted on 2026-11-10 when Deep Cores were dropped. Fortune is back in Woodland Mansions, Looting in Jungle Temples, Thorns in the Bastion's hoglin stables and Knockback on Ravagers.
