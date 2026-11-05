# Plan: 28 Ancient City book

## Context / end goal

Every structure book is a prize with a cost. The Ancient City's is Swift Sneak, the enchantment that exists for sneaking past Wardens, and the cost is the Warden itself: taking the book always summons one, angry at the taker. "Always" is the point. A player must never be able to take the book quietly, so if there is nowhere for the Warden to appear, the book stays put. Closing the ticket means the city's book exists in every Ancient City, in sight in front of the portal, and taking it is one action that either gives the book and an angry Warden, or nothing.

## What the evidence says (26.3 sources and templates)

- The start pool `ancient_city/city_center` holds three templates, `city_center_1..3`, each 18×31×41, with the same footprint. The portal frame (reinforced deepslate) stands in the plane x=13, from z=10 to 31 and y=17 to 24, on a pillar (x 11–15, y 11–16) over a soul fire trench (x 12–14, y 10). The plaza in front of it, on the −x side, has its floor top at y=9.
- The three centres differ in front of the portal: centre 2 has a sunken pit with a chest and stairs at z 17–22, so no spot on the centre line has a full floor in all three. The block column x=10, z=23 does: a full deepslate brick floor at y=9, and y 10–12 empty in all three (beard_box clears the piece's empty cells).
- `SpawnUtil.trySpawnMob` with `spawnRangeXZ = 0` and `spawnRangeY = 0` checks exactly one spot: the block below must have a full top face (`ON_TOP_OF_COLLIDER`), the spot itself no collision, the world border, then `Warden.checkSpawnObstruction` (no liquid, no `blocksBuilding` entity such as a player, and `noCollision` for the full 0.9×2.9 box). The structure book has no collision, so a Warden can rise through it.
- `Warden.increaseAngerAt(entity, amount, playSound)` is public; anger is clamped at 150 and drops by 1 a second; 80 is angry. It does nothing for an entity the Warden can't target (creative, spectator).
- `WardenSummoning.spawnNear(level, player)` (ticket 27) is the summoning search without the shrieker's warning-level and nearby-Warden checks. A spawned Warden opens its Subspace through ticket 25's `ENTITY_LOAD` hook.

## Decisions

1. **Spot.** `StructureBookSpots.ancientCity`: the start piece, if it places one of the three city centres, with the book at local (10, 11, 23) through `inPoolElement`, so rotation is handled. That's head height above the plaza floor directly in front of the portal, 2.5 blocks off its centre line. Placement is `OPEN`. A city whose start isn't a known centre (a datapack's) gets no book.
2. **The book's spot for a Warden** is the block under the book, where a Warden stands on the plaza floor with the book at its chest. Checked and spawned in one call, `SpawnUtil.trySpawnMob(WARDEN, TRIGGERED, level, book.below(), 1, 0, 0, ON_TOP_OF_COLLIDER, false)`, so it's rechecked at the moment of taking, with vanilla's emerging animation.
3. **Taking.** `StructureBooks.take` gains one step, after the lock and inventory checks (so a refused take for those spawns nothing) and before the book is given: if the book is in an Ancient City (`getStructureWithPieceAt(pos, ANCIENT_CITY)`, as the Bastion finds itself), summon its Warden: `spawnNear(level, player)`, else the book's spot. If neither has room, the player gets the overlay message "No room here for the book's Warden" and nothing changes. Otherwise the take goes on as before, firing `TAKEN`.
4. **Angry.** The Warden gets 150 anger at the player, the most a Warden holds, so it's still angry when it has emerged and roared, about 70 seconds in all, and vibrations keep it going from there. Not a game rule: the spec only says "angry".
5. **Module.** A new `structurebooks.AncientCityWarden` holds the Ancient City's wake: whether a book is the city's, and summoning its Warden. It depends on `wardensummoning.WardenSummoning`; nothing depends back.
6. **Peaceful and `spawn_wardens`.** Jackson's call: follow vanilla's shriekers. On Peaceful, or with the `spawn_wardens` game rule off, no Warden is summoned and the book is taken as any other. The room checks are skipped too, so the book is never stuck there.

## API sketch

```java
/** The Ancient City's wake event: taking its book summons a Warden, angry at the player. */
final class AncientCityWarden {
	/** Whether taking the book at this position summons a Warden: it's an Ancient City's, and Wardens can be summoned. */
	static boolean guards(ServerLevel level, BlockPos book);

	/**
	 * Summons the book's Warden through the summoning search near the player, else standing under the book, angry at the
	 * player. Empty, with nothing spawned, when neither has room.
	 */
	static Optional<Warden> summon(ServerLevel level, ServerPlayer player, BlockPos book);
}
```

Consumed only by `StructureBooks.take`.

## Invariants

- The Ancient City's book is never taken without a Warden appearing in the same action.
- A refused take (locked, no inventory room, no Warden room) changes nothing: no Warden, book in place, no `TAKEN`.
- Other structures' takes are unchanged.

## Tests (GameTests, `AncientCityBookGameTests`)

Cities are planned at their own far chunks; the GameTest world is flat near the bottom, so a placed city stands in open air.

- Planning: every city planned holds exactly one book, Swift Sneak, in its start piece; `enchantmentsFor(ANCIENT_CITY)` is Swift Sneak.
- Room: for each of the three centres (planning until each is seen), the built book's chunk has the book at its spot, a full floor under the block below it and no collision for a Warden's box there.
- Portal: in each built centre, the book is 3 blocks out from the portal frame's plane and within the frame's span along it, found from the reinforced deepslate, independent of the spot's local coordinates.
- Taking (alone in its batch, `SubspaceArena` for cleanup): the player stands on a floor laid high above the city, so the near ring is sure to have room, and takes the book (`take` has no reach check). They hold Swift Sneak I, the block is gone, one Warden is 5–15 blocks across from them, angry at them, owning a Subspace.
- Full inventory in a city: refused, with no Warden or Subspace, the book in place.
- Peaceful, and `spawn_wardens` off: the book is taken and no Warden appears.
- Blocked search: the player hangs in midair well above the city's top, so the search has nothing within its scans; the Warden rises standing under the book, angry.
- Fully blocked: the same, with stone in the block under the book; the take is refused with the message, the book stays, there's no Warden and the player has no book.
- Existing structure book, Bastion and Warden summoning tests keep passing.

## Exclusions

- Hand check of the book's look in a real city (sculk features may grow at its feet; the take rechecks then).
