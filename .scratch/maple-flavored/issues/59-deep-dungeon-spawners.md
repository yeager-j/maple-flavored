# 59: Deep dungeon spawners

**What to build:** A dungeon generated below Y 0 holds a Deep variant spawner: Deep Zombie, Deep Skeleton or Deep Spider in place of the mob vanilla chose. Deep dungeons are as dangerous as the caves around them.

**Blocked by:** 53 (Deep Skeleton, Deep Creeper and Deep Spider)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder, user stories 165f and 165h, the Deep variants module, and the Dungeons bullet under Loot and worldgen data.

- [ ] When a dungeon's spawner block is below Y 0, the zombie, skeleton or spider vanilla chose for it becomes that mob's Deep variant. Dungeons at Y 0 or above keep vanilla's mob
- [ ] The dungeon's book still follows its mob: Smite for a Deep Zombie or Deep Skeleton, Bane of Arthropods for a Deep Spider
- [ ] What a deep dungeon spawns are full Deep variants: their health and damage from the Deep variant game rules, their advantage, their base mob's loot and double XP. They count towards Clearing like any other spawned mob
- [ ] Every other spawner keeps its mob at any depth: Mineshaft cave spider spawners, Fortress Blaze spawners, the Stronghold silverfish spawner, trial spawners, and spawners placed or changed by players
- [ ] `docs/project-overview.md`'s Deep variants paragraph says deep dungeons hold Deep variant spawners
- [ ] GameTests: a dungeon placed with the game's own feature below Y 0 holds a Deep variant and one at Y 0 or above holds the vanilla mob, each with the right book; a deep dungeon's spawner spawns a Deep Zombie with Deep stats; a Mineshaft cave spider spawner below Y 0 keeps cave spiders

## Notes

- `MonsterRoomFeatureMixin` already wraps the dungeon's `setEntityId` call to give the spawner its book. Swap the type there, before the book is chosen, from the Deep variants module's base-to-variant map, so the book's tag check sees the Deep variant.
- `SpawnerBooks` picks the book from `sensitive_to_smite` and `sensitive_to_bane_of_arthropods`, which the Deep variants are in, so no book change should be needed. Confirm it with the test.
- A deep dungeon farm gives double XP per kill, since Deep variants drop twice their base mob's XP. That follows the spec; flag it in playtesting if XP farms get too strong.
