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
- **One book per structure.** Some common structures can hold one of several enchantments, such as Pillager Outposts, Ocean Ruins, Dungeons and Buried Treasure. These have trash loot in vanilla. Each one is given a single book from its list, and you're guaranteed to get it.
- **Some books wake the structure.** Each book is a floating book in the structure's most important spot. Not every structure needs to wake: for some, the challenge is finding them. Where grabbing the book does wake the structure, it triggers that structure's own danger, turned up. In Strongholds and Woodland Mansions, it angers every Illager. The Ancient City is the first real event, and the system can grow from there. Two examples:
  - **Ancient City:** the book floats in the middle of the city, in front of the portal. Grabbing it spawns a Warden, and you have to sneak around it to escape. Its [domain](boss-domains.md) stops you from flying away.
  - **Bastion:** the book floats in the treasure room. Grabbing it angers every Piglin in the structure. Good luck.

The Bastion's book is Fire Aspect. It has only 2 levels, so you don't need to clear many Bastions, which are very difficult structures. It's also a better enchantment than Fire Protection, the other contender, which would be too lame a prize for a structure that tough.

Vanilla Bastions come in four layouts, and only one has a treasure room, so each layout holds its own prize: Fire Aspect in the treasure room, Soul Speed on the bridge, Flame in the housing units and Thorns in the hoglin stables.

## Where books are found

The full list lives in the [Enchantment Sources](https://claude.ai/artifact/F42Mxw4d32XTQbe5wwaUWf) spreadsheet. Some placements carry a reason worth keeping:

- **Unbreaking is in Strongholds.** Vanilla's first ring has only 3, so Unbreaking III's fourth book means heading out to the second ring. It also gives Eyes of Ender a use after the End Portal is lit.
- **Feather Falling is in End City buildings.**
- **Zephyr drops from the Ender Dragon.** You use it until you find an Elytra, so it makes exploring the End easier. Putting it on the End Ship would hand out two movement upgrades at once. Every kill drops a book, since Zephyr III takes 4 and respawning the Dragon costs End Crystals.

Zephyr goes on boots and gives a double jump, like Torrent's in Elden Ring. Each level makes the second jump higher. As in Elden Ring, fall damage counts the whole fall, from where it started to where you next touch the ground, so the double jump does nothing to prevent it. Vanilla only adds to fall distance while you move down, so the drop after the second jump would count on top: the height the jump regains should come off again, so the double jump is neutral. The double jump also "clutches": it cuts the distance you've fallen so far by 10% at level I, 15% at II and 25% at III. Only the distance before the jump is cut, so the later you jump, the more you save, and jumping just before landing cuts nearly the whole fall. That makes it a skill with no timing window to check. It stays well below Feather Falling IV's 48%. Matcha's version also gives a higher sneak jump and slow falling, which makes Feather Falling obsolete, so Maple leaves those out.

Withered is a new enchantment, taken from Matcha Flavoured with credit. It goes on the same weapons as Fire Aspect, can't be combined with it, and has 2 levels. Hitting a mob gives it the Wither effect. The Wither drops its books, one per kill, so the two bosses each drop their own enchantment. Fire Aspect cooks food drops, while Withered still works on fire-immune mobs, which makes it the better choice in the Nether, though not against Wither Skeletons or the Wither.

The curses are removed. Found gear is plain, so they'd have no source, and they don't have a use anyway.
- **Fire Protection is in Nether Fortresses.** They're common and full of fire, which suits an enchantment that takes 8 books.
- **Fortune is in Woodland Mansions, on purpose.** Mansions are rare, but 26.3's Explorer Maps lead to structures, and Abandoned Camps, also new in 26.3, carry them as loot.
- **Specialized enchantments come early and general ones later.** Smite and Bane come from Dungeons, Blast Protection from Desert Pyramids and Projectile Protection from Pillager Outposts. Sharpness and Protection come later. Early gear has real tradeoffs, and a narrow enchantment is the best you can get for a while rather than a dud.

## Mending is the library's capstone

The table offers Mending only while its library is fully maxed: every other enchantment shelved at its max level. Mending isn't found anywhere else.

Mending invalidates a lot of gameplay. Unbreaking becomes obsolete, diamonds stop mattering, and Bastions stop being worth raiding. So you should be entirely finished with your save before you get it: "congrats, you 100%'d Minecraft." That's the Mending rework the Enchanting ticket asked for.

It's a live condition of the library, not a one-time reward. Nothing is recorded, so there's nothing to farm or duplicate, and moving the books moves Mending with them. Lending your books lends your library, which fits communal books. Mending already on gear stays if the library later shrinks. The table shows a progress bar towards it.

A full library is about 250 found books, including 16 each for the level V enchantments, 4 Ender Dragon kills for Zephyr and 4 Woodland Mansions for Fortune. The 42 maxed books fit exactly in 7 Chiseled Bookshelves.

## Sources outside structures

A few enchantments come from elsewhere, each from the activity it belongs to:

- **Fishing** gives Luck of the Sea and Lure books. Every other enchanted book is removed from fishing loot.
- **Librarians buy books instead of selling them.** That gives extra books a use once an enchantment is maxed out.
- **Ominous Trial Chamber vaults** give the Mace's enchantment books. Vaults already give each player their loot once.
- **Illagers** drop some books as chance drops, such as Sharpness from Vindicators and Protection from Evokers. Strongholds are filled with Illagers, which gives these books a home outside raids and gives empty Strongholds a purpose. Illagers spawned by a raid don't drop books, except a few that are raid-only, like Knockback from Ravagers. Pillager Captains drop books too: they come from outposts and patrols, which are hard to farm and dangerous. Evokers can have several drops, since they're incredibly tough.
- **Grabbing a Stronghold's book alerts every Illager nearby.** It turns them hostile even to Orichalcum, and Evokers attack through walls, so it's super nasty.
- **Reaching the End Portal is a challenge, finally.** Every player has to get through a Stronghold full of Illagers. Blood Moons Restore Stronghold and mansion Illagers, so their books stay slowly renewable without a farm.

Found enchanted gear is removed. Gear from mobs, chests, fishing and bartering is just normal gear.

Enchanted books are removed from all other loot tables, and each chest entry is replaced with a single diamond. It isn't very impactful, but it keeps chests from being lame. Vault pools keep only the Mace's books. Bartering loses its Soul Speed books and iron boots without replacements, since the higher weights for the useful drops are good enough. Fishing's book entry becomes Luck of the Sea and Lure books.

Bartering stays a game of chance. It's technically a loot box, but most of its results are really useful, and the only real duds are things like water bottles and iron boots. Vanilla enchanting is different: Smite and Bane really are pointless, and nobody wants them.

## Books and the anvil

- **Every book found in the wild is level I.**
- **Two books of the same level combine into the next level**, following vanilla's anvil rules. A level III book takes four found books, so higher levels are a serious prize. Sharpness V takes 16, even alone, and that isn't too many: 8 books already make Sharpness IV, so 16 is the step from IV to V, not the unlock. It's a gradual climb in power that slows the game down on purpose.
- **The anvil combines books** to upgrade them, and still repairs tools and armor.
- **Tools can no longer be combined** to raise their enchantment levels. Found books are the only source of levels.

## Books are communal

A book is an item that fits in a Chiseled Bookshelf and powers a nearby enchanting table. The table can offer an enchantment shelved nearby, at that book's level, and the book isn't used up. Anyone can use the table, so a group shares one library.

The table still needs 15 bookshelves around it, as in vanilla, and any of them can be a Chiseled Bookshelf. That's room for 90 books. Books are combined in the anvil, one book per enchantment, so a complete library of 42 enchantments fits in 7 Chiseled Bookshelves. Without combining, it would take about 250 found books, far more than the shelves hold.

Books resist fire and explosions, like Netherite. Griefers aren't worth protecting against: if they can't burn a book, they'll just steal it.

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

The extra slot doesn't make Orichalcum strictly best, because the other Alloys' perks compete with whole enchantments. At level 60, a pickaxe fits Efficiency and Fortune III. Electrum gives Fortune one level above the cap, while an Orichalcum pickaxe fits Unbreaking as well, and there's no correct answer. Raiding an End City at level 60, your armor has Protection and probably Unbreaking. A full Steel set makes you immune to Levitation, while Orichalcum adds a slot for something like Feather Falling. The choice is non-trivial and non-arbitrary.

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

There's no prior-work penalty. In vanilla it stopped players stacking enchantments by combining items over and over, and found books and level slots do that job now. All it would still do is block repairs after about five, and grinding, repairing and re-enchanting gets around that, so it would only add steps. Anvil costs are flat.

## Repairing

Two tools can no longer be combined to repair them. It doesn't make sense: are you duct-taping them together? Items are repaired with their material instead.

- **An item takes one less unit than its recipe to repair fully, and at least 1.** A pickaxe takes 3 to craft, so 2 repair it, each restoring 50%. A shovel or sword takes 1.
- **One Netherite or Alloy ingot repairs any item fully.** Alloy gear is repaired with its own Alloy, not a plain Netherite Ingot. That keeps Steel's durability important, and keeps raiding Bastions for Ancient Embers worth doing.

Repairing is always a discount on crafting a new item: it's that or craft a new one. The discount is smallest on big recipes, 1 unit in 8 for a chestplate, and largest on tools, 1 in 3 for a pickaxe and 1 in 2 for a sword.

| Item | Recipe | Units to repair fully |
| --- | --- | --- |
| Pickaxe, axe | 3 | 2 |
| Sword, hoe | 2 | 1 |
| Shovel | 1 | 1 |
| Helmet | 5 | 4 |
| Chestplate | 8 | 7 |
| Leggings | 7 | 6 |
| Boots | 4 | 3 |

## The enchanting table

The table is no longer upgraded with XP, because level already gives XP its job. The table's job is to read the library: it offers any enchantment shelved nearby.

The 3-slot random offer goes. A level choice is dominant if it's deterministic, so the table always gives the highest level available.

The table works like a checklist. You insert a tool, and if it's already enchanted, its enchantments are pre-selected. You add or change which ones are applied, then click Save. Saving costs one Lapis per selected enchantment, so going from 4 to 3 costs 3. Lapis is plentiful, so this hardly matters. The first save on an unenchanted item also takes its ingot.

You can't save until the item fits your level. At level 60, a sword with 5 enchantments has to drop to 2 before you can save, so rearranging gear above your level means asking the enchanter. That's the price of using gear above your level: go get more XP. The gear still works if the enchanter is offline or quits, and the grindstone can always strip it. In single player, this never comes up.

Changing enchantments is cheap, so situational ones become worth having. You can consider Smite or Fire Protection if you know you're going somewhere they'll be useful, then switch back when you get home. That's planning for the next trip, which is the sandbox's game, not managing a loadout mid-fight.

## Open questions

These came up in discussion and are left for the Enchanting ticket.

### Who gets each book

Communal books suit one book per structure per world. Vanilla's Trial Chamber vaults give each player their loot once, but a per-player book would multiply finds on a server, which fights the shared library.

### Clearing the structure first

Players may clear a Bastion before grabbing its book, so nothing is left to anger. That may be fine, since the fight still happens either way. A Blood Moon Restores Bastion mobs, but books never return. A Warden spawned on grab can't be avoided this way, though digging and pillaring may still cheese it.

### Library rewards

The bar's earlier tiers are open. They could be perks the table gives while its library is big enough, following the same live-condition rule as Mending, or the bar could simply show progress towards Mending.

### Bad enchantments become bad finds

Clearing a structure to find Bane of Arthropods brings the eye-rolling back. The checklist table helps a lot: cheap swaps make Smite before a Wither fight or Fire Protection before a Nether trip a real choice. That may be enough to keep them, and to keep vanilla's vocabulary. If some stay unwanted even then, merging has a gameplay reason: the elemental Protections into Protection, and Smite and Bane into Sharpness. Their niches could also move to Alloy traits.

### Early-game pacing

If Efficiency or Unbreaking needs an Ancient City, the early game gets worse than vanilla. The essentials need early structures such as mineshafts or ruined portals, or a small starter set the table offers from the start. Looting can sit late.

### Repair materials

Items with no simple recipe need a repair material: Elytra, tridents, the Mace, shields, bows, crossbows, fishing rods, shears, and flint and steel. Sensible defaults can come later, alongside the enchanting materials.

### Withered against players

The Wither effect causes Withered Hearts on players, which stay until they return to the Overworld. A Withered weapon in PvP would wither another player's hearts. That may be intended, but it's a lot stronger than Fire Aspect against players. The name also sits close to Withered Heart in the glossary, so the enchantment may need a distinct name.

### Still to place

- What the level 5 Raid rewards now that Mending comes from the library
- How Electrum's one level above the Fortune and Looting caps works with found books
- Which other structures wake when their book is taken, as the system grows
