# 27: Warden summoning in Warden Territory

**What to build:** Shrieks only count for players in Warden Territory, which closes the Warden switch.

**Blocked by:** 25 (Warden Subspace)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [ ] Warden Territory is the Deep Dark biome or an Ancient City's bounds
- [ ] A shriek gives warning levels and cooldowns only to players in Warden Territory
- [ ] The Warden spawns 5 to 15 blocks from the player who caused the shriek, or the nearest player in Warden Territory; with no room, the search widens to 50 blocks from that player; if that fails, the cooldown clears
- [ ] With nobody in Warden Territory, the summon is cancelled
- [ ] GameTests cover a remote shriek, a friend's shriek and a failing spot
