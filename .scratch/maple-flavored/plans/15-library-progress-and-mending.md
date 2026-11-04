# Plan: 15 Library Progress and Mending

## Context / end goal

Ticket 14 made the Library and the checklist table, and held Mending back entirely (`Library.offers()` strips it). This ticket gives the Library its long-term goal. The table shows how close its books are to a **Full Library** (all 42 other enchantments at max level), and offers Mending only while it is Full. That makes Mending the capstone the manifesto asks for: a live condition of the books, never recorded. Moving or lending the books moves access to Mending. Gear that already has Mending keeps it until it is next saved at a table without a Full Library.

The ticket is closed when:
- the table's screen shows a Library Progress bar fed by menu data;
- a Full table offers Mending, and stops offering it the moment a book leaves;
- players using a Full table earn "Well Read";
- GameTests pin the counting rule, the 42/130 roster, and Mending appearing and disappearing.

Ticket 16 (golden apples) unlocks effects at 15/30/50/75% Progress, so `Library.Progress` is built to be read there without rework.

Size: M. One ticket, built in order; no split.

## Decisions

1. **Roster = every registered enchantment except Mending.** It's derived from the registry, not a tag or a constant. Ticket 13 left exactly 43 registered (vanilla minus the curses, plus Zephyr and Blight), so the roster is 42 and the sum of their max levels is 130. Both numbers are asserted in a GameTest, which pins the spec's figures while the code keeps one authority (the registry).
2. **Library Progress** = Σ over roster enchantments of `min(highest shelved level, max level)`. Out of `fullLevels` = Σ roster max levels (130).
   - Duplicates count once: two level I books count 1, and I with II counts 2.
   - The cap keeps a creative over-max book from passing 100%. It also makes "Full" exactly `levels == fullLevels`, so there's one rule.
   - Mending books never count.
   - Progress is about the books, so it ignores the 15-bookshelf power count. Offers still need 15.
3. **Mending offer.** `Library.offers()` keeps stripping shelved Mending. When `progress().isFull()` (and ≥15 bookshelves, which `offers()` already requires), it adds Mending at its max level (I). A shelved Mending book changes nothing either way.
4. **Gear with Mending.** No new code: Mending on an inserted item is `CARRIED`. Without a Full Library its offer is 0, so `Checklist.problems` already yields `NotInLibrary(mending)` and Save is refused until it's unticked. That is spec line 335's "Mending included". With a Full Library it's offered at I, so it saves.
5. **Library stores the registry and derives everything else.** It becomes `Library(Registry<Enchantment> enchantments, int bookshelves, Map<…> highestLevels)`. `progress()` and `offers()` are computed on demand, so no derived field can drift. Iterating 43 entries per tick is trivial.
6. **Menu data.** Add one `DataSlot` holding `progress().levels()` (≤130, fits 16 bits), written by the same `Library.read` that refreshes the offers (renamed `refreshLibrary`). `EnchantingTableMenu.progress()` returns `new Library.Progress(syncedLevels, Library.fullLevels(enchantments))` on both sides. The client computes `fullLevels` from its synced registry, so only one int crosses the wire.
7. **Advancement "Well Read"** (Jackson's choice): a `PlayerTrigger` `maple_flavored:full_library`, fired from the menu's server-side refresh whenever the table's Library is Full and the menu's player is a `ServerPlayer`. It fires on open, and each tick while open, so the last book shelved with the screen open also counts. Once granted, the listener is gone, so repeat calls cost nothing. The menu keeps `inventory.player`, as vanilla's `ItemCombinerMenu` does.
   - Every group member who uses a Full table earns it.
   - It ignores the 15-bookshelf count, like Progress.
   - `data/maple_flavored/advancement/story/full_library.json`: parent `minecraft:story/enchant_item`, frame `goal`, icon `minecraft:chiseled_bookshelf`, no reward.
   - Title "Well Read". Description "Shelve every enchantment but Mending at its max level".
8. **Progress strip with checkpoints (client).** Jackson asked for room to mark rewards, so the panel grows 20 px (206 → 226) and the menu's inventory slots move down to y 144. A full-width strip sits between the list and the "Inventory" label: a 162×5 track (160 px fill, about 1.2 px per level) at y 123, and 8 px reward icons at y 113, leaving 3 px under the list, 2 px between icons and bar and 4 px above the label. The track and lapis-blue fill are original art drawn with Pillow into the atlas, with the fill sprite at v=226. No Mojang pixels are copied, and the XP bar's green is avoided.
   - **`LibraryCheckpoint(percent, icon, reward)`** in `mapleflavored.enchanting`, with `ALL` in rising order. Today it holds only `MENDING` at 100%. Ticket 16 adds the four apple effects at 15/30/50/75%.
   - **One rule:** `Progress.levelsFor(percent)` rounds up (15% of 130 needs 20), and `Progress.reached(checkpoint)` compares against it. `Library.offers()` gates Mending with `reached(LibraryCheckpoint.MENDING)`, and the screen uses the same list, so a threshold can't differ between server and screen.
   - **Drawing:** each checkpoint below 100% gets a 1 px notch, dark blue once reached and grey before. The reward's icon is drawn at half scale over its notch, clamped to the bar. Unreached icons are shaded after a `nextStratum()`, because items draw above everything else in their stratum.
   - **Tooltips:** an icon shows its reward, then "Unlocked" or "Needs n% Library Progress (m levels)". The bar shows "Library Progress: 87/130", plus "Full Library" when Full. It doesn't claim Mending is offered: a Full Library below 15 bookshelves offers nothing.

## API sketch

```java
// mapleflavored.enchanting.Library
public record Library(Registry<Enchantment> enchantments, int bookshelves, Map<Holder<Enchantment>, Integer> highestLevels) {
	public static Library read(Level level, BlockPos tablePos);            // unchanged signature
	public static boolean holdsShelf(Level level, BlockPos shelfPos);       // unchanged
	/** Max levels summed over every enchantment but Mending: 130. */
	public static int fullLevels(Registry<Enchantment> enchantments);
	public Progress progress();
	/** Each shelved enchantment at its highest level, plus Mending at max while Full; empty below 15 shelves. */
	public Map<Holder<Enchantment>, Integer> offers();

	/** Library Progress: shelved levels toward a Full Library, each enchantment counted up to its max level. */
	public record Progress(int levels, int fullLevels) {
		public boolean isFull();     // levels >= fullLevels
		public float fraction();     // clamped 0..1; ticket 16 compares against 0.15 etc.
	}
}

// EnchantingTableMenu
public Library.Progress progress();   // synced; screen draws it, GameTests assert it

// MapleTriggers
public static final PlayerTrigger FULL_LIBRARY = register("full_library", new PlayerTrigger());
```

Consumers:
- `EnchantingTableMenu`: reads the Library once per refresh, writes offers and the progress slot, fires `FULL_LIBRARY`.
- `EnchantingTableScreen`: `menu.progress()` for the bar and its tooltip.
- GameTests: `menu.progress()`, `checklist().offers()`.
- Ticket 16 later: `progress().fraction()`.

## Files

- `src/main/java/mapleflavored/enchanting/Library.java`: registry field, `Progress`, `fullLevels`, `isFull`, Mending in `offers`. Update the class doc, and the `offers` doc that says "Mending is never offered yet".
- `src/main/java/mapleflavored/enchanting/EnchantingTableMenu.java`: player field, progress `DataSlot`, `refreshOffers` → `refreshLibrary` (offers + progress + trigger), `progress()`.
- `src/main/java/mapleflavored/advancements/MapleTriggers.java`: `FULL_LIBRARY`.
- `src/client/java/mapleflavored/client/enchanting/EnchantingTableScreen.java`: bar and tooltip.
- `src/main/resources/assets/maple_flavored/textures/gui/container/enchanting_table.png`: bar track and fill sprites.
- `src/main/resources/assets/maple_flavored/lang/en_us.json`: advancement title and description, `container.maple_flavored.enchanting_table.progress` ("Library Progress: %s/%s"), `…full_library` ("Full Library").
- `src/main/resources/data/maple_flavored/advancement/story/full_library.json`.
- `src/gametest/java/mapleflavored/gametest/EnchantingTableGameTests.java`: new cases plus a `fullLibrary(helper)` book-list helper. The old "offers (never Mending)" assertion stays valid, since that Library isn't Full.
- `.scratch/maple-flavored/issues/15-…md`: tick the boxes, except the hand check until it's done.
- `GLOSSARY.md` already defines Full Library and Library Progress, so no change. `CREDITS.md`: nothing reused.

## Steps

1. `Library` changes and `MapleTriggers.FULL_LIBRARY`, plus the advancement JSON and lang.
2. Menu: data slot, `progress()`, trigger.
3. GameTests (below). Run the enchanting tests, then the full suite.
4. Texture and screen bar. Run the client for the hand check.
5. Full `gametest` suite, LARP review, then the PR (branch `15-library-progress-and-mending`).

## Acceptance matrix (GameTests, in `EnchantingTableGameTests`)

`fullLibrary(helper)` builds 42 books, one per roster enchantment at max level, split into 7 lists of 6 for `placeTable`'s lower ring. The upper ring adds 16 plain shelves, for 23 in all.

| Case | Expected |
| --- | --- |
| Roster | registry minus Mending has 42 entries; `Library.fullLevels` = 130 |
| Two Sharpness I books | `progress().levels()` = 1 |
| Sharpness I + Sharpness II | levels = 2 |
| Sharpness I + Smite III + a Mending book | levels = 4 (Mending doesn't count) |
| Over-max Sharpness VII book (by command) | counts 5 |
| `fullLibrary` | levels = 130, `isFull`; offers include Mending at 1 and every roster enchantment at max |
| Full Library with Blight at I instead of II | levels 129, not Full, no Mending (Blight counts) |
| Full Library without the Zephyr book | not Full, no Mending (Zephyr counts) |
| Full, then the Zephyr book removed from its shelf with the menu open, `broadcastChanges` | Mending offer gone; put back → offered again (access follows the books) |
| Full books in 7 Chiseled shelves only, no plain shelves (7 < 15) | progress 130 and Full, but nothing offered, no Mending; a fresh player opening the table through `useBlock` is still granted "Well Read" (the trigger isn't gated on offers) |
| Full: plain diamond sword, tick Mending, Save | sword has Mending I |
| Sword with Mending (by command), Library not Full | Mending row `CARRIED`, Save refused with `NotInLibrary(mending)` and unchanged; untick Mending, tick Sharpness → saves, Mending gone |
| Same sword at a Full Library | pre-selected Mending saves and stays |
| Player opens a menu on a Full table | granted `maple_flavored:story/full_library`; a menu on a not-Full table doesn't grant it; the advancement's parent is `minecraft:story/enchant_item` and its frame is `goal` |
| Not Full at open, last book shelved, `broadcastChanges` | granted |

Existing tests keep passing. `theLibraryOffersTheHighestShelvedLevel` still expects no Mending, since that Library is far from Full.

## Invariants

- Nothing about Progress or Fullness is stored. Each is derived from a fresh `Library.read`, and the client only displays the synced levels.
- Mending is offered iff the Library is Full and has ≥15 bookshelves, never because a Mending book is shelved.
- `progress().levels() ≤ fullLevels`, and `isFull() ⇔ levels == fullLevels`.
- Save's rule is unchanged: Mending on gear is judged like any other carried enchantment.

## Hand checks

- The strip at 0%, partial and Full (both a powered table and a 7-shelf one): notches, the half-scale Mending icon, its shade before Full, both tooltips, spacing against the list and the "Inventory" label, and the moved inventory slots; the bar updating live when a second player shelves or removes a book.
- Mending appearing in the list at a Full table.

## Exclusions

- Golden apple effects and their thresholds (16).
- A special "needs a Full Library" problem message for Mending. The generic "Mending isn't in this Library" plus the bar tooltip cover it.
- Firing the advancement from the shelf mixin (Jackson chose table use).
- Electrum and other enchantments added by later tickets. They join the roster automatically, and the 42/130 test will flag the change.
