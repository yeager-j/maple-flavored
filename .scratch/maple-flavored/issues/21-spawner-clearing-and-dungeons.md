# 21: Spawner Clearing and more dungeons

**What to build:** Normal spawners fight back: they ignore light and can't be broken until Cleared. Dungeons are more common. This ticket also wires the Crystal Heart advancements, whose parent is here.

**Blocked by:** 07 (Crystal Heart, Pith and Netherite recipe), 18 (Structure book framework, with Mineshafts)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [x] Normal spawners spawn regardless of light; trial spawners stay vanilla
- [x] Unbreakable and explosion-proof until Cleared: 12 deaths of mobs it spawned plus 2 per additional player who entered its 16-block range since counting began; the requirement never drops; any death counts
- [x] Cleared stays Cleared; progress shows as a world-border forcefield that closes in with the count, and a Cleared spawner has none
- [x] Uncleared spawners spawn twice as fast; Cleared ones go back to vanilla speed
- [x] Breaking a Cleared spawner with a pickaxe drops 1 Pith. Advancement: Break a Cleared spawner (Adventure, under Monster Hunter), to the breaker
- [x] Advancements under it: Consume a Crystal Heart; Be saved by a Crystal Heart (under Consume); Reach 30 Max Hearts (challenge, 100 XP, under Consume)
- [x] A Cleared event is exposed for the Fortress lock
- [x] Dungeons: 15 attempts per chunk above Y 0 and 8 below
- [x] Dungeon books, moved here from 20: the book fights the dungeon's mob (Smite for zombies and skeletons, Bane of Arthropods for spiders), given to the spawner by the dungeon room's own placement, since Dungeons are placed features. The Cleared spawner drops it with its Pith when broken. Every Mineshaft cave spider spawner holds Bane of Arthropods the same way
- [x] GameTests cover light, the count with several players, lava kills, explosions, the drop and the advancements; worldgen data check for dungeon attempts
- [ ] Hand check: the forcefield, and dungeon frequency in a fresh world
