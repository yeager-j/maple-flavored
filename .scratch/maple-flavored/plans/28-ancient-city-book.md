# Plan: 28 Ancient City book

Rewritten on 2026-11-05 after Jackson dropped the Warden summon. The book's spot, over the altar, was built under the first plan and stays.

## Context / end goal

The Ancient City's prize is Swift Sneak, and it should take stealth to earn, not a flight in and out. The Warden is a punishment for being loud, so taking the book summons nothing. Instead:
- The book is locked until the city's keepers, up to three sculk shriekers in a ring near its edge, are broken. Breaking a block makes a vibration, so a careless player can still call a Warden.
- The city keeps players Grounded until its book is taken, so no one glides in, grabs it and glides out.

Closing the ticket means every Ancient City generates with its keepers and a locked book, and Grounded ends once the book is taken.

## What the evidence says

- `docs/research/vanilla-ancient-city-sculk.md`:
  - No template holds a shrieker.
  - Each sculk patch is a `PoolElementStructurePiece` whose `FeaturePoolElement` places `minecraft:sculk_patch_ancient_city` at the piece's position. That position is fixed when the city is planned.
  - The patch runs once, when the chunk holding its origin is built. Its 50% catalyst goes at the origin.
- The existing lock machinery:
  - `BookLocks` (level saved data) holds each locked book's keys and which are done. A lock is planned from a worldgen thread and recorded on the server thread.
  - The client's locked look comes from `StructureBookBlockEntity.getUpdateTag` reading `BookLocks.isLocked`, and is resent when a lock opens.
  - A refused take sends trails to the remaining keys.
  - `StructureBookRule.LockRule` makes the keys from the pieces, but doesn't see the book's spot.
  - `StructureBookPiece` saves Brute keepers' keys so pieces built later can find them.
  - `StructureStartMixin` wraps every piece's `postProcess` for `BruteKeepers.build`.
- `LevelChunk.setBlockState` is the one place a loaded chunk's block changes: breaking, explosions, pistons, `/setblock` and fluids all pass through it. Worldgen writes go to proto chunks, not through it.
- Grounded is a status effect. `WardenSubspaces` keeps it applied with a 60-tick effect topped up below 40 ticks, skipping creative and spectator players.

## Decisions

1. **Remove the Warden summon.** Delete `AncientCityWarden`, its step in `StructureBooks.take`, the `no_room_for_warden` message and the Warden GameTests. `take` goes back to as it was, plus the new lock kind.
2. **Keeper choice** is a pure function, `ShriekerKeepers.choose(BlockPos book, List<BlockPos> origins, RandomSource random) : List<BlockPos>`, run when the city is planned, with the book planning random:
   - Distances and angles are horizontal from the book. Only origins at least 30 blocks away are eligible.
   - **First keeper:** sort the eligible origins farthest first, and pick with equal weight from the first `ceil(n / 2)`. With an odd count, the farther half includes the middle (Jackson).
   - **Second and third:** the farthest unused eligible origin within 30° of the first's direction plus 120°, then plus 240°. A window that finds none widens to 45°, then 60°, and is left empty after that. The 120° keeper is chosen first.
   - Up to three keepers, in that order. None leaves the book unlocked.
   - The origins are the positions of the city's pieces whose element is a `FeaturePoolElement` of `sculk_patch_ancient_city`, read through a new `FeaturePoolElementAccessor`.
3. **Lock.**
   - `BookLocks.Kind.SHRIEKERS` is added, with an index from each keeper's key (its patch origin) to its book, like the spawners' index.
   - `LockRule.keys` gains the book's spot: `keys(pieces, book, random)`. The existing lock rules ignore it.
   - The Ancient City's rule locks with `Keys(SHRIEKERS, choose(...))`.
   - `StructureBookPiece` saves the keepers' keys for both keeper kinds, Brutes and shriekers, under the existing `Keepers` tag.
4. **Placing keepers.**
   - `StructureStartMixin` calls a new `StructureBooks.buildPiece` that runs `BruteKeepers.build` and then `ShriekerKeepers.placeIfKeeper`.
   - After a sculk patch piece whose position is one of the book piece's keepers is built, the shrieker goes at the origin. If the patch put a catalyst there, it goes on top of the catalyst instead.
   - The shrieker has `can_summon: true` and is waterlogged if it replaces a water source. It replaces whatever was there.
5. **Breaking.** A `LevelChunkMixin` on `setBlockState`'s return calls `ShriekerKeepers.onBlockChanged(level, pos, oldState)` on the server.
   - A keeper's spot is its origin or the block above it, since a catalyst may lift it. The spot is checked as the index at `pos` or `pos.below()`, never by looking for the catalyst, so removing the catalyst first can't hide a keeper.
   - When a sculk shrieker with `can_summon: true` leaves a keeper's spot, `BookLocks` marks that key done. Player-placed shriekers have `can_summon: false`, so placing one above a keeper and breaking it doesn't count.
   - Any later block there changes nothing, and a done key stays done. This works whether or not the book's chunk is loaded.
6. **Refused take.** It shows "Locked until every keeper shrieker here is broken (%s of %s)", with trails from the book to each standing keeper's key.
7. **City Grounding.** A new `AncientCityGrounding` checks each player every level tick, skipping creative, spectator and dead players (Jackson).
   - A player inside an Ancient City's bounding box (`getStructureAt`, as Warden Territory reads it) is Grounded while that city's book is still in place.
   - "In place" means the city has a book piece and that book hasn't been taken. A city with no book Grounds nobody (Jackson).
   - Taken books are recorded in a small `TakenBooks` saved data from the `TAKEN` event, keyed by the book's position, so it works while the book's chunk is unloaded.
   - `Grounded.keepGrounded(ServerPlayer)` holds the top-up both sources share. `WardenSubspaces` uses it for Grounded, and keeps its own top-up for Darkness.
8. **Docs.** Jackson's three answers go into the spec's defaults: the farther half includes the middle, creative and spectator players aren't Grounded by the city, and a city with no book Grounds nobody. His uncommitted doc edits are committed with this work.

## API sketch

```java
public final class ShriekerKeepers {
	/** Up to three keepers from the city's sculk patch origins, by the ring rule. */
	public static List<BlockPos> choose(BlockPos book, List<BlockPos> origins, RandomSource random);
	/** Places the keeper shrieker after a keeper's sculk patch piece is built. */
	static void placeIfKeeper(StructurePiece piece, List<StructurePiece> pieces, WorldGenLevel level, BoundingBox chunkBox);
	/** Counts a keeper broken when a sculk shrieker leaves its spot. Called from LevelChunk.setBlockState. */
	public static void onBlockChanged(ServerLevel level, BlockPos pos, BlockState oldState);
}

public final class Grounded {
	/** Keeps the player Grounded a few seconds longer. Call every tick they should stay Grounded. */
	public static void keepGrounded(ServerPlayer player);
}
```

## Invariants

- Taking the city's book never spawns anything.
- The book can't be taken while any keeper is standing, and every keeper that has generated is a real shrieker.
- A broken keeper stays broken. Only a shrieker leaving the keeper spot counts.
- The city's Grounded stops for everyone once its book is taken, while a Warden Subspace's goes on.

## Tests (GameTests, `AncientCityBookGameTests`)

- **Choice** (pure): the 30-block minimum, the farther half including the middle, the 120° and 240° windows, widening to 45° and 60°, no origin used twice, and none eligible giving no keepers.
- **Spread:** in planned cities, every keeper is a patch origin at least 30 blocks from the book, there are at most three, and the second and third are within 60° of their directions.
- **Placement:** a keeper's patch built with a catalyst at its origin gets the shrieker on top, and one without gets it at the origin, both `can_summon`. Tested by building a planned keeper patch piece at a known spot.
- **Breaking:**
  - With the book's chunk unloaded, breaking each keeper counts it, and putting a shrieker back doesn't undo that.
  - A keeper on a catalyst still counts when the catalyst is removed first.
  - A player-placed shrieker over a keeper doesn't count.
  - Partial progress survives the locks being saved and reloaded, and the last break unlocks the book.
- **Refused take:** the book stays, the message counts the broken keepers and no taken event fires. With all keepers broken, the take succeeds and nothing spawns.
- **Grounded:**
  - A survival player in the city is Grounded, and a creative or spectator one isn't.
  - A city with no book piece Grounds nobody.
  - Once the book is taken, a player outside any Subspace loses Grounded after the effect's 60 ticks, while one inside a Warden's Subspace keeps it.
  - Taken status survives `TakenBooks` being saved and reloaded.
- Existing tests keep passing.
- **Hand check** (ticket gate, Jackson's), in an Ancient City:
  - the locked book over the altar
  - the trails to the keepers
  - gliding ending at the city's edge
  - Elytra working again once the book is taken
