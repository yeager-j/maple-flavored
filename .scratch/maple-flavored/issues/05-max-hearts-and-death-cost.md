# 05: Max Hearts and death cost

**What to build:** A player starts at 10 Max Hearts. Death costs Max Hearts instead of items: 1 per death, 2 on Hard, never below the Heart Floor, which is fixed at 10 until Milestones exist. This ticket builds the Hearts module's state and death hook, and the Keep Hearts and Keep Inventory game rules.

**Blocked by:** 01 (GameTest harness)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [ ] Max Hearts are per player, start at 10, apply as max health, and survive respawn and relog
- [ ] Death on Peaceful, Easy and Normal costs 1 Max Heart and on Hard costs 2, never going below the Heart Floor and never raising Max Hearts
- [ ] Hardcore keeps vanilla's death rule
- [ ] Keep Hearts game rule, default off; when on, death costs no Max Hearts
- [ ] Keep Inventory defaults to on in new worlds and decides only items (XP is handled by Keep XP in the Remnants ticket)
- [ ] The death screen shows the Max Hearts lost
- [ ] GameTests cover each difficulty, the floor clamp and both game rules with mock players
- [ ] Hand check: the death screen line
