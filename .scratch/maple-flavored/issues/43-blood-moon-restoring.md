# 43: Blood Moon Restoring

**What to build:** After a Blood Moon, structures' original mobs return at their original spots, making Embers and Pith slowly renewable.

**Blocked by:** 42 (Blood Moon night)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [x] Bastion Piglins, Brutes and Hoglins, Ocean Monument Elder Guardians, End City Shulkers and Mansion Vindicators and Evokers are tagged with their spots at generation
- [x] A spot empties only when its mob is killed, converted or despawned; Restored mobs keep the original's persistence
- [x] A spot empty when a Blood Moon rose is pending: it fills when its chunk loads, or every few seconds while loaded, once it is clear, its block holds and no online player is within 32 blocks
- [x] Pending spots don't expire at dawn; a mob that dies after a Blood Moon waits for the next one; missed Blood Moons don't stack; no forced loading
- [x] Blocks, chests, books, Allays and broken spawners never return
- [x] GameTests cover a killed mob returning, a moved mob holding its spot, a missing block, a nearby player who then leaves a loaded chunk, a fill after dawn and reloads
- [ ] Hand check: in a new world, kill a Bastion Piglin, `/time set maple_flavored:blood_moon`, go more than 32 blocks away and back: it has returned
