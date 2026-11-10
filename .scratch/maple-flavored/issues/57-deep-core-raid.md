# 57: Deep Core raid

**What to build:** Hitting a Deep Core starts a raid of Deep variant waves with a boss bar. Winning breaks the core, drops a Fortune book and Pith, and frees the cave. Losing leaves it ready to try again.

**Blocked by:** 53 (Deep Skeleton, Deep Creeper and Deep Spider), 56 (Deep Core snuffing and buffs)

**Status:** wontfix

Spec: the Maple Flavored spec in this folder, user stories 193g–193l and 193n, the Deep Cores module's Raid bullets, the Lights Out advancement under Milestones and advancements, the Deep Core game rules, and the raid bullets under Spec defaults.

- [ ] A survival or adventure player hitting the block starts its raid, if none is running. On Peaceful the first hit wins at once
- [ ] A boss bar named "Deep Core" and its level, seen by players in the zone. It shows the current wave's remaining health and refills between waves with vanilla's Raid delay
- [ ] 3 waves on Easy, 5 on Normal and 7 on Hard, plus an extra wave at level II and above, from the spec's wave table of Deep Zombies, Deep Skeletons and Deep Spiders
- [ ] Each count is multiplied by 1 plus the **Deep Core player scaling percent** (25) for each extra player in the zone at the start, rounded randomly
- [ ] A wave spawns all at once at valid spots 8 to 24 blocks from the core, inside the zone, preferring spots 8 or more blocks from every player. Wave mobs are persistent, and any death counts
- [ ] When a wave has 2 or fewer mobs left after 600 ticks, they glow
- [ ] Win: the block breaks and drops a level I Fortune book and 1, 2 or 3 Pith by level. Snuffing and buffs end, and Lights Out goes to every player in the zone
- [ ] Loss: the raid ends after **Deep Core loss time** (600) ticks with no player in the zone, or once every player who was there at the start has died. The core can be started again, and spawned mobs stay as ordinary mobs
- [ ] Lights Out: a task under Diamonds! in Story, "Win a Deep Core's raid", with no reward and a custom trigger
- [ ] Fortune's only source is the Deep Core. If the Enchanted Books tab exists, Fortune joins the depths branch as "Found in Deep Cores"
- [ ] GameTests:
  - a hit starts the raid
  - wave counts per difficulty and level, with two mock players
  - mobs spawn in range
  - killing every wave breaks the core with the right drops and advancement
  - leaving the zone, and the starting players dying, each lose the raid
  - Peaceful wins at once
  - stragglers glow
- [ ] Hand check: a full Normal raid at level III in full iron, the boss bar, and the cave lighting up after the win

## Notes

- Confirm vanilla's Raid delay between waves and its boss bar progress on the 26.3 jar. Reuse what fits rather than copying it.
- The raid state belongs on the block entity, so it survives the chunk unloading. Decide what happens to a raid whose chunk unloads mid-wave, and record it in the spec if it isn't obvious.
- `src/main/java/mapleflavored/mobs/RaidWaves.java` already tracks vanilla raid waves for book drops. Check whether any of it applies.

## Comments


Closed on 2026-11-10: Deep Cores were dropped before they were built. Diamond scarcity (ticket 61) takes their place. The reasons are in the last comment of `.scratch/maple-flavored-design/issues/21-deep-caves.md`.
