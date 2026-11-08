# 56: Deep Core snuffing and buffs

**What to build:** A Deep Core's zone keeps its cave dark and its mobs strong. Light that appears inside fizzles out with a trail to the core, and hostile mobs get buffs by the core's level.

**Blocked by:** 55 (Deep Core generation)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder, user stories 193b–193f, the Deep Cores module's Zone, Snuffing and Buffs bullets, the Deep Core game rules, and the snuffing and overlap bullets under Spec defaults.

- [ ] The zone is a box from the **Deep Core zone half width** (50) and **half height** (35) game rules, centred on the core
- [ ] In a fully generated chunk, any block state that emits light and appears in a zone is snuffed, however it got there: placed, lit, grown, spread or powered. A `lit` block turns unlit, fire is removed, and anything else breaks and drops as an item
- [ ] Lava and lava cauldrons are exempt, and light already there when the core's chunks finished generating stays
- [ ] Each snuff plays smoke and an extinguish sound and sends a trail like a hit Creaking's to the core, or to the nearest core when zones overlap
- [ ] Every hostile mob in a zone gets Speed I, Strength I, Resistance I, Strength II and Regeneration I from the levels their game rules set (1, 2, 3, 4 and 5 by default), refreshed while it's inside, with particles. Overlapping zones give the highest level's buffs
- [ ] GameTests:
  - placing a torch, glowstone and a lantern in a zone drops them
  - lighting a furnace and a campfire turns them unlit
  - fire is removed
  - bone-mealed glow lichen spreading in a zone is snuffed
  - a lava bucket isn't snuffed
  - a torch outside the zone stays
  - a level V core gives a zombie all its buffs, and a level I core only Speed
  - a mob leaving the zone loses its buffs
- [ ] Hand check: the smoke, sound and trail when a torch fizzles, and buff particles on mobs in a dark cave

## Notes

- One hook on block state changes in loaded chunks is the likely seam: check the new state's light emission and the loaded-core index. Keep the check cheap, since block changes are a hot path.
- Vanilla's Creaking Heart trail is the reference for the trail particle.
