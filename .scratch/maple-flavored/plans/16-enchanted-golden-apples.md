# Plan: 16 Enchanted golden apples

## Context / end goal

Tickets 14 and 15 turned the Enchanting Table into a checklist over a Library and gave the Library a progress bar whose only reward was Mending at 100%. This ticket makes that bar pay out along the way and closes the villager loop the design depends on:

- A plain golden apple is just food. The table turns it into the vanilla `enchanted_golden_apple` item carrying the effects the player picks, each unlocked by Library Progress (15/30/50/75%) and costing an Enchantment Slot and a lapis, with a block of gold as the deposit on the first save.
- Chests can't skip the table: every enchanted golden apple in loot becomes a plain one.
- Curing a zombie villager takes any enchanted golden apple, so the first villager needs the Nether (Weakness), a 20-level Library and level 30.

Closing the ticket means a survival player can only get an enchanted golden apple from the table, it behaves like gear there and at the grindstone (deposit in, deposit out, lapis lost), it stacks only with identical apples, it cures zombie villagers, and the advancement tree shows the step (Enchanter → An Apple a Day → Zombie Doctor).

Size: L, one ticket. The work is one change to the table's model plus small data and mixin pieces that only make sense together. No split.

## Decisions

1. **The table holds one of two checklists, chosen once from the item.** `Checklist` becomes a small sealed interface ("the table's checklist for the item in it") with two records:
   - `EnchantmentChecklist`: today's `Checklist`, renamed, unchanged in behavior.
   - `AppleChecklist`: effects on a golden apple.
   The menu picks one from the item, and Save, the problems, the screen's rows and the slot counter all go through the interface. Neither the menu's save nor the screen branches on the item kind. (The deletion test: without the interface, both the menu and the screen would branch on "is this an apple?" in several places.)
2. **Shared vocabulary lives on the interface:** `Cost`, `Problem` (no longer sealed, since its records now span files), `TooMany`, `NeedsLapis`, `NeedsMaterial`, the `Row` view and one helper for the slot and payment checks. Enchantment-only problems (`NothingSelected`, `NotForItem`, `NotInLibrary`, `Incompatible`) and `State` move with `EnchantmentChecklist`. The apple adds `NoEffectSelected` and `NotOffered(effect)`.
3. **Apples have no levels and no CARRIED state.** An apple's choice is a set of effects. Effects already on the apple start chosen. A chosen effect the table doesn't offer right now blocks Save until it's unticked. This is the gear rule ("every selected enchantment is offered by the current Library") applied to effects.
4. **`AppleEffect` enum is the one home for the apple's numbers:** Regeneration II 400 ticks at 15%, Fire Resistance 6000 ticks at 30%, Absorption IV 2400 ticks at 50%, Resistance 6000 ticks at 75%. These match vanilla's enchanted golden apple, checked in the 26.3 jar (`Consumables.ENCHANTED_GOLDEN_APPLE`). The enum order is the threshold order, which is the row order and the order effects are written to the apple.
5. **The apple's effects live in vanilla's `consumable` component, nowhere else.** A saved apple is `transmuteCopy(ENCHANTED_GOLDEN_APPLE, 1)` with `consumable` = `Consumables.defaultFood()` plus one `ApplyStatusEffectsConsumeEffect` listing the chosen effects in enum order. `AppleEffect.on(stack)` reads them back by matching each effect instance's `MobEffect` holder. Eating it is vanilla. Stacking follows from component equality: `MobEffectInstance` and the records involved implement `equals`, so apples with the same effects and deposit stack and others don't. The deposit is always one block of gold, so splitting or merging stacks never changes a deposit. There's no second component that could drift.
6. **Plain golden apple:** `DefaultItemComponentEvents.MODIFY` sets the golden apple's default `consumable` to `Consumables.DEFAULT_FOOD`. Its `food` component (4 nutrition, 1.2 saturation, always edible) stays vanilla.
7. **Offers need 15 bookshelves, like enchantments.** `Library.appleOffers()` is empty below 15 bookshelves. Otherwise it returns each effect whose percent `progress()` has reached. That set is synced to the client as a bitmask `DataSlot`, so the client never re-derives the bookshelf rule.
8. **Checkpoints:** `LibraryCheckpoint.ALL` becomes the four apple checkpoints plus Mending. Each apple checkpoint's icon is a potion bottle tinted with the effect's color (an `ItemStackTemplate` with a custom-color `potion_contents`), so the four 8 px icons can be told apart. Its reward text is "%s on golden apples". `LibraryCheckpoint` depends on `AppleEffect` and never the reverse, which avoids a static-init cycle. `Progress.reached(int percent)` is the one threshold rule, and `reached(LibraryCheckpoint)` delegates to it.
9. **Menu buttons:** `SAVE_BUTTON` = 0, apple effects 1..4 (`toggleButton(AppleEffect)`), enchantments from 5 (`toggleButton(Holder<Enchantment>, Registry)`, same signature as now). Two new `DataSlot`s carry bitmasks for the offered and chosen effects.
10. **Save is uniform:** check problems → `cost` → `saved = checklist.saved()` (a copy with the choices applied: `setEnchantments` for gear, the apple transmute above) → take lapis → pay the deposit onto `saved` when `cost.deposit()` → put `saved` in the item slot → stat, `ENCHANTED_ITEM` trigger, sound → preselect. For apples, `cost.deposit()` is "the item is a plain golden apple". An enchanted golden apple (for example a creative one) is already "saved" and pays none, matching gear's "first save of an unenchanted item".
11. **Grindstone:** a mixin on the two input slots (`GrindstoneMenu$2`, `$3`) caps both at one item (`getMaxStackSize() = 1`; gear and books already stack to 1). Only the top slot also accepts `enchanted_golden_apple`. Vanilla's quick move fills both inputs from a stack, which would grind nothing under the never-merge rule, so with apples in the top slot only, a shift-clicked stack moves exactly one apple. In `computeResult` at HEAD, a lone enchanted golden apple in the top slot results in `transmuteCopy(GOLDEN_APPLE, 1)` with `consumable` set back to the golden apple's default, which clears the patch. It keeps a custom name, as gear does. A value returned at HEAD skips the RETURN injection, so this path removes the deposit itself. The existing result-slot mixin refunds the input's deposit, so the block of gold comes back by the same path as gear's. No lapis, no XP.
12. **Loot:** add `ENCHANTED_GOLDEN_APPLE → GOLDEN_APPLE` to `LootSubstitutions`.
13. **Cure:** a MixinExtras `@ModifyExpressionValue` on the read of `Items.GOLDEN_APPLE` in `ZombieVillager.mobInteract` swaps it for `ENCHANTED_GOLDEN_APPLE`. In bytecode, `ItemStack.is` there is the erased `is(Object)`, so the field read is the stable target. Any enchanted golden apple cures under Weakness, and a plain golden apple no longer does anything. There's no Fabric event that can start the private conversion, so a mixin is the narrowest seam.
14. **Advancement "An Apple a Day":** `data/maple_flavored/advancement/story/enchant_golden_apple.json`, parent `minecraft:story/enchant_item`, frame `task`, icon `enchanted_golden_apple`. Criterion: vanilla `minecraft:enchanted_item` with an item predicate for `minecraft:enchanted_golden_apple`. The spec lists "making an enchanted golden apple" among custom triggers, but the platform rule prefers vanilla data, and the table already fires `enchanted_item` with the saved item, so no new trigger is needed. Description: "Make an enchanted golden apple at an Enchanting Table". Zombie Doctor is overridden at `data/minecraft/advancement/story/cure_zombie_villager.json` with the parent changed to it, and everything else copied from 26.3. A Balanced Diet is unchanged, because it checks the item id, which saved apples keep.

## API sketch

```java
// mapleflavored.enchanting
public sealed interface Checklist permits EnchantmentChecklist, AppleChecklist {
	static boolean accepts(ItemStack item, Registry<Enchantment> enchantments); // gear or a golden apple of either kind
	ItemStack item();
	int chosenCount();
	List<Row> rows(List<Problem> problems);   // what the screen lists, each with the problems about it
	Cost cost(boolean infiniteMaterials);
	List<Problem> problems(int playerLevel, ItemStack lapis, ItemStack material, boolean infiniteMaterials);
	ItemStack saved();                        // a copy of the item with the choices applied, no payment

	record Row(Component name, boolean chosen, int button, List<Problem> problems) {}
	record Cost(int lapis, boolean deposit) {}
	interface Problem { Component describe(); }
	record TooMany(int chosen, int slots) implements Problem {}
	record NeedsLapis(int lapis) implements Problem {}
	record NeedsMaterial(Component material) implements Problem {}
}

public record EnchantmentChecklist(ItemStack item, Map<Holder<Enchantment>, Integer> offers,
	Map<Holder<Enchantment>, State> states, Registry<Enchantment> enchantments) implements Checklist {
	// today's Checklist; rows() renamed listed(); NothingSelected/NotForItem/NotInLibrary/Incompatible/State nested here
}

public record AppleChecklist(ItemStack item, Set<AppleEffect> offered, Set<AppleEffect> chosen) implements Checklist {
	static boolean accepts(ItemStack item);          // golden_apple or enchanted_golden_apple
	List<AppleEffect> listed();                      // offered, on the apple, or chosen; enum order
	record NoEffectSelected() implements Problem {}
	record NotOffered(AppleEffect effect) implements Problem {}
}

public enum AppleEffect {
	REGENERATION, FIRE_RESISTANCE, ABSORPTION, RESISTANCE;
	int percent();
	MobEffectInstance instance();                    // fresh each call
	Component describe();                            // "Regeneration II (0:20)"
	static Set<AppleEffect> on(ItemStack apple);     // effects an enchanted golden apple gives; empty otherwise
	static ItemStack enchant(ItemStack apple, Set<AppleEffect> effects);
	static ItemStack plain(ItemStack apple);         // back to a golden apple, keeping its name
}

// Library
public Set<AppleEffect> appleOffers();
// Library.Progress
public boolean reached(int percent);

// EnchantingTableMenu
public Checklist checklist();                       // the one for the item in the table
public EnchantmentChecklist enchantmentChecklist(); // was checklist()
public AppleChecklist appleChecklist();
public static int toggleButton(AppleEffect effect);
```

Consumers:
- `EnchantingTableMenu`: slot `mayPlace` uses `Checklist.accepts`. `save`, `problems` and toggles go through `checklist()`.
- `EnchantingTableScreen`: `menu.checklist().rows(menu.problems(player))` for drawing, clicks and row tooltips. `chosenCount()` and `item()` for the slot counter. `LibraryCheckpoint.ALL` for the bar.
- Grindstone mixin: `AppleEffect.plain`.
- GameTests: `enchantmentChecklist()` where they used `checklist()`, `appleChecklist()` for apples.

## Files

- `enchanting/Checklist.java`: becomes the interface (decision 2).
- `enchanting/EnchantmentChecklist.java`: new home of the gear checklist (git rename).
- `enchanting/AppleChecklist.java`, `enchanting/AppleEffect.java`: new.
- `enchanting/Library.java`: `appleOffers`, `Progress.reached(int)`.
- `enchanting/LibraryCheckpoint.java`: apple checkpoints in `ALL`, and an `of(AppleEffect)` factory.
- `enchanting/EnchantingTableMenu.java`: two bitmask slots, button layout, uniform save.
- `enchanting/Enchanting.java`: the plain golden apple's default `consumable`.
- `loot/LootSubstitutions.java`: the apple swap.
- `mixin/GrindstoneMenuMixin.java`: the apple result. `mixin/GrindstoneInputSlotMixin.java`: new, accept and cap. `mixin/ZombieVillagerMixin.java`: new. Register both in `maple_flavored.mixins.json`.
- `client/.../EnchantingTableScreen.java`: draw `Row`s.
- `lang/en_us.json`: advancement title and description, `…no_effect_selected` ("Select at least one effect"), `…not_offered` ("This Library doesn't offer %s"), `…apple_reward` ("%s on golden apples").
- Data: `maple_flavored/advancement/story/enchant_golden_apple.json`, `minecraft/advancement/story/cure_zombie_villager.json`.
- GameTests: `EnchantingTables.java` (new fixture, holding the table helpers moved out of `EnchantingTableGameTests`, which then uses it), `GoldenAppleGameTests.java` (new, registered in the gametest `fabric.mod.json`), `loot_table/enchanted_golden_apples.json` fixture. `EnchantingTableGameTests` gets mechanical renames (`checklist()` → `enchantmentChecklist()`, `Checklist.State`/`NotInLibrary` → `EnchantmentChecklist.…`), and its checkpoint test also pins the five percents `[15, 30, 50, 75, 100]`.
- Docs: the ticket's boxes, and this plan. GLOSSARY already covers Library Progress and Deposit. Nothing is reused from Matcha, so `CREDITS.md` is unchanged.

## Steps

1. Rename and split `Checklist` (no behavior change). Update the menu, screen and tests. Run the enchanting GameTests.
2. `AppleEffect`, `AppleChecklist`, `Library.appleOffers`, checkpoints.
3. Menu: data slots, buttons, uniform save. Screen rows.
4. Plain apple component, loot swap, grindstone and zombie villager mixins, advancement data and lang.
5. GameTests below, then the full `gametest` run.
6. LARP review, PR.

## Acceptance matrix (`GoldenAppleGameTests`)

| Case | Expected |
| --- | --- |
| Eat a plain golden apple at food 10 | food 14, no effects |
| Library at 19/20, 38/39, 64/65, 97/98 levels (16 plain shelves + books worth n) | offers {}, {Regen}, {Regen}, {Regen, Fire}, {…Fire}, {…Absorption}, {…Absorption}, all four |
| Full Library with only 7 Chiseled shelves (<15) | no apple offers |
| Level 29, Regen chosen | refused, `TooMany(1, 0)`, nothing changes |
| Level 30, Regen | saves; level 30, two effects → refused `TooMany(2, 1)` (no gold slot); level 60, two → saves |
| First save at a Full Library, Regen + Fire, 2 lapis, 1 gold block | slot holds an `enchanted_golden_apple` with exactly those effects; lapis 0, gold 0, deposit = a block of gold |
| Same without the gold block | refused, `NeedsMaterial` |
| Nothing ticked on a golden apple | refused, `NoEffectSelected` |
| Re-save that apple adding Absorption (3 lapis, no gold in the slot) | saves; three effects; no gold needed |
| Apple with Resistance at a 20-level table | Resistance listed and chosen, refused `NotOffered(RESISTANCE)`; untick Resistance, tick Regen → saves |
| Eat an apple with all four | literal Regeneration 400/1, Fire Resistance 6000/0, Absorption 2400/3, Resistance 6000/0; Balanced Diet's `enchanted_golden_apple` criterion done |
| Eat a Fire + Absorption apple | exactly two effects, Fire Resistance 6000/0 and Absorption 2400/3 |
| Two separately made Regen apples / Regen vs Fire | same components (stack) / different (don't) |
| Shift-click a stack of 3 golden apples into the table, save Regeneration with 2 gold blocks in the slot | 1 apple in the table, 2 left unchanged in the inventory; 1 gold block spent |
| Shift-click a stack of 3 identical enchanted apples into the grindstone, take the result | 1 apple in the top slot, the lower slot empty and refusing apples, 2 left with their effects and deposits; the result `matches` a fresh golden apple and stacks with one; +1 block of gold; no lapis; no XP; a plain golden apple is refused |
| Zombie villager with Weakness, player holds a Fire-only apple | converting, apple consumed |
| Same with a plain golden apple | not converting, apple kept |
| Fixture loot table with 2 enchanted golden apples | 2 golden apples |
| Save an apple | `maple_flavored:story/enchant_golden_apple` granted (parent Enchanter); a sword save doesn't grant it; Zombie Doctor's parent is it |

Existing tests keep passing after the mechanical renames, except `theTableTakesOnlyEnchantableGear`. It becomes `theTableTakesOnlyEnchantableGearAndGoldenApples`: both apples are accepted, and plain books, enchanted books, apples and dirt are still refused.

## Invariants

- An apple's effects are its `consumable` component. Nothing else records them.
- Offered effects come from one place (`Library.appleOffers`), synced as data. Save and the screen read the same synced set.
- Every saved enchanted golden apple carries exactly one block of gold as its deposit, so stack operations never change deposits.
- The table and grindstone slots hold one item.
- A refused save changes nothing.

## Hand checks

- An apple in the table: rows named "Regeneration II (0:20)" etc., ticking, the Save tooltip, the slot counter, the material slot outline taking a gold block.
- The progress bar's four tinted bottles at 15/30/50/75 with their tooltips, shaded until reached.

## Exclusions

- Abandoned villages, Weakness brewing and the igloo villager (other tickets).
- Any extra slot for apples. Re-checking vanilla food values (unchanged, from the jar).
