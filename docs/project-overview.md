![Maple Flavored logo](../maple_flavored.png)

# Maple Flavored

## Summary

Maple Flavored is a slow-paced progression overhaul for Minecraft 26.3, built as a Fabric mod. Enchantments are found in the world and shared through a Library. Villagers and farms become rewards to work towards, equipment offers lasting choices, and death risks health and experience while keeping your items safe.

The mod supports single player and shared multiplayer worlds, requires installation on both client and server, and is intended for new worlds. It stands alone without required content mods. Inspired by Matcha Flavoured and Raspberry Flavoured, it uses CC BY-NC-SA 4.0 and credits reused work in [CREDITS.md](../CREDITS.md).

This describes the intended design; balance values remain subject to playtesting. The [design map and resolved tickets](../.scratch/maple-flavored-design/map.md) hold the detailed rules.

## Why

Vanilla Minecraft hands out strong gear fast, and then most of the world stops mattering. A village found on day one gives an iron farm, trades and enchanted books. Enchanting is a loot box with one obvious choice. After the first diamond set there is no reason to visit most structures. Netherite is strictly the best material, so everything else becomes dead weight in storage. Death either costs your whole inventory or, with Keep Inventory, costs nothing.

Matcha Flavoured fixes some of this, but it is built for players who don't build farms. Maple Flavored is for the technical player who loves farms: it slows progress down while keeping farms worthwhile and the sandbox intact. It starts from vanilla and adds each change on purpose.

Five rules guide the changes:

- **Vanilla's vocabulary stays.** A new or changed thing needs a gameplay reason, and lore never replaces vanilla's.
- **Farms are a reward.** Automation can be delayed by progression but never made pointless.
- **The world never changes permanently.** Progress and boss victories never switch off parts of the sandbox.
- **Every step keeps mattering.** A mechanic you've forgotten an hour later gets cut.
- **Every material has a use.** No resource ends up as dead weight, and no gear is strictly best.

The [manifestos](manifestos/) explain the thinking behind the design.

## Feature Breakdown

### Enchanting and the Library

Vanilla enchanting feels like a loot box: spend experience and lapis on a partly hidden, random result, then grind it away and try again if it disappoints. The strongest offer is usually the obvious choice, and general enchantments like Sharpness overshadow situational ones like Smite. Simply letting players choose would remove the luck without adding much gameplay.

Maple moves the decisions out into the world: which structure to seek, which danger to face, and how far to push an expedition. Like brewing, the final process can be deterministic because earning its ingredients took play.

Every found book is level I. Combine two matching books in the anvil to make the next level, so a level III book takes four finds and Sharpness V takes sixteen. Shelve books in Chiseled Bookshelves around an enchanting table to form a shared **Library**. The table offers every shelved enchantment at its highest level without consuming the book, so anyone at the base can use it. The random three-slot offer is gone. The table is a checklist: insert an item, its current enchantments are pre-selected, choose what you want, then save.

Your experience level sets your **Enchantment Slots**: one enchantment per item at level 30, plus one more every thirty levels, without a cap. The limit is checked only when saving, so an experienced player can enchant equipment for friends. Losing levels never disables existing gear.

Saving costs one lapis per enchantment and, on an item's first save, one unit of its material. The grindstone removes every enchantment and returns that material, so it works as a deposit. Changes are cheap enough to prepare Smite for an undead fight or Fire Protection for a Nether trip, then switch back at home. Applying enchantments spends no experience; combining books and renaming items cost experience points, and repairs cost none.

**Library Progress** measures the collection: the highest shelved level of each enchantment, added up. As it grows, the table unlocks effects you can select for golden apples, each using an Enchantment Slot. Plain golden apples keep their food value but have no status effects. Completing the Library with every other enchantment at maximum level unlocks Mending, so repairs and material gathering stay useful until the very end.

Two new enchantments join the collection. **Zephyr**, from the Ender Dragon, gives boots a double jump that can soften a fall. **Blight**, from the Wither, trades Fire Aspect's stronger damage for Wither damage that works underwater and against fire-immune enemies. Both curses are removed.

A new **Enchanted Books** advancement tab lists every enchantment, says where its book is found, and unrolls source by source as you explore.

Details: [Enchanting](../.scratch/maple-flavored-design/issues/10-enchanting.md), [the enchanting manifesto](manifestos/enchanting.md) and [the role of XP](../.scratch/maple-flavored-design/issues/12-role-of-xp.md).

### Finding books

Most books float in fixed, visible places in existing structures, with one guaranteed book per structure per world. Specialized enchantments arrive early: Dungeons offer Smite or Bane of Arthropods, Desert Pyramids Blast Protection, and Pillager Outposts Projectile Protection. General enchantments come later, and the enchantments with the most levels sit in structures that demand travel: Unbreaking III needs four Strongholds and Looting III four Woodland Mansions.

Some books are chance drops from Illagers, such as Sharpness from Vindicators and Protection from Evokers. Strongholds are filled with Illagers, with the strongest guard and the Unbreaking book in the portal room, so reaching the End Portal is a fight. Raid-spawned Illagers drop no books, so raid farms give none.

Some rewards demand more than finding the building:

- **Strongholds and Woodland Mansions.** Taking the book sends every Illager in the structure rushing toward you.
- **Bastions.** The book is locked until three keeper Brutes are dead, and taking it sets every Piglin in the structure hunting you.
- **Nether Fortresses.** The book unlocks once you've dealt with ever Blaze spawner in the structure.
- **Ancient Cities.** The book is locked until three keeper shriekers near the city's edge are broken. The city keeps you Grounded, unable to fly, until the book is taken. A held Echo Shard lets you hear nearby shriekers pulse, even through walls.

Fishing gives Luck of the Sea and Lure books, Trial Chamber ominous vaults give the Mace's books, and bosses drop their own. Every other enchanted book is removed from loot, with each chest entry replaced by a diamond, and all found equipment is plain. Librarians buy surplus enchanted books instead of selling them, paying emeralds and experience.

Details: [Structures](../.scratch/maple-flavored-design/issues/14-structures.md), [Stronghold Illagers](../.scratch/maple-flavored-design/issues/18-stronghold-illagers.md), [Illager book drops](../.scratch/maple-flavored-design/issues/19-illager-book-drops.md) and [Librarian book trades](../.scratch/maple-flavored-design/issues/20-librarian-book-trades.md).

### Spawners and Pith

Normal spawners ignore light and spawn twice as fast until they are **Cleared** by defeating twelve of their mobs, plus two more for each additional player who approached. Any death counts, including traps and lava. Until then a spawner cannot be broken or blown up, and a forcefield around it shrinks as the count rises. Once Cleared, it spawns at vanilla's pace and stays Cleared, so you can keep it for a farm or break it for **Pith**. Dungeon and cave spider spawners also hold an enchanted book that drops with the Pith. Dungeons are more common, and trial spawners are unchanged.

Pith is the rare material behind health and Netherite. It comes from broken spawners, Elder Guardians, Evokers, Piglin Brutes, guarded chests, and a Nether Star, which crafts into nine. A Crystal Heart is eight diamonds around one Pith, and a Netherite Ingot needs four Pith in place of gold.

Details: [Structures](../.scratch/maple-flavored-design/issues/14-structures.md).

### Hearts, death and Remnants

Keep Inventory is on by default. Death instead costs maximum health and leaves your experience behind to recover.

Players start with 10 **Max Hearts** and can grow to 30. Death removes one heart, or two on Hard, down to a personal **Heart Floor**. On Normal and Hard the floor starts at 10 and falls by one for each **Milestone** you reach, down to 3: your first copper ingot, iron ingot, diamond, Netherite Ingot and Eye of Ender, and entering the Nether and the End. Peaceful and Easy keep the floor at 10; Hardcore retains its death rule.

**Crystal Hearts** restore or increase maximum health. Earn them through boss advancements and guarded chests, or craft them from diamonds and Pith. Held in either hand, one can instead save you from a lethal hit like a Totem of Undying.

The Wither effect turns usable hearts into **Withered Hearts**, one every five seconds. A Withered Heart is unusable and effectively decreases your maximum HP. They return once the effect ends, provided you are outside the Nether and outside a Wither's area of effect.

Death leaves all your experience in a **Remnant** that only you can see and collect. It cannot be destroyed and never despawns, but each further death reduces every uncollected Remnant by 20%. Recovering it restores your points directly. Early deaths risk health; later expeditions also risk the levels that make you a powerful enchanter.

In addition to Keep Inventory, there are two new game rules: Keep XP and Keep Hearts.

Details: [Hearts and death](../.scratch/maple-flavored-design/issues/03-hearts-and-death.md) and [Remnants](../.scratch/maple-flavored-design/issues/12-role-of-xp.md).

### Early equipment and mobs

Copper replaces stone tools: a wooden pickaxe mines copper ore, and the ladder runs wood, copper, iron, diamond, Netherite, then Alloys. There are no new early workstations or heat sources, because early steps that are forgotten an hour later aren't worth adding.

Gold tools, weapons and armor gain an extra Enchantment Slot. Chainmail gains a lasting use through **Reinforcement**: at a smithing table, a Reinforcement template, an armor piece and a respective chainmail piece give that armor +1 toughness, once. It carries through every later upgrade and holds you over before Protection. Master Armorers sell the template, and chainmail stays uncraftable.

Zombies on Normal and Hard are fast and frail and step up a full block. Skeletons, creepers, baby zombies and several other mobs have less health, so fights are dangerous without being long. Husks become slow, armored brutes, and Wither Skeletons swing Netherite swords. Creepers spawn naturally only below Y 63 in the Overworld. Mobs drop their gear without its enchantments.

Below Y 0, natural spawns of adult zombies, skeletons, creepers and spiders become **Deep variants**, spider jockeys included. Each has half again its base mob's health and hits twice as hard, and each has a threat of its own: Deep Zombies disable a raised shield as an axe does, Deep Skeletons spot players from twice as far and shoot Slowness arrows, Deep Creepers keep a creeper's health but explode as hard as TNT, and Deep Spiders' bites blind. Each looks like deepslate, drops its base mob's loot and twice its XP. Spawners, spawn eggs and structures still make the ordinary mob, so farms are unaffected and a player in copper gear thinks twice about caving deep.

Details: [Early equipment](../.scratch/maple-flavored-design/issues/07-early-game-tiers.md) and [Mob changes](../.scratch/maple-flavored-design/issues/05-mob-changes.md).

### Earning villagers and farms

Villages are about five times rarer and always abandoned, populated by zombie villagers with no iron golems, no hay bales and no brewing stands. Crops, beds and workstations offer the beginnings of a settlement.

The first living villagers must be cured with Weakness and an enchanted golden apple. Weakness potions are brewed by inverting Strength potions, fixing the strange Vanilla recipe. The first apple enchantment needs 15% Library Progress and enchanted golden apples follow the enchantment slot rules, so you also need to be Level 30+. Once earned, villagers breed and work as usual, and the Master Armorer's Reinforcement template is an early reward.

Villagers sell plain gear wherever vanilla sells enchanted gear, and none sells Pith, Crystal Hearts or diamonds.

Farms are delayed, never removed:

- Iron farms, raid farms and trading halls wait for villagers.
- A Cleared spawner keeps spawning, so a dungeon becomes a farm.
- Evoker and Wither farms produce Pith, and experience farms keep feeding Enchantment Slots.
- Creeper farms sit below Y 63. Every other farm stays vanilla.

Details: [Villagers](../.scratch/maple-flavored-design/issues/08-villagers-and-villages.md) and [Farm pacing](../.scratch/maple-flavored-design/issues/09-farm-pacing.md).

### Netherite and Alloys

Netherite needs Pith in place of gold. Beyond it, five **Alloys** offer competing strengths. An Alloy Furnace, built like a blast furnace from gold and polished blackstone bricks, combines a Netherite Ingot, a storage block and an **Ancient Ember**, earned by killing Piglin Brutes.

| Alloy | Block ingredient | Identity |
| --- | --- | --- |
| Steel | Iron | Durability, knockback resistance, sonic boom damage reduction, and full-set immunity to knockback and Levitation |
| Electrum | Gold | An extra Enchantment Slot, full-set fire and lava immunity, and resistance to Withered Heart buildup |
| Shakudo | Copper | Faster mining, attacks, and movement, with full-set freedom from terrain slowdown |
| Adamant | Diamond | Damage, armor toughness, ignoring block hardness above a certain level, and bedrock-breaking pickaxes |
| Orichalcum | Emerald | Better drops, Illager neutrality, and a rechargeable full-set death save |

Netherite gear becomes Alloy gear at a smithing table with one ingot and no template, and any Alloy can be changed into another. Grind enchanted gear before changing its material, then reapply its enchantments. Materials also maintain equipment: anvil repairs cost materials instead of XP, one Alloy ingot repairs its own gear fully, and items no longer combine with each other. Alloy blocks work as beacon bases.

An **Armored Elytra** permanently combines Elytra with an Alloy chestplate, retaining its protection and contribution to set bonuses at the cost of weaker flight. Plain Elytra remain the faster travel option.

Details: [Alloys](../.scratch/maple-flavored-design/issues/06-material-roles-and-alloys.md).

### Bosses and Subspaces

Every boss is a repeatable reward, so each one's cheapest escape has to go. The Warden and Wither create **Subspaces**, areas that preserve their encounters' challenges.

- **Warden.** Its Subspace applies Darkness and **Grounded**, which disables Elytra flight, Riptide and Zephyr and sharply shortens Ender Pearl throws. The Warden knows where everything inside is, but anger still comes only from noise. Sneaking, hiding and finding a route out remain ways to survive, and surviving it earns a Crystal Heart. Warden spawning has changed, so Warden switches are now ineffective—they now spawn near the player who made the noise instead of near the Shrieker.
- **Wither.** Withered Hearts never return inside its Subspace, and it passes through bedrock and obsidian, so it can't be pinned. Each kill drops a Blight book and a Nether Star's worth of Pith, and the first earns a Crystal Heart.
- **Elder Guardian.** Drops Pith, and the first kill earns a Crystal Heart. The fight is unchanged.
- **Ender Dragon.** Drops a Zephyr book every kill, and freeing the End earns a Crystal Heart. The fight is unchanged.

Everyone nearby gets boss credit.

Details: [Mob changes](../.scratch/maple-flavored-design/issues/05-mob-changes.md) and [the boss Subspaces manifesto](manifestos/boss-subspaces.md).

### Blood Moons

Every thirty-two in-game days, a **Blood Moon** brings back selected mobs in Bastions, Ocean Monuments, End Cities, Woodland Mansions and Strongholds. This renews Ancient Embers, Pith and Illager books, giving you reasons to return.

Only mobs return: blocks, chests, fixed books and broken spawners stay gone. Restoration depends on preserving the mobs' original spots and suitable blocks, so dismantling a structure can stop its renewal. The night turns red, but sleeping remains possible and general monster spawning does not increase.

Details: [Blood Moons](../.scratch/maple-flavored-design/issues/15-blood-moons.md).

### Playing together

Max Hearts, the Heart Floor, Remnants and Enchantment Slots belong to each player. Libraries, structure books, spawner counts and boss credit are shared. One Library enchants everything in the base, a group's highest-level player becomes its enchanter, and a server can't multiply finds, since each structure holds one book for everyone.

### Smaller changes

- Potions stack to sixteen, with a shared one-second cooldown on player-thrown splash and lingering potions. Dispensers are unaffected.
- Enchanted books resist fire and explosions.
- Enchanted golden apples in loot become plain golden apples.
- New advancements sit in vanilla's own tabs and teach the first step of each system, with Milestones marked by a broken heart.
- Challenging structure chests gain rolls for Pith and Crystal Hearts.

### What stays vanilla

Food, hunger, natural regeneration and storage keep their roles, and shulker boxes remain a reason to reach End Cities. Raids, the Elder Guardian and Ender Dragon fights, villager breeding and professions, trial spawners and most farms are untouched. No new structures are added. Deeper End changes are reserved for a separate End Update mod that requires Maple Flavored.
