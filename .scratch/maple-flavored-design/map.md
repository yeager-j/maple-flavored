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
- [Blood moons](issues/15-blood-moons.md): every 32 days, bastion, monument, End city, mansion and Stronghold mobs return to their original spots if the right blocks remain, making Embers and Pith slowly renewable; blocks and loot never return.
- [Enchanting](issues/10-enchanting.md): books are found once per structure, combine from level I, and are shared through a Library around the table; level sets Enchantment Slots, enchanting costs the item's material and lapis, and Mending needs a Full Library.
- [Early-game tiers](issues/07-early-game-tiers.md): no early-only steps; stone tools are removed so wood mines copper ore; gold and Electrum gear get +1 Enchantment Slot, with Orichalcum taking the drop bonus; a Master Armorer template Reinforces any armor with chainmail for +1 toughness.
- [Getting villagers and removing villages](issues/08-villagers-and-villages.md): every village is abandoned and 5.5x rarer; villagers are earned only by curing, with Weakness brewed from Strength and an enchanted golden apple made at the table from 15% Library Progress and level 30, its effects picked like enchantments.
- [Healing and food](issues/04-healing-and-food.md): vanilla food, hunger and natural regeneration stay for now, since no design beat them; the enchanted golden apple's effects moved to Brewing.
- [Mob changes](issues/05-mob-changes.md): fast, frail zombies and lower health on skeletons, creepers and others; creepers don't spawn above sea level; mobs drop gear but not enchantments; the Warden and Wither open Subspaces, and Wardens summon near a player in Warden Territory, which closes the Warden switch.
- [Farm pacing](issues/09-farm-pacing.md): no new gates; earning villagers paces iron and raid farms, and other farms stay vanilla apart from creeper farms sitting below Y 63.
- [The role of XP](issues/12-role-of-xp.md): level sets uncapped Enchantment Slots; only combining books and renaming cost XP, while repairs are free; dying drops all XP as a Remnant only you can collect, and each further death cuts uncollected Remnants by 20%.

## Not yet specified

- Nether and End progression, including whether bosses need new rewards
- Trading economy details
- New recipes
- Armor and combat changes beyond alloys, including the shield

## Out of scope

- Horror elements and secrets
- Custom music and discs
- Translations beyond English
- Fishing as a progression track
