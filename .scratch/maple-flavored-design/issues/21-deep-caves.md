# Deep caves

Type: grilling
Status: resolved
Blocked by: 05, 10, 14

## Question

How do we make deep caving dangerous for a day-one player without making the surface harder? Today a player with some iron, a shield and torches can find a huge deepslate cave full of exposed diamonds and reach full diamond gear in a few hours. Caving should be easy once you have Netherite, but before that it should make you think twice about going deep.

## Answer

**Changed on 2026-11-10:** Deep Cores are dropped, and the book moves under Books are undone. The three tiers and the Deep variants stand. The sections on Deep Cores, the raid and Books below are kept as a record only. See the last comment.

Danger rises in three tiers by height. Surface mobs stay weak, underground mobs are about vanilla strength, and below Y 0 every common hostile is a much stronger Deep variant. The big, loot-rich caves get a Deep Core, which keeps them dark and strengthens their mobs until players beat its raid. Deep variants make all of deepslate dangerous, tunnels included, while Deep Cores shut down the jackpot rooms, so neither works alone. Terms are in `GLOSSARY.md`. Numbers are starting values that playtesting can change. The rules are fixed.

The balance target is a player in full iron with an iron sword and a shield. They can beat one of each Deep variant and survive two or three with care. A player in leather or copper with a copper sword loses to two.

### Three tiers

This changes [Mob changes](05-mob-changes.md).

- **Surface, Y 63 and up:** the Mob changes stats as they are. Creepers still don't spawn here.
- **Underground, Y 0 to 62:** adult zombies, baby zombies and their kin, and skeletons and their kin have vanilla health. Creepers are fully vanilla. Every change that isn't health stays: zombie speed, step-up and Hard's damage and jump, and spider jump and no fall damage. Cave spiders and silverfish keep their weaker numbers.
- **Deep, below Y 0:** Deep variants.

The tier is judged where a mob spawns, and a mob keeps its stats wherever it goes.

### Deep variants

Every natural spawn of an adult zombie, skeleton, creeper or spider below Y 0 is its Deep variant. Spawners, baby zombies, zombie villagers, drowned and cave spiders are unchanged. Deep variants have 1.5 times their base mob's vanilla health and deal double its damage, which vanilla then scales by difficulty. They drop their base mob's vanilla drops and double XP, and each has its own look.

| Deep variant | Health | Damage | Advantage |
| --- | --- | --- | --- |
| Deep Zombie | 30 | 6 | Each hit disables a shield for 5 seconds, like an axe. Underground zombie speed and step-up. |
| Deep Skeleton | 30 | Double arrow damage | Shoots a stray's Slowness arrows. 32 follow range. |
| Deep Creeper | 20 | Explosion power 4, like TNT | Vanilla fuse |
| Deep Spider | 24 | 4 | Its bite gives Blindness for a few seconds. |

There's no global light rule below Y 0. Vanilla's block light 0 spawning stays.

### Deep Cores

A Deep Core is a small structure in a large open cave below Y 0. Its block and template are designed later.

**Placement.** The structure follows the cell scan in [the cave structure research](../../../docs/research/vanilla-cave-structure-placement.md), section 6: `random_spread` spacing 8, relaxed probes at ±12 blocks, a minimum open height of 14, a floor between Y −52 and −4, and a guard against the Deep Dark and Ancient Cities. No Deep Core generates within 25 blocks of another, measured in 3D. A cell can keep a second Deep Core when it sits more than 35 blocks above or below the first, outside its zone, so stacked cave systems each get one. A prototype world showed about 20 Deep Cores per km², and placement may change after playtesting.

**Zone.** A box 100 blocks wide, 100 long and 70 high, centred on the core. Zones can overlap, so a core on the far side of a cave still covers most of it.

**Level.** Each Deep Core has a level from I to V, set by its depth: I from Y −4 to −13, II from −14 to −23, III from −24 to −33, IV from −34 to −43 and V from −44 to −52. The block shows its level, and the boss bar names it, as in "Deep Core III".

**Snuffing.** Any block that emits light and appears in the zone after the Deep Core generated is snuffed, fire included. Blocks with an unlit state, such as furnaces, campfires, candles and redstone lamps, switch to it. Everything else pops off as an item with smoke and a fizz, and a trail points from it to the Deep Core, the way a hit Creaking points to its Creaking Heart. Light already there when the core generated stays. Lava is exempt, so pouring it is a risky way to light a cave. Night Vision still works.

**Buffs.** Every hostile mob in the zone has these effects while it's there:

| Level | Buffs |
| --- | --- |
| I | Speed I |
| II | Speed I, Strength I |
| III | Speed I, Strength I, Resistance I |
| IV | Speed I, Strength II, Resistance I |
| V | Speed I, Strength II, Resistance I, Regeneration I |

**The block.** Until its raid is won, a Deep Core can't be mined, is immune to explosions, can't be pushed by pistons and gives no light.

### The raid

A Deep Core reuses vanilla Raid mechanics.

- **Start.** A player starts the raid by hitting the Deep Core. It's opt-in, like drinking an Ominous Bottle.
- **Waves.** 3 on Easy, 5 on Normal and 7 on Hard, with an extra wave at level II and above. Waves are only Deep variants, with no creepers, and the buffs replace a support mob.
- **Spawning.** A wave arrives all at once, at random valid spots 8 to 24 blocks from the core, inside the zone, preferring spots at least 8 blocks from any player.
- **Pacing.** The boss bar reads as the Deep Core's health and refills between waves, with vanilla's Raid delay. If a wave has 2 or fewer mobs left after 30 seconds, they glow.
- **Players.** Each wave adds about 25% more mobs per extra player in the zone when the raid starts.
- **Win.** When the last wave dies, the Deep Core breaks and snuffing ends. It drops 1 Fortune book and Pith: 1 at levels I and II, 2 at III and IV, and 3 at V. A Deep Core has no Cleared but still standing state, so it can't be a farm. "Cleared" stays a spawner word.
- **Loss.** If no player is in the zone for 30 seconds, or every player who started the raid dies, the raid ends and the Deep Core heals to full. Mobs that already spawned stay.
- **Gone for good.** Blood Moons don't restore a beaten Deep Core.
- **Advancement.** One new advancement for beating your first Deep Core. It isn't a Milestone.

| Mob | W1 | W2 | W3 | W4 | W5 | W6 | W7 | Extra |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Deep Zombie | 2-3 | 2-3 | 2-4 | 3-4 | 3-5 | 4-5 | 5-6 | 5-6 |
| Deep Skeleton | 1-2 | 1-2 | 2-3 | 2-3 | 3-4 | 3-4 | 3-4 | 4-5 |
| Deep Spider | | 1 | 1-2 | 1-3 | 2-3 | 2-3 | 3-4 | 3-4 |
| **Total** | 3-5 | 4-6 | 5-9 | 6-10 | 8-12 | 9-12 | 11-14 | 12-15 |

A Normal run is 26 to 42 mobs, a little fewer than a Raid's first five waves (31 to 53), because every Deep variant is tougher and buffed.

### Books

This changes [Enchanting](10-enchanting.md) and [Illager book drops](19-illager-book-drops.md).

| Enchantment | Old source | New source |
| --- | --- | --- |
| Fortune | Woodland Mansion | Deep Core |
| Looting | Jungle Temple | Woodland Mansion |
| Thorns | Bastion, hoglin stables | Jungle Temple |
| Knockback | Ravager (uncommon) | Bastion, hoglin stables |

Projectile Protection stays on Pillager Outposts only. Fortune III now takes 4 Deep Cores, not 4 Woodland Mansions, so it's much easier to reach. Each Deep Core is a raid in the dark, which keeps it earned. Ravagers no longer drop Knockback, and what Raids give instead is left open.

### Tuning

- **Playtest game rules in `MapleGameRules`:** zone size, buffs per level, multiplayer scaling, Deep variant health and damage, and the 30-second loss timer.
- **World-generation data:** placement, the 25-block spacing and the depth bands. They're read during world generation, so a datapack can change them, but only for newly generated chunks.

### Left open

- What Raids give in place of Knockback.
- The Deep Core block and structure template.

## Comments

Settled in discussion with Jackson before grilling (2026-11-07):

- **Deep variants.** Deep Zombie, Deep Skeleton, Deep Creeper and Deep Spider spawn below Y 0. They have more health and deal more damage than their base mobs, since you shouldn't fight them with copper tools.
  - Deep Zombie hits disable shields.
  - Deep Skeleton arrows inflict Slowness.
  - Deep Creeper explosions have power 4, like TNT, against vanilla's 3 and a charged creeper's 6.
  - Deep Spider's advantage is still open.
- **Cores.** A small structure generates in large open caves below Y 0. Its block and structure template are designed later.
  - Inside its zone, placed light sources pop off as items, and the snuffed light sends a stream of particles toward the core. This guides players to it and tells a story. Natural light isn't affected.
  - It acts as a reverse beacon: hostile mobs nearby are buffed.
  - It reuses Clearing from spawners, and drops Pith and the Fortune book.
  - The zone is a box 100 wide, 100 long and 70 high, centred on the core: 50 blocks out horizontally, 35 up and 35 down. Jackson calibrated this in a large cave system that got 4 cores, where it covered the system without much overlap. Some overlap is healthy, so a core on the far side of a cave doesn't leave much of it uncovered.
  - No core generates within 25 blocks of another.
  - Placement follows the cell scan in [the cave structure research](../../../docs/research/vanilla-cave-structure-placement.md), section 6, with Jackson happy with it in a prototype world so far. It may need tweaking in playtesting.
- **Fortune moves to cores.** Woodland Mansions need a replacement book.
- Tears of the Kingdom's Gloom and Lightroots inspired the cores. The concept is fair game, but the names aren't ours to use.

Grilling round 1 (2026-11-07):

- The core is the **Deep Core**.
- Three tiers: surface mobs stay weaker, underground mobs are about vanilla strength, and Deep variants are much stronger. Caving from Y 60 down to Y 0 should be somewhat challenging too.
- Every natural spawn of an adult zombie, skeleton, creeper or spider below Y 0 is its Deep variant. Spawners, baby zombies, zombie villagers, drowned and cave spiders are unchanged.
- Balance target: full iron with an iron sword and a shield beats one of each Deep variant and survives two or three with care. Leather or copper with a copper sword loses to two.
- Deep variants drop their base mob's vanilla drops and double XP.
- No global light rule below Y 0.
- Books: Fortune goes to Deep Cores, Looting to Woodland Mansions, Thorns to Jungle Temples, and Knockback to the Bastion's hoglin stables. Projectile Protection stays on Pillager Outposts only, since 8 finds from rare Jungle Temples is too many. Raids get something to replace Knockback later.
- A Deep Core reuses Raid mechanics: a health bar and waves of mobs, with numbers comparable to vanilla Raids.
- Each Deep Core has a difficulty, like a Raid. Higher difficulties give stronger buffs, and Resistance and Regeneration are on the table for high ones.
- Snuffing covers fire too.
- Snuffed light points to its Deep Core the way a hit Creaking points to its Creaking Heart.
- The 25-block spacing shouldn't ignore height. A Deep Core high up, around Y −6, shouldn't stop a huge cave system below it from getting its own.
- The Deep Creeper keeps vanilla's fuse.
- Deep Skeletons shoot a stray's Slowness arrows. A Deep Zombie's hit disables a shield for 5 seconds, like an axe.
- A Deep Spider's bite gives Blindness for a few seconds.

Grilling round 2 (2026-11-07):

- The surface ends at Y 63, judged where a mob spawns, the same line as the creeper rule.
- Below Y 63:
  - Adult zombies, baby zombies and their kin, and skeletons and their kin go back to vanilla health.
  - Every change that isn't health stays everywhere: zombie speed, step-up and Hard's damage and jump, and spider jump and no fall damage.
  - Creepers are fully vanilla, which effectively removes their row.
  - Cave spiders and silverfish keep their weaker numbers.
- A player hitting the Deep Core starts its raid. It's opt-in, like drinking an Ominous Bottle, and the health bar reads as the core's own.
- Waves follow vanilla Raids: 3 on Easy, 5 on Normal and 7 on Hard, with an extra wave at Deep Core difficulty II and above.
- Difficulty is levels I to V, set by depth, deeper being harder.
- The boss bar names the level ("Deep Core III"), and the block shows it.
- Buffs reach every hostile mob in the zone at all times. Starting shape, with each level a playtest game rule:

  | Level | Buffs |
  | --- | --- |
  | I | Speed I |
  | II | Speed I, Strength I |
  | III | Speed I, Strength I, Resistance I |
  | IV | Speed I, Strength II, Resistance I |
  | V | Speed I, Strength II, Resistance I, Regeneration I |

- Winning: when the last wave dies, the core breaks, drops its Pith and Fortune book, and snuffing ends. A Deep Core has no Cleared but still standing state, so it can't be a farm. "Cleared" stays a spawner word.
- Losing: if no player is in the zone for 30 seconds, or every player who started the raid dies, the raid ends and the core heals to full. Mobs already spawned stay.
- Each wave adds about 25% more mobs per extra player in the zone when the raid starts.
- Rewards: always 1 Fortune book. Pith is 1 at levels I and II, 2 at III and IV, and 3 at V.
- The 25-block spacing is measured in 3D. A cell can keep a second Deep Core when it sits more than 35 blocks above or below the first, outside its zone.
- Ravagers stop dropping Knockback. Raids get a replacement later, as its own question.
- Waves are made only of Deep variants. The reverse beacon makes a support mob unnecessary.

Grilling round 3 (2026-11-07):

- Vanilla doesn't change mob health by difficulty, only damage. Deep variants have 1.5 times their base mob's vanilla health and deal double its damage, which vanilla then scales by difficulty. Deep Creepers keep 20 health, with explosion power 4.
- Creepers arguably make a raid easier, since they blow up and take other mobs with them. Leave them out of waves, or use them only in early waves.
- Levels by depth, in 10-block bands: I from Y −4 to −13, II from −14 to −23, III from −24 to −33, IV from −34 to −43, and V from −44 to −52. The bands are world-generation data and may move after playtesting.
- Wave mobs spawn all at once at random valid spots 8 to 24 blocks from the core, inside the zone, preferring spots at least 8 blocks from any player.
- Between waves, the boss bar refills with vanilla's Raid delay. If a wave has 2 or fewer mobs left after 30 seconds, they glow.
- Snuffing is a blanket rule: any block that emits light and appears in the zone after the Deep Core generated is snuffed. Blocks with an unlit state switch to it, and everything else pops off as an item. Light already there when the core generated stays.
- Lava is exempt.
- The Deep Core can't be mined, is immune to explosions, can't be pushed by pistons and gives no light, until its raid is won.
- Beaten Deep Cores stay gone. Blood Moons don't restore them.
- One new advancement for beating your first Deep Core. It isn't a Milestone.
- Playtest game rules: zone size, buffs per level, multiplayer scaling, Deep variant health and damage, and the 30-second loss timer. World-generation data: placement, the 25-block spacing and the depth bands.

Reversal (2026-11-10):

- Deep variants stay. In playtesting, Jackson was more hesitant to go into a massive cave for diamonds on day one, which was the aim.
- Deep Cores are dropped before any of them was built.
  - They only covered big caves, which hold about 10% of exposed diamonds.
  - Jackson wasn't happy with how they generated.
  - A mob switch turns off their passive effects.
- Ideas considered and set aside:
  - Intense Darkness: light sources below Y 0 reach only 1 to 2 blocks until a Deep Core is beaten.
  - Deep Cores on an even grid, each owning the territory nearest to it, like Lightroots.
  - Beating a core permanently changes a region, which sits badly with design rule 3.
- Diamond scarcity replaces them: an ore drops a piece, and 4 craft 1 diamond, as Netherite scrap does. It slows every route to diamonds, branch mining included. It's ticket 61 in the build tracker.
- The book moves are undone, since Fortune leaving the Woodland Mansion was their only cause. Fortune is back in Woodland Mansions, Looting in Jungle Temples, Thorns in the Bastion's hoglin stables and Knockback on Ravagers. "What Raids give in place of Knockback" is no longer open.
- Jackson's Deep Core template, `src/main/resources/data/maple_flavored/structure/deep_core.nbt`, was deleted with ticket 61. It is in git history if Deep Cores come back.
