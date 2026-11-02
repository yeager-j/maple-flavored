# Design Maple Flavored

Labels: wayfinder:map

## Destination

A complete design spec for Maple Flavored, covering the whole game from first night through the End, with enough numbers to break into build tickets. Build order is decided after the spec.

## Notes

Domain: a Fabric mod for Minecraft 26.3, required on client and server, for new worlds only, using real registered items and blocks. It stands alone, with no required content mods; library and API mods are fine.

Every grilling ticket uses the `grilling` and `domain-modeling` skills, and reads [the Matcha manifesto](../../docs/manifestos/matcha-flavoured.md) for the opinions behind the design.

**Start from vanilla.** This isn't a port of Matcha Flavoured. Vanilla is the baseline, and each feature is included on purpose, so the spec records what we add or change, never what we chose not to copy. Matcha mechanics can be reused with credit.

Design rules:

1. **Vanilla's vocabulary stays.** A new or changed thing needs a gameplay reason. New lore can build on vanilla's but never replaces it.
2. **Farms are a reward.** Automation can be delayed by progression but never made pointless. Iron farms stay possible, just not on day 1.
3. **The world never changes permanently.** Progress doesn't switch off parts of the sandbox.
4. **Every step keeps mattering.** A mechanic you've forgotten an hour later gets cut or reworked.
5. **Every material has a use.** No resource ends up as dead weight, and no gear is strictly best.

Standing choices:

- Vanilla's difficulty setting is used as it is.
- Multiplayer is a first-class target: hearts are per player, and balance assumes a shared world.
- The spec describes one intended experience, with no design-level toggles.

## Decisions so far

- [What Matcha's heart system does](issues/01-matcha-heart-system.md): death costs 1 heart down to a per-player floor that falls from 10 to 3 with progress; Crystal Hearts add 1, up to 30.
- [How Raspberry Flavoured designs alloys and progression](issues/02-raspberry-alloys.md): alloys also sit on host materials, with traits replacing removed enchantments, uses beyond gear, and durability independent of tier.
- [Hearts and death](issues/03-hearts-and-death.md): death costs hearts, not items, down to a Heart Floor that falls from 10 to 3 over seven Milestones; Crystal Hearts come from one-time boss kills, chests and crafting with Pith; Wither temporarily withers hearts until you return to the Overworld.
- [Material roles and alloys](issues/06-material-roles-and-alloys.md): Steel, Electrum, Shakudo, Adamant and Orichalcum are made from Netherite, storage blocks and Ancient Embers from Piglin Brutes; each beats Netherite differently, with set bonuses and Armored Elytra.
- [Blood moons](issues/15-blood-moons.md): every 32 days, bastion, monument, End city and mansion mobs return to their original spots if the right blocks remain, making Embers and Pith slowly renewable; blocks and loot never return.

## Not yet specified

- Nether and End progression, including whether bosses need new rewards
- Trading economy details
- New recipes and foods
- Armor and combat changes beyond alloys, including the shield

## Out of scope

- Horror elements and secrets
- Custom music and discs
- Translations beyond English
- Fishing as a progression track
