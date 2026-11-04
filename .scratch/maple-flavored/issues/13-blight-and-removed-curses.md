# 13: Blight and removed curses

**What to build:** Blight is a new weapon enchantment that applies Wither. Curse of Binding and Curse of Vanishing are removed.

**Blocked by:** 01 (GameTest harness)

**Status:** done

Spec: the Maple Flavored spec in this folder.

- [x] Blight is data-driven: Fire Aspect's weapons, 2 levels, exclusive with Fire Aspect, Wither I for 4 seconds per level on hit
- [x] Each hit re-applies Wither and refreshes its duration, so a player landing repeated hits keeps the effect running and can wither hearts. Check whether vanilla's effect refresh already does this before adding code
- [x] Curses are removed from every source and aren't counted in the Library
- [x] ~~Credit Matcha Flavoured for Blight~~ Dropped: Matcha has no Wither-applying enchantment to credit, so Blight is Maple's own
- [x] GameTests cover the effect on hit, the refresh on repeated hits and the exclusivity

Vanilla's effect refresh already keeps Wither running: a re-hit with the same amplifier takes the longer duration on the same effect, so no code was needed. The curses are filtered out of the enchantment registry, which leaves 43 enchantments (a Full Library is all but Mending). Vanilla's Trade Rebalance experiment names Curse of Binding in its trades, so it is unsupported and left out of the GameTest server.
