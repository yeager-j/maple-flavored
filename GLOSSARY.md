# Maple Flavored

The language of Maple Flavored's game design, a Fabric mod that builds on vanilla Minecraft.

## Language

### Hearts

**Max Hearts**:
A player's current permanent health.
_Avoid_: Hearts, health, heart containers

**Heart Floor**:
The lowest a player's Max Hearts can fall through death.
_Avoid_: Minimum hearts, minimum

**Milestone**:
One of seven advancements that each lower a player's Heart Floor by one while the player holds it.

**Crystal Heart**:
An item that raises Max Hearts by one when consumed, or saves its holder from a lethal hit.
_Avoid_: Heart container

**Withered Heart**:
A heart made temporarily unusable by the Wither effect. It doesn't reduce Max Hearts.
_Avoid_: Broken heart, gloom

**Pith**:
A rare material used to craft Crystal Hearts and Netherite Ingots.
_Avoid_: Divine Fragment

### Materials

**Alloy**:
A material made from a Netherite Ingot and a storage block: Steel from iron, Electrum from gold, Shakudo from copper, Adamant from diamond, or Orichalcum from emerald. Each improves on Netherite in its own way.

**Alloy gear**:
The Alloy version of a Netherite item, made at a Smithing Table from the Netherite item, or another Alloy's version of it, and one Alloy ingot, with no template. It has Netherite's stats plus its Alloy's traits, and is repaired and enchanted with its own Alloy ingot.

**Alloy Furnace**:
The block that smelts a Netherite Ingot and a storage block into an Alloy ingot, using an Ancient Ember as fuel.

**Ancient Ember**:
The Alloy Furnace's fuel, dropped by Piglin Brutes when a player kills them and found in Bastion chests alongside ancient debris. One makes one Alloy ingot.
_Avoid_: Brute Ember

**Diamond Shard**:
What diamond ore drops, and what safe, early loot gives in place of a diamond. Four craft one diamond.
_Avoid_: Diamond piece, diamond fragment

**Armored Elytra**:
An Alloy chestplate permanently combined with an Elytra at a Smithing Table, one per Alloy. It counts as that Alloy's chestplate for armor, traits, enchanting, repairs and set bonuses, and glides worse than a plain Elytra.

**Illager neutrality**:
Orichalcum armor's trait: Illagers, and an Evoker's Vexes, leave a player wearing any Orichalcum piece alone, except in a raid or for a while after the player attacks one or takes a structure's book.

**Orichalcum drop bonus**:
Orichalcum tools' trait: drops that Fortune or Looting affect gain half again on top of the enchantment, except Ancient Embers, Pith and enchanted books.

**Material unit**:
The one material that repairs and enchants an item, such as an iron ingot for iron gear or string for a bow. A full repair takes one fewer than the item's recipe uses, at least 1.
_Avoid_: Repair item, repair material

**Reinforced**:
Said of an armor piece given +1 armor toughness by smithing it with chainmail and a Reinforcement template.
_Avoid_: Plated, chained

**Reinforcement template**:
The smithing template that Reinforces an armor piece with a chainmail piece. Master Armorers sell it, and 7 Diamond Shards and a block of iron copy one into 2. It is in no loot.
_Avoid_: Chainmail template, reinforcement upgrade

### World

**Blood Moon**:
A periodic red full moon that brings back mobs that structures don't otherwise replace.

**Abandoned Village**:
The only kind of village: vanilla's zombie village, decayed, populated by zombie villagers and with no iron golems, so every villager is earned by curing.
_Avoid_: Ruined village, zombie village

**Cleared**:
A spawner whose mobs have died enough times, 12 plus 2 per additional player, so it can be broken. Until then it spawns twice as fast and glows hot, cooling back to its plain look in patches as its mobs die. A Cleared spawner looks like any other and stays Cleared.

**Restored**:
Said of a structure whose mobs have returned after a Blood Moon.
_Avoid_: Respawned

**Subspace**:
An area a boss opens around itself to shut down the cheapest way of avoiding its fight. Players can enter and leave; the Warden's restricts movement, and the Wither's stops Withered Hearts from recovering. Unrelated to vanilla's "Subspace Bubble" advancement.
_Avoid_: Domain, aura

**Grounded**:
An effect that stops Elytra gliding, greatly increases Ender Pearl gravity to shorten throws, and stops Riptide and Zephyr's double jump. It applies inside a Warden's Subspace, and inside an Ancient City until its book is gone, however it went.
_Avoid_: Heavy, gravity

**Warden Territory**:
An Ancient City's bounds or the Deep Dark biome: the only places a shriek applies to a player.
_Avoid_: Warden zone

### Deep caves

**Deep variant**:
A stronger form of an adult zombie, skeleton, creeper or spider that takes the place of every natural spawn of that mob below Y 0: the Deep Zombie, Deep Skeleton, Deep Creeper and Deep Spider.
_Avoid_: Elite, deep mob

### Enchanting

**Keeper**:
One of up to three guards chosen when a structure is planned, which unlock its book once all are gone. A structure with nothing to choose a keeper from has none, and its book is free to take. A Bastion's keepers are Piglin Brutes: any death counts, and a refused take makes the living ones glow and sends trails to them. An Ancient City's keepers are sculk shriekers placed in a ring near its edge: breaking them by any means counts, and a refused take sends trails to the ones still standing. A Stronghold's keepers are Evokers, one in the portal room and one in each of two big rooms chosen far from it and from each other: any death counts, and a refused take makes the living ones glow and sends trails to them.

**Structure book**:
The one level I enchanted book a structure or Dungeon holds, in a fixed spot. Most float where they can be seen. Buried Treasure keeps its book in the treasure chest, Trail Ruins keep theirs in one suspicious gravel, and a Dungeon's spawner holds its book and drops it with its Pith once Cleared and broken. Mineshaft cave spider spawners each hold a Bane of Arthropods book the same way, besides the Mineshaft's own book. The Nether Fortress's book is locked until every Blaze spawner the fortress generated with is Cleared, and a Bastion's or Ancient City's until its keepers are gone. A locked book floats closed in a gold cage, which breaks away in patches as those spawners are Cleared or keepers go. Taking it can wake the structure, as a Bastion's, Stronghold's or Woodland Mansion's does.
_Avoid_: Floating book, book pedestal

**Library**:
The enchanted books shelved in Chiseled Bookshelves around an enchanting table, which set the enchantments and levels it can apply.
_Avoid_: Collection, book collection

**Full Library**:
A Library holding every enchantment except Mending at its max level. Only a table with one offers Mending.
_Avoid_: Complete library, maxed library

**Library Progress**:
How close a Library is to Full: the sum of the highest level shelved for each enchantment, as a share of a Full Library's. It unlocks enchanted golden apple effects before Mending.
_Avoid_: Collection progress, completion

**Enchantment Slot**:
Room for one enchantment on an item, set by the level of the player who enchants it: one at level 30, and one more every 30 levels.
_Avoid_: Enchant limit, capacity

**Deposit**:
The material unit an item pays on its first save at the Enchanting Table. It stays on the item, and the grindstone hands it back when it strips the item.
_Avoid_: Enchanting fee, ingot cost

**Remnant**:
The XP a player drops on death, which only they can collect. Each further death shrinks it. Unrelated to vanilla's "Respecting the Remnants" advancement.
_Avoid_: Bloodstain, grave, souls

**Blight**:
A weapon enchantment that gives a struck mob the Wither effect.
_Avoid_: Withered, Withering

**Zephyr**:
A boots enchantment that gives a double jump.

**Clutch**:
A Zephyr double jump during a fall, which cuts the distance fallen so far.
