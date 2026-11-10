# 08: Structure book placement

**What to fix:** The architecture findings for deciding where each structure's book goes and placing it at generation. Each finding stands alone unless it says otherwise.

**Blocked by:** None

**Status:** done

**Priority:** P2 (Medium)

**Estimate:** L

**Source:** Architectural Review 11/9. Findings 1 and 2 were raised by the placement, locks and strongholds reviewers together. Nothing was built or run, and the placement reviewer had no vanilla source to check against, so claims about vanilla are marked.

What held up: `StructureBookRules` is the single table for which structure holds which enchantment, saving the book as a structure piece is sound and properly tested, and the shared `StructureStartMixin` and `StructurePieceMixin` carry three features acceptably.

Related: ticket 01 holds this slice's P1, and its fix overlaps finding 1 here.

Paths: `sb/` is `src/main/java/mapleflavored/structurebooks/`, `st/` is `src/main/java/mapleflavored/strongholds/`, `mixin/` is `src/main/java/mapleflavored/mixin/`, and `test/` is `src/gametest/java/mapleflavored/gametest/`.

## Findings

### 1. A lock's kind and keepers are decided at planning, discarded, and re-derived at build (P2)

**Principle:** Single Choice (Meyer). Parse, don't validate (King). Information hiding (Parnas).

Planning resolves a structure to a rule that returns `BookLocks.Keys(kind, positions)`. `withBook` then saves only the positions on the piece. At build time nothing knows the kind, so three places probe piece types to ask "does this piece place one of the book's keepers?".

One reviewer rated this P1 because of the silent permanent lock below. Two rated it P2 because `theBookIsLockedUntilEveryEvokerIsDead` guards today's behaviour. It is filed as P2.

- **Three derivations.** `StructureBookSpots.keeperKey` tries a Bastion Brute template and otherwise falls through to `StrongholdIllagers.keeperKey` (`sb/StructureBookSpots.java:291-314`). `StrongholdIllagers.placeGuards` decides the same question again for the same piece (`st/StrongholdIllagers.java:153-155`). `ShriekerKeepers.placeIfKeeper` decides it a third way for the Ancient City (`sb/ShriekerKeepers.java:120-125`).
- **A silent permanent lock.** The Stronghold Evoker is spawned by the mod's own code, yet it is tagged through the `MobKeepers.PLACING` thread-local (`sb/MobKeepers.java:36`, `:109-113`). That is set only because `mixin/StructureStartMixin.java:62-72` nests `placeGuards` inside the `Runnable` passed to `StructureBooks.buildPiece`. If the two keeper decisions disagree, or `placeGuards` runs outside that scope, `tagIfPlacing` and `forgiveIfPlacing` both do nothing (`sb/MobKeepers.java:75-89`) and the key can never be done. The ordering rule is a comment (`st/StrongholdIllagers.java:146`).
- **"The book is the last piece" is decoded at every consumer.** `withBook` appends the book last (`sb/StructureBookRules.java:165`). Four production sites re-encode that with `pieces.getLast() instanceof StructureBookPiece`: `sb/MobKeepers.java:110`, `sb/ShriekerKeepers.java:121`, `sb/AncientCityGrounding.java:46` and `st/StrongholdIllagers.java:153-154`. `linkAnchors` scans every piece instead (`sb/StructureBookPiece.java:114-122`). Four test files carry their own `bookPiece` helper.
- **A package cycle.** `structurebooks` imports `strongholds` (`sb/StructureBookRules.java:67`, `sb/StructureBookSpots.java:311`), and `strongholds` imports `structurebooks` (`st/StrongholdIllagers.java:13-14`).
- **"Up to three keepers" is encoded three ways:** `BASTION_KEEPERS = 3`, `FAR_KEEPERS = 2` plus the portal room, and `TURNS` plus the first pick. None can be a playtest game rule, because planning has no level.
- **The key scheme is private.** "Key = piece origin" is private to `StrongholdIllagers`, and tests recompute it four times (`test/StrongholdGameTests.java:150`, `:174`, `:189`, `:407`, `:503-507`, `:582`).

More citations: `sb/StructureBookRules.java:152-163` (the kind is dropped at `:160`); `sb/StructureBooks.java:131-134`, `:175-213`; `sb/StructureBookPiece.java:47`, `:58`, `:110`, `:159`; `st/StrongholdIllagers.java:138-142`, `:170`, `:316`.

### 2. A book's position is its identity, and three derivations of it must coincide (P2)

**Principle:** One authority with explicit derivations. Names tell the truth.

The book's position exists as the piece's bounding box (the planned spot), as `spot()` (the anchor's current box plus an offset), and as the placed block's position. These are used interchangeably as keys:

- `BookLocks.plan` is keyed by the planned position (`sb/StructureBookRules.java:163`).
- `StructureBooks.take` looks the lock up by block position (`sb/StructureBooks.java:153`).
- `MobKeepers` stamps keepers with `spot()` as it reads while their piece builds (`sb/MobKeepers.java:112`).
- `AncientCityGrounding` asks `TakenBooks` about `spot()` (`sb/AncientCityGrounding.java:46`), while takes are recorded by block position (`sb/StructureBookBlockEntity.java:72`; `sb/TakenBooks.java:37`, `:42-46`).

This works only because the four locked or Grounding structures never settle onto terrain. Nothing states or enforces that. If a settling structure ever gets a lock, the lock silently never applies.

`spot()` is also time-dependent, though its doc says "where the book is" (`sb/StructureBookPiece.java:38-40`, `:77-80`, `:124-129`, `:144-150`). Not verified: the reviewer recalls that a reloaded Igloo recomputes its box, which would make `spot()` return the planned height again.

### 3. `TakenBooks` records the take, but its only reader asks whether the book is still in place (P3)

**Principle:** One authority. Model only what is read.

The authority for "the book is in place" is the block. `TakenBooks` is updated on only one of the ways the block can stop being there. A creative break, a `/setblock`, or a book that was planned but never placed leaves an Ancient City Grounding forever with nothing to take. Shriekers already get any-cause treatment through `LevelChunkMixin`; the book does not. The class doc also says "every structure book taken", but chest, gravel and spawner books never appear in it.

- `sb/TakenBooks.java:13-16`, `:33-46`
- `sb/StructureBookPiece.java:176-182`
- `mixin/LevelChunkMixin.java:20-25`
- `sb/AncientCityGrounding.java:44-47`

### 4. The Fortress retry mixin holds its own rule and duplicates what a Blaze spawner room is (P3)

**Principle:** DRY. Thin mixins.

`WoodlandMansionPiecesMixin` delegates its rule to `MansionPortalRoom.make`. `NetherFortressStructureMixin` implements `hasBlazeSpawner` inline as "a `MonsterThrone` piece", and `StructureBookSpots.fortressBlazeSpawners` states the same fact independently. `StructureBookSpots` is package-private, so the mixin couldn't call it.

- `mixin/NetherFortressStructureMixin.java:23-37`
- `sb/StructureBookSpots.java:48`, `:243-248`
- In contrast: `mixin/WoodlandMansionPiecesMixin.java:36-42`, `sb/MansionPortalRoom.java:33`

### 5. One Buried Treasure book costs a global change to every loot chest's persistence (P3)

**Principle:** Proportion. Prefer established primitives.

Placing a book in a chest needs `putInChest`, `BaseContainerBlockEntityInvoker` and `ChestBlockEntityMixin`, and the mixin changes save and load for every unrolled loot chest in the game. The mod already grants guaranteed books from loot tables, and a pool in `chests/buried_treasure` would give the same outcome. The trade-off is that the entry would leave the `StructureBookRules` table, so this needs a decision.

- `mixin/ChestBlockEntityMixin.java:24-41`, `mixin/BaseContainerBlockEntityInvoker.java`
- `sb/StructureBookPiece.java:206-215`
- `sb/StructureBookRules.java:69-73`

### 6. The Bastion special case is branched twice (P3)

**Principle:** Single Choice.

`structure.equals(BASTION_REMNANT)` appears in both `enchantmentsFor` and `ruleFor`, because the rule map can't express a layout-dependent rule.

- `sb/StructureBookRules.java:80-81`, `:111-117`, `:169-175`

### 7. Evidence is discarded and re-checked, or never required (P3)

**Principle:** Parse, don't validate.

- `ancientCity` validates through `startTemplate`, then re-casts `pieces.getFirst()` (`sb/StructureBookSpots.java:226-230`, `:251-257`).
- The `StructureBookPiece` constructor takes both `anchorIndex` and `anchor`, one fact passed twice (`sb/StructureBookPiece.java:69-83`, `:136-142`).
- A spot rule and its `Placement` are separate arguments that must agree; `buriedTreasure` paired with `OPEN` would replace the chest (`sb/StructureBookRules.java:69-78`; `sb/StructureBookPiece.java:197-204`).
- A structure book block with no enchantment is representable. It is unbreakable, and `take` refuses it with no message (`sb/StructureBookBlockEntity.java:25`, `:52`; `sb/StructureBooks.java:147-151`).
- The `keepers()` Javadoc omits the Stronghold.

### 8. Tests reach around the interface, and one oracle is the code under test (P3)

**Principle:** Let the interface be the test surface. Negative controls.

- Four test files hand-assemble what `mixin/StructureMixin.java:48-50` does: a five-argument `TerrainPreview.of` and a seven-argument `withBook` (`test/OrdinaryStructureBookGameTests.java:350-368`, `:378-387`; `test/MansionBookGameTests.java:95`, `:113-114`, `:252-263`; `test/BastionBookGameTests.java:278-282`; `test/FortressBookGameTests.java:143-147`).
- `everyMineshaftHoldsExactlyOneBookInAHallway` checks the book's coordinate against production's `MineshaftBookSpots.middleSupport`; its only independent check is `openSides >= 2` (`test/StructureBookGameTests.java:96-99`).
- `StructureBookPiece.floating` is public production API used by one Stronghold test (`sb/StructureBookPiece.java:91-99`; `test/StrongholdGameTests.java:585`).
- `onlyBook`, `bookPiece`, `assertCount` and `enchantmentAt` are duplicated across files, and `MansionBookGameTests` borrows statics from `BastionBookGameTests` (`test/OrdinaryStructureBookGameTests.java:411-431`, `:446`; `test/BuriedStructureBookGameTests.java:285-298`; `test/StructureBookGameTests.java:462`).

### 9. Legacy save formats are carried and tested in an unreleased mod (P3)

**Principle:** Proportion.

The mod is at `0.1.0` (`gradle.properties:16`) and is intended for new worlds. `StructureBookPiece` still keeps three legacy paths, pinned by a dedicated test: the pre-`Placement` `Pocket` flag, a no-anchor path, and a default-to-`POCKET` fallback. Separately, `placement.name()` makes the enum's constant names the save format.

- `sb/StructureBookPiece.java:48-51`, `:101-111`, `:118-120`, `:157`
- `test/StructureBookGameTests.java:418-456`

## Decisions

Claude made these while implementing; Jackson has not reviewed them.

- **`structurebooks` no longer imports `strongholds`.** The rule table still names the Stronghold's lock, but the choice of keeper rooms is handed to it at mod init (`StrongholdIllagers.register`), and the table turns the rooms into keys with `MobKeepers.roomKey`. Until that call, a Stronghold's book is planned unlocked.
- **A keeper this mod spawns is tagged with an explicit call.** `MobKeepers.keeperPlacedBy` says which keeper a piece places, and the Stronghold tags or forgives its Evoker with the answer. The thread-local is left only for a Bastion's Brutes, which the game spawns from a template.
- **A locked book stays where it was planned.** It doesn't follow its anchor, so its planned spot, its block and its lock's key are one position by construction. If a settling structure is ever given a lock, the book is visibly left behind and an error is logged when it's built, where before it was silently never locked. `spot()` documents when it is the book's block.
- **The Igloo reload case is real.** An Igloo saves where it was planned, so a built Igloo loaded again reads its book's `spot()` at the planned height. Shipwrecks, Ocean Ruins and temples save where they settled. Building is unaffected, since it always runs after the anchor settles. A GameTest pins this.
- **The block says whether an Ancient City's book is gone.** `TakenBooks` is replaced by `GoneBooks`, which reads the block when its chunk is loaded and remembers what it says for when it isn't, so a player at the city's edge with the centre unloaded still gets the right answer. A book is also remembered gone the moment its block leaves a loaded chunk, through `LevelChunkMixin`. The saved data's id changed from `taken_books` to `gone_books`.
- **A book block can't always hold an enchantment**, because the game makes the block entity before giving it one. A blank book, as `/setblock` with no data places, now crumbles with a message when a player tries to take it, where before it was unbreakable and refused silently.
- **Legacy save paths are removed (Jackson, 2026-11-10: "Legacy saves aren't an issue. The mod hasn't been released yet.").** `StructureBookPiece` no longer reads the `Pocket` flag, a missing anchor, a missing placement or the kind-less `Keepers` list, and their test is gone. A placement is saved by an explicit name, not its constant's. A book piece saved in an older format now fails to load, so its structure loses its book. Left for ticket 09, which owns those files: `BookLocks.Lock.CODEC` still reads the old `spawners` and `cleared` field names with a default kind, and `PiglinAnger`'s hunt still reads an optional `quarry_deaths`.
- **The Buried Treasure book comes from a loot pool (Jackson, 2026-11-10).** `loot/ChestBooks` adds a pool to `chests/buried_treasure` that gives one book, Aqua Affinity or Respiration at even odds, and `ChestBlockEntityMixin`, `BaseContainerBlockEntityInvoker` and the `IN_CHEST` placement are removed. The chest's loot seed is drawn from the worldgen random when the treasure generates, so the world seed still decides the book. `LootEnchantments` turns every other chest-table book into a diamond, so it keeps the books `ChestBooks` gives, as it keeps the ominous vault's Mace books. Buried Treasure is no longer in `StructureBookRules`, and plans no book piece.
- **"Up to three keepers" is still encoded three ways.** No checklist item covers it, and planning has no level to read a game rule from.

## Checklist

- [x] 1. (The book piece saves the lock's kind, and `StructureBookPiece.among` is the lookup: done with ticket 01. The rest is open.) The book piece saves the lock's kind, one `StructureBookPiece` lookup over a piece list replaces every `getLast()` check, the Stronghold tags its keeper Evoker with an explicit call, and `structurebooks` no longer imports `strongholds`.
- [x] 2. A book has an identity that doesn't move, or `withBook` refuses a lock rule on an anchor that settles and `spot()` documents when it is valid. The Igloo reload case is checked.
- [x] 3. An Ancient City stops Grounding when its book is gone for any reason, including one that was never placed.
- [x] 4. One predicate says what a Blaze spawner room is, and the Fortress mixin and the lock's key list both use it.
- [x] 5. Jackson has decided whether the Buried Treasure book moves to a loot pool; if it does, `ChestBlockEntityMixin` and the invoker are removed.
- [x] 6. The rule map resolves a layout-dependent rule, and `BASTION_REMNANT` is named once.
- [x] 7. The piece constructor takes each fact once, a spot rule carries its own placement, and a book block always has an enchantment.
- [x] 8. Tests plan a book through one entry point, the mineshaft test has an oracle independent of production, and `floating` is not public production API.
- [x] 9. Jackson has decided whether any playtest world needs the legacy paths; if none does, they and their test are removed.
