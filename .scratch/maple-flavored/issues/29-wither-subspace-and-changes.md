# 29: Wither Subspace and Wither changes

**What to build:** The Wither opens a Subspace where Withered Hearts never return, and can't be trapped.

**Blocked by:** 07 (Crystal Heart, Pith and Netherite recipe), 08 (Withered Hearts), 13 (Blight and removed curses), 25 (Warden Subspace)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [x] The Wither opens a 100-block Subspace where it is summoned; Withered Hearts never return inside, in any dimension; it applies no effect itself
- [x] The Wither stays within 50 blocks of the centre, is immune to suffocation and passes through bedrock and obsidian
- [x] It drops a level I Blight book every kill
- [x] Advancement: Kill the Wither (Nether goal, under Withering Heights), Crystal Heart reward, to every player inside its Subspace
- [x] GameTests cover recovery blocked inside and allowed outside, and the bedrock and obsidian rules
- [ ] Hand check: build a Wither in the Overworld and confirm Withered Hearts don't return within 100 blocks but do past it, that it won't follow you past 50 blocks from where it was built, that it flies through a bedrock or obsidian cage (such as the Nether roof) without taking suffocation damage, and that a kill drops a Blight I book and grants Kill the Wither with a Crystal Heart to everyone inside

The leash covers every move the Wither makes (its chase, its flight, knockback and pistons), measured in 3D from the centre like the Subspace's sphere. A move that collisions would push past the leash is undone. Killing it credits every non-spectator player inside; a Wither removed any other way, such as by Peaceful, credits nobody. The radii are constants, not game rules.
