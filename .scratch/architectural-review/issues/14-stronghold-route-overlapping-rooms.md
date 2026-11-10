# 14: A Stronghold route from a spot in two rooms is arbitrary

**What to fix:** Vanilla Stronghold rooms can overlap. `StrongholdRoutes.route` reads a spot inside two rooms as being in whichever room it meets first, and it meets them in the order of a map keyed by object identity. The same spot can give routes of different lengths from one call to the next, and from one run to the next.

**Blocked by:** None

**Status:** done

**Priority:** P3 (Low)

**Estimate:** S

**Source:** Found while closing ticket 03, when `StrongholdGameTests.everyRoomHasARouteToThePortalRoomOneDoorwayAtATime` moved to a new site and failed in one run of three.

## What was found

- `roomAt` returns the first room whose box holds the position, from `touching.keySet()` (`src/main/java/mapleflavored/strongholds/StrongholdRoutes.java`, `route` and `roomAt`). `touching` is a `HashMap` keyed by `StructurePiece`, which has no `hashCode`, so its order follows object identity.
- The Stronghold planned at that test's site has more than ten pairs of overlapping rooms. One is the room at x 16330577 to 16330587 and a corridor at x 16330575 to 16330579, which share three blocks of x.
- In the failing run, a route of five doorways had a first waypoint that then read as six doorways from the Portal Room, not four.
- The test now skips a waypoint that is inside more than one room, with a comment naming the cause.

Not verified: whether a hunter standing in an overlap changes its route between checks in play, and whether that is visible.

## Checklist

- [x] A spot inside two overlapping rooms always reads as the same room.
- [x] The route test checks every waypoint again.

## Outcome

A spot inside several rooms now reads as the one the Stronghold planned first. The opposite rule, the room planned last, fails the route test at the overlap named above every time: the corridor is planned after the room, and a route from the corridor is six doorways where the room's is four.

Not fixed: two overlapping rooms are not joined in the route. Only rooms whose walls touch are. A spot in that corridor outside the room still routes the long way round, though the corridor opens straight into the room.
