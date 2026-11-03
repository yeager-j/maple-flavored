# 09: Remnants

**What to build:** Death drops all of a player's XP as a Remnant that only they can see and collect, which lasts until collected and shrinks with each further death.

**Blocked by:** 01 (GameTest harness)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [ ] Keep XP game rule, default off; when on, death keeps XP and makes no Remnant. Vanilla's Keep Inventory no longer decides XP
- [ ] A registered Remnant entity holds all the player's XP points on death; a death at 0 XP makes none; a Crystal Heart or Totem save makes none
- [ ] Placement: an uncollectable death spot (void, lava) sends it to the last solid ground in that dimension, else the respawn point; otherwise it stays where the player died. Safe placement always wins. (The Warden Subspace exception is tested in the Warden Subspace ticket)
- [ ] When it is moved, the player's recorded death location is set to its spot, so the vanilla Recovery Compass leads to it
- [ ] Only the owner sees it (its glow) and collects it by walking over it, getting the points directly, not as orbs
- [ ] It can't be damaged, never despawns and persists while its owner is offline or its chunk is unloaded
- [ ] Lazy decay: a persistent per-player death count; each later death makes the points 80% of current, rounded down; the creating death doesn't cut it; at 0 points it is removed. Works for unloaded Remnants and deaths in other dimensions
- [ ] Advancement: Collect your own Remnant (Adventure)
- [ ] GameTests cover two players (no stealing), decay across an unload, a 0 XP death, the 1 point boundary, void and lava placement, the compass target and Keep XP
- [ ] Hand check: owner-only glow
