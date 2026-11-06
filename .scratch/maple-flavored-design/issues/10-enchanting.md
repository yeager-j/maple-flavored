# Enchanting

Type: grilling
Status: resolved
Blocked by: 06

## Question

What do we change about enchanting, which new enchantments do we include, starting with Zephyr, and how do they get obtained?

Mending needs a rework because it's too strong. Steel's durability bonus depends on the outcome, so check how much durability matters afterwards. Electrum goes one level above the Fortune and Looting caps.

Orichalcum gear has enchantability 30 and only gets offered the highest level its power allows. Unwanted enchantments such as Knockback on a sword are still open for the whole table.

## Answer

Enchanting stops being a loot box. Enchantments are found in the world as books and shared through a Library, and players stand apart by level. The reasons are in [the enchanting manifesto](../../../docs/manifestos/enchanting.md), and terms are in `GLOSSARY.md`. Numbers are starting values that balance work can change. The rules are fixed.

### Finding books

- Every book found is level I. Two books of a level combine in the anvil into the next level, so a level n book takes 2^(n-1) finds.
- Most books float in a fixed spot in a structure: one book per structure, per world, guaranteed. A structure with several possible enchantments is given one when it generates.
- Some books wake their structure when taken. A Bastion's angers every Piglin. Taking a Stronghold's or Woodland Mansion's alerts every Illager nearby and turns them hostile, even to Orichalcum. Other structures don't wake; finding them is the challenge.
- The Ancient City's book wakes nothing, since a Warden should come only from poor play. It's locked until three keeper shriekers in a ring near the city's edge are broken, and players inside the city are Grounded until it's taken, so it can't be flown in and out for.
- Strongholds are filled with Illagers. Blood Moons Restore them, so reaching the End Portal is a fight.
- Some books are chance drops. Illagers drop some, including from Strongholds, and Evokers can drop several. Illagers spawned by a raid don't, except raid-only drops such as Knockback from Ravagers.
- The Ender Dragon drops a Zephyr book and the Wither a Blight book, every kill.
- Fishing gives Luck of the Sea and Lure books. Ominous vaults give the Mace's books.
- Enchanted books are removed from every other loot table, and each chest entry becomes 1 diamond. Bartering loses Soul Speed books and iron boots with no replacement. Found gear is never enchanted.
- Librarians buy books and no longer sell them. Raids are otherwise unchanged.

| Enchantment | Max | Source |
| --- | --- | --- |
| Aqua Affinity | I | Buried Treasure |
| Bane of Arthropods | V | Dungeons |
| Blast Protection | IV | Desert Pyramid |
| Blight | II | Wither |
| Breach | IV | Ominous vault |
| Channeling | I | Ocean Ruins |
| Density | V | Ominous vault |
| Depth Strider | III | Shipwreck |
| Efficiency | V | Mineshaft |
| Feather Falling | IV | End City |
| Fire Aspect | II | Bastion, treasure room |
| Fire Protection | IV | Nether Fortress |
| Flame | I | Bastion, housing units |
| Fortune | III | Woodland Mansion |
| Frost Walker | II | Igloo |
| Impaling | V | Ocean Ruins |
| Infinity | I | Pillager Captain (rare) |
| Knockback | II | Ravager (uncommon) |
| Looting | III | Jungle Temple |
| Loyalty | III | Ocean Ruins |
| Luck of the Sea | III | Fishing |
| Lunge | III | Abandoned Camp |
| Lure | III | Fishing |
| Mending | I | Full Library |
| Multishot | I | Pillager Outpost |
| Piercing | IV | Pillager Outpost |
| Power | V | Pillager Captain (uncommon) |
| Projectile Protection | IV | Pillager Outpost |
| Protection | IV | Evoker |
| Punch | II | Pillager Captain (rare) |
| Quick Charge | III | Pillager Outpost |
| Respiration | III | Buried Treasure |
| Riptide | III | Ocean Ruins |
| Sharpness | V | Vindicator (uncommon) |
| Silk Touch | I | Trail Ruins |
| Smite | V | Dungeons |
| Soul Speed | III | Bastion, bridge |
| Sweeping Edge | III | Vindicator (rare) |
| Swift Sneak | III | Ancient City |
| Thorns | III | Bastion, hoglin stables |
| Unbreaking | III | Stronghold |
| Wind Burst | III | Ominous vault |
| Zephyr | III | Ender Dragon |

### The Library and the table

- A book is shelved in a Chiseled Bookshelf and isn't used up. The table offers every enchantment shelved nearby, always at its highest level. The 3-slot random offer is gone.
- The table still needs 15 bookshelves, any of which can be Chiseled. Books are combined to one per enchantment, so a Full Library of 42 books fits in 7 Chiseled Bookshelves.
- Mending is offered only while the table has a Full Library. It's a live condition: moving the books moves Mending, and Mending already on gear stays. The table shows a progress bar towards it.
- The table is a checklist. Insert an item, and its enchantments are pre-selected. Change them and Save. Saving costs 1 lapis per selected enchantment, and the first save on an unenchanted item also takes 1 unit of its material, such as an iron ingot for an iron pickaxe or an Adamant ingot for an Adamant one.
- The grindstone removes all enchantments and returns that unit, but not the lapis.
- Saving needs the item to fit the saving player's Enchantment Slots: 1 at level 30 and 1 more every 30 levels. Gold and Electrum tools and armor get 1 extra. The limit is checked only at the table.
- Enchanted gear can't be smithed. Grind it, upgrade it and enchant it again.
- Books resist fire and explosions, like Netherite.

### The anvil

- The anvil combines books and repairs items. It charges XP points, not levels, at a flat cost with no prior-work penalty.
- Tools can't be combined with each other, either to add enchantments or to repair.
- Repairing fully takes one less unit of material than the recipe, at least 1, each unit restoring an equal share. One Netherite or Alloy ingot repairs any item fully, and Alloy gear takes its own Alloy.

### New, changed and removed enchantments

- **Zephyr** (boots, III) gives a double jump. Each level jumps higher. Fall distance isn't reset: the height the jump regains is taken off again, so the jump itself is neutral. It also Clutches, cutting the distance fallen before the jump by 10%, 15% or 25%.
- **Blight** (Fire Aspect's weapons, II, incompatible with it) gives Wither I for 4 seconds per level, about half Fire Aspect's damage.
- **Orichalcum** tools add a flat 50% to drops that Fortune or Looting affect, added rather than multiplied, skipping Ancient Embers, Pith and enchanted books. This replaces Electrum going one level above the cap.
- Curse of Binding and Curse of Vanishing are removed.

### Left open

- Rewards on the Library progress bar before Mending: the enchanted golden apple's effects (see [Brewing](11-brewing.md))
- Enchanting and repair materials for items with no ingot or simple recipe, such as Elytra, tridents, the Mace and bows: see [Trading, combat and recipes](16-trading-combat-and-recipes.md)
- Which other structures wake when their book is taken: none, and the Nether Fortress's book is locked instead (see [Structures](14-structures.md))

## Comments

Read [the enchanting manifesto](../../../docs/manifestos/enchanting.md) first. It sets the direction: enchantments are found as level I books in fixed spots in structures, grabbing a book wakes that structure's danger, books combine in the anvil, and tools no longer combine. Its open questions are this ticket's starting agenda, especially how many books it takes and what the XP-upgraded table does.

Zephyr goes on boots. Matcha's version is probably too strong: holding sneak on the ground and jumping goes much higher, holding sneak in the air gives slow falling, and charging sneak before a jump allows a double jump. Slow falling makes Feather Falling obsolete. Maple's Zephyr is just a double jump, like Torrent's in Elden Ring. Each level makes the second jump higher, and every Ender Dragon kill drops a Zephyr book. The second jump doesn't reset fall distance; fall damage counts the whole fall, as in Elden Ring. The double jump clutches, cutting the distance fallen so far by 10%, 15% and 25% at levels I to III, so jumping later saves more.

From [Early-game tiers](07-early-game-tiers.md): the extra Enchantment Slot moved from Orichalcum to Electrum, and the drop bonus from Electrum to Orichalcum. Gold gear also gets the extra slot, carrying vanilla's high enchantability into the new system. The manifesto's Electrum-versus-Orichalcum pickaxe example still holds with the names swapped.

From [Getting villagers and removing villages](08-villagers-and-villages.md):

- The Library progress bar now has a defined measure, Library Progress: the sum of the highest level shelved for each enchantment, over a Full Library's 130. Its first reward before Mending is the enchanted golden apple's Regeneration II at 15%, with more apple effects to follow (see Brewing). The table also takes golden apples, with a block of gold as their material unit.

From [The role of XP](12-role-of-xp.md): repairs cost no XP, so the anvil charges points only for combining books (50 × the resulting level) and renaming (10). The anvil can't apply books to gear, the grindstone gives no XP, and the crafting grid and grindstone can't combine damaged items.

Jackson's change on 2026-11-05: taking the Ancient City's book no longer summons an angry Warden. Summoning one punished the correct play of finding and taking the book, while the Warden is meant to punish being loud in the Deep Dark. The lock follows the Fortress's and the Bastion's, and the city's Grounded stops an Elytra grab once the Warden no longer guards the book. Keepers are chosen from the city's sculk patch origins, which are fixed when it's planned. Its shriekers aren't, since none are in the templates (see `docs/research/vanilla-ancient-city-sculk.md`). The full rule is in the spec and ticket 28.

From [Advancements](17-advancements.md): a golden apple saved at the table with effects becomes the vanilla `enchanted_golden_apple` item, and the grindstone turns it back into a `golden_apple`, so A Balanced Diet still works. The reworked table must still fire vanilla's `enchanted_item` trigger for Enchanter.
