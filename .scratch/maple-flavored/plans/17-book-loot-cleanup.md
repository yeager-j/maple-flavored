# Plan: 17 Book loot cleanup

## End goal

Enchantments come only from the Library. After this ticket, nothing a player finds hands them a shortcut past it:

- Chests never hold an enchanted book or enchanted gear. Where a chest used to roll a book, it now rolls 1 diamond, so it still feels worth opening.
- The only enchanted books in loot are the ones the design wants there, always one enchantment at level I: fishing's Luck of the Sea and Lure, the ominous vault's Breach, Density and Wind Burst, and (later tickets) mob drops.
- Fishing and bartering give plain gear. Bartering no longer gives Soul Speed books or iron boots.
- A mob's own gear (what it spawned with) drops at vanilla's chance but plain, so mobs are never an enchantment source. Gear a mob picked up, such as a player's dropped sword, comes back exactly as it was.

Size: M, one ticket. No split.

## Vanilla facts this plan rests on (26.3 jar)

- `LootTableEvents.MODIFY_DROPS` wraps `LootTable.getRandomItemsRaw(LootContext, …)`, which nested `loot_table` entries also call. So every table in a nest fires the event with its own holder, and a parent sees its children's already-modified drops again. Every roll-time rule must be idempotent.
- Vault reward tables (`chests/trial_chambers/reward*`) are declared `"type": "minecraft:chest"`, but `VaultBlockEntity` rolls them with `LootContextParamSets.VAULT`.
- Enchanted books in loot: 16 vanilla chest tables, fishing treasure, bartering, and the two vault "rare" tables. Enchanted gear: 13 chest tables, fishing treasure, bartering (iron boots), the two vault rare tables, and the three `equipment/trial_chamber*` tables (mob gear, out of scope).
- `Mob.dropCustomDeathLoot` and `Mob.equipItemIfPossible` (swapping a held item for a picked-up one) are the two places a mob drops a slot's gear through `spawnAtLocation`. Every pickup path (`equipItemIfPossible`, Dolphin, Fox, Panda, Piglin `holdIn*Hand`, dispensers, wolf and horse armor) calls `setGuaranteedDrop`/`setItemSlotAndDropWhenKilled`, which sets the slot's drop chance to 2.0, "preserved". Spawned gear keeps a chance ≤ 1 (default 0.085). `DropChances.isPreserved(slot)` is that distinction, and it is saved with the mob.

## Decisions

1. **Roll-time rules for invariants, keyed on each table's declared type.** A new `mapleflavored.loot.LootEnchantments` registers one `MODIFY_DROPS` handler that rewrites each drop with the table's `getParamSet()`:
   - An enchanted book from a **chest** table becomes 1 diamond. This is "each chest entry becomes 1 diamond": enchanted books come only from book entries, one per roll, so one book is one entry's result.
   - Any other enchanted book, from any table, keeps exactly one enchantment, at level I. If it held several, one is kept at random from the loot context's random (only consumed when there's a choice). Vanilla, after this ticket, never rolls a multi-enchantment book; this covers other packs.
   - Drops from **chest, vault, fishing and barter** tables lose their `enchantments` component (gear becomes plain). Books keep theirs, since books store enchantments in `stored_enchantments`. Equipment tables are untouched (mob gear is handled at the mob's drop, decision 4), and block tables are untouched because a block can drop what it stored.
   - Why roll-time and not JSON overrides of every table: about 30 overrides would copy vanilla wholesale, go stale silently on version updates, miss other packs' chests, and collide with tickets 38 and 41 which edit the same chests. One rule keyed on table type covers all of them and composes with `LootSubstitutions`, which works the same way.
2. **JSON overrides only where entries change** (each differs from vanilla only in what's listed; everything else is copied verbatim, including gear entries' enchant functions, which decision 1 strips):
   - `gameplay/fishing/treasure.json`: the book entry's `enchant_with_levels` becomes `enchant_randomly` over `luck_of_the_sea` and `lure`. Level I comes from decision 1.
   - `gameplay/piglin_bartering.json`: the Soul Speed book (weight 5) and Soul Speed iron boots (weight 8) entries are removed. No replacement, so every other entry gets a little likelier.
   - `chests/trial_chambers/reward_ominous.json` and `reward_ominous_rare.json`: declared `"type": "minecraft:vault"`, which is what the vault rolls them as. That's what lets decision 1 tell the ominous vault from a chest without naming tables in code, and it survives nesting (the parent is a vault table too). In `reward_ominous_rare`, the Knockback/Punch/Smite/Looting/Multishot book entry becomes `minecraft:diamond` with its weight 2. The Breach/Density and Wind Burst entries stay.
   - The normal vault (`reward`, `reward_rare`) stays a chest table, so its books become diamonds like any chest's.
3. **Rule order inside the handler:** book to diamond (chest), else book to level I, else plain gear. Each step is idempotent, so a parent table re-seeing a child's drops changes nothing.
4. **Mobs: vanilla's preserved flag is the memory of what they picked up.** A `MobMixin` `@ModifyArg`s the stack passed to `spawnAtLocation` in `dropCustomDeathLoot` and `equipItemIfPossible`, with the slot captured by MixinExtras `@Local EquipmentSlot`. `SpawnedGear.asDropped(mob, slot, stack)` returns the stack unchanged when the slot is preserved, else a plain copy. The drop roll, Looting, the chance and vanilla's damage randomization are untouched. No new saved state: the drop chance already persists and already means "picked up". A slot set to chance 0 (the Wither Skeleton's sword) still never drops. `require = 2`, so a missing target fails at startup.
5. **One "plain" operation.** `LootEnchantments.plain(stack)` returns a copy with `enchantments` set to empty (which removes the patch, so the copy equals a fresh item with the same damage, name and other data), or the stack itself when it isn't enchanted. `SpawnedGear` uses it.
6. **Existing copper fixture.** `stone_pickaxe_with_data.json` is a chest table, and its test checks a swapped stone pickaxe keeps its Unbreaking. Chest gear is now plain, so the fixture's type becomes `minecraft:equipment`, the loot kind where an enchanted stone tool still turns up (mob gear). The test's claim, that the swap keeps damage and enchantments, is unchanged.

## API sketch

```java
package mapleflavored.loot;

/** Enchantments in loot: chest books become diamonds, every other book is one enchantment at level I, and gear from
 * chests, vaults, fishing and bartering is plain. */
public final class LootEnchantments {
	public static void register();               // MapleFlavored.onInitialize, after LootSubstitutions
	static ItemStack plain(ItemStack stack);     // package-private, shared with SpawnedGear
}

/** Gear a mob spawned with drops plain; gear it picked up drops as it was. */
public final class SpawnedGear {
	public static ItemStack asDropped(Mob mob, EquipmentSlot slot, ItemStack gear);  // MobMixin
}
```

Consumers: `MapleFlavored` (register), `MobMixin` (`asDropped`). Nothing else.

## Files

- `src/main/java/mapleflavored/loot/LootEnchantments.java`, `SpawnedGear.java` (new). `MapleFlavored.java` registers.
- `src/main/java/mapleflavored/mixin/MobMixin.java` (new), listed in `maple_flavored.mixins.json`.
- `src/main/resources/data/minecraft/loot_table/gameplay/fishing/treasure.json`, `gameplay/piglin_bartering.json`, `chests/trial_chambers/reward_ominous.json`, `chests/trial_chambers/reward_ominous_rare.json` (new overrides).
- `src/gametest/java/mapleflavored/gametest/BookLootGameTests.java`, `SpawnedGearGameTests.java` (new), registered in the gametest `fabric.mod.json`.
- `src/gametest/resources/data/maple_flavored_gametest/loot_table/multi_enchantment_book.json` (new fixture: a gift table with a book given Sharpness V and Smite III), `named_stone_pickaxe_in_chest.json` (new fixture: a chest table with a damaged, named, Unbreaking II stone pickaxe). `stone_pickaxe_with_data.json` retyped (decision 6).
- Ticket boxes; this plan. No glossary term (nothing new is named for players). Nothing reused, so `CREDITS.md` is unchanged.

## Acceptance matrix

`BookLootGameTests`. Each table is rolled with a fixed seed, using params built for its declared type (origin, a holder entity, and a fishing rod as the tool, each only where the type allows it). Rates use a band of 4 binomial standard deviations around the expected rate.

| Case | Expected |
| --- | --- |
| Every loaded chest, vault, fishing and barter table, 50 rolls each | no item with enchantments; every enchanted book is a vault or fishing book with one enchantment at level I; no enchanted book from a chest table |
| `chests/stronghold_library`, 500 rolls | no enchanted books; 1-diamond stacks only; of the main pool's selections (2 to 10 per chest, one stack each, so every stack but the trim template), diamonds make up the book entry's 10/52 (it has no other diamonds) |
| `chests/trial_chambers/reward_rare` (normal vault), 1000 rolls | no enchanted books; exactly-1 diamond stacks at 4/23 per roll; crossbows, bows, axes and chestplates plain |
| `chests/trial_chambers/reward_ominous_rare`, 1000 rolls | books only Breach, Density or Wind Burst, all three seen, each one enchantment at level I; book rate 4/29; 1-diamond stacks at 2/29; crossbow, diamond axe and chestplate seen and plain |
| `reward_ominous` (whole ominous vault, through the nest), 300 rolls | same book rule; some Mace book seen |
| `gameplay/fishing/treasure`, 600 rolls | books only Luck of the Sea or Lure, both seen, level I, at 1/6; bows and fishing rods seen and plain |
| `gameplay/piglin_bartering`, 2000 rolls | every barter gives exactly one stack, and the set of outputs is exactly vanilla's 16 others (no books, no iron boots, nothing added in their place) |
| Fixture `named_stone_pickaxe_in_chest` | a copper pickaxe, not enchanted, still 32 damage and named "Old Pick" |
| Fixture `multi_enchantment_book`, 40 seeds | always one enchantment at level I, either Sharpness or Smite; both seen |

`SpawnedGearGameTests` (kills are a mock player's `playerAttack`, bare-handed):

| Case | Expected |
| --- | --- |
| Zombie given a Sharpness III iron sword at the default drop chance, random seeded, killed 300 times | sword drops at about 8.5% (band as above), each dropped sword has no enchantments |
| Zombie holding a Sharpness III iron sword set to chance 1.0 (still spawned gear), killed | one iron sword, no enchantments |
| Zombie that picks up a dropped Sharpness III, Unbreaking II diamond sword item entity from the ground, killed | the sword drops matching the original exactly (enchantments and damage) |
| Zombie holding a spawned Sharpness III iron sword at chance 1.0 picks up a Netherite sword | the iron sword it threw down has no enchantments; killing it drops the Netherite sword unchanged |
| Existing `witherSkeletonsNeverDropTheirNetheriteSword` | still passes |

## Invariants

- Every rule in `LootEnchantments` is idempotent, because nested tables run it more than once on the same drops.
- Only a chest table's type turns a book into a diamond; only a vault or fishing table can hand out a non-mob book.
- A mob's preserved slot always drops exactly what it holds.

## Exclusions

- The `trade_rebalance` experiment. Maple doesn't support it (its trades name a removed curse, so the test server leaves it out), though its chests would fall under the chest rule anyway.

- Mob equipment tables (trial chamber mobs still wear enchanted gear; it drops plain).
- Structure books (18 to 23), Illager and dragon book drops (30, 46), trades (40), village chest contents (38), Pith and Crystal Hearts in chests (41).
- `Raider.pickUpItem`'s banner swap drops a captain's old head item through its own code. Raiders never spawn with an enchanted helmet, so it's left alone.
