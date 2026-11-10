# 05: Enchanting and Library

**What to fix:** The architecture findings for the Library, the checklist enchanting table, Enchantment Slots, apple effects, the Deposit and the removed curses. Each finding stands alone unless it says otherwise.

**Blocked by:** None

**Status:** ready-for-human

**Priority:** P2 (Medium)

**Estimate:** L

**Source:** Architectural Review 11/9, the enchanting and Library reviewer. Nothing was built or run.

What held up: the Library is read live and stores nothing, a Full Library is derived from the registry, problems are typed records that carry their evidence, and the reviewer traced the anvil, smithing, loot, mob-drop and crafting-repair paths without finding one that strips enchantments while leaving a Deposit.

Paths: `en/` is `src/main/java/mapleflavored/enchanting/`, `mixin/` is `src/main/java/mapleflavored/mixin/`, `client/` is `src/client/java/mapleflavored/client/`, `test/` is `src/gametest/java/mapleflavored/gametest/`, and `data/` is `src/main/resources/data/`.

## Findings

### 1. The 15-bookshelf rule never reaches the client (P2)

**Principle:** Single Choice (Meyer). Empty and absent conflated.

The server offers enchantments and apple effects only when the table has 15 bookshelves and enough Library Progress. The bookshelf count is read and then dropped before sync, and the screen decides "unlocked" from progress alone. A Full Library with 7 shelves shows every checkpoint as "Unlocked" and "Full Library" while offering nothing, and the only explanation is wrong: "X isn't in this Library".

- `en/Library.java:124`, `:143`: both offer methods return empty below `REQUIRED_BOOKSHELVES`, the same value as "nothing shelved".
- `en/Library.java:63-71`: `Progress.reached` ignores bookshelves.
- `en/LibraryCheckpoint.java:15-16`: the doc says the server gates with `Progress#reached`, which is not the whole gate.
- `en/EnchantingTableMenu.java:351-364`: only offers and progress levels are synced.
- `client/enchanting/EnchantingTableScreen.java:273-275`, `:319-321`
- `en/EnchantmentChecklist.java:78-83`, `:246` and `en/AppleChecklist.java:29-33`, `:86-88`: the misleading problems.
- `test/EnchantingTableGameTests.java:658-674` and `test/GoldenAppleGameTests.java:104-113` pin the server behaviour.

Related: "Full" is written three ways, as `Progress.isFull()` (`en/Library.java:47-49`), as `reached(LibraryCheckpoint.MENDING)` at 100 (`:131`), and as `checkpoint.percent() < 100` in the screen (`client/enchanting/EnchantingTableScreen.java:293`).

### 2. The Deposit has no single owner (P2)

**Principle:** Information hiding (Parnas). Deep modules (Ousterhout).

The Deposit's lifecycle is spread over five files in three packages, and `EnchantingDeposit` hides almost none of it. The invariant "an item holds a deposit only while it is enchanted" is stated nowhere. Nothing is broken today, but any new way to strip enchantments has to rediscover the rule.

- Whether a deposit is due is decided by two proxies: `en/EnchantmentChecklist.java:210-215` (`!item.isEnchanted()`) and `en/AppleChecklist.java:72-78`.
- `en/EnchantingDeposit.java:16`: `COMPONENT` is public, and mixins use it directly.
- `en/EnchantingDeposit.java:33-36`: `pay` silently overwrites an existing deposit. `:39`: `refund` only reads.
- `mixin/GrindstoneMenuMixin.java:24-40`: the deposit is removed in two places because a HEAD cancel skips the RETURN hook, guarded only by a comment.
- `mixin/GrindstoneResultSlotMixin.java:36-45` re-derives which input was ground and always refunds one unit, which is correct only because `mixin/GrindstoneInputSlotMixin.java:32-35` caps input stacks at 1.
- `src/main/java/mapleflavored/smithing/MaterialUpgrades.java:7-9` carries part of the invariant.

The anvil has a domain home (`AnvilResults`); the grindstone rule lives only in mixins.

### 3. The shared save rule is implemented once per checklist and re-decided by callers (P2)

**Principle:** Single Choice. Deep modules.

`Checklist` states the save rule once in prose. The code decides it in several places.

- `cost()` is copied in `en/EnchantmentChecklist.java:211-215` and `en/AppleChecklist.java:74-78`.
- "Creative pays nothing" is branched in both `cost()` methods and at `en/Checklist.java:94`.
- The saver's context travels as four loose parameters: `en/EnchantingTableMenu.java:333-337`, `:308`; `en/Checklist.java:84`, `:90`, `:96`.
- `en/Checklist.java:72`: `rows(List<Problem>)` takes the checklist's own `problems()` output, so the screen builds two checklists per call, several times a frame (`client/enchanting/EnchantingTableScreen.java:336-338`).
- `client/enchanting/EnchantingTableScreen.java:141-147` recomputes slots instead of reading the `TooMany` problem.
- `en/Checklist.java:90`: `slotAndPaymentProblems` is a public default method.
- `en/EnchantmentChecklist.java:203` and `en/AppleChecklist.java:66` call `EnchantingTableMenu.toggleButton`, so the rule types depend on the menu's wire protocol.
- `Checklist.KEY_PREFIX` (`en/Checklist.java:20`) is retyped in `en/LibraryCheckpoint.java:41` and `client/enchanting/EnchantingTableScreen.java:34`, `:83-84`, `:145`, `:314`, `:323`.

### 4. Apple versus gear is half resolved, and an empty table is not a state (P2)

**Principle:** Single Choice. Parse, don't validate.

The sealed `Checklist` resolves the kind for cost, problems and rows. Toggling, preselecting, state storage and the wire layout stay per kind in the menu, and the grindstone mixins decide the kind again. An empty item slot is modelled as a gear checklist over `ItemStack.EMPTY`.

Likely defect, read from code and not run: with nothing in the table, the Save tooltip lists "Select at least one enchantment" plus a "Needs … as a deposit" line naming the empty stack. The path is `en/EnchantingTableMenu.java:122-124`, then `en/EnchantmentChecklist.java:214`, then `en/Checklist.java:101-105`, shown by `client/enchanting/EnchantingTableScreen.java:115-118`.

- `en/EnchantingTableMenu.java:185-187`: the button range, not the item, picks which toggle runs.
- `en/EnchantingTableMenu.java:277-301`: two toggles with two state encodings. `:343-347`: `preselect` writes both kinds.
- `en/EnchantingTableMenu.java:61`, `:118`, `:185`: the "after Save" offset `1` is written three times.
- `mixin/GrindstoneInputSlotMixin.java:29` and `mixin/GrindstoneMenuMixin.java:28` test the kind again.
- `en/AppleChecklist.java:67` matches row problems by record equality, while `en/EnchantmentChecklist.java:204` uses `EnchantmentProblem.concerns`.

### 5. An apple's stored effects are the authority, but the tooltip re-derives them (P3)

**Principle:** One authority with explicit derivations.

Saving bakes effect instances into the apple, and eating applies exactly those. Reading back keeps only the effect type, and the tooltip prints the enum's current strength and duration. They diverge after any retune in an existing world, for command-made apples, and at once if these values become game rules.

- `en/AppleEffect.java:68-75` bakes the instances. `:46-62`: `on()` matches by type only and silently drops effects outside the enum.
- `client/enchanting/AppleTooltip.java:20-28`
- `en/EnchantingTableMenu.java:346` preselects from the same lossy parse.

### 6. Curse removal lives in Java and in five hand-copied tag files (P3)

**Principle:** DRY. A test that has always been green.

`RemovedEnchantments.KEYS` looks like the one list, but removal also needs `replace: true` copies of five vanilla tags with the curses deleted by hand. A Minecraft update that adds a value to those tags needs a manual edit that nothing checks, and the override discards other packs' additions. The test checks one member per tag.

- `src/main/java/mapleflavored/enchantments/RemovedEnchantments.java:16-21`
- `data/minecraft/tags/enchantment/curse.json`, `on_random_loot.json`, `tooltip_order.json`, `tradeable.json`, `treasure.json`
- `test/RemovedCursesGameTests.java:14-17`, `:34-43`

`tooltip_order.json` omits Zephyr and Blight, so the table lists them after everything else (`en/EnchantmentChecklist.java:135-138`).

### 7. Enchanting tests duplicate helpers at different strengths and never exercise the client half (P3)

**Principle:** Let the interface be the test surface. Prove a gate can go red.

- Both test classes carry their own `assertSaves`, `assertRefuses`, `tableSlots` and `toggle`: `test/EnchantingTableGameTests.java:784-808` and `test/GoldenAppleGameTests.java:410-431`.
- The gear `assertRefuses` asserts only "refused and unchanged" (`test/EnchantingTableGameTests.java:795-802`); the apple one also asserts the specific `Problem`.
- The design's central claim, that server and client build the same checklist from the same data (`en/Checklist.java:15-16`), has no test. The client constructor at `en/EnchantingTableMenu.java:83-85` is never built in one.
- Untested: an underpowered table's messages, an empty slot's problems, and the material branch of `moveIntoTable` (`en/EnchantingTableMenu.java:272-274`).

## Checklist

- [x] 1. A table below 15 bookshelves tells the player so, the screen shows no checkpoint as unlocked there, and "Full" is decided in one place.
- [x] 2. One module owns when a Deposit is due, paid, carried and handed back. `EnchantingDeposit.COMPONENT` is not read outside it, and the grindstone mixins delegate to it.
- [x] 3. `cost()` and the creative exemption exist once, the screen builds one checklist per frame, and the checklists no longer call into `EnchantingTableMenu`.
- [ ] 4. An empty table is its own state with no deposit line, confirmed in game, and the item decides which toggle runs.
- [x] 5. An apple's tooltip and preselection read the effects stored on the stack.
- [x] 6. The five tags are derived from `RemovedEnchantments.KEYS`, or a test asserts each tag equals vanilla's minus the keys. Zephyr and Blight have a place in the tooltip order.
- [x] 7. The save and refuse helpers exist once and name the `Problem`, and a test builds the client menu and compares its checklist with the server's.

## Left for Jackson

- 4 is built and covered by GameTests, but not confirmed in game. Open a table with nothing in it and hover Save: it should read "Put in gear or a golden apple" with no deposit line.
- 5: a carried apple effect now saves exactly as the apple stores it, the way carried enchantments keep their level. Effects the table doesn't know, as on a command-made apple, are still dropped on save. Decide whether they should be kept.
- 6: Zephyr and Blight sit at the end of the tooltip order, after Mending. Placing them anywhere else means replacing vanilla's `tooltip_order` tag again.
