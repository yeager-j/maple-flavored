# 47: Custom Crystal Heart use animation

**What to build:** Consuming a Crystal Heart gets its own first-person pose in place of the eating animation: the heart held upright toward the centre of the screen, rising as the 1.6 seconds run out.

**Blocked by:** 07 (Crystal Heart, Pith and Netherite recipe)

**Status:** ready-for-human

**When:** after 1.0. This is polish.

Spec: the Maple Flavored spec in this folder.

- [ ] In first person, from either hand, the heart is held upright and facing the player, not turned 90 degrees, and rises toward the centre while in use
- [ ] Third person uses a built-in pose that fits, or a matching custom one
- [ ] The rest of the use stays the same: 1.6 seconds, the amethyst chime, no consume particles
- [ ] Hand check in game: both hands, first and third person

## Notes

These built-in animations were tried in game for ticket 07:

- `toot_horn` shows nothing in first person.
- `block` turns the heart 90 degrees.
- `eat` works but looks goofy. It is the current animation, set in the Crystal Heart's consumable in `MapleItems`.

Minecraft 26.3 has no hook for new use animations. First-person use poses come from a `switch` on the use animation in `FirstPersonHandsAndItemsRenderer`, and third-person arm poses from `HumanoidModel` and `AvatarRenderer`. A custom pose likely needs a client mixin that applies its own transform when the item in use is a Crystal Heart.
