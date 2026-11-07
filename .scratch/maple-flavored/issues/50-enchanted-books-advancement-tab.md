# 50: Enchanted Books advancement tab

**What to build:** A new advancement tab with one advancement per enchantment, each telling the player where its book is found. It's laid out by source, so it unrolls as the player explores, and it takes over the Library's first step and its capstone.

**Blocked by:** 14 (Library and checklist table), 15 (Library Progress and Mending), 18 (Structure book framework)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder, user stories 205, 206 and 211c–211g, and the Enchanted Books bullets under Milestones and advancements.

- [ ] A `maple_flavored:enchanted_books/` tab whose root is Shelf Life (shelve an enchanted book in a Library), moved from Story, with its own background
- [ ] Well Read (Full Library) moves to the tab as a goal directly under the root
- [ ] Enchanter goes back under Diamonds! in Story; the enchanted golden apple and Zombie Doctor stay where they are
- [ ] One task per enchantment the Library counts, Mending excluded (42, Zephyr and Blight included), earned by `inventory_changed` on an enchanted book storing that enchantment at any level
- [ ] Each book advancement shows a toast, doesn't announce in chat, isn't hidden, gives no reward, uses an enchanted book icon and the enchantment's name as its title
- [ ] Descriptions name places and mobs, never chances, and cover every source of that book (Bane of Arthropods names both spider Dungeons and Mineshaft cave spider spawners)
- [ ] Branches hang from the root as chains ordered roughly by progression: early Overworld structures; Illagers and their strongholds; the Nether; the depths; fishing; bosses and the End. Several enchantments from one source sit side by side under the same parent
- [ ] The advancements come from datagen, driven by the same source table as the structure books and loot data, or a GameTest proves every Library enchantment has exactly one advancement whose description matches its sources
- [ ] GameTests cover: getting a book grants its advancement, a combined or handed-over book counts, a plain book and a Mending book grant nothing, and no book advancement announces in chat

## Notes

- `MapleDataGenerator` (client datagen) already exists. The existing `story/shelve_enchanted_book.json` and `story/full_library.json` move to the new folder, and their lang keys move with them. Their ids change; the mod is for new worlds, so no migration.
- The book sources live in the spec's "Source list" (structure books) and the Mobs and Loot bullets (Illager drops, Dragon and Wither drops, fishing and ominous vaults). Read them from code where they already exist rather than copying them into the generator.
- Vanilla reveals a locked advancement when its parent or grandparent is earned, and shows any advancement once one of its descendants is earned. A book picked up before the first shelving therefore still shows the tab. Check this on the 26.3 jar before relying on it for the layout.
- A tab root needs a `background` texture. A bookshelf or chiseled bookshelf texture fits.
