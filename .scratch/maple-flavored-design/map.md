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
- [Mob changes](issues/05-mob-changes.md): fast, frail zombies and lower health on skeletons, creepers and others; creepers don't spawn above sea level; mobs drop gear but not enchantments; the Warden opens a barrierless Subspace of 100 block radius, with Grounded weakening flight and greatly increasing Ender Pearl gravity; Wardens summon near a player in Warden Territory, which closes the Warden switch; the Wither's Subspace blocks Withered Heart recovery.
- [Farm pacing](issues/09-farm-pacing.md): no new gates; earning villagers paces iron and raid farms, and other farms stay vanilla apart from creeper farms sitting below Y 63.
- [The role of XP](issues/12-role-of-xp.md): level sets uncapped Enchantment Slots; only combining books and renaming cost XP, while repairs are free; dying drops all XP as a Remnant only you can collect, and each further death cuts uncollected Remnants by 20%.
- [Storage and End City rewards](issues/13-storage-and-end-rewards.md): storage stays vanilla; End City chests only gain rolls for Pith (50%, 1 or 2) and a Crystal Heart (15%).
- [Brewing](issues/11-brewing.md): vanilla apart from potions stacking to 16 with a shared 1 second throw cooldown for players; the enchanted golden apple offers vanilla's four effects, unlocking at 15%, 30%, 50% and 75% Library Progress.
- [Structures](issues/14-structures.md): spawners ignore light and must be Cleared (12 deaths plus 2 per extra player) before breaking; the Fortress book unlocks once its Blaze spawners are Cleared; dungeons are more common; Crystal Hearts and Pith sit in guarded chests at half Matcha's rates or less.
- [Trading, combat and recipes](issues/16-trading-combat-and-recipes.md): villagers sell plain gear; armor and combat stay vanilla apart from Reinforcement; a Nether Star crafts into 9 Pith; the Alloy Furnace mirrors the blast furnace with gold and polished blackstone bricks; each Alloy has a block that works as a beacon base.
- [Advancements](issues/17-advancements.md): vanilla's tabs stay, with 22 new advancements that teach each system; the seven Milestones are advancements that set the Heart Floor; Crystal Hearts come from four boss advancements, the Warden's for surviving it; only But why?, an all-Alloy beacon, is hidden.
- [Stronghold Illagers](issues/18-stronghold-illagers.md): Vindicators, Pillagers and 1 to 3 Evokers fill each Stronghold by piece, about 12 to 20 in all, with the strongest guard and the book in the portal room; no Captains or Ravagers; spots sit only on the stone bricks family, infested variants included, so every one can be Restored.
- [Illager book drops](issues/19-illager-book-drops.md): books drop on player kills from any Illager a raid didn't spawn, each on its own roll; Vindicator Sharpness 10% and Sweeping Edge 2.5%, Captain Power 20%, Punch and Infinity 5%, Evoker Protection 15% on one roll, Ravager Knockback 10%; Looting adds 20% of the base per level.
- [Librarian book trades](issues/20-librarian-book-trades.md): every Novice Librarian buys any enchanted book, paying per find 4 emeralds, 10 villager XP and 50 player XP points, so level V pays 64 emeralds; fixed price, 12 uses; levels 2 to 4 lose their book slot.

## Not yet specified

Nothing.

## Out of scope

- Horror elements and secrets
- Custom music and discs
- Translations beyond English
- Fishing as a progression track
- Deeper End changes, including an End City rework, which belong to a separate End Update mod that requires this one; the End City ideas so far are in [Storage and End City rewards](issues/13-storage-and-end-rewards.md)
