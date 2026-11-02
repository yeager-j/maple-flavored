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
  - **Ancient City:** the book floats in the middle of the city, in front of the portal. Grabbing it spawns a Warden, and you have to sneak around it to escape. Its [domain](boss-domains.md) stops you from flying away.
  - **Bastion:** the book floats in the treasure room. Grabbing it angers every Piglin in the structure. Good luck.

The Bastion's book is Fire Aspect. It has only 2 levels, so you don't need to clear many Bastions, which are very difficult structures. It's also a better enchantment than Fire Protection, the other contender, which would be too lame a prize for a structure that tough.

Vanilla Bastions come in four layouts, and only one has a treasure room, so each layout holds its own prize: Fire Aspect in the treasure room, Soul Speed on the bridge and Flame in the housing units. The hoglin stables still need theirs.

## Where books are found

The full list lives in the [Enchantment Sources](https://claude.ai/artifact/F42Mxw4d32XTQbe5wwaUWf) spreadsheet. Some placements carry a reason worth keeping:

- **Unbreaking is in Strongholds.** Vanilla's first ring has only 3, so Unbreaking III's fourth book means heading out to the second ring. It also gives Eyes of Ender a use after the End Portal is lit.
- **Fire Protection is in Nether Fortresses.** They're common and full of fire, which suits an enchantment that takes 8 books.
- **Fortune is in Woodland Mansions, on purpose.** Mansions are rare, but 26.3's Explorer Maps lead to structures, and Abandoned Camps, also new in 26.3, carry them as loot.
- **Specialized enchantments come early and general ones later.** Smite and Bane come from Dungeons, Blast Protection from Desert Pyramids and Projectile Protection from Pillager Outposts. Sharpness and Protection come later. Early gear has real tradeoffs, and a narrow enchantment is the best you can get for a while rather than a dud.

## Sources outside structures

A few enchantments come from elsewhere, each from the activity it belongs to:

- **Fishing** gives Luck of the Sea and Lure books. Every other enchanted book is removed from fishing loot.
- **Raids** give Mending. Librarians award a Mending book for defeating a level 5 Raid.
- **Librarians buy books instead of selling them.** That gives extra books a use once an enchantment is maxed out.
- **Ominous Trial Chamber vaults** give the Mace's enchantment books. Vaults already give each player their loot once.

Found enchanted gear is removed. Gear from mobs, chests, fishing and bartering is just normal gear.

Enchanted books are removed from all other loot tables, and their chest entries are replaced with diamonds so chests aren't entirely lame. Vault pools keep only the Mace's books. Bartering loses its Soul Speed books and iron boots without replacements, since the higher weights for the useful drops are good enough. Fishing's book entry becomes Luck of the Sea and Lure books.

Bartering stays a game of chance. It's technically a loot box, but most of its results are really useful, and the only real duds are things like water bottles and iron boots. Vanilla enchanting is different: Smite and Bane really are pointless, and nobody wants them.

## Books and the anvil

- **Every book found in the wild is level I.**
- **Two books of the same level combine into the next level**, following vanilla's anvil rules. A level III book takes four found books, so higher levels are a serious prize. Sharpness V takes 16, even alone, and that isn't too many: 8 books already make Sharpness IV, so 16 is the step from IV to V, not the unlock. It's a gradual climb in power that slows the game down on purpose.
- **The anvil combines books** to upgrade them, and still repairs tools and armor.
- **Tools can no longer be combined** to raise their enchantment levels. Found books are the only source of levels.

## Books are communal

A book is an item that fits in a Chiseled Bookshelf and powers a nearby enchanting table. The table can offer an enchantment shelved nearby, at that book's level, and the book isn't used up. Anyone can use the table, so a group shares one library.

Sharpness V still takes 16 found books, but you and your friends can combine your efforts. One library enchants everything in the base, and a broken tool loses only the ingot paid to enchant it, never the books.

## Level sets how many enchantments you can apply

Communal books mean everyone in a group can reach the same enchantments, so players need another way to stand apart. Your level sets how many enchantments an item can hold: 1 at level 30, and 1 more every 30 levels.

This isn't as brutal as it sounds. Keep Inventory is on by default, so you don't lose XP when you die.

A step of 30 levels is fine, because XP is plentiful: most of what you do earns it, and you no longer lose it on death. An XP farm can reach level 150 in a few hours. Vanilla's XP curve makes each slot cost much more than the last:

| Enchantments | Level | Total XP |
| --- | --- | --- |
| 1 | 30 | 1,395 |
| 2 | 60 | 8,670 |
| 3 | 90 | 24,045 |
| 4 | 120 | 47,520 |
| 5 | 150 | 79,095 |

The highest-level player becomes the group's enchanter, and maybe charges a fee. That's a fine way for players to stand apart, so the limit is only checked at the table.

Orichalcum tools and armor get 1 extra enchantment slot. This replaces Orichalcum's Better enchanting perk, as the Material roles ticket expected.

The extra slot doesn't make Orichalcum strictly best, because the other Alloys' perks compete with whole enchantments. At level 60, a pickaxe fits Efficiency and Fortune III. Electrum gives Fortune one level above the cap, while an Orichalcum pickaxe fits Unbreaking or Mending as well, and there's no correct answer. Raiding an End City at level 60, your armor has Protection and probably Unbreaking. A full Steel set makes you immune to Levitation, while Orichalcum adds a slot for something like Feather Falling. The choice is non-trivial and non-arbitrary.

## Paying for enchanting

Enchanting no longer costs XP.

- **One ingot of the item's material per session.** Open the table, give your pickaxe four enchantments, and pay one ingot. An iron pickaxe takes an iron ingot, and an Adamant pickaxe takes an Adamant ingot.
- **One lapis per enchantment.** Four enchantments use up 4 lapis.
- **The grindstone removes the enchantments and gives the ingot back.** The lapis isn't returned. So the ingot is a deposit stored in the item and only lost if the item breaks, while lapis is the real cost.

Items with no ingot get sensible defaults later. Vanilla's repair materials cover most of them: planks for wood and shields, cobblestone for stone, leather, diamonds, phantom membrane for Elytra, a breeze rod for the Mace, and scutes for turtle shells and wolf armor. Bows, crossbows, tridents, fishing rods, shears, and flint and steel have no repair material, so each needs one.

## Enchanted gear can't be upgraded

Smithing refuses enchanted gear. Otherwise, enchanting a diamond pickaxe with a diamond, upgrading it to Netherite and grinding it would turn a diamond into a Netherite ingot.

This sounds like a horrible idea until you remember that enchanting is now nearly free. Grind your diamond pickaxe to get the diamond back, upgrade it, then pick your enchantments again and pay with a Netherite ingot.

## The anvil keeps its XP costs

The anvil still costs XP, which can now also lower your enchantment slots. Combining books is a powerful move, so it should cost something that matters.

The anvil charges XP points, not levels. Higher levels hold more points, at 112 points per level at level 30 and 922 at level 120, so a cost in levels would charge the group's enchanter about 8 times as much as a newer player. Points make the same job cost everyone the same.

## The enchanting table

The table is no longer upgraded with XP, because level already gives XP its job. The table's job is to read the library: it offers any enchantment shelved nearby.

The 3-slot random offer goes. A level choice is dominant if it's deterministic, so the table always gives the highest level available.

The table works like a checklist. You insert a tool, and if it's already enchanted, its enchantments are pre-selected. You add or change which ones are applied, then click Save. Saving costs one Lapis per selected enchantment, so going from 4 to 3 costs 3. Lapis is plentiful, so this hardly matters. The first save on an unenchanted item also takes its ingot.

You can't save until the item fits your level. At level 60, a sword with 5 enchantments has to drop to 2 before you can save, so rearranging gear above your level means asking the enchanter. That's the price of using gear above your level: go get more XP. The gear still works if the enchanter is offline or quits, and the grindstone can always strip it. In single player, this never comes up.

Changing enchantments is cheap, so situational ones become worth having. You can consider Smite or Fire Protection if you know you're going somewhere they'll be useful, then switch back when you get home. That's planning for the next trip, which is the sandbox's game, not managing a loadout mid-fight.

## Open questions

These came up in discussion and are left for the Enchanting ticket.

### Plain bookshelves

Vanilla Chiseled Bookshelves don't power tables, so the library is a change. Plain bookshelves need a decision: whether they still do anything, or are only decoration.

### Combining in the anvil or on the shelf

Books could be combined in the anvil into one book per level, as above, or the table could count several level I books on its shelves. Combining keeps the library small, since a Chiseled Bookshelf holds 6 books. Counting avoids a merged book holding 16 finds' worth of work in one item.

### Who gets each book

Communal books suit one book per structure per world. Vanilla's Trial Chamber vaults give each player their loot once, but a per-player book would multiply finds on a server, which fights the shared library.

### Protecting the library

Shelved books are a base's most valuable items. Fire, creepers and griefers could take 16 structures' worth of work in one go. Books could resist fire and explosions, like Netherite, or drop when their shelf is destroyed.

### Clearing the structure first

Players may clear a Bastion before grabbing its book, so nothing is left to anger. That may be fine, since the fight still happens either way. A Blood Moon Restores Bastion mobs, but books never return. A Warden spawned on grab can't be avoided this way, though digging and pillaring may still cheese it.

### Diamonds from chests

Diamonds are non-renewable, which is what limits Crystal Hearts and Adamant. Chests don't refill, so diamonds stay non-renewable, but the count matters. Some chests weight enchanted books heavily, so swapping each entry one for one could add a lot of diamonds. The real weights need checking. The amount per entry should be set with the Structures ticket's Crystal Heart and Pith odds. Fishing must never give diamonds, since AFK fish farms would make them renewable.

### Bad enchantments become bad finds

Clearing a structure to find Bane of Arthropods brings the eye-rolling back. The checklist table helps a lot: cheap swaps make Smite before a Wither fight or Fire Protection before a Nether trip a real choice. That may be enough to keep them, and to keep vanilla's vocabulary. If some stay unwanted even then, merging has a gameplay reason: the elemental Protections into Protection, and Smite and Bane into Sharpness. Their niches could also move to Alloy traits.

### Early-game pacing

If Efficiency or Unbreaking needs an Ancient City, the early game gets worse than vanilla. The essentials need early structures such as mineshafts or ruined portals, or a small starter set the table offers from the start. Mending and Looting can sit late.

### The anvil's prior-work penalty

Vanilla's prior-work penalty limits stacking through repeated combining. Found books now do that job. With the penalty, repaired gear eventually hits "Too Expensive!" The penalty probably goes or changes for repairs, though re-enchanting a fresh tool is now an alternative.

### Repairing with two tools

Repairing two copies of a tool in the anvil needs a rule for which enchantments survive.

### Still to place

- Where Zephyr is found, and how the Mending rework changes the Raid reward
- How Electrum's one level above the Fortune and Looting caps works with found books
- Which structure holds each enchantment, and what grabbing each book wakes
