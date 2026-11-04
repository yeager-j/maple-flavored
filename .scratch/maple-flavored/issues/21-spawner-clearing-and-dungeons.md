# 21: Spawner Clearing and more dungeons

**What to build:** Normal spawners fight back: they ignore light and can't be broken until Cleared. Dungeons are more common. This ticket also wires the Crystal Heart advancements, whose parent is here.

**Blocked by:** 07 (Crystal Heart, Pith and Netherite recipe), 18 (Structure book framework, with Mineshafts)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [ ] Normal spawners spawn regardless of light; trial spawners stay vanilla
- [ ] Unbreakable and explosion-proof until Cleared: 12 deaths of mobs it spawned plus 2 per additional player who entered its 16-block range since counting began; the requirement never drops; any death counts
- [ ] Cleared stays Cleared; progress shows as cracks that grow with the count
- [ ] Breaking a Cleared spawner drops 1 Pith. Advancement: Break a Cleared spawner (Adventure, under Monster Hunter), to the breaker
- [ ] Advancements under it: Consume a Crystal Heart; Be saved by a Crystal Heart (under Consume); Reach 30 Max Hearts (challenge, 100 XP, under Consume)
- [ ] A Cleared event is exposed for the Fortress lock
- [ ] Dungeons: 15 attempts per chunk above Y 0 and 8 below
- [ ] Dungeon books, moved here from 20: Bane of Arthropods or Smite with equal weight, placed by the dungeon room's own placement, since Dungeons are placed features. Starting idea: floating just above the spawner
- [ ] GameTests cover light, the count with several players, lava kills, explosions, the drop and the advancements; worldgen data check for dungeon attempts
- [ ] Hand check: crack feedback, and dungeon frequency in a fresh world
