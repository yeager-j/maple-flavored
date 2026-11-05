# Plan: 23 Bastion books and Piglin anger

## Context / end goal

The enchanting manifesto makes the Bastion's book the prize of the Nether's hardest structure: "Grabbing it angers every Piglin in the structure. Good luck." Vanilla Bastions come in four layouts and only one has a treasure room, so each layout holds its own book: Fire Aspect in the treasure room, Soul Speed on the bridge, Flame in the housing units, Thorns in the hoglin stables. This is the first structure whose book has a wake event, so it also proves the `StructureBooks.TAKEN` hook that the Ancient City, Stronghold and Mansion will use ("a structure that wakes when its book is taken listens here, finding itself from the position").

Closing the ticket means every new Bastion holds exactly one level I book matching its layout, at a fixed, visible spot, and taking it angers every loaded Piglin and Piglin Brute standing in the Bastion's pieces at the player, gold armour or not, however far away they are. A refused take (full inventory) angers nobody.

## What the evidence says (26.3 sources and templates)

- `BastionPieces.START` has four start elements, one per layout, all `single` pool elements: `bastion/treasure/big_air_full`, `bastion/bridge/starting_pieces/entrance_base`, `bastion/units/air_base`, `bastion/hoglin_stable/air_base`. A planned Bastion's first piece is a `PoolElementStructurePiece` placing one of them, so the layout is known at planning.
- Jigsaw pieces don't move after planning (rigid projection, fixed start height), so a planned spot is final.
- Layout chests, read from the templates' block NBT (`LootTable`):
  - Treasure: every `treasure/bases/centers/center_0..3` (the gold pile in the lava basin, one always placed) has 1–2 `chests/bastion_treasure` chests. No other template uses that table.
  - Bridge: `bridge/starting_pieces/entrance` (always placed from `entrance_base`) has the layout's only `chests/bastion_bridge` chest at (9, 16, 4). Vanilla's Soul Speed also comes from this table.
  - Housing units: no layout-specific table. Each `units/center_pieces/center_0..2` (one always placed, the courtyard every unit opens onto) has one `chests/bastion_other` chest.
  - Hoglin stables: `chests/bastion_hoglin_stable` appears only in `large_stables/inner_3` and `small_stables/inner_2`, which are optional. `hoglin_stable/walls/wall_base` (always placed, the only element of its pool) has two `bastion_other` chests.
- Anger: `PiglinAi.setAngerTarget` and `PiglinBruteAi.setAngerTarget` set `ANGRY_AT` (player UUID, 600 ticks) after erasing `CANT_REACH_WALK_TARGET_SINCE`; Piglins also get `UNIVERSAL_ANGER` when that game rule is on. The Piglin version only applies when `Sensor.isEntityAttackableIgnoringLineOfSight` passes, which caps range at 16 blocks. `angerNearbyPiglins` (gold chests) uses a 16-block box and only idle Piglins. A far Piglin with `ANGRY_AT` set attacks once the player is within its targeting range, for the 600 ticks. Both brains are public: `getBrain().setMemoryWithExpiry` / `eraseMemory`. A brain without a memory type ignores writes to it.
- `StructureManager.getStructureWithPieceAt(pos, structure)` finds a start from the chunk's structure references. Worldgen registers starts and references; our `Structures` fixture doesn't (the Ordinary tests' `beardAt` does it by hand with `setStartForStructure` / `addReferenceForStructure`).

## Decisions

1. **One rule per layout.** `StructureBookRules` keeps `Map<ResourceKey<Structure>, StructureBookRule> RULES` for structures with one rule and adds `BASTION_LAYOUTS: Map<Identifier, StructureBookRule>`, keyed by start template. `withBook` resolves the rule once: `ruleFor(key, pieces)` returns the Bastion layout's rule (from `StructureBookSpots.startTemplate(pieces)`) or `RULES.get(key)`. Each layout rule has a single enchantment, so `chooseEnchantment` keeps working, and `StructureBookRule` is unchanged. `enchantmentsFor(BASTION_REMNANT)` returns the four, in layout order. An unknown start template (a datapack layout) gets no book.
2. **Spots: just above the layout's chest**, like the Shipwreck, `Placement.OPEN`. The chest is the layout's landmark, the book is visible beside the loot, and a chest is never blocked by the book (it isn't a redstone conductor, as for the Shipwreck). New helper `aboveChest(pieces, templates, Predicate<Identifier> template, ResourceKey<LootTable> table)` reads chests from pool-piece templates with `filterBlocks` and the piece's rotation, as `blocksIn` does, and returns the first match's position `.above()`.
   - Treasure room: first `bastion_treasure` chest.
   - Bridge: the `bastion_bridge` chest.
   - Housing units: the chest in the `bastion/units/center_pieces/` piece.
   - Hoglin stables: the first `bastion_hoglin_stable` chest, else the first chest in `bastion/hoglin_stable/walls/wall_base`.
   No match means no book, as with every rule.
3. **Anger lives in a new `PiglinAnger` class** (package `structurebooks`, registered from `StructureBooks.register()`), listening on `TAKEN`. It looks the Bastion up with `level.structureManager().getStructureWithPieceAt(pos, bastion)` (the block is gone by then, but the start and its book piece aren't). If valid, it angers every `AbstractPiglin` (Piglins and Brutes, babies too) in `AABB.of(start.getBoundingBox())` whose `blockPosition()` is inside one of the start's pieces (`structureHasPieceAt`). So "in the structure" is the same piece test vanilla uses. Zombified Piglins aren't Piglins and are left alone.
4. **How a Piglin is angered:** mirror `setAngerTarget` without its 16-block check: erase `CANT_REACH_WALK_TARGET_SINCE`, set `ANGRY_AT` to the player for 10 minutes (12,000 ticks), and set `UNIVERSAL_ANGER` for as long when the game rule is on. This writes the brain directly, so it reaches the whole structure. Jackson's call after the hand check: vanilla's 30 seconds let the far side calm down before the player arrives. Vanilla still erases `ANGRY_AT` when its target dies.
5a. **Hunting (Jackson's follow-up):** an angry Piglin beyond 16 blocks didn't know where the player was, so it wasn't scary. Each Piglin the book angers is now *hunting*: a persistent `maple_flavored:hunting_until` attachment holds the hunt's end time, and while the hunt lasts it carries a transient +64 `FOLLOW_RANGE` modifier. Vanilla's sensors reset their shared static targeting ranges to the follow range of whichever mob's sensor ticked last, and `PathNavigation` sizes its search from follow range, so this lets the hunter sense and path to the player from across the Bastion. Mixins on `PiglinAi` and `PiglinBruteAi.findNearestValidAttackTarget` (`@ModifyReturnValue`) put the hunted player first while their anger names them and they can be attacked. That keeps a far hunter from dropping its target when a 16-range mob's sensor resets the shared range, and it's where the modifier and attachment are removed once the hunt is over. Vanilla anger without the attachment is unchanged.
5. **Fixture:** `Structures.register(level, start)` sets the start in its chunk and adds a reference in every chunk it covers, as worldgen does. Bastion tests call it after `place`. Existing tests are untouched.

## API sketch

```java
// StructureBookRules
public static List<ResourceKey<Enchantment>> enchantmentsFor(ResourceKey<Structure> structure); // Bastion: the four
private static Optional<StructureBookRule> ruleFor(Holder<Structure> structure, List<StructurePiece> pieces);

// StructureBookSpots (package-private)
static Optional<Identifier> startTemplate(List<StructurePiece> pieces);
static Optional<BookSpot> bastionTreasureRoom(pieces, planning);
static Optional<BookSpot> bastionBridge(pieces, planning);
static Optional<BookSpot> bastionHousingUnits(pieces, planning);
static Optional<BookSpot> bastionHoglinStables(pieces, planning);

// PiglinAnger (package-private)
static void register(); // TAKEN listener
```

Consumers: `StructureMixin` → `withBook` (unchanged signature). Tests observe anger through each Piglin's `ANGRY_AT` memory.

## Files

- `structurebooks/StructureBookRules.java`: layout rules, `ruleFor`, `enchantmentsFor`.
- `structurebooks/StructureBookSpots.java`: Bastion spots, `startTemplate`, `aboveChest`; class doc mentions the Bastion.
- New `structurebooks/PiglinAnger.java`; `StructureBooks.register()` calls it.
- GameTests: new `BastionBookGameTests`, registered in the gametest `fabric.mod.json`; `Structures.register`.
- Docs: ticket 23 boxes; glossary/spec only if wording changes (the spec already says this). Nothing reused, so `CREDITS.md` is unchanged.

## Acceptance matrix (`BastionBookGameTests`)

Bastions are planned at far chunks (row at chunk 6000) and scanned until the wanted layout turns up, since the GameTest seed isn't fixed.

| Case | Expected |
| --- | --- |
| `enchantmentsFor(BASTION_REMNANT)` | Fire Aspect, Soul Speed, Flame, Thorns |
| Each layout (4 tests): place, register, find books | exactly 1 book, holding the layout's enchantment, with a chest right below |
| Same 4 tests: spawn a Piglin inside a piece more than 16 blocks from the book, a Brute inside, a Piglin outside the bounding box and one inside the box but in no piece; survival player in gold armour takes the book | taken; every `AbstractPiglin` inside the pieces (the template's own and ours, at least 1 of the template's) has `ANGRY_AT` = player; the two outsiders have none |
| An adult Piglin beside a gold-wearing player, its AI ticked by hand | no attack target before the take; the player once the book is taken |
| 80 Bastions planned | each has exactly 1 book piece, holding its layout's enchantment |
| Hoglin stables re-planned without the two stable templates that have a chest | 1 Thorns book, above the base wall's chest |
| Full inventory | refused; no Piglin angered |
| A hand-set book outside any Bastion, Piglin beside it | taken; Piglin not angered |
| Existing GameTests | still pass |

## Invariants

- One book per Bastion, its enchantment fixed by the layout.
- Anger happens only on a successful take, only for the Bastion whose piece holds the book.

## Verification

`JAVA_HOME=C:/Users/jackson/.jdks/jdk-25.0.4.1+1`, `gradlew build`, `gradlew runGameTest`, check `build/gametest/report.xml`. Then `larp review`, fix, commit on `23-bastion-books-and-piglin-anger`, push, open a ready PR, watch Codex with `/watch-prs`.

## Hand checks (left unchecked)

Each layout's book is easy to see next to its chest; the angered Bastion feels like a fight.

## Exclusions

Piglins in unloaded chunks (they aren't in the world to anger), Hoglins, Blood Moon restores, other structures' wake events.

## Addendum: the Brute lock (Jackson, 2026-11-04)

Taking the book needs a fight first: each Bastion's book is locked until three of its Piglin Brutes, its **keepers**, chosen when the Bastion is planned, are gone. Any death counts (lava, falling, a player or not), so sneaking and luring is fine, and natural deaths count rather than leaving the book stuck. A refused take sends trails to each remaining keeper, like the Fortress's, and makes them glow for 60 ticks, the bell's raider glow. Once open, taking the book still sets the rest of the Bastion hunting.

Evidence:
- Brutes come only from two single-mob templates, `bastion/mobs/melee_piglin` and `melee_piglin_always`, each its own planned `PoolElementStructurePiece`. 300 plans: treasure room 11–21 Brute pieces, housing units 4–16, stables 3–15, bridge 1–14 (fewer than 3 in 3 of 70).
- Jigsaw pieces place their template mobs with `finalizeEntities`, calling `PiglinBrute.finalizeSpawn(..., STRUCTURE, null)` before adding them. `StructureStart.placeInChunk` builds each piece with `postProcess`, with the whole start at hand, and the book piece is its last piece.
- `Entity.RemovalReason`: `KILLED` and `DISCARDED` destroy the entity (death of any kind, conversion, Peaceful's monster removal, `/kill`); unloading and `CHANGED_DIMENSION` don't.
- `BellBlockEntity` glows raiders with `MobEffects.GLOWING` for 60 ticks.

Decisions:
1. **Keys, not just spawners.** `BookLocks.Lock(book, kind, keys, done)`, with `Kind.SPAWNERS` (Fortress: Blaze spawners Cleared) or `Kind.BRUTES` (Bastion: keepers gone). Saved field names stay `spawners`/`cleared`, with an optional `kind` defaulting to spawners, so saved Fortress locks still load. `remainingSpawners` becomes `remainingKeys`. Cleared events only count toward spawner locks.
2. **Choosing keepers.** `StructureBookRule.SpawnerLock` becomes `LockRule.keys(pieces, random) -> BookLocks.Keys`. Each Bastion layout rule locks with `StructureBookSpots.bastionKeepers`: the origins of the Brute pieces, 3 picked with the book's planning random (all of them if fewer, none means unlocked). A Brute piece's origin is its keeper's key.
3. **Tagging at placement.** The book piece saves its keepers' keys. New `StructureStartMixin` wraps each piece's `postProcess` in `placeInChunk`: when the start's last piece is a book with keepers and this piece is a Brute piece whose origin is one, `BruteKeepers` holds a thread-local `Keeper(GlobalPos book, BlockPos key)` around the call. New `PiglinBruteMixin` at the head of `finalizeSpawn` with reason `STRUCTURE` tags the Brute with that keeper as a persistent attachment.
4. **Counting the dead.** `ServerEntityEvents.ENTITY_UNLOAD`: a keeper whose removal reason destroys it marks its key done in the book's level's `BookLocks`, as the Cleared event does, and updates a loaded book's clients.
5. **Refused take.** For a Brute lock: "Locked until every keeper Brute here is dead (n of m)". Each remaining keeper loaded in the book's level glows for 60 ticks and gets trails from the book; a keeper that isn't loaded gets trails to its spawn spot. The locked look is the Fortress's.
6. **Peaceful opens Brute locks for good** (from the plan review; Jackson confirmed). Peaceful never creates keeper Brutes and drops saved ones as their chunk loads, both without a removal event, so `BookLocks` opens every Brute lock in a Peaceful level each tick, and the done keys stay done after the difficulty changes back.

Acceptance additions:
| Case | Expected |
| --- | --- |
| 80 plans | each Bastion's lock lists min(3, Brute pieces) keys, each a Brute piece's origin |
| Placed Bastion | each key has one tagged, living keeper Brute; the book is sent locked |
| Locked take | refused with nothing changed and no hunt; remaining keepers glow for 60 ticks |
| Keepers killed, discarded, burnt; a non-keeper killed; a keeper moved to another dimension | only the keepers' deaths count; moving doesn't; the last death unlocks and sends the book unlocked |
| Unlocked take | taken, and the Bastion hunts as before |
| Fortress tests | unchanged behaviour on the renamed API |
