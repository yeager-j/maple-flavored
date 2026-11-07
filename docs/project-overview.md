![Maple Flavored logo](../maple_flavored.png)

# Maple Flavored

## Summary

Maple Flavored is a slow-paced progression overhaul for Minecraft 26.3, built as a Fabric mod. Enchantments are earned through exploration and shared through a Library. Villagers and farms become rewards to work towards, equipment offers lasting choices, and death risks health and experience while keeping your items safe.

The mod supports single player and shared multiplayer worlds, requires installation on both client and server, and is intended for new worlds. It stands alone without required content mods. Inspired by Matcha Flavoured and Raspberry Flavoured, it uses CC BY-NC-SA 4.0 and credits reused work in [CREDITS.md](../CREDITS.md).

This describes the intended design; balance values remain subject to playtesting. The [design map and resolved tickets](../.scratch/maple-flavored-design/map.md) hold the detailed rules.

## Why

Progress should keep exploring, building, and fighting worthwhile throughout a save. Maple slows the rush to powerful equipment, gives structures lasting rewards, and keeps materials useful beyond the first set of diamond gear.

Three principles guide the changes:

- **Earn progression.** Powerful equipment and automation take work, and farms remain useful rewards for that work.
- **Make choices last.** Materials have distinct uses, final equipment has competing strengths, and new mechanics keep mattering.
- **Keep the sandbox active and familiar.** Changes need a gameplay purpose, vanilla's vocabulary stays, and boss victories never permanently switch off parts of the world.

Ordinary food, hunger, natural regeneration, and storage retain their vanilla roles. Shulker boxes remain a reason to reach End Cities. The [manifestos](manifestos/) explain the thinking behind the design.

## Feature Breakdown

### Enchanting and the Library

Vanilla enchanting feels like a loot box: spend experience and lapis on a partly hidden, random result, then grind it away and try again if it disappoints. There are few meaningful decisions. The strongest offer is usually the obvious choice, and general enchantments like Sharpness overshadow situational ones like Smite. Simply letting players choose their enchantments would remove the luck without adding much gameplay.

Maple moves the decisions out into the world: which structure to seek, which danger to face, and how far to push an expedition. Like brewing potions, the final process can be deterministic because earning its ingredients took play. Once you've earned an enchantment, you can choose it directly at the table.

Find enchanted books, combine matching copies to improve them, and shelve them in Chiseled Bookshelves around an enchanting table. This shared **Library** lets anyone at the base apply its enchantments without consuming the books. Every found book starts at level I, so repeated expeditions build stronger books and a wider collection. At home, the table becomes a checklist: choose the enchantments you want, then save.

Your experience level sets your **Enchantment Slots**: one enchantment per item at level 30, plus one more every thirty levels, without a cap. The limit is checked when saving, so an experienced player can enchant equipment for friends. Losing levels never disables existing gear.

Saving costs lapis and, on an item's first save, one unit of its material. The grindstone removes enchantments and returns that material. Changes are cheap enough to prepare Smite for an undead fight or Fire Protection for a Nether trip, then switch back at home. Applying enchantments spends no experience; combining books and renaming items do.

**Library Progress** measures the collection's variety and enchantment levels. As it grows, the table unlocks effects you can select for golden apples, each using an Enchantment Slot. Plain golden apples have no status effects. Completing the Library with every other enchantment at maximum level unlocks Mending, leaving repairs and material gathering useful throughout progression.

Two new enchantments join the collection: **Zephyr**, from the Ender Dragon, gives boots a double jump that can soften a fall; **Blight**, from the Wither, trades Fire Aspect's stronger damage for Wither damage that works underwater and against susceptible fire-immune enemies. Both curses are removed.

Details: [Enchanting](../.scratch/maple-flavored-design/issues/10-enchanting.md) and [the enchanting manifesto](manifestos/enchanting.md).

### Exploration and structures

Most books float in fixed, visible places in existing structures, with one guaranteed book per structure per world. Specialized enchantments arrive early: Dungeons offer Smite or Bane of Arthropods, while Sharpness and Protection come from tougher Illagers. Fishing, bosses, and ominous vaults supply enchantments suited to those activities. Found equipment is plain, making the Library the route to enchanted gear.

Some rewards demand more than finding the building. Strongholds gain Illager guards. Bastion books require defeating keeper Brutes, then taking the book angers the Piglins. Ancient Cities require breaking keeper shriekers while flight is disabled.

Normal spawners ignore light and must be **Cleared** by defeating enough of their mobs before they can be broken. Once Cleared, keep the spawner for a farm or break it for **Pith**, a rare material used for health upgrades and Netherite, and an Enchanted Book. A Nether Fortress's book unlocks after its Blaze spawners are Cleared, so earning it preserves the option of a Blaze farm.

Dungeons become more common. Guarded chests and dangerous mobs provide further rare rewards; Pith also comes from Elder Guardians, Evokers, Piglin Brutes, and Nether Stars.

Details: [Structures](../.scratch/maple-flavored-design/issues/14-structures.md) and [book sources](../.scratch/maple-flavored-design/issues/10-enchanting.md#finding-books).

### Hearts, death, and Remnants

Keep Inventory is on by default. Death instead costs maximum health and leaves your experience behind to recover.

Players start with 10 **Max Hearts** and can grow to 30. Death removes one heart, or two on Hard, down to a personal **Heart Floor**. On Normal and Hard, progression Milestones gradually lower that floor from 10 to 3, increasing the stakes as you advance. Peaceful and Easy keep it at 10; Hardcore retains its death rule.

**Crystal Hearts** restore or increase maximum health. Earn them through boss advancements and guarded chests, or craft them from diamonds and Pith. Held in either hand, one can instead save you from a lethal hit like a Totem of Undying.

Death leaves all your experience in a **Remnant** that only you can see and collect. It cannot be destroyed and never despawns, but each further death reduces every uncollected Remnant by 20%. Recovering it restores your points directly. Early deaths risk health; later expeditions also risk the levels that make you a powerful enchanter.

Details: [Hearts and death](../.scratch/maple-flavored-design/issues/03-hearts-and-death.md) and [Remnants](../.scratch/maple-flavored-design/issues/12-role-of-xp.md).

### Earning villagers and farms

Villages are rarer and always abandoned, populated by zombie villagers with no iron golems. Crops, beds, and workstations offer the beginnings of a settlement.

The first living villagers must be cured with Weakness and an enchanted golden apple. Weakness is brewed from Strength, making the Nether part of the intended route. The first apple needs 15% Library Progress and level 30. This ties trading halls, iron farms, and raid farms to exploration and enchanting progress. Once earned, villagers breed and work as usual.

Librarians buy surplus enchanted books instead of selling them, paying emeralds and experience. Equipment trades supply plain gear. Brewing also becomes easier to carry: potions stack to sixteen, with a shared one-second cooldown on player-thrown splash and lingering potions.

Details: [Villagers](../.scratch/maple-flavored-design/issues/08-villagers-and-villages.md), [farms](../.scratch/maple-flavored-design/issues/09-farm-pacing.md), and [brewing](../.scratch/maple-flavored-design/issues/11-brewing.md).

### Equipment and Alloys

Copper replaces stone tools, so the tool ladder begins wood, copper, iron, diamond, then Netherite. Gold tools and armor gain an extra Enchantment Slot. Chainmail gains a lasting use through **Reinforcement**: combine armor with the respective Chainmail piece to permanently increase that armor's toughness. The smithing template and Chainmail armor are sold by Armorer villagers.

Netherite needs Pith in place of gold. Beyond it, five **Alloys** offer competing strengths. An Alloy Furnace combines Netherite, a storage block, and an **Ancient Ember**, earned by killing Piglin Brutes.

| Alloy | Block ingredient | Identity |
| --- | --- | --- |
| Steel | Iron | Durability, resistance, and full-set immunity to knockback and Levitation |
| Electrum | Gold | An extra Enchantment Slot and full-set fire and lava immunity |
| Shakudo | Copper | Faster mining, attacks, and movement, with full-set freedom from terrain slowdown |
| Adamant | Diamond | Damage, armor toughness, faster mining through hard blocks, and bedrock-breaking pickaxes |
| Orichalcum | Emerald | Better drops, Illager neutrality, and a rechargeable full-set death save |

Materials also maintain equipment: anvil repairs cost materials without spending experience. You can change any Alloyed tool or armor into any other Alloy using the Smithing Table. Grind enchanted gear before changing its material, then reapply its enchantments.

An **Armored Elytra** permanently combines Elytra with an Alloy chestplate, retaining its protection and contribution to set bonuses at the cost of weaker flight. Plain Elytra remain the faster travel option.

Details: [Early equipment](../.scratch/maple-flavored-design/issues/07-early-game-tiers.md) and [Alloys](../.scratch/maple-flavored-design/issues/06-material-roles-and-alloys.md).

### Combat and boss Subspaces

Faster zombies and lower health on several enemies make encounters dangerous without making every fight lengthy. Creepers spawn naturally only below Y 63 in the Overworld.

The Warden and Wither create **Subspaces**, areas that preserve their encounters' challenges while allowing players to enter and leave.

The Warden's Subspace applies Darkness and **Grounded**, disabling Elytra flight, Riptide, and Zephyr while sharply shortening Ender Pearl throws. Sneaking, hiding, and finding a route out remain ways to survive.

The Wither effect also creates **Withered Hearts**, temporarily reducing usable health. They recover once the effect ends, provided you are outside the Nether and a Wither's Subspace. The Wither can pass through bedrock and obsidian, preventing those blocks from safely pinning it in place.

Details: [Mob changes](../.scratch/maple-flavored-design/issues/05-mob-changes.md).

### Blood Moons

Every thirty-two in-game days, a **Blood Moon** brings back selected mobs in Bastions, Ocean Monuments, End Cities, Woodland Mansions, and Strongholds. This renews sources of Ancient Embers, Pith, and Illager books, giving you reasons to return.

Only mobs return: blocks, chests, fixed books, and broken spawners stay gone. Restoration depends on preserving the mobs' original spots and suitable blocks, so dismantling a structure can stop its renewal. The night turns red, but sleeping remains possible and general monster spawning does not increase.

Details: [Blood Moons](../.scratch/maple-flavored-design/issues/15-blood-moons.md).

[Advancements](../.scratch/maple-flavored-design/issues/17-advancements.md) teach the new systems within vanilla's existing tabs. Deeper End changes are reserved for a separate End Update mod that requires Maple Flavored.
