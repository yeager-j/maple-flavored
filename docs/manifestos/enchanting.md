# What I want from enchanting

Jackson's opinions on enchanting, worked out in discussion. They're a summary of intent rather than a quote, and they set the direction for [the Enchanting ticket](../../.scratch/maple-flavored-design/issues/10-enchanting.md). Where a resolved ticket on the design map differs, the ticket wins. The open questions at the end aren't decided.

## The problem

Enchanting is the only part of Minecraft that feels like a loot box. Everything else is fairly deterministic. Potions and crafting are exact recipes. Mob drops have chance, but you know ahead of time which mobs drop which item, and you lose nothing by killing one. The only other system that really relies on luck is villager trading, which isn't a great system either.

A game happens when the player is presented with a small handful of non-arbitrary, non-trivial options. Enchanting is both arbitrary and trivial.

- **Arbitrary:** you have no way of controlling the outcome.
- **Trivial:** there's always a dominant choice. At the table you pick 3 levels over 2 or 1. Among enchantments, you pick Sharpness over Smite.

A lot of enchantments are trash. Everyone rolls their eyes at Smite, Bane of Arthropods, and Blast, Projectile and Fire Protection. Inventory space and resources are limited, and carrying four sets of armor and three swords is impractical.

Vanilla gets away with all this by making enchanting cheap. A level 30 enchantment costs 3 levels, which is easy with an XP farm, and lapis is renewable through villagers. If you get Smite IV, you shrug, use the grindstone and go back to farming. Nothing stops you from getting the enchantments you want except time.

## Fixes that don't work

- **Letting players pick.** Inside the current system, this makes enchanting too easy. Saying "give me Sharpness IV, Looting III, Sweeping Edge III and Unbreaking III" takes zero brainpower.
- **Rebalancing the bad enchantments.** Making Smite competitive misidentifies Minecraft's game. The game isn't managing your inventory or giving your character the best stats for the situation. It's not an action RPG. It's an infinite sandbox: explore, build, fight. Enchantments let you do those things better, so they're a means to an end. Making players spend brainpower on the wrong game is a mistake.

## The direction

Enchanting should work like brewing. Brewing is fully deterministic, but it's gated by exploration: you need Nether Wart and Blaze Rods. Nobody calls it trivial, because the game is in getting the ingredients, not in the brewing stand.

So the choices that matter are at the sandbox level: where to go next and what to take on. Enchanting rewards those choices. Once the enchantments are earned, the final shopping list is fine. Choosing Sharpness IV takes no thought, but getting it took a lot of play.

## Enchantments are found in structures

Enchantments are found in the world, much like Skyrim's words of power. Vanilla already does this with Swift Sneak in Ancient Cities and Soul Speed in Bastions, and this extends it to everything.

- **Structures, not mob kills.** Unlocks are tied to structures for the most part, to encourage exploration over farming.
- **A fixed place, not a chest roll.** A random 5% chest drop just moves the loot box into the chest. The book should be in a known spot, guaranteed to be there.
- **Taking the book wakes the structure.** Each book is a floating book in the structure's most important spot, and grabbing it triggers that structure's own danger, turned up. Two examples:
  - **Ancient City:** the book floats in the middle of the city, in front of the portal. Grabbing it spawns a Warden, and you have to sneak around it to escape.
  - **Bastion:** the book floats in the treasure room. Grabbing it angers every Piglin in the structure. Good luck.

## Books and the anvil

- **Every book found in the wild is level I.**
- **Two books of the same level combine into the next level**, following vanilla's anvil rules. A level III book takes four found books, so higher levels are a serious prize.
- **The anvil combines books** to upgrade them, and still repairs tools and armor.
- **Tools can no longer be combined** to raise their enchantment levels. Found books are the only source of levels.

## Books are communal

A book is an item that fits in a Chiseled Bookshelf and powers a nearby enchanting table. The table can offer an enchantment shelved nearby, at that book's level, and the book isn't used up. Anyone can use the table, so a group shares one library.

Sharpness V still takes 16 found books, but you and your friends can combine your efforts. One library enchants everything in the base, and a broken tool loses only the cost of enchanting it again, never the books.

## The enchanting table

The table should be upgraded permanently by spending XP, and fully upgrading it should take a large amount of XP, far more than vanilla's 3 levels. Fifteen bookshelves isn't a real gate. This gives XP a real job, which Matcha took away, and makes XP farms a reward.

How the table fits with books found in the wild is an open question below.

## Open questions

These came up in discussion and are left for the Enchanting ticket.

### How many books it takes

If one found book is level I and combining doubles, a level n book needs 2^(n-1) finds. Communal books make this a cost per library rather than per item or per player, so Sharpness V is 16 finds for the whole group. A solo player still needs all 16, and the map's balance assumes a shared world, so it's open whether that's acceptable alone. Options if it isn't:

- **Lower the max levels.** Fewer levels mean fewer doublings.
- **Match the source to the max level.** Enchantments with high max levels come from common structures, and Ancient Cities and Bastions hold single-level or low-level prizes.

### What the table does

The table's job is now to read the library. Its XP upgrades need a role that books don't already fill, or they go. Options:

- Upgrades cap the level the table can read from its shelves. This gates levels twice, by books and by XP, which may be one gate too many.
- Upgrades add range or shelf capacity, so a bigger library needs a stronger table.
- Upgrades go, and XP is spent each time you enchant instead. Books never run out, so this is the ongoing XP sink.

Upgrading with XP alone only costs time once you have a farm. Each upgrade tier could also need one material that only progression gets you, so the table's upgrade path follows the game.

Whatever its role, the 3-slot random offer goes. A level choice is dominant if it's deterministic, so the table always gives the highest level available. This replaces Orichalcum's Better enchanting perk, as the Material roles ticket expects.

Vanilla Chiseled Bookshelves don't power tables, so this is a change. Plain bookshelves need a decision too: whether they still add power, or are only decoration.

### Combining in the anvil or on the shelf

Books could be combined in the anvil into one book per level, as above, or the table could count several level I books on its shelves. Combining keeps the library small, since a Chiseled Bookshelf holds 6 books. Counting avoids a merged book holding 16 finds' worth of work in one item.

### Who gets each book

Communal books suit one book per structure per world. Vanilla's Trial Chamber vaults give each player their loot once, but a per-player book would multiply finds on a server, which fights the shared library.

### Protecting the library

Shelved books are a base's most valuable items. Fire, creepers and griefers could take 16 structures' worth of work in one go. Books could resist fire and explosions, like Netherite, or drop when their shelf is destroyed.

### Clearing the structure first

Players may clear a Bastion before grabbing its book, so nothing is left to anger. That may be fine, since the fight still happens either way. A Blood Moon Restores Bastion mobs, but books never return. A Warden spawned on grab can't be avoided this way, though digging and pillaring may still cheese it.

### Bypasses

Anything else that hands out enchanted books gets around found books. That means librarian trades, chest loot, fishing treasure and Piglin bartering. Enchanted gear dropped by mobs or found in chests also needs a rule, even if it can't be combined.

### Bad enchantments become bad finds

Clearing a structure to find Bane of Arthropods brings the eye-rolling back. Players are never forced to take bad enchantments now, but they shouldn't be the prize. Merging them has a gameplay reason: the elemental Protections into Protection, and Smite and Bane into Sharpness. Their niches could also move to Alloy traits.

### Early-game pacing

If Efficiency or Unbreaking needs an Ancient City, the early game gets worse than vanilla. The essentials need early structures such as mineshafts or ruined portals, or a small starter set the table offers from the start. Mending and Looting can sit late.

### The anvil's prior-work penalty

Vanilla's prior-work penalty limits stacking through repeated combining. Found books now do that job. With the penalty, repaired gear eventually hits "Too Expensive!" The penalty probably goes or changes for repairs, though re-enchanting a fresh tool is now an alternative.

### Repairing with two tools

Repairing two copies of a tool in the anvil needs a rule for which enchantments survive.

### Still to place

- Where Mending, after its rework, and Zephyr are found
- How Electrum's one level above the Fortune and Looting caps works with found books
- Which structure holds each enchantment, and what grabbing each book wakes
