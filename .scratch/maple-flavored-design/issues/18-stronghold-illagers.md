# Stronghold Illagers

Type: grilling
Status: resolved

## Question

Which Illagers fill a Stronghold, how many and where, so that reaching the End Portal is a fight and Illager books have a home outside raids? The answer must work with Blood Moon Restoring, which needs the stone bricks family beneath each spot.

## Answer

Strongholds hold Vindicators, Pillagers and Evokers, placed by piece at generation on spots that a Blood Moon can always Restore. Counts are starting values for playtesting.

### Mobs

- Vindicators, Pillagers and Evokers only.
- No Pillager Captains. A Captain drops an ominous bottle, so Blood Moons would make raids and Captain books renewable. Captain books stay with outposts and patrols, which are hard to farm.
- No Ravagers. They barely fit the Stronghold's doorways, and their Knockback book already has the raid as its home.
- Evokers are capped at 1 in the portal room and 1 per library, so 1 to 3 per Stronghold. Each always drops a Totem of Undying and has a 25% chance of Pith, so a Blood Moon renews about 0.25 to 0.75 Pith per Stronghold. Their Vexes pass through walls, which is what makes the book alert so dangerous.

### Placement

The Stronghold's Unbreaking book sits in the portal room, which vanilla always generates, so the strongest guard stands where every player is going.

| Piece | Illagers |
| --- | --- |
| Portal room | 1 Evoker, 2 Vindicators |
| Library | 1 Evoker, 1 Vindicator |
| Room crossing | 2 rolls |
| Prison hall | 1 Vindicator |
| Five-way crossing | 1 roll |
| Corridors, turns and stairs | 25% chance of 1 roll |
| Starting staircase | none |

A roll is a Vindicator or a Pillager, 50/50. A Stronghold holds about 12 to 20 Illagers. The starting staircase stays empty so players can step inside before the first fight.

### Spots

- A spot is picked only where the stone bricks family is beneath it, the same check a Blood Moon makes, so every Stronghold Illager can be Restored. Oak plank floors and spots where caves or Mineshafts cut through are skipped.
- The stone bricks family is stone bricks, cracked, mossy and chiseled stone bricks, their slabs and stairs, and the infested variants of all four. Strongholds are full of infested blocks, and players can't tell them apart.
- Each spot needs a clear 1×2×1 space above it. No two spots share a block, and no spot sits in a doorway.

### Behavior

- Illagers are persistent and wander as mansion Illagers do, with no home restriction. One that wanders into a cave still holds its spot until it dies.
- Counts are fixed at generation and don't scale with player count. On a server the first player through clears the way until the next Blood Moon, and taking the book still alerts every Illager left in the structure.
- The portal room's silverfish spawner and the Stronghold's infested blocks stay vanilla.

## Comments

Resolves the "Stronghold Illagers" design task in the Maple Flavored spec (ticket 02 in `.scratch/maple-flavored/issues/`).
