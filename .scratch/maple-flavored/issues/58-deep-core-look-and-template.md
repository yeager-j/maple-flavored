# 58: Deep Core look and structure template

**What to build:** The Deep Core block's textures for each level, and the small structure built around it.

**Blocked by:** 55 (Deep Core generation)

**Status:** ready-for-human

Spec: the Maple Flavored spec in this folder, user story 193f and the Deep Cores module.

- [ ] A block model and textures for levels I to V, readable in a dark cave without giving off light
- [x] The decoration around the core: the `maple_flavored:deep_core` template, in `src/main/resources/data/maple_flavored/structure/deep_core.nbt`
- [ ] Decide whether the core should sit within reach of a player standing on the template's lower stairs (see Notes)
- [ ] Any reused asset listed in `CREDITS.md`

## Notes

- Jackson built the template in a dev world (2026-11-07). It's 3 wide, 7 tall and 3 deep, all deepslate, and symmetric, so it can rotate:
  - y 0: a 3 × 3 deepslate base
  - y 1: a ring of deepslate brick stairs around a deepslate tile centre
  - y 2–3: a deepslate tile pillar with air around it
  - y 4: a pedestal of stairs around a tile centre
  - y 5: the core, with deepslate tile walls on the corners and open sides
  - y 6: a stair and tile roof
- An uncharged respawn anchor at y 5 stands in for the Deep Core block, and it gives no light. Ticket 55 swaps it for the real block when the piece places the template.
- Reach: with the base on the cave floor, the core's centre is about 5.5 blocks up, beyond vanilla's 4.5-block reach, and the y 4 pedestal blocks the angle from the y 1 stairs. A player has to pillar up about 3 blocks to hit it. That may be intended, since it makes starting the raid deliberate.
- The 20 saved air blocks clear whatever they land on, such as dripstone. Structure voids would stop that where it isn't wanted.
