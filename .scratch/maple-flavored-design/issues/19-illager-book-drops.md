# Illager book drops

Type: grilling
Status: resolved

## Question

What odds sit behind the "uncommon" and "rare" Illager book drops? Does Looting help, and how many books can an Evoker drop? The answer must account for the Stronghold Illagers from [Stronghold Illagers](18-stronghold-illagers.md).

## Answer

Each Illager that drops books rolls for each of its books on its own, with odds set per mob from how many of that mob a world gives. "Uncommon" and "rare" only order the books within one mob. Odds are starting values for playtesting.

### Odds

| Mob | Book | Chance |
| --- | --- | --- |
| Vindicator | Sharpness | 10% |
| Vindicator | Sweeping Edge | 2.5% |
| Pillager Captain | Power | 20% |
| Pillager Captain | Punch | 5% |
| Pillager Captain | Infinity | 5% |
| Evoker | Protection | 15% |
| Ravager | Knockback | 10% |

### Rules

- Books drop only when a player kills the mob, as with Ancient Embers, so traps at Restored structures don't farm them.
- Looting adds 20% of the base chance per level, the same rule as Ancient Embers, so Looting III gives 1.6×. Orichalcum's drop bonus still skips enchanted books.
- Each book is its own roll, so a Captain can drop Power and Punch from one kill.
- An Evoker gets one Protection roll, not several. It still always drops a Totem of Undying and has a 25% chance of Pith.
- Every Illager not spawned by a raid wave can drop books, wherever it came from: Strongholds, Woodland Mansions, outposts, patrols and Blood Moon Restores. An Illager that later joins a raid keeps its drops.
- Ravagers always roll Knockback. They only spawn naturally in raids, so this is the raid's one book.
- Plain Pillagers drop no books. Bow books belong to Captains.

### Supply

Expected kills for each target, without Looting / with Looting III. A Woodland Mansion holds about 20 Vindicators and 1 to 3 Evokers. A Stronghold holds about 8 Vindicators and 1 to 3 Evokers.

| Target | Books | Kills |
| --- | --- | --- |
| Sharpness III | 4 | 40 / 25 Vindicators, about 2 Mansions |
| Sharpness V | 16 | 160 / 100 Vindicators |
| Sweeping Edge III | 4 | 160 / 100 Vindicators |
| Power III | 4 | 20 / 13 Captains |
| Power V | 16 | 80 / 50 Captains |
| Punch II | 2 | 40 / 25 Captains |
| Infinity | 1 | 20 / 13 Captains |
| Protection I | 1 | 7 / 4 Evokers |
| Protection IV | 8 | 53 / 33 Evokers |
| Knockback II | 2 | 20 / 13 Ravagers, about 6 Hard raids |

- Captain odds are high because Captains are scarce without a farm: patrols come about every 50 minutes. An outpost farm, where 6% of spawned Pillagers are Captains, gets there in hours. That's the farm's reward, and it's a harder build than a raid farm, which gives no bow books.
- Sharpness V and Sweeping Edge III land together, about 5 Mansions' worth of Vindicators counting Blood Moon Restores.

## Comments

Resolves the "Illager book drops" design task in the Maple Flavored spec (ticket 03 in `.scratch/maple-flavored/issues/`). Vanilla counts came from the Minecraft Wiki; the Mansion counts are estimates.

From [Deep caves](21-deep-caves.md): Knockback moved to the Bastion's hoglin stables, so Ravagers no longer drop it. What Raids give instead is left open on the design map.
