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

## The enchanting table

The table should be upgraded permanently by spending XP, and fully upgrading it should take a large amount of XP, far more than vanilla's 3 levels. Fifteen bookshelves isn't a real gate. This gives XP a real job, which Matcha took away, and makes XP farms a reward.

How the table fits with books found in the wild is an open question below.

## Open questions

These came up in discussion and are left for the Enchanting ticket.

### How many books it takes

If one found book is level I and combining doubles, a level n book needs 2^(n-1) finds. If books are used up when applied, that's per item:

| Goal | Books |
| --- | --- |
| Sharpness V on one sword | 16 |
| Protection IV on a full armor set | 32 |
| Efficiency V on a pickaxe, shovel and axe | 48 |

On a server, every player needs their own. Ancient Cities are rare, so this is probably too many unless something changes:

- **Copy, don't consume.** A found book teaches something, such as the table or a library, and copies are made from it for XP and lapis. Combining found books raises what can be copied. Then four Sharpness finds means Sharpness III on all your gear.
- **Lower the max levels.** Fewer levels mean fewer doublings.
- **Match the source to the max level.** Enchantments with high max levels come from common structures, and Ancient Cities and Bastions hold single-level or low-level prizes.

### What the table does

If books come from the wild and levels come from the anvil, the XP-upgraded table needs a role. Options:

- It copies books you've found, up to a level its upgrades allow. This pairs with "copy, don't consume".
- It holds a library: Chiseled Bookshelves nearby hold found books, the table can only offer what's shelved, and XP upgrades set how high it can go. This keeps vanilla's vocabulary, stores progress in the world rather than hidden data, and works for a shared base.
- It's dropped, and the anvil does everything.

Upgrading with XP alone only costs time once you have a farm. Each upgrade tier could also need one material that only progression gets you, so the table's upgrade path follows the game.

Whatever its role, the 3-slot random offer goes. A level choice is dominant if it's deterministic, so the table always gives the highest level available. This replaces Orichalcum's Better enchanting perk, as the Material roles ticket expects.

### Who gets each book

In a shared world, a book that can be taken once gives the prize to whoever gets there first. Vanilla's Trial Chamber vaults give each player their loot once. A floating book could work the same way, waking the structure again for each player.

### Clearing the structure first

Players may clear a Bastion before grabbing its book, so nothing is left to anger. That may be fine, since the fight still happens either way. A Blood Moon Restores Bastion mobs, but books never return. A Warden spawned on grab can't be avoided this way, though digging and pillaring may still cheese it.

### Bypasses

Anything else that hands out enchanted books gets around found books. That means librarian trades, chest loot, fishing treasure and Piglin bartering. Enchanted gear dropped by mobs or found in chests also needs a rule, even if it can't be combined.

### Bad enchantments become bad finds

Clearing a structure to find Bane of Arthropods brings the eye-rolling back. Players are never forced to take bad enchantments now, but they shouldn't be the prize. Merging them has a gameplay reason: the elemental Protections into Protection, and Smite and Bane into Sharpness. Their niches could also move to Alloy traits.

### Early-game pacing

If Efficiency or Unbreaking needs an Ancient City, the early game gets worse than vanilla. The essentials need early structures such as mineshafts or ruined portals, or a small starter set the table offers from the start. Mending and Looting can sit late.

### The anvil's prior-work penalty

Vanilla's prior-work penalty limits stacking through repeated combining. Found books now do that job. With the penalty, repaired gear eventually hits "Too Expensive!", which is harsh when its enchantments took dozens of books. The penalty probably goes or changes for repairs.

### Losing enchanted gear

A broken tool takes its books with it. This makes durability and repair much more important, so it depends on the Mending rework and Steel's durability bonus. Repairing two copies of a tool in the anvil also needs a rule for which enchantments survive.

### Still to place

- Where Mending, after its rework, and Zephyr are found
- How Electrum's one level above the Fortune and Looting caps works with found books
- Which structure holds each enchantment, and what grabbing each book wakes
