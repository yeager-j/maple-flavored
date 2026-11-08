# 51: Underground mob health

**What to build:** In the Overworld, the Mobs table's health values apply only to mobs that spawn at Y 63 or above. Lower down, zombies, skeletons and creepers have vanilla health, so caving is somewhat harder while the surface stays easier.

**Blocked by:** 31 (Common mob changes)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder, user stories 158–160a, the Mobs module's first paragraph, and the Y 63 bullet under Spec defaults.

- [ ] Overworld mobs that spawn below Y 63 skip the table's health values and keep vanilla health: adult zombies, baby zombies, husks, drowned and zombie villagers, skeletons, strays, bogged and parched, and creepers
- [ ] Cave spider and silverfish rows apply at any height
- [ ] Every change other than health (zombie speed, step-up, Hard damage and jump, spider jump and no fall damage, husk stats) applies at any height
- [ ] The height is judged once, at spawn. A mob keeps its stats when it moves
- [ ] Other dimensions keep the whole table
- [ ] GameTests spawn each affected mob above and below Y 63 on Normal and check max health, plus a cave spider below Y 63 and a Nether skeleton

## Notes

- The table lives in `src/main/java/mapleflavored/mobs/CommonMobs.java`. Keep the split there, so balance stays one edit.
- "Spawns" covers every spawn reason the table already covers, spawners included. A dungeon below Y 63 now spawns vanilla-health zombies.
