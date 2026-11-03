# 26: Warden Subspace sculk and shriekers

**What to build:** A Warden Subspace looks and grows like sculk, and the shriekers it grows stop summoning when it ends.

**Blocked by:** 25 (Warden Subspace)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [ ] Sculk spreads from the centre with vanilla catalyst rules, inside the Subspace, while active, at a playtest rate
- [ ] Grown shriekers record their Subspace and can summon; they lose summoning when it ends, updated on load if unloaded
- [ ] Ending one overlapping Subspace cleans up only its own shriekers
- [ ] The client shows a sculk sky inside
- [ ] GameTests cover spread limits and shrieker cleanup, including an unloaded shrieker
- [ ] Hand check: sculk sky and spread
