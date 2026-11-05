# 27: Warden summoning in Warden Territory

**What to build:** Shrieks only count for players in Warden Territory, which closes the Warden switch.

**Blocked by:** 25 (Warden Subspace)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder. Plan: `../plans/27-warden-summoning.md`.

- [x] Warden Territory is the Deep Dark biome or an Ancient City's bounds
- [x] A shriek gives warning levels and cooldowns only to players in Warden Territory
- [x] The Warden spawns 5 to 15 blocks from the player who caused the shriek, or the nearest player in Warden Territory; with no room, the search widens to 50 blocks from that player; if that fails, the cooldown clears
- [x] With nobody in Warden Territory, the summon is cancelled
- [x] GameTests cover a remote shriek, a friend's shriek and a failing spot
- [ ] Hand check: in an Ancient City, a shriek at the fourth warning summons a Warden 5 to 15 blocks from you, not at the shrieker; an arrow fired from outside Warden Territory at a shrieker gives you no warning; and after a summon with no room, the next shriek summons again without waiting out a cooldown
