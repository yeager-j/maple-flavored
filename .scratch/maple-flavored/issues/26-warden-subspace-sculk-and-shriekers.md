# 26: Warden Subspace sculk and shriekers

**What to build:** A Warden Subspace looks like sculk and grows a little, and the shriekers it grows stop summoning when it ends.

**Blocked by:** 25 (Warden Subspace)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder. Plan: `../plans/26-warden-subspace-sculk-and-shriekers.md`.

- [x] A weak sculk patch spreads where the Warden appeared, with vanilla catalyst rules, within about 5 blocks, while the Subspace is active and its Warden loaded (changed from spreading across the whole Subspace)
- [x] Grown shriekers record their Subspace and can summon; they lose summoning when it ends, updated on load if unloaded
- [x] Ending one overlapping Subspace cleans up only its own shriekers
- [x] The client shows a sculk sky inside and a sculk shell at the edge
- [x] The Warden Subspace radius is 50, and it and the patch's radius and charge are playtest game rules
- [x] GameTests cover spread limits and shrieker cleanup, including an unloaded shrieker
- [ ] Hand check: the sculk sky and shell (from inside and outside, through caves, against water and glass, with Fabulous on and off), the patch, grown shriekers silenced when the Warden dies, and the game rules taking effect on the next Warden
