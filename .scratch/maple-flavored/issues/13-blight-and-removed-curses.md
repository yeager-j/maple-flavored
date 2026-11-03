# 13: Blight and removed curses

**What to build:** Blight is a new weapon enchantment that applies Wither. Curse of Binding and Curse of Vanishing are removed.

**Blocked by:** 01 (GameTest harness)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [ ] Blight is data-driven: Fire Aspect's weapons, 2 levels, exclusive with Fire Aspect, Wither I for 4 seconds per level on hit
- [ ] Each hit re-applies Wither and refreshes its duration, so a player landing repeated hits keeps the effect running and can wither hearts. Check whether vanilla's effect refresh already does this before adding code
- [ ] Curses are removed from every source and aren't counted in the Library
- [ ] Credit Matcha Flavoured for Blight
- [ ] GameTests cover the effect on hit, the refresh on repeated hits and the exclusivity
