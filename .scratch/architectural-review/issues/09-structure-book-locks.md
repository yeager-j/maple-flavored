# 09: Structure book locks

**What to fix:** The architecture findings for keeping a structure's book locked, the consequences of taking it, Ancient City grounding and the Echo Shard's shrieker pulse. Each finding stands alone unless it says otherwise.

**Blocked by:** None

**Status:** done

**Priority:** P2 (Medium)

**Estimate:** M

**Source:** Architectural Review 11/9, the structure book locks reviewer, with one finding merged from the alloys reviewer. Nothing was built or run.

What held up: the mixins in the slice are thin, `ShriekerKeepers.choose` is a pure and directly tested rule, `MobKeepers` handles death, conversion, discard and dimension change correctly, and `ShriekerPulses` is a deep module whose range is already a game rule.

Other tickets hold findings from this slice:

- Ticket 01 holds its P1.
- Ticket 08 finding 1 covers keeper identity being discarded at planning, and finding 2 covers a book's position as its identity.
- Ticket 10 finding 1 covers `BookLocks.Kind`'s throwing codec.
- Ticket 11 finding 6 covers the Ancient City bounds written twice.
- Ticket 04 finding 9 covers the playtest values compiled in.

Paths: `sb/` is `src/main/java/mapleflavored/structurebooks/`, `al/` is `src/main/java/mapleflavored/alloys/`, and `test/` is `src/gametest/java/mapleflavored/gametest/`.

## Findings

### 1. A lock kind's behaviour is decided in about ten places (P2)

**Principle:** Single Choice (Meyer).

`BookLocks.Kind` is consulted throughout core logic, and its three classifiers are defined by negation, so a fifth kind silently becomes a "block" and a "keeper".

- The predicates: `isBlock = !isMob`, `isMob = BRUTES || EVOKERS`, `isKeeper = != SPAWNERS` (`sb/BookLocks.java:243-258`).
- Kind-guarded entry points: `sb/BookLocks.java:101`, `:121`, `:181`, `:194`, `:223`.
- Presentation: two switches in `sb/StructureBooks.java:178-183` and `:187-190`. The second is a switch statement, so the compiler doesn't check it is exhaustive; a new kind would show no trails.
- Elsewhere: `sb/StructureBookRules.java:160`, `sb/ShriekerKeepers.java:151`, `sb/KeepersCommand.java:76`.
- `BRUTES` and `EVOKERS` behave identically inside `BookLocks`; they differ only by refusal message.

The reviewer's direction: let each kind carry its own answers, including how a key completes, whether Peaceful opens it, its message, how remaining keys are pointed out, and whether the world can confirm a key is done. Ticket 01 needs the last one.

### 2. The hunt rule is written twice, and an Illager hunt's deadline is stored twice (P2)

**Principle:** DRY as shared knowledge, not shared shape. One authority.

`PiglinAnger` and `IllagerAlert` each implement the same hunt: the duration, the death rule and the range are one decision. The targeting mechanics do differ (brain memory versus goal) and should stay separate.

- `HUNT_TICKS` and `HUNT_RANGE_BONUS`: `sb/PiglinAnger.java:41-46`, `sb/IllagerAlert.java:46-50`. `sb/IllagerAlert.java:45` says "As long as a Bastion's hunt"; nothing enforces it.
- The `Hunt` record and `HuntStatus`: `sb/PiglinAnger.java:136-168`, `sb/IllagerAlert.java:178-202`.
- `deaths` and `keepHuntRange`: `sb/PiglinAnger.java:87-101`, `sb/IllagerAlert.java:120-134`.
- The structure sweep: `sb/PiglinAnger.java:103-113`, `sb/IllagerAlert.java:71-81`.
- Which structures wake is a third table (`sb/IllagerAlert.java:43`, `sb/PiglinAnger.java:104`) beside `StructureBookRules`.
- The records differ only because `PiglinAnger.Hunt.quarryDeaths` is optional for hunts "saved before this was recorded" (`sb/PiglinAnger.java:133-140`), which is save compatibility for a ten-minute attachment.

The deadline has two stores. `sb/IllagerAlert.java:84-85` writes the same end time into `IllagerNeutrality`'s anger map (`al/IllagerNeutrality.java:32-35`, `:100-107`) and its own hunt attachment. `calm` exists only so `sb/IllagerAlert.java:106` can keep the copy in step (`al/IllagerNeutrality.java:110-116`). A test named for a book alert calls `IllagerNeutrality.anger` directly (`test/AlloyCombatGameTests.java:204-215`).

### 3. `standingSpot` conflates "not loaded" with "not standing" (P3)

**Principle:** Empty and absent conflated. Parse, don't validate.

`ShriekerKeepers.standingSpot` returns empty for both cases (`sb/ShriekerKeepers.java:98-104`), so `KeepersCommand.status` repeats the chunk check to tell them apart (`sb/KeepersCommand.java:84`). It also tests `lock.done().contains(key)` twice (`:58`, `:81`). Ticket 01's reconciliation needs the same keeper status, computed once.

### 4. "No lock" is modelled as an empty spawner lock (P3)

**Principle:** Empty and absent conflated. Names tell the truth.

`Keys.NONE` is `new Keys(Kind.SPAWNERS, List.of())` (`sb/BookLocks.java:268`), and `plan` drops empty keys (`:86`). "Nothing locks this structure" and "this keeper-locked structure found no keepers" are the same value, with a placeholder kind. An Ancient City with no patch origin 30 blocks out (`sb/ShriekerKeepers.java:73`) gets a freely takeable book while still Grounding players, and so does a Bastion with no Brute piece. This is documented as intended, but it is invisible in data and to `/keepers`.

### 5. Lock tests share state by convention and have gaps (P3)

**Principle:** Isolate tests by construction. A test that has always been green.

- Every `Structures.plan` leaks a lock into the shared level through the static queue. Tests keep apart by bare row numbers across files (`test/BastionBookGameTests.java:120-135`; `test/BastionLockGameTests.java:93`, `:131`, `:175`, `:237`), and two tests work around the leak (`test/FortressBookGameTests.java:262-263`, `test/BastionLockGameTests.java:218-219`).
- `sentLocked`, `takes`, the `TAKES` listener and the lock reload exist in up to three files, all reaching through `BookLocks.TYPE.codec()`.
- `aCityWithoutABookGroundsNobody` never shows the bookless city is findable at the player, so it passes if registration failed (`test/AncientCityBookGameTests.java:428-443`).
- Untested: a keeper that only unloads still counts as alive (`sb/MobKeepers.java:26-27`), and `PiglinAnger`'s clock expiry (`sb/PiglinAnger.java:159`); tests erase the memory instead (`test/BastionBookGameTests.java:225`).

### 6. Smaller `BookLocks` items (P3)

- `/keepers` lists only Ancient City keepers (`sb/KeepersCommand.java:23-25`, `:76`), though Keeper also covers Brutes and Evokers.
- `recordPlanned` re-enters itself through `markKeeperGone` and `of` (`sb/BookLocks.java:167-171`, `:98`, `:147`).
- `ShriekerKeepers.choose` compares `Math.hypot` and `atan2` results at exact thresholds that feed a seeded pick (`sb/ShriekerKeepers.java:67-76`, `:193-200`). An integer squared distance would be exact.

## Decisions

Claude made these while implementing; Jackson has not reviewed them.

- **Findings against `main` after tickets 01, 08 and 13.** Finding 1 held, with its sites moved: ticket 08 had put the piece-side checks in `StructureBookPiece`. Findings 2, 3 and 4 held as written. Finding 5 held in a changed form: ticket 03's `TestSites` had already replaced the bare row numbers, and the helpers and the two workarounds were still there. Finding 6 held as written.
- **A kind's answers come from its key form.** `BookLocks.Kind` takes a `KeyForm` (`STRUCTURE_BLOCK`, `KEEPER_BLOCK`, `KEEPER_MOB`) and its refusal message, and implements its own world check. Whether keys are keepers, are found by position, open in Peaceful or are known done only by the lock are exhaustive switches over the form, so `BRUTES` and `EVOKERS` share one set of answers.
- **A lock with no keys is recorded.** `Keys.NONE` is gone. A book piece holds an `Optional<Keys>`: empty when nothing locks the book, and present with no positions when its structure looked for keys and found none. The second is saved with the piece, recorded in `BookLocks` as a lock that never locks, and reported by `/keepers`. Nothing changes in play. A piece with any lock stays at its planned spot, where before only one with at least one key did; the four structures with locks never settle.
- **`/keepers` lists Brutes and Evokers.** A keeper mob is alive, gone for good, or not loaded, in which case the line gives where it was placed. The command's messages no longer say "Ancient City".
- **The hunt rule is `structurebooks/Hunt.java`.** It owns the ten minutes, the death rule, the range bonus and the sweep of a structure's mobs. `PiglinAnger` keeps the Piglin's anger memory and `IllagerAlert` keeps its target goal. Which structures wake is still a constant in each of those two.
- **`IllagerNeutrality` asks `IllagerAlert` whether a hunt is on.** `calm` and the `anger` overload that took a duration are removed with their test. `alloys` now imports `structurebooks`, and `structurebooks` no longer imports `alloys`.
- **Legacy save forms are removed, under Jackson's 2026-11-10 decision recorded in ticket 08.** `BookLocks.Lock` saves `kind`, `keys` and `done`; the old `spawners` and `cleared` names and the default kind are gone with their test. A hunt's `quarry_deaths` is required. A world saved before this loses its recorded locks, which are planned again from each book's structure; a keeper mob killed before then is not counted.
- **Keeper directions use `StrictMath`.** Distances are compared as whole blocks squared. Directions still need `atan2`, so they go through `StrictMath`, which gives every platform the same result for the seeded pick.

## Checklist

- [x] 1. Each lock kind carries its own answers, no predicate is defined by negation, and adding a kind fails to compile until every answer is given.
- [x] 2. One hunt rule owns the duration, the death rule and the range. `IllagerNeutrality` asks whether a hunt is on and stores no deadline of its own.
- [x] 3. `ShriekerKeepers` returns one keeper status (standing, broken, unloaded, missing), and `KeepersCommand` reads it without its own chunk check.
- [x] 4. "No lock" and "a lock that found no keepers" are different values, and `/keepers` can show the second.
- [x] 5. Lock tests can't see each other's locks, the lock helpers exist once, `aCityWithoutABookGroundsNobody` has a control, and the unloaded-keeper and clock-expiry cases are tested.
- [x] 6. `/keepers` covers every Keeper, `recordPlanned` doesn't re-enter itself, and `choose` compares integer distances.
