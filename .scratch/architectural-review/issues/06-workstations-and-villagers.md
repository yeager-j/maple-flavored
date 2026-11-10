# 06: Workstations and villagers

**What to fix:** The architecture findings for anvil repairs, Material units, smithing, Reinforcement, villager trades, abandoned villages and brewing. Each finding stands alone unless it says otherwise.

**Blocked by:** None

**Status:** needs-triage

**Priority:** P2 (Medium)

**Estimate:** M

**Source:** Architectural Review 11/9, the workstations and villagers reviewer. Nothing was built or run.

What held up: the reviewer looked for correctness faults first and found none in the anvil cost sync, the menu data slots, the book trade's guards, Reinforced toughness or the smithing refusal flag. `AbandonedVillages` and `Brewing` agree with their JSON about who owns what.

Other tickets hold three findings from this slice: the weaker `survivalPlayer` copies and the client test that asserts nothing are in ticket 04, findings 2 and 3, and the XP constants are in ticket 04 finding 9.

Paths: `main/` is `src/main/java/mapleflavored/`, `mixin/` is `src/main/java/mapleflavored/mixin/`, `client/` is `src/client/java/mapleflavored/client/`, `test/` is `src/gametest/java/mapleflavored/gametest/`, and `data/` is `src/main/resources/data/`.

## Findings

### 1. The anvil's refusal is decided on the server, thrown away, then re-derived on the client (P2)

**Principle:** Parse, don't validate (King). Single Choice (Meyer).

`AnvilResults.compute` returns `Optional.empty()` for five outcomes: nothing to do, must enchant at the table, wrong material, mismatched books and maximum level. The screen rebuilds the reason by calling two boolean predicates in its own order. No value ever says what kind of attempt the slots are.

- The same checks run up to three times: `compute` calls `triesToEnchant` then `offersWrongMaterial`, which calls `triesToEnchant` again.
- The tooltip choice lives in a client mixin that server GameTests can't reach. The tests assert the two predicates, not the decision.
- `Result.materialUsed` uses 0 to mean "use the whole slot", which is vanilla's protocol leaking into the domain record.
- Smithing solves the same problem the other way, by syncing a server-decided flag.

Citations:

- `main/anvil/AnvilResults.java:34-36`, `:49-66`, `:73-86`, `:124-127`
- `client/mixin/AnvilScreenMixin.java:76-95`
- `test/AnvilGameTests.java:283-316`, `:364-372`

### 2. The book trade's multiplier is carried in mutable fields on the slot and the villager (P2)

**Principle:** Home state according to its lifetime.

A value that exists for one `onTake` call is stored on two longer-lived objects and handed between them by a set-then-reset protocol. It is stored in a `@Unique` field on `MerchantResultSlot`, copied onto the `Villager` through `TradeMultiplierHolder` with a try/finally reset, and read back inside `rewardTradeXp`. Any other caller of `notifyTrade` silently gets a multiplier of 1.

- `mixin/MerchantResultSlotMixin.java:29-38`, `:44-57`, `:60-63`
- `main/trades/TradeMultiplierHolder.java:7-10`
- `mixin/VillagerMixin.java:22-42`

The reviewer's direction: add the extra XP in the one `WrapOperation` around `notifyTrade`, and use MixinExtras `@Share` within `onTake`.

### 3. The book trade's rule is split between JSON and Java (P2)

**Principle:** One authority (Hunt and Thomas). Single Choice.

JSON owns the level I price of 4 emeralds and 10 villager XP. Java owns the player XP, the scaling and the cap.

- **Identity by shape.** `BookTrade.isBookTrade` recognises the trade as "cost is an enchanted book and the result is an emerald", asked at five sites across four mixins. Any other trade of that shape gets scaled, and a data pack edit to the result item silently turns scaling off.
- **The cap assumes the JSON price.** `MOST_FINDS = 16` is right only because the JSON gives 4 and a stack is 64. At 5 emeralds, a level V book would produce an 80-count stack. The only guard is a test that mirrors the literal 4.
- **A constant written twice.** `MOST_FINDS` is also `1 << (HIGHEST_COUNTED_LEVEL - 1)`.

Citations:

- `main/trades/BookTrade.java:15-21`, `:27-29`, `:44-55`
- `data/maple_flavored/villager_trade/librarian/1/enchanted_book_emerald.json`
- `mixin/MerchantContainerMixin.java:28-35`, `mixin/MerchantMenuMixin.java:41`, `mixin/MerchantResultSlotMixin.java:37`, `mixin/VillagerMixin.java:39`
- `test/TradeGameTests.java:174`

### 4. "Material upgrade" is defined in two places (P3)

**Principle:** Single Choice. Put rules with the context that can enforce them.

`MaterialUpgrades` documents that trims aren't upgrades, but it never sees the recipe. "Material upgrade means `SmithingTransformRecipe`" lives in a mixin target and again in an `instanceof` in the menu mixin. The menu also rebuilds a `SmithingRecipeInput` and calls `refuses` a second time to infer why the result is empty.

- `main/smithing/MaterialUpgrades.java:6-18`
- `mixin/SmithingTransformRecipeMixin.java:18-23`, `:27-35`
- `mixin/SmithingMenuMixin.java:48-60`

Related: the condition half of "Alloy gear keeps its share of durability" sits in the mixin, while the arithmetic is in `AlloyGear.keptDamage`.

### 5. `MaterialUnits` hands back an `Optional<Ingredient>`, so callers rebuild the same question (P3)

**Principle:** Deep modules (Ousterhout).

"Is this stack the Material unit of that item" is spelled `materialOf(item).map(m -> m.test(stack)).orElse(false)` at four sites, two of them in the enchanting package.

- `main/materialunits/MaterialUnits.java:62-64`
- `main/anvil/AnvilResults.java:73-75`, `:85`, `:127`
- `main/enchanting/Checklist.java:101-104`, `main/enchanting/EnchantingTableMenu.java:272`, `main/enchanting/EnchantmentChecklist.java:109-113`

Related: `AnvilResults.triesToEnchant` uses "has a Material unit" as a stand-in for "the Enchanting Table enchants it". The table's own rule also needs an applicable enchantment, so the two can disagree. Wolf armor may be such a case; the reviewer did not confirm 26.3's enchantable tags.

### 6. `fullRepairUnits` has a redundant second authority and rescans recipes on every anvil event (P3)

**Principle:** One authority. Home state according to its lifetime.

- `isIngotRepaired` and the `ALLOY_INGOTS` tag return 1 for Netherite and Alloy gear, but the fallback `craftingUnits.orElse(1)` already gives 1. Deleting the explicit rule would leave every anvil test passing.
- The tag is hand-written JSON, while `alloys.Alloy` is the code authority for the ingots. Ticket 07 finding 2 covers that tag.
- `cheapestCraftingUnits` scans every recipe on every `createResult` that reaches a repair, including each rename keystroke. The answer changes only on recipe reload.

Citations:

- `main/materialunits/MaterialUnits.java:47`, `:81-100`, `:128-137`
- `mixin/AnvilMenuMixin.java:58-71`
- `test/AnvilGameTests.java:59-70`, `:95-96`
- `data/maple_flavored/tags/item/alloy_ingots.json`

### 7. Reinforcement eligibility has two authorities and an unused constant (P3)

**Principle:** One authority. Model only what somebody reads.

`Reinforcement.REINFORCEABLE` is a public tag key that no Java code reads; the recipe JSON names the tag by string. Separately, `armorSlot` grants toughness only to humanoid armor. If the tag gained a non-armor member, that item would consume a template and show "Reinforced" with no effect.

- `main/reinforcement/Reinforcement.java:59-60`, `:122-128`
- `data/maple_flavored/recipe/reinforcement_smithing.json`
- `data/maple_flavored/tags/item/reinforceable.json`

## Checklist

- [ ] 1. `AnvilResults` returns one classification (rename, combine books, repair, refused with a reason, nothing), and both `compute` and the screen read it. A GameTest asserts the refusal reason.
- [ ] 2. `TradeMultiplierHolder` and the villager field are gone, and the multiplier lives no longer than the trade.
- [ ] 3. The book trade is identified once, not by shape, and its cap is derived from the offer.
- [ ] 4. One place says what a material upgrade is, and the smithing menu learns the refusal reason without re-running the rule.
- [ ] 5. `MaterialUnits` answers "is this the Material unit of that item" directly, and the wolf armor case is checked in game.
- [ ] 6. `fullRepairUnits` has one rule for ingot-repaired gear, and its recipe scan runs once per recipe reload.
- [ ] 7. One authority decides what can be Reinforced, and an item that can't gain toughness can't take a template.
