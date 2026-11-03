# Maple Flavored

Maple Flavored is a slow-paced progression overhaul for Minecraft 26.3, built as a Fabric mod. Exploration earns enchantments, equipment offers lasting choices, and death puts health and experience at risk while keeping your items safe. It is designed for both single player and shared multiplayer worlds.

The mod starts from vanilla, with inspiration from Matcha Flavoured and Raspberry Flavoured. It stands alone without required content mods, uses real registered items and blocks, and is required on both the client and server. It is intended for new worlds. Reused work is credited in [CREDITS.md](../CREDITS.md), and the project uses CC BY-NC-SA 4.0.

This is the intended design, with balance values still subject to playtesting. The [design map](../.scratch/maple-flavored-design/map.md) and its resolved tickets hold the detailed decisions; they take precedence over earlier ideas in the manifestos and research.

## Why

Minecraft is an infinite sandbox about exploring, building, and fighting. Progress should give those activities more purpose without making parts of the world irrelevant. Maple Flavored slows the path to powerful equipment, gives structures lasting rewards, and keeps resources useful after the first set of diamond gear.

The [Matcha manifesto](manifestos/matcha-flavoured.md) sets the direction. Matcha's Crystal Hearts, dangerous zombies, earned villagers, and material choices are strong starting points. Maple builds its own systems around them, with technical players and farm building treated as part of the game.

Five rules guide the changes:

- **Keep vanilla's vocabulary.** A change needs a gameplay reason. Lore alone is not a reason to rename or replace familiar things.
- **Make farms a reward.** Progress can delay automation, but the result must remain useful. Earning an iron farm is good; making iron farms pointless is not.
- **Keep the sandbox active.** Beating a boss must not permanently switch off a part of the world, such as surface monster spawning.
- **Make new steps last.** A new workstation or mechanic should still matter after its first hour.
- **Give materials a purpose.** Resources should keep useful roles, and the final equipment choices should have competing strengths.

The same reasoning shapes healing and storage. Vanilla food and natural regeneration stay because no proposed replacement worked better. Bundles, Ender Chests, and shulker boxes keep their places in progression, so End Cities still offer a major storage reward.

## Feature Breakdown

### Hearts and death

Death costs permanent health instead of equipment. Keep Inventory is on by default in new worlds, and players start with 10 **Max Hearts**, which can grow to 30. Losing a heart makes death matter without the loss of a full inventory that can make a player quit a save.

Each player has a **Heart Floor**, the lowest their Max Hearts can fall through death. On Normal and Hard, it starts at 10 and falls by one for each of seven **Milestones**, down to 3:

- Obtain a copper ingot, iron ingot, diamond, Netherite Ingot, or Eye of Ender.
- Enter the Nether or the End.

The item Milestones count as soon as the item enters your inventory, however you obtained it. They can happen in any order. Lowering the Heart Floor does not remove hearts; it increases what future deaths can cost. A red chat message announces each drop, and the death screen shows the Max Hearts lost.

| Difficulty | Max Hearts lost per death | Heart Floor |
| --- | --- | --- |
| Peaceful or Easy | 1 | Fixed at 10 |
| Normal | 1 | Falls from 10 to 3 with Milestones |
| Hard | 2 | Falls from 10 to 3 with Milestones |

Difficulty remains the world's chosen vanilla setting. Hardcore keeps vanilla's death rule.

**Crystal Hearts** restore or increase Max Hearts. Consuming one takes about 1.6 seconds and adds one Max Heart, up to 30. At the cap, use is refused without consuming it. A Crystal Heart held in either hand can instead save you from a lethal hit like a Totem of Undying; that uses it without adding a heart. A Totem or Crystal Heart save is not a death and costs neither hearts nor experience.

Each player earns a Crystal Heart for their first kill of an Elder Guardian, Wither, and Ender Dragon, and for surviving a Warden until it digs back down, with nearby players sharing credit. These come as advancement rewards. More come from guarded structure chests or crafting eight diamonds around one **Pith**. Fishing and villagers do not supply Crystal Hearts.

Pith connects health to equipment progression. A broken normal spawner gives one, an Elder Guardian gives two, an Evoker has a 25% chance to give one, and a Piglin Brute has a separate 10% chance to give one. A Nether Star also crafts into nine Pith, with no reverse recipe. Pith can become renewable, but diamonds do not, so crafting Crystal Hearts still requires exploration and mining.

The Wither effect also creates **Withered Hearts**: every five seconds, one heart becomes temporarily unusable, down to one usable heart. This does not change Max Hearts. All Withered Hearts recover when the effect is gone and you are outside the Nether and outside a Wither's Subspace. Death clears them; logging out does not.

Details: [Hearts and death](../.scratch/maple-flavored-design/issues/03-hearts-and-death.md).

### Early equipment and Reinforcement

The tool ladder becomes wood, copper, iron, diamond, Netherite, then Alloys. Stone tools, including the stone spear, are removed. A wooden pickaxe can mine copper ore, deepslate copper ore, and raw copper blocks. Other blocks that needed stone tools need copper or better. Trades and loot that supplied stone tools supply their copper equivalents instead.

There are no new early smelting steps or temporary workstations. The furnace, smoker, blast furnace, and campfire keep their vanilla roles. Copper fills stone's place, while leather armor, chainmail, and wooden tools remain.

Gold gains a lasting use: every enchantable gold tool, weapon, and armor piece gets one extra Enchantment Slot. Its familiar strength in enchanting carries into the new system.

**Reinforcement** helps armor through the long period before Protection becomes available. At a Smithing Table, combine one armor piece, one corresponding chainmail piece, and a Reinforcement template. The result gains one armor toughness. Each piece can be Reinforced once, consuming the chainmail and template.

Reinforcement works on all player armor, including the turtle helmet and Armored Elytra. It survives material upgrades, stacks with Alloy bonuses and trims, and shows a tooltip line and chainmail overlay. It can be applied to enchanted armor. Plain Elytra and other wearable objects do not qualify.

A Master Armorer sells the template for 32 emeralds. It is not chest loot. Seven diamonds, a block of iron, and an existing template make two templates. Chainmail remains uncraftable, giving its mob drops, chest loot, and Armorer trades a continuing use.

Details: [Early-game tiers](../.scratch/maple-flavored-design/issues/07-early-game-tiers.md).

### Enchanting

Enchanting becomes a reward for exploration, followed by a predictable choice at home. Players find books, build a shared **Library**, and select the enchantments they want at the table. Books are reusable; experience level sets how many enchantments a player can apply to one item.

The [enchanting manifesto](manifestos/enchanting.md) explains why. Vanilla enchanting repeatedly asks players to spend resources on an outcome they cannot control. Simply revealing the result would remove the luck but leave little game in the table itself. Maple puts the meaningful decisions in the sandbox: which structure to seek, which danger to face, and how far to push an expedition. Like brewing, the final recipe can be exact because obtaining its ingredients took play.

#### Finding and improving books

Every found book is level I. Most float in a fixed, visible place in a structure, with one guaranteed book per structure per world. Where a structure has several possible enchantments, it chooses one when it generates. Finding the structure guarantees its book, rather than another chance roll in a chest.

Two matching books of the same level combine in an anvil to make the next level. A level III book needs four level I finds, a level IV needs eight, and a level V needs sixteen. Higher levels build on useful earlier rewards: eight Sharpness books already give Sharpness IV while you work towards V.

Some structures react when you take their book:

- **Ancient City:** an angry Warden appears and opens a Subspace with a 100-block radius, applying Darkness and Grounded.
- **Bastion:** every Piglin in the structure becomes angry.
- **Stronghold or Woodland Mansion:** nearby Illagers are alerted and become hostile, even towards a player wearing Orichalcum.
- **Nether Fortress:** the book stays locked until every Blaze spawner that generated there is Cleared. The spawners can remain for farms. A fortress with none starts unlocked.

Other books need no added event; finding them is the challenge. Clearing a structure before taking its book is a valid approach. Blood Moons can bring its mobs back, but never its book.

Specialized enchantments arrive before general ones. Smite and Bane of Arthropods come from Dungeons, Blast Protection from Desert Pyramids, and Projectile Protection from Outposts. Sharpness and Protection come from tougher Illagers. Narrow enchantments can therefore be the best available option for a meaningful stretch of play.

#### Enchantment sources

| Source | Enchantments and maximum levels |
| --- | --- |
| Abandoned Camp | Lunge III |
| Ancient City | Swift Sneak III |
| Bastion treasure room | Fire Aspect II |
| Bastion bridge | Soul Speed III |
| Bastion housing units | Flame I |
| Bastion hoglin stables | Thorns III |
| Buried Treasure | Aqua Affinity I, Respiration III |
| Desert Pyramid | Blast Protection IV |
| Dungeon | Bane of Arthropods V, Smite V |
| End City | Feather Falling IV |
| Igloo | Frost Walker II |
| Jungle Temple | Looting III |
| Mineshaft | Efficiency V |
| Nether Fortress | Fire Protection IV |
| Ocean Ruins | Channeling I, Impaling V, Loyalty III, Riptide III |
| Pillager Outpost | Multishot I, Piercing IV, Projectile Protection IV, Quick Charge III |
| Shipwreck | Depth Strider III |
| Stronghold | Unbreaking III |
| Trail Ruins | Silk Touch I |
| Woodland Mansion | Fortune III |
| Pillager Captain | Power V, uncommon; Infinity I and Punch II, rare |
| Vindicator | Sharpness V, uncommon; Sweeping Edge III, rare |
| Evoker | Protection IV |
| Ravager | Knockback II, uncommon |
| Ender Dragon | Zephyr III, one level I book every kill |
| Wither | Blight II, one level I book every kill |
| Fishing | Luck of the Sea III, Lure III |
| Ominous vault | Breach IV, Density V, Wind Burst III |
| Full Library | Mending I |

Strongholds gain Illagers, giving their books a home outside raids and making the journey to the End Portal a fight. Illagers spawned by raids do not drop books, except designated raid-only rewards such as Ravager Knockback. Fishing supplies only its two fishing enchantments, and ominous vaults supply the Mace enchantments.

Other enchanted-book loot is removed. Each removed chest entry becomes one diamond. Bartering loses Soul Speed books and iron boots without replacements. Gear supplied by chests, fishing, trading, and bartering is plain. Gear that mobs spawned with drops with enchantments removed; gear they picked up from a player drops unchanged. Librarians buy books rather than sell them, giving surplus finds a use.

The placements also create reasons to return. Unbreaking III needs four Stronghold books, taking exploration beyond the first ring of three Strongholds. Fortune III needs four Mansions. Zephyr III needs four Dragon kills and helps with End exploration before Elytra.

#### The Library and the table

Put enchanted books in Chiseled Bookshelves around an enchanting table. The table offers each shelved enchantment at its highest available level, without consuming the book. Anyone can use the same Library, so friends can pool their finds and keep one set of books for the whole base.

The table still needs fifteen bookshelves, which can include Chiseled Bookshelves. A **Full Library** holds all 42 enchantments other than Mending at their maximum levels. Once combined, those 42 books fit in seven Chiseled Bookshelves.

The table is a checklist. Insert an item to see its current enchantments selected, change the selection, and press Save. The three random offers are gone. Saving costs one lapis for every selected enchantment. The first save on an unenchanted item also takes one unit of its material, such as an iron ingot for an iron pickaxe or an Adamant ingot for an Adamant pickaxe. Later edits pay lapis again, but do not need another material unit.

The grindstone removes all enchantments and returns that material unit, but not the lapis. The material is therefore a deposit that stays useful until the item breaks. Enchanted books resist fire and explosions.

Cheap changes make situational enchantments useful. You can prepare Smite for an undead fight or Fire Protection for a Nether trip, then change back at home. The choice supports planning an expedition without requiring several full equipment sets.

#### Experience and Enchantment Slots

Your level sets your **Enchantment Slots**, the number of enchantments you can save on one item. You get one at level 30 and one more every thirty levels, with no cap.

| Experience level | Enchantment Slots | Total experience points to reach it |
| --- | --- | --- |
| 30 | 1 | 1,395 |
| 60 | 2 | 8,670 |
| 90 | 3 | 24,045 |
| 120 | 4 | 47,520 |
| 150 | 5 | 79,095 |

Gold and Electrum tools and armor get one extra slot. Other Alloy traits compete with that extra enchantment, so material choice remains useful.

The slot check happens only when saving at the table. Equipment keeps working if its owner's level falls or if a higher-level friend enchanted it. A group's experienced enchanter can prepare equipment for everyone. To edit an item above your own limit, you must reduce its enchantments to fit or ask someone with enough levels.

Applying enchantments costs no experience. Combining books costs points, not levels: 50 times the resulting book level, from 100 points for level II to 250 for level V. Renaming costs ten points. There is no prior-work penalty, and repairs cost no experience. A Full Library takes 27,100 points in book-combining costs, separate from the experience kept for slots.

This gives experience two useful roles: a shared Library records the group's exploration, while levels distinguish the players who use it. Experience farms remain worthwhile.

#### Mending and Library Progress

Mending is the reward for a Full Library. It has no book source, and the table offers it only while every other enchantment is shelved at maximum level. Moving the books moves access to Mending. Gear already carrying it keeps it if the Library shrinks.

The reason for placing it last is that Mending invalidates multiple game systems, including the Unbreaking enchantment, repairs, mining, and Bastion hunting. Mending is therefore the final endgame reward after you've essentially done everything the game has to offer.

The table shows **Library Progress**: add the highest shelved level for each enchantment and divide by the Full Library total of 130. Two Sharpness I books count as one level; Sharpness I alongside Sharpness II counts as two. Mending is unlocked as an available enchantment at 100% progress, along with other rewards at lower progress points.

#### Repairs and material upgrades

The anvil combines books and repairs items with materials. It cannot apply books to gear, and items cannot be combined with each other for repairs or enchantments. The crafting grid and grindstone also lose damaged-item combining, and the grindstone gives no experience.

A full repair normally takes one fewer material unit than crafting the item, with a minimum of one. Each unit restores an equal share: a pickaxe needs two units for a full repair, a sword one, and a chestplate seven. One Netherite Ingot fully repairs Netherite gear; Alloy gear needs one ingot of its own Alloy.

Items without a simple material use these enchanting and repair materials:

| Item | Material |
| --- | --- |
| Elytra | Phantom membrane |
| Mace | Breeze rod |
| Bow, crossbow, fishing rod | String |
| Trident | Prismarine shard |
| Shears, flint and steel | Iron ingot |

Material upgrades refuse enchanted gear, so a diamond deposit cannot turn into a Netherite or Alloy ingot. Grind the item, upgrade it, then enchant it again. Reinforcement is an exception because it does not change the item's material.

#### Zephyr and Blight

**Zephyr** is a boots enchantment with three levels. It gives a double jump, with more height at each level. It does not reset fall distance, and the height regained by the jump does not add extra fall damage. A jump during a fall also **Clutches**, reducing the distance already fallen by 10%, 15%, or 25% (depending on level). Jumping later reduces more of the fall. It adds movement and a useful skill without replacing Feather Falling with slow falling or complete fall protection.

**Blight** goes on the same weapons as Fire Aspect and cannot combine with it. It applies Wither I for four seconds per level, up to level II. It deals about half Fire Aspect's damage, but works in water and against fire-immune enemies that are vulnerable to Wither. Against players it can create Withered Hearts (though they only take effect in the Nether). Fire Aspect retains its stronger damage and cooked food drops.

Curse of Binding and Curse of Vanishing are removed.

Details: [Enchanting](../.scratch/maple-flavored-design/issues/10-enchanting.md), [the role of experience](../.scratch/maple-flavored-design/issues/12-role-of-xp.md), and [repair materials](../.scratch/maple-flavored-design/issues/16-trading-combat-and-recipes.md).

### Enchanted golden apples

Enchanted golden apples become a craftable part of enchanting progression. A plain golden apple keeps its vanilla food value but has no status effects. It is the base item you take to the enchanting table, like an unenchanted pickaxe.

The table offers apple effects as Library Progress grows. Select any available combination, with one Enchantment Slot per effect. Gold's extra slot applies to tools and armor, so apples do not get it.

| Effect | Strength and duration | Library Progress required |
| --- | --- | --- |
| Regeneration II | 20 seconds | 15% |
| Fire Resistance | 5 minutes | 30% |
| Absorption IV | 2 minutes | 50% |
| Resistance | 5 minutes | 75% |

Saving costs one lapis per selected effect and, on the first save, one block of gold. The block is in addition to the gold already used to craft the plain apple. As with gear, the grindstone removes the effects and returns the plain apple and material deposit, but not the lapis.

The first apple therefore needs level 30 (to unlock the first slot) and 15% Library Progress, meaning at least 20 total shelved enchantment levels. An apple with two effects needs level 60, three needs 90, and all four needs 120. The Library threshold and the maker's level are separate requirements: a large Library alone cannot make a four-effect apple. No particular effect is mandatory, but one must be present.

Any enchanted golden apple cures a zombie villager under Weakness, whatever effects were selected. Every enchanted golden apple in loot becomes a plain one, so finding a chest cannot skip the table requirement.

Enchanted Golden Apples become a food you can create as a reward for filling your Library.

Details: [Villager curing and apple creation](../.scratch/maple-flavored-design/issues/08-villagers-and-villages.md) and [apple effects](../.scratch/maple-flavored-design/issues/11-brewing.md).

### Earning villagers and farms

Every village generates as an **Abandoned Village**, with zombie villagers and no iron golems. Villages are about 5.5 times rarer, using spacing 80 and separation 50. Hay bales and brewing stands are removed. Crops, beds, bells, and other workstations remain, giving you the start of a settlement rather than an instant supply of food and equipment.

Curing is the only way to get the first living villagers. The living villager in an igloo basement becomes a second zombie villager. Natural zombie villagers and zombie conversion otherwise work as in vanilla.

A cure needs a splash potion of Weakness and an enchanted golden apple. Weakness is brewed by adding a fermented spider eye to Strength, so the brewing route needs the Nether. Strength becomes Weakness for 1:30, Long Strength becomes Long Weakness for 4:00, and Strength II becomes ordinary Weakness. A fermented spider eye no longer changes a water bottle. Witches can still provide Weakness for Skyblock players.

The intended route to villagers therefore includes the Nether, a modest Library, and level 30. This delays iron and raid farms and villager trading halls without nerfing them (for the most part) or making them more cumbersome to build. Once villagers are earned, breeding, professions, workstations, and cure discounts stay vanilla. Master Armorers remain available to supply Reinforcement before Protection is likely to arrive.

Toolsmiths, Armorors, and Weaponsmiths sell unenchanted equipment, Librarians buy books instead of selling them, and no villager sells Pith, Crystal Hearts, or raw diamonds. Other trades stay vanilla.

Details: [Villagers and villages](../.scratch/maple-flavored-design/issues/08-villagers-and-villages.md) and [farm pacing](../.scratch/maple-flavored-design/issues/09-farm-pacing.md).

### Netherite and Alloys

Netherite remains the base for the final equipment choices. Its ingot recipe becomes four Netherite Scrap and four Pith. Five **Alloys** then improve on Netherite in different ways, with no single choice intended to replace all the others.

An **Alloy Furnace** takes one Netherite Ingot, one storage block (ie an iron block or gold block), and one **Ancient Ember** as fuel to make one Alloy ingot. Its recipe uses five gold ingots across the top and sides, a blast furnace in the center, and three polished blackstone bricks along the bottom. Hoppers work with it, but it has no other processing role.

Piglin Brutes drop Ancient Embers on player kills at 25%, plus five percentage points per Looting level. This roll is separate from Pith. Blood Moons bring Brutes back, so Embers are slowly renewable, but a fully automatic farm cannot supply them.

| Alloy | Storage block ingredient | Tools and weapons | Armor, per piece | Full armor set |
| --- | --- | --- | --- | --- |
| Steel | Iron | 50% more durability and more weapon knockback | 50% more durability, more knockback resistance, 10% less Warden sonic boom and explosion damage | Knockback and Levitation immunity |
| Electrum | Gold | One extra Enchantment Slot | One extra slot, 15% less fire damage, counts as gold to Piglins | Fire and lava immunity; Withered Hearts form every ten seconds instead of five |
| Shakudo | Copper | 25% faster mining and 10% faster attacks | 2.5% faster movement | No slowdown from soul sand, honey, cobwebs, or powder snow |
| Adamant | Diamond | One extra damage | One extra armor toughness | 25% of melee damage ignores armor points, but not Protection |
| Orichalcum | Emerald | A flat 50% bonus to Looting/Fortune drops | 10% less magic damage; Illager neutrality | Survive a lethal hit with Totem effects, recharging over thirty minutes of play |

Orichalcum's drop bonus adds to Fortune or Looting rather than multiplying them, and affects only drops those enchantments already affect. It excludes Ancient Embers, Pith, and enchanted books. One Orichalcum armor piece grants Illager neutrality, which ends during raids, after attacking an Illager, or when a structure's book alerts them. Its death save belongs to the player and triggers before a held Totem or Crystal Heart.

Gold's extra slot and Electrum's extra slot preserve gold's enchanting identity. Emerald gives Orichalcum its wealth theme.

At a Smithing Table, Netherite gear and one Alloy ingot make the Alloy version without a template. This covers the equipment that has a Netherite version. You can change an Alloy item to another Alloy with another ingot, losing the old material. Gear cannot be salvaged. An unused Alloy ingot can be smelted back into a Netherite Ingot, losing the storage block and Ember.

Each Alloy has a storage block made from nine ingots, which crafts back into nine and works as a beacon base.

An **Armored Elytra** permanently combines Elytra with an Alloy chestplate at a Smithing Table. It keeps that chestplate's armor and effects and counts towards its full set bonus. Plain Netherite chestplates do not qualify. The added protection costs flight performance: horizontal glide movement gets an additional 0.995 multiplier each tick, and Firework Rocket boost is reduced by 25%. A plain Elytra remains useful when travel matters more than armor.

Details: [Material roles and Alloys](../.scratch/maple-flavored-design/issues/06-material-roles-and-alloys.md). The [Raspberry alloy research](research/raspberry-alloys.md) informed material choices; [BetterEnd research](research/betterend-armored-elytra.md) informed the separate glide and rocket penalties.

### Experience and Remnants

Experience remains useful throughout a save because levels keep adding Enchantment Slots. Spending points to combine books or rename items can lower your slot count, but it does not disable existing gear. Repairs are free, and experience sources and Mending's repair behavior otherwise stay vanilla.

Death drops all your experience points as a **Remnant**. Only you can see or collect it. It cannot be destroyed and does not despawn while you are offline or its chunk is unloaded. Walking over it returns the points directly, so Mending cannot consume them during recovery.

The Remnant appears where you died, or at your last solid ground if the death location would make recovery impossible, such as the void or lava. The Recovery Compass remains the only tracker and points towards your most recent death.

You can have several Remnants, but each further death reduces every uncollected one by 20% of its current points, even if you died carrying no experience. Recovering an earlier Remnant therefore matters.

This shifts the stakes as a save develops. Early on, the Heart Floor protects you while losing Max Hearts matters most. Later, a large reserve of experience gives a dangerous expedition something valuable to risk without putting your inventory on the ground.

Details: [The role of experience](../.scratch/maple-flavored-design/issues/12-role-of-xp.md).

### Boss Subspaces

The [boss manifesto](manifestos/boss-subspaces.md) identifies a problem: a boss loses its purpose if one cheap escape avoids the whole encounter. A Warden that cannot reach a flying player is the clearest example. Once a player has an Elytra, they can simply fly away from the Warden if they summon one by accident. Ancient Cities become incredibly easy to explore. A **Subspace** changes the area around a boss to preserve the encounter it is meant to create.

#### The Warden

The Warden's **Subspace** is a fixed sphere with a 100-block radius, centered where it spawns. Darkness and **Grounded** make sneaking, hiding, and finding a route out the ways to survive.

Grounded applies an additional 0.97 multiplier to horizontal glide movement each tick and reduces Firework Rocket boost by 95%. These stack with Armored Elytra penalties. It also greatly increases Ender Pearl gravity, so pearls fall sharply and travel much shorter distances. Their teleports still work, letting you reach nearby cover or cross a gap. The exact gravity multiplier needs playtesting. Riptide and Zephyr's double jump do not work; Levitation, Slow Falling, and Chorus Fruit still do.

The Warden is aware of all entities within the Subspace and will attempt to pathfind to the nearest player. Anger and the sniffing mechanic are unchanged. You can escape by leaving the active Subspaces, or hide until their Wardens go away after an undisturbed minute. Each Subspace ends when its own Warden goes away. Death respawns you normally and applies its normal costs, including if your bed or anchor is inside.

Sculk spreads across the ground and the area gains a sculk skybox. New sensors, catalysts, and shriekers can grow. Those shriekers can summon while the Subspace is active, but lose that ability when it ends.

Every Warden opens its own Subspace at its spawn point, even inside another one. Subspaces can overlap, and Grounded does not stack with itself. There are no overlap-based shifts or summon failures. Taking an Ancient City's book always summons an angry Warden regardless of Shrieker level or nearby Wardens.

Summoning also changes to stop a distant, repeatedly failing shrieker from disabling the danger. Shriek warnings and cooldowns affect only players in **Warden Territory**, meaning the Deep Dark biome or an Ancient City's bounds. The Warden searches five to fifteen blocks from the player who caused the shriek, or the nearest player in Warden Territory if needed, then widens its search to other spots within the Subspace up to fifty blocks from that player. This spawn search distance is separate from the 100-block Subspace radius. If it still cannot spawn, the cooldown clears. With nobody in Warden Territory, summoning is cancelled. These changes make Warden switches ineffective.

#### The Wither

The Wither creates a **Subspace**  one hundred blocks in radius. Inside, Withered Hearts cannot recover, even outside the Nether. The Subspace does not apply Wither by itself; skull attacks do that.

The Wither stays within fifty blocks of the center. It is immune to suffocation and can pass through bedrock and obsidian, preventing those blocks from pinning it in place. Other blocks remain subject to its destruction.

The point is to stop safe trapping and recovery, while leaving room to move and fight. The Elder Guardian and Ender Dragon retain their existing encounters unless playtesting identifies a comparable problem.

Details: [Mob changes](../.scratch/maple-flavored-design/issues/05-mob-changes.md) and the [Subspace manifesto](manifestos/boss-subspaces.md).

### Structures and their rewards

Existing structures carry the exploration progression. Maple adds no new structures. Their enchantment books, guarded health rewards, and material sources give reasons to visit them.

Normal spawners ignore light and cannot be broken or destroyed by explosions until **Cleared**. Clearing requires twelve deaths among mobs that spawner created, plus two for each additional player who entered its sixteen-block activation range after counting began. The count is shared, never drops when someone leaves, and any cause of death counts. Traps and lava are valid tools.

A Cleared spawner stays Cleared. It can be broken for one Pith or kept for a farm. Its progress is visible through feedback such as particles or cracks. Trial spawners keep their vanilla rules.

Dungeons become more common, with fifteen placement attempts per chunk above Y 0 and eight below, compared with ten and four in vanilla. They keep a single spawner. This provides an early supply of books and Pith without adding a new structure type.

Crystal Hearts appear in chests guarded by substantial threats. Pith appears more widely. These are separate rolls per chest, giving one item unless shown otherwise:

| Reward location | Crystal Heart | Pith |
| --- | --- | --- |
| Ancient City | 9% | 18% |
| Stronghold corridor | 12.5% | 6% |
| Bastion treasure | 25% | 5% |
| Other Bastion chests | — | 5% |
| Woodland Mansion | 12.5% | — |
| Dungeon | — | 5% |
| Mineshaft | — | 5% |
| Ominous vault, per opening | — | 10% |
| End City | 15% | 50%, giving one or two |

Most rates are half Matcha's or lower. End City rewards use their own rates to help replace lost enchanted gear. Chests never refill. End Cities keep their existing buildings, Shulkers, ships, and guaranteed Elytra, alongside their Feather Falling book.

Village chests lose diamonds, obsidian, and iron gear without replacements, so an abandoned village feels picked over. Stone tools become copper and enchanted golden apples become plain ones, following the wider loot rules.

Details: [Structures](../.scratch/maple-flavored-design/issues/14-structures.md) and [End City rewards](../.scratch/maple-flavored-design/issues/13-storage-and-end-rewards.md). The [Matcha structure research](research/matcha-structures.md) supplies comparisons, rather than a list of additions to copy.

### Blood Moons

A **Blood Moon** brings back selected structure mobs every fourth full moon, or every thirty-two in-game days. It makes rare materials slowly renewable and keeps cleared structures useful without changing the whole world's rules after a boss kill.

| Structure | Mobs Restored | Required block at the original spot |
| --- | --- | --- |
| Bastion | Piglins, Piglin Brutes, Hoglins | Blackstone family or basalt beneath |
| Ocean Monument | Elder Guardians | Water, with prismarine family beneath |
| End City | Shulkers | Attachment to purpur family or end stone bricks |
| Woodland Mansion | Vindicators, Evokers | Birch planks, cobblestone, or smooth stone slab beneath |
| Stronghold | Its added Illagers | Stone bricks family beneath |

Each structure is **Restored** when its chunks next load after a Blood Moon, filling missing members of its original population at their original spots. The spots must be clear, with the required blocks still present. Missed Blood Moons do not stack, distant chunks are not loaded for the event, and no mob appears within thirty-two blocks of an online player.

Only mobs return. Blocks, chests, books, Allays, and broken spawners do not. Removing a structure or its required blocks ends its renewal, while an intact Bastion used as a base can still receive visitors.

The night has a red moon and sky and one chat announcement in every dimension. Sleeping remains possible, and it adds no extra general monster spawns. It is a chance to revisit structures for Ancient Embers, Pith, and eligible Illager books.

Details: [Blood Moons](../.scratch/maple-flavored-design/issues/15-blood-moons.md).

## Misc Changes

- **Food and healing:** hunger, saturation, natural regeneration, and ordinary food values stay vanilla. Plain golden apples lose their effects as described above. Food does not replace brewing.
- **Potion storage:** drinkable, splash, and lingering potions and water bottles stack to sixteen. Splash and lingering throws share a one-second player cooldown. Dispensers keep vanilla speed. Brewing stand bottle slots still hold one bottle each, so production speed does not increase.
- **Creepers:** natural Overworld spawning is limited to below Y 63, including caves and structures. Spawners are unaffected.
- **Mob health and movement:** faster, more fragile zombies and lower health on several other enemies make combat dangerous without making every encounter slow. The starting values below come from Matcha Flavoured, with credit. Health is in points, with two points per heart.

| Mob | Easy | Normal | Hard |
| --- | --- | --- | --- |
| Adult zombie | Vanilla | 10 health, 0.34 speed, steps up one block | 15 health, 0.38 speed, steps up one block, 5 damage, 0.6 jump |
| Baby zombie, husk, drowned, or zombie villager | 4 health | 4 health | 7 health |
| Skeleton, stray, bogged, or parched | 10 health | 10 health | 12 health |
| Creeper | 14 health | 16 health | 18 health |
| Cave spider | 2 health, 0.35 speed | 4 health, 0.40 speed | 6 health, 0.42 speed |
| Silverfish | 2 health | 2 health | 4 health |
| Spider | Vanilla | 0.85 jump, no fall damage | 0.85 jump, no fall damage |
| Adult husk | Vanilla | 30 health, 0.25 speed, 6 damage, 12 armor, 50 follow range, 0.9 knockback resistance | 40 health, 0.265 speed, 8 damage, 14 armor, 60 follow range, full knockback resistance |

Adult zombie villagers keep vanilla stats, so curing one does not require chasing a fast zombie. Unlisted mob rules, armor, and combat stay vanilla apart from the systems above.

“Getting an Upgrade” asks for a copper pickaxe. The wider advancement design for Alloys, the Library, Crystal Hearts, and other new systems is [still open](../.scratch/maple-flavored-design/issues/17-advancements.md).

Deeper End changes are reserved for a separate End Update mod that requires Maple Flavored. The proposed vault-based End Cities and changed Elytra rewards are outside this mod's scope. Custom music and discs, horror elements and secrets, a fishing progression system, and translations beyond English are also outside the current scope.
