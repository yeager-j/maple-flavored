# 05: Max Hearts and death cost

**What to build:** A player starts at 10 Max Hearts. Death costs Max Hearts instead of items: 1 per death, 2 on Hard, never below the Heart Floor, which is fixed at 10 until Milestones exist. This ticket builds the Hearts module's state and death hook, and the Keep Hearts and Keep Inventory game rules.

**Blocked by:** 01 (GameTest harness)

**Status:** done

Spec: the Maple Flavored spec in this folder.

- [x] Max Hearts are per player, start at 10, apply as max health, and survive respawn and relog
- [x] Death on Peaceful, Easy and Normal costs 1 Max Heart and on Hard costs 2, never going below the Heart Floor and never raising Max Hearts
- [x] Hardcore keeps vanilla's death rule
- [x] Keep Hearts game rule, default off; when on, death costs no Max Hearts
- [x] Keep Inventory defaults to on in new worlds and decides only items (XP is handled by Keep XP in the Remnants ticket)
- [x] The death screen shows the Max Hearts lost
- [x] GameTests cover each difficulty, the floor clamp and both game rules with mock players
- [ ] Hand check: the death screen line
