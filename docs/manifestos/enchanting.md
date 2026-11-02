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

Sharpness V still takes 16 found books, but you and your friends can combine your efforts. One library enchants everything in the base, and a broken tool loses only the ingots paid to enchant it, never the books.

## Level sets how many enchantments you can apply

Communal books mean everyone in a group can reach the same enchantments, so players need another way to stand apart. Your level sets how many enchantments an item can hold: 1 at level 30, and 1 more every 30 levels.

This isn't as brutal as it sounds. Keep Inventory is on by default, so you don't lose XP when you die.

Vanilla's XP curve makes each slot cost much more than the last:

| Enchantments | Level | Total XP |
| --- | --- | --- |
| 1 | 30 | 1,395 |
| 2 | 60 | 8,670 |
| 3 | 90 | 24,045 |
| 4 | 120 | 47,520 |
| 5 | 150 | 79,095 |

Orichalcum tools get 1 extra enchantment slot. This replaces Orichalcum's Better enchanting perk, as the Material roles ticket expected.

## Enchanting is paid with the item's material

Enchanting no longer costs XP. You pay with an ingot of the item's own material: an iron pickaxe takes an iron ingot, and an Adamant pickaxe takes an Adamant ingot.

The grindstone removes enchantments and gives the ingot back. So the ingot is a deposit stored in the item rather than a spend, and it's only lost if the item breaks.

## The enchanting table

The table should be upgraded permanently by spending XP, and fully upgrading it should take a large amount of XP, far more than vanilla's 3 levels. Fifteen bookshelves isn't a real gate. This gives XP a real job, which Matcha took away, and makes XP farms a reward.

Using level to set enchantment slots may replace this, since it gives XP a job without spending it. That conflict is an open question below.

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
- Upgrades go. Enchanting is paid in ingots, and level already sets your slots.

Upgrading with XP alone only costs time once you have a farm. Each upgrade tier could also need one material that only progression gets you, so the table's upgrade path follows the game.

### XP as a rank or a currency

If level sets your enchantment slots, spending XP lowers your rank. That makes every XP cost feel bad, including table upgrades, anvil repairs and combining books. The cleanest rule is that XP is never spent, and only Mending competes with your level, by eating orbs before you collect them. This belongs to the Role of XP ticket too.

### When the slot limit is checked

If the limit is checked only at the table, the highest-level player can enchant everyone's gear, and communal books plus shared enchanting erase the differences between players. Checking it while the item is in use keeps players distinct. Enchantments past the holder's limit would go dormant, most recently applied first, and show greyed out. It's more complex, but it's the version that does what the slot limit is for.

### The step size

How many slots matter depends on how many enchantments an item wants. A sword can take about six (Sharpness, Looting, Sweeping Edge, Unbreaking, Mending, Fire Aspect), a pickaxe about four, and an armor piece four or five. Merging the bad enchantments lowers these. With a step of 30 levels, 4 slots needs about 47,500 XP, a few hours at a good farm.

### Orichalcum's extra slot

The Material roles ticket gives Orichalcum's armor Better enchanting too, so its armor probably gets the extra slot as well. An extra slot may be stronger than other Alloys' perks, such as Adamant's +1 damage, since it can hold any enchantment. Design rule 5 says no gear is strictly best, so this needs checking against the others.

### Paying in ingots

- **Per enchantment or per enchanting?** One ingot per enchantment makes a four-enchantment Adamant pickaxe cost 4 Adamant ingots, which is 36 diamonds held in one tool.
- **Items with no ingot.** Vanilla's repair material is a natural rule: planks for wood and shields, cobblestone for stone, leather, diamonds, phantom membrane for Elytra, a breeze rod for the Mace, scutes for turtle shells and wolf armor. Bows, crossbows, tridents, fishing rods, shears, and flint and steel have no repair material, so each needs one.
- **Smithing upgrades.** Upgrading an enchanted diamond pickaxe to Netherite keeps its enchantments. If the grindstone returned the current material, enchanting in diamonds and grinding as Netherite would turn diamonds into Netherite. The item should record what was paid, so the grindstone returns exactly that.
- **Lapis loses its job.** Design rule 5 says every material has a use, so lapis needs a new one or stays as a second cost.

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

Vanilla's prior-work penalty limits stacking through repeated combining. Found books now do that job. With the penalty, repaired gear eventually hits "Too Expensive!" The penalty probably goes or changes for repairs, though re-enchanting a fresh tool is now an alternative. If XP is never spent, the anvil also needs a cost other than levels.

### Repairing with two tools

Repairing two copies of a tool in the anvil needs a rule for which enchantments survive.

### Still to place

- Where Mending, after its rework, and Zephyr are found
- How Electrum's one level above the Fortune and Looting caps works with found books
- Which structure holds each enchantment, and what grabbing each book wakes
