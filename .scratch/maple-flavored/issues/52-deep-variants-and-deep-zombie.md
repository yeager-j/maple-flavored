# 52: Deep variants and the Deep Zombie

**What to build:** The Deep variant framework, with the Deep Zombie as its first mob. Below Y 0, natural spawns of adult zombies become Deep Zombies: tougher, harder-hitting zombies that disable shields.

**Blocked by:** 31 (Common mob changes)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder, user stories 165a–165c, 165f and 165g, the Deep variants module, the two Deep variant game rules, and the Deep variant bullet under Spec defaults.

- [ ] A registered `maple_flavored:deep_zombie` entity type built on the zombie, with a renderer, its own texture and a spawn egg. It's in every entity tag a zombie is in
- [ ] In the Overworld, a natural spawn that picks an adult zombie at a spot below Y 0 spawns a Deep Zombie instead. A baby roll stays a baby zombie. Spawners, spawn eggs, commands, structures, conversions and Blood Moon Restores are unaffected
- [ ] A Deep Zombie's reinforcements are Deep Zombies
- [ ] 30 health and 6 attack, from the **Deep variant health percent** (150) and **Deep variant damage percent** (200) game rules applied to the zombie's vanilla values at spawn
- [ ] The Mobs table's adult zombie row applies apart from health and Hard's damage: zombie speed, step-up and Hard's jump
- [ ] Each hit on a blocking player disables their shield for 100 ticks, as an axe does
- [ ] Drops the zombie's loot table and twice a zombie's vanilla XP
- [ ] Deep variants are described in `docs/project-overview.md` (the glossary entry already exists)
- [ ] GameTests: a natural-style spawn below Y 0 gives a Deep Zombie and one above gives a zombie, a spawner below Y 0 gives a zombie, health and attack match the game rules, a hit disables a mock player's shield, Smite deals its bonus, and drops and XP match
- [ ] Hand check: the Deep Zombie texture reads as a deepslate zombie in a dark cave, and a raised shield goes on cooldown when hit

## Notes

- Confirm on the 26.3 jar how an axe disables a shield (the cooldown length and how it's applied) and reuse that path.
- Hook natural spawning where vanilla picks the mob type, so spawn weights and caps stay vanilla. Where the code already wraps spawning (`SpawnPlacementsMixin`, `CreeperSpawns`), follow its pattern.
- The texture can start as a deepslate-crusted version of the zombie's, made with the image tools (Pillow or ImageMagick). Jackson reviews it in game. List any reused asset in `CREDITS.md`.
- Build the framework so the next three variants (ticket 53) only add their entity type, numbers and advantage.
