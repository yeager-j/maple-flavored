# Plan: 14 Library and checklist table

## Context / end goal

Enchanting stops being a loot box. Ticket 11 shut every other route to enchanted gear: the anvil only combines books, and nothing merges items. This ticket opens the only route left. Books a group shelves in Chiseled Bookshelves around an enchanting table become a shared, reusable Library. The table turns into a predictable checklist: you see exactly what the Library offers, at its highest level, and you pick. Level stops buying random rolls and sets how many enchantments a player can save (Enchantment Slots), so XP keeps a use. The costs are lapis plus a one-time material deposit that the grindstone hands back. Found books also stop being easy to lose, since they resist fire and explosions.

The ticket is closed when a survival player can build a 15-shelf table, shelve books (and get the new advancement), insert gear, tick enchantments, and Save under the spec's rules. A refused save must change nothing. The grindstone must return the deposit. GameTests must drive all of it through the real menu.

Ticket 15 (Library Progress, Mending) and ticket 16 (golden apples) build on the `Library` and the menu built here, so both are shaped to take those additions without rework.

Size: one ticket, about XL. Its parts all share the menu and the Library, so it isn't worth splitting into separate tickets. It's built in the order below. Book fire resistance is the only independent piece, and it's tiny.

## Decisions

1. **Library = the table's 32 vanilla offsets, read live.** Add `minecraft:chiseled_bookshelf` to `data/minecraft/tags/block/enchantment_power_provider.json` (`replace: false`). The table then counts Chiseled Bookshelves toward its power, gets their particles, and they go through vanilla's `isValidBookShelf` transmitter check.
   - `Library.read(level, tablePos)` counts valid shelves among `EnchantingTableBlock.BOOKSHELF_OFFSETS`. For each valid offset holding a `ChiseledBookShelfBlockEntity`, it reads every enchanted book's `STORED_ENCHANTMENTS` and keeps the highest level per enchantment.
   - Offers: none if there are fewer than 15 valid shelves of either kind. Otherwise every shelved enchantment at its highest level, except Mending. Mending stays unoffered until ticket 15 adds Full Library. That way no table offers Mending in between.
   - Nothing is stored. The menu re-reads the Library in every `broadcastChanges` (each server tick while open) and again inside Save. Books are never consumed.
2. **Own menu type, opened by a mixin on `EnchantingTableBlock.getMenuProvider`.** No Fabric event covers this. The client builds the menu through the `(int, Inventory)` constructor, with `ContainerLevelAccess.NULL` and the registry taken from `inventory.player.registryAccess()`. The mixin returns a `SimpleMenuProvider` for `EnchantingTableMenu` with the table's display name, so custom names still work. Vanilla `EnchantmentMenu`, its seed, its random offers and its XP cost go unused. Register `MenuType` `maple_flavored:enchanting_table` with `FeatureFlags.VANILLA_SET`. `stillValid` checks `Blocks.ENCHANTING_TABLE`.
3. **Slots:** item (0, max 1), lapis (1, lapis only), material (2, any item). Then the player inventory at vanilla's position (8, 84).
   - The item slot accepts gear with a material unit (`MaterialUnits.materialOf`) and at least one registered enchantment whose `canEnchant` accepts it. Golden apples and enchanted golden apples are refused for now (ticket 16). Plain and enchanted books have no material, so they're refused. Using `canEnchant` (the anvil set) rather than `isPrimaryItem` (vanilla's table set) lets the table take shields, Elytra, shears and flint and steel, because the table is now the only way to enchant.
   - Shift-click: lapis goes to slot 1; an acceptable item to slot 0 if it's empty; the current item's material to slot 2; anything else doesn't move.
   - `removed` returns all three slots to the player, like vanilla.
4. **Checklist rows, server-owned selection.** The rows are every enchantment the Library offers that the item accepts, plus every enchantment already on the item, plus every enchantment currently ticked. Without the last group, a book removed after its enchantment was ticked would leave an invisible selection that blocks Save and can't be unticked; with it, the row shows its Library problem and can be unticked. They're ordered by `#minecraft:tooltip_order`, then by registry id. Each enchantment has one of three states:
   - `UNSELECTED`
   - `SELECTED`: the level is the Library's live offer.
   - `CARRIED`: pre-selected from the item. The level is `max(item level, offer)`. So it follows the Library upward (item III, Library V saves V) and keeps the item's level when the Library is lower (item V, Library III stays V). In that case the row is invalid, and Save is blocked until the player unticks it. Ticking it again makes it `SELECTED` at III, which is how a level is lowered.

   Clicks: `CARRIED` → `UNSELECTED` → `SELECTED` → `UNSELECTED`. A toggle on an enchantment that isn't a row is ignored. When the item slot changes, every enchantment on the new item starts `CARRIED`, and every other one starts `UNSELECTED`.
5. **Sync through menu data slots (16-bit).** Two slots per enchantment registry id: the offered level (0 = none) and the state (0, 1, 2). The menu adds them in its constructor from `registryAccess().lookupOrThrow(ENCHANTMENT).size()`, which is the same on both sides, since the registry syncs by id as vanilla's `enchantClue` relies on.
   - Button ids: `0` = Save; `1 + registry id` = toggle.
   - On the client, `clickMenuButton` returns true and changes nothing, because the server syncs the result back. On the server, only clicks that change something return true.
6. **One rule, two callers: `Checklist.problems(...)`.** It's a pure function from the item, offers, states, the player's level, the lapis stack, the material stack and `hasInfiniteMaterials` to a list of problems. Save runs only when the list is empty. The screen shows the same list as the Save button's tooltip. Problems:
   - nothing selected
   - an enchantment not in this Library at that level (shows the offered level, or "not shelved")
   - an enchantment that doesn't fit this item
   - two incompatible enchantments (`Enchantment.areCompatible`, the first conflicting pair)
   - more enchantments than Enchantment Slots
   - not enough lapis
   - no deposit material (names it with `MaterialUnits.materialName`)

   Creative players (`hasInfiniteMaterials`) skip the lapis, material and slot checks, as vanilla skips their level and lapis checks. The Library check still applies to them.
7. **Enchantment Slots:** `EnchantmentSlots.available(playerLevel, item)` = `playerLevel / 30`, plus 1 if the item is in a new item tag, `maple_flavored:extra_enchantment_slot`. The tag holds the golden sword, shovel, pickaxe, axe, hoe and spear, and the golden helmet, chestplate, leggings and boots. Horse and nautilus armor are left out. Electrum joins the tag in ticket 33. 30 lives in one constant.
8. **Save (server, after an empty `problems` from a fresh `Library.read`):**
   - Set `ENCHANTMENTS` to exactly the selection at its computed levels.
   - Consume 1 lapis per selected enchantment.
   - If the item was unenchanted before this save, consume one item from the material slot and store it on the item as the deposit. Only that case pays.
   - Award `Stats.ENCHANT_ITEM`. Fire `CriteriaTriggers.ENCHANTED_ITEM` with the selected count, which is what Enchanter listens to.
   - Play `ENCHANTMENT_TABLE_USE`.
   - Re-run pre-selection, so every row on the item becomes `CARRIED`.

   No XP is taken, and `onEnchantmentPerformed` is never called.
   - The charge follows the spec literally: the first save of an unenchanted item. Gear enchanted elsewhere (creative, loot before ticket 17) holds no deposit and edits for lapis alone; grinding it returns nothing. The grindstone always returns exactly what was paid, and nothing when nothing was paid. Survival can't make an unenchanted item that holds a deposit: Save needs at least one enchantment, and the grindstone removes both together.
9. **Deposit component `maple_flavored:enchanting_deposit`** (`ItemStack`, persistent and network-synced). It holds the exact one-item stack paid, so oak planks come back as oak planks.
   - Grindstone: a `GrindstoneMenu.computeResult` return mixin removes the component from the stripped result.
   - The existing `GrindstoneResultSlotMixin` (`GrindstoneMenu$4`) gets an `onTake` HEAD inject. It reads the deposit from whichever input holds the item, through the outer menu's public `getSlot(INPUT_SLOT)` and `getSlot(ADDITIONAL_SLOT)` (reached through the slot class's outer reference, a shadowed `this$0`, whose name is checked during implementation; no accessor needed). It gives the deposit to the player with `Inventory.placeItemBackInInventory(stack, Prediction.SERVER_ONLY)`, which drops it if the inventory is full. This runs on the server only: `onTake` also runs on the client as a prediction, and the client must not give an item.
   - The lapis is never returned. XP stays 0 (ticket 11).
   - Vanilla already strips every enchantment, because ticket 13 emptied `#curse`.
10. **Shelving advancement.** Add a `PlayerTrigger` `maple_flavored:enchanted_book_shelved` to `MapleTriggers`. A HEAD inject on `ChiseledBookShelfBlock.addBook` (server side, enchanted book, `ServerPlayer`) fires it when `Library.holdsShelf(level, shelfPos)` is true, meaning some enchanting table sits at a vanilla offset whose `isValidBookShelf` check passes for that shelf. The 15-shelf count doesn't matter here, since the books are still that table's Library.
    - Advancement `maple_flavored:story/shelve_enchanted_book`: parent `minecraft:story/mine_diamond`, task, icon chiseled bookshelf. Title "Shelf Life", description "Shelve an enchanted book in a Library".
    - Override `data/minecraft/advancement/story/enchant_item.json`, copied from vanilla with the parent changed to the new advancement.
11. **Enchanted books resist fire and explosions.** A `DefaultItemComponentEvents.MODIFY` handler gives `Items.ENCHANTED_BOOK` `DAMAGE_RESISTANT` over a new damage-type tag `maple_flavored:enchanted_book_resists`, which holds `#minecraft:is_fire` and `#minecraft:is_explosion`. The tag is resolved from the event's `HolderLookup.Provider`. `ItemEntity.hurtServer` and `fireImmune` already honour the component, the way Netherite does.
12. **Screen (client):** `EnchantingTableScreen extends AbstractContainerScreen<EnchantingTableMenu>`, registered with `MenuScreens.register`.
    - An original 176×166 background PNG in vanilla's panel style (drawn with Pillow, no Mojang pixels copied). It has three slots at the left and a list well at the right.
    - The list shows 3 visible rows of 19 px. Rows draw vanilla's `container/enchanting_table/enchantment_slot{,_highlighted,_disabled}` sprites by id, plus a check mark and `Enchantment.getFullname(level)`. The scroller works like the stonecutter's (mouse wheel and drag, using vanilla's scroller sprites by id).
    - Rows with a problem draw in red, and the problem shows as the row's tooltip.
    - A vanilla `Button` "Save" sits above the slots, with a slot counter "Slots: used / available" beside it. The button is active only when `Checklist.problems` is empty, and its tooltip lists the problems.
    - Clicking a row or Save sends `handleInventoryButtonClick`. The client offers are synced data, so nothing is recomputed client side.
    - New strings go in `en_us.json`.

## API sketch

```java
package mapleflavored.enchanting;

/** The enchanted books shelved around one enchanting table, read live from the world. Nothing is stored. */
public record Library(int bookshelves, Map<Holder<Enchantment>, Integer> highestLevels) {
	public static final int REQUIRED_BOOKSHELVES = 15;

	public static Library read(Level level, BlockPos tablePos);
	/** Whether the shelf sits at a vanilla bookshelf position of any enchanting table. */
	public static boolean holdsShelf(Level level, BlockPos shelfPos);
	/** Each offered enchantment at its highest shelved level; empty below 15 shelves; never Mending (ticket 15). */
	public Map<Holder<Enchantment>, Integer> offers();
}

/** Enchantment Slots: one per 30 levels, plus one for gear in #maple_flavored:extra_enchantment_slot. */
public final class EnchantmentSlots {
	public static int available(int playerLevel, ItemStack item);  // Checklist.problems calls it; so does the screen's counter
}

/** Whether a checklist can be saved, and why not. Pure; the menu and the screen both call it. */
public final class Checklist {
	public enum State { UNSELECTED, SELECTED, CARRIED }
	public sealed interface Problem { Component describe(); }  // NothingSelected, NotInLibrary, NotForItem, Incompatible, TooMany, NeedsLapis, NeedsMaterial
	public static boolean accepts(ItemStack item, HolderLookup<Enchantment> enchantments);
	public static List<Holder<Enchantment>> rows(ItemStack item, Map<Holder<Enchantment>, Integer> offers, HolderLookup.RegistryLookup<Enchantment> registry);
	public static int levelOf(Holder<Enchantment> enchantment, State state, ItemStack item, Map<Holder<Enchantment>, Integer> offers);
	public static List<Problem> problems(ItemStack item, Map<Holder<Enchantment>, State> states, Map<Holder<Enchantment>, Integer> offers,
		int playerLevel, ItemStack lapis, ItemStack material, boolean infiniteMaterials);
}

/** The material unit an item paid on its first save, returned by the grindstone. */
public final class EnchantingDeposit {
	public static final DataComponentType<ItemStack> COMPONENT;
	public static Optional<ItemStack> of(ItemStack item);
}

public class EnchantingTableMenu extends AbstractContainerMenu {
	public static final int ITEM_SLOT = 0, LAPIS_SLOT = 1, MATERIAL_SLOT = 2, SAVE_BUTTON = 0;
	public static int toggleButton(Holder<Enchantment> enchantment, Registry<Enchantment> registry);  // 1 + id
	public Map<Holder<Enchantment>, Integer> offers();          // synced
	public Map<Holder<Enchantment>, Checklist.State> states();  // synced
}

/** Registers the menu type, deposit component, book resistance, and opens the menu from the table. */
public final class Enchanting { public static void register(); }  // called from MapleFlavored.onInitialize
```

Consumers: the menu (Save and rows), the screen (rows, tooltips, Save state), `GrindstoneMenuMixin` and `GrindstoneResultSlotMixin` (deposit), `ChiseledBookShelfBlockMixin` (`holdsShelf`). Ticket 15 adds progress and Full to `Library` and menu data. Ticket 16 adds an apple branch to `Checklist` and `Library.offers`.

## Files

- New: `src/main/java/mapleflavored/enchanting/` `Library.java`, `Checklist.java`, `EnchantmentSlots.java`, `EnchantingDeposit.java`, `EnchantingTableMenu.java`, `Enchanting.java`
- New mixins: `mixin/EnchantingTableBlockMixin.java` (menu provider) and `mixin/ChiseledBookShelfBlockMixin.java` (trigger). `GrindstoneMenuMixin` gets a `computeResult` return inject, and `GrindstoneResultSlotMixin` gets `onTake`. Register them in `maple_flavored.mixins.json`.
- `MapleFlavored.java`: `Enchanting.register()`. `advancements/MapleTriggers.java`: the new trigger.
- Client: `client/enchanting/EnchantingTableScreen.java`, registered in `MapleFlavoredClient`.
- Assets: `assets/maple_flavored/textures/gui/container/enchanting_table.png` (original art), `lang/en_us.json` (advancement, screen strings, problems).
- Data:
  - `data/minecraft/tags/block/enchantment_power_provider.json`
  - `data/maple_flavored/tags/item/extra_enchantment_slot.json`
  - `data/maple_flavored/tags/damage_type/enchanted_book_resists.json`
  - `data/maple_flavored/advancement/story/shelve_enchanted_book.json`
  - `data/minecraft/advancement/story/enchant_item.json`
- GameTests: `src/gametest/java/mapleflavored/gametest/EnchantingTableGameTests.java` (including the grindstone deposit cases, which reuse its table setup), `EnchantedBookGameTests.java`, registered in the gametest `fabric.mod.json`.
- `GLOSSARY.md`: add **Deposit**. `CREDITS.md`: nothing (no reused assets). Ticket file: tick the boxes on completion.

## Steps

1. Data: the power-provider tag, the slot tag and the damage tag. Add the deposit component and the book resistance, with their tests.
2. `Library` and `EnchantmentSlots`, then `Checklist`'s pure functions.
3. `EnchantingTableMenu`, its menu type and the provider mixin. Then the table GameTests.
4. Deposit return at the grindstone, with tests.
5. Trigger, mixin and advancements, with tests.
6. Screen and texture. Run the client for the hand check.
7. Full `gametest` suite, then the LARP review, then the PR.

## Acceptance matrix (GameTests)

Setup helper: in the 8×8×8 empty structure, a table at (3,1,3). A ring of 16 shelf positions at y 1 in the 5×5 square around it (x or z ∈ {1, 5}), with the inner ring left as air. Mixed bookshelves and Chiseled Bookshelves are filled by setting the block entity's items. Players are survival mock players with levels set by `setExperienceLevels`. The menu is built on the placed table, as `AnvilGameTests` does. Save goes through `clickMenuButton(player, SAVE_BUTTON)`.

| Case | Expected |
| --- | --- |
| 14 bookshelves + 1 Chiseled holding Sharpness II | 15 shelves → Sharpness offered at II |
| Same with one plain shelf removed (14) | nothing offered |
| Chiseled with Sharpness I and Sharpness III books, plus a Smite I book in another Chiseled shelf | Sharpness III and Smite I offered |
| Chiseled at an offset with a block in the transmitter gap | its books are not offered |
| Mending book shelved | Mending not offered |
| Insert a diamond sword, tick Sharpness, level 30, 1 lapis, 1 diamond, Save | sword Sharpness III; lapis 0; diamond slot empty; deposit = 1 diamond; books still on the shelves; ENCHANTED_ITEM fired (Enchanter granted once its parent is held, or the trigger is observed through the advancement) |
| Re-insert that sword, add Smite (incompatible) | refused: nothing changes (item, lapis, slots) |
| Level 30 player saves 2 enchantments | refused, unchanged; level 60 player same table → saved, lapis −2, no material taken (deposit already held) |
| Golden sword at level 0, 1 enchantment | saved (gold extra slot); 2 enchantments at level 30 → saved; 3 → refused |
| Iron sword with Sharpness V inserted (enchanted by command, no deposit), Library only Sharpness III | row CARRIED at V, Save refused; toggle twice → SELECTED III, Save works, 1 lapis taken, no material taken, no deposit stored |
| Iron sword with Sharpness III, Library Sharpness V | pre-selected; Save → Sharpness V |
| Plain sword: tick Sharpness and Unbreaking, then remove the Sharpness book while the menu is open | Sharpness row stays, flagged; Save refused, nothing changes; untick Sharpness → Save gives Unbreaking only |
| Level 90 player, plain iron sword, 3 compatible enchantments | saved with exactly those 3 |
| Level 29 player, plain iron sword, 1 enchantment | refused (0 slots) |
| Sword with Sharpness and Unbreaking: untick Unbreaking, Save | item has Sharpness only (the map is replaced, not merged); 1 lapis |
| Opening the placed table through a mock player's block interaction (`useBlock`) | `player.containerMenu` is an `EnchantingTableMenu`; a Save through it works |
| Shared Library: player A (level 30) and player B (level 60) each open their own menu on the same table | both see the same offers; B saves 2, A is refused at 2 and saves 1 |
| Nothing ticked (unenchanted item) | Save refused |
| Not enough lapis; missing or wrong material on first save | refused, unchanged |
| Item slot refuses a book, an enchanted book, a golden apple, dirt; accepts a shield and shears | as stated |
| Creative player with no lapis or material | saves; no deposit stored |
| Toggling an enchantment the item can't take (Protection on a sword) | ignored |

Every successful Save also asserts the player's XP level and points are unchanged, and the shelved books' full contents (items and stored enchantments) are unchanged. Every refused Save compares the item, lapis, material and selection states before and after.

Grindstone (in `EnchantingTableGameTests`, which saves the items first): grinding a table-saved iron sword gives back a plain sword without the component, and the player's inventory gains 1 iron ingot. Oak-plank deposit returns oak planks. A sword enchanted by command and then edited at the table (still no deposit) returns nothing. No lapis comes back, and XP stays 0.

Advancement: a mock player inserting an enchanted book into a Chiseled Bookshelf at a table offset (through the block's `useItemOn` or the `addBook` path) is granted `shelve_enchanted_book`. A shelf with no table nearby grants nothing. A plain book grants nothing. Enchanter's parent is the new advancement (read from the server's advancement tree).

Books: an enchanted book item entity in lava, in fire, and next to a TNT explosion survives. A plain book (the control) is destroyed.

Then the full `gametest` suite.

## Invariants

- Save is all or nothing. Any problem leaves the item, lapis, material and selection unchanged.
- An item holds a deposit iff it was saved in survival while unenchanted and not ground since. The grindstone returns exactly the stored stack, or nothing.
- The server decides offers, states and Save. The client only displays synced data and `Checklist.problems`.
- Shelved books are never consumed or modified.

## Hand checks

- The checklist screen: rows, check marks, red problem rows and tooltips, the scroller with more than 3 rows (diamond boots in a full Library), the Save tooltip, the slot counter, the material slot, shift-click routing, and no flicker after toggles.
- Chiseled Bookshelf particles flow to the table.
- A custom-named table opens with its name, and offers update while the screen is open when a second player removes or adds a book.

## Exclusions

- Library Progress, the progress bar, Mending and Full Library (ticket 15).
- Golden apples at the table and grindstone (16).
- Book loot cleanup (17).
- Electrum in the slot tag (33).
- A tooltip line showing the deposit on items.
- Grinding enchanted books stays vanilla (book → plain book).
