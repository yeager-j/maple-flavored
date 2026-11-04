# 12: Zephyr

**What to build:** Zephyr is a boots enchantment with 3 levels that gives a double jump. A jump during a fall Clutches, cutting the fall so far.

**Blocked by:** 01 (GameTest harness)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [x] Registered Zephyr enchantment on boots, 3 levels
- [x] Double jump reaches 1.5, 2.25 and 3 blocks at levels I to III (starting values), once per air time, with client input and server checks
- [x] Fall distance isn't reset, and the height the jump regains is taken off again, so the jump is neutral
- [x] Clutch cuts the distance fallen before the jump by 10%, 15% or 25%
- [x] Credit Matcha Flavoured for the idea
- [x] GameTests cover the server's checks and fall-distance results
- [ ] Hand check: jump feel, heights and Clutch timing
