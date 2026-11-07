# 48: Gradual Withered Heart recovery

**What to build:** Withered Hearts return one at a time, one every 10 seconds, instead of all at once. Leaving the Nether starts the recovery but doesn't finish it, so the Wither effect still costs something after you escape.

**Blocked by:** 08 (Withered Hearts)

**Status:** done

Spec: the Maple Flavored spec in this folder. This changes its Withered Hearts rule ("All return at once"), and user story 28.

- [x] While the player has no Wither effect and is outside the Nether, one Withered Heart returns every 10 seconds until none are left
- [x] The recovery timer starts once both conditions hold, and resets whenever either stops holding (Wither applied again, or entering the Nether), so recovery never carries over between visits
- [x] Recovery timer progress is saved through logout, like the withering timer
- [x] A returned heart comes back empty, as now, and health is not refilled
- [x] "Second Wind" (Get a Withered Heart back) is granted when the first heart returns
- [x] Death still clears every Withered Heart at once
- [x] The spec's Withered Hearts rule and user story 28 say hearts return one every 10 seconds
- [x] GameTests cover: no heart back before 10 seconds, one at 10, the next at 20; the reset on re-applied Wither and on entering the Nether; logout keeping progress; death still clearing all

## Notes

- The rule lives in `hearts/WitheredHearts.java`. `tick` now calls `recover`, which clears every Withered Heart once `canRecover` holds. That becomes a second timer attachment, shaped like `WITHERING_TICKS`, that counts while `canRecover` holds and removes one heart each time it fills.
- Keep `canRecover` as the single place that decides whether recovery can run. Ticket 29 adds the Wither Subspace there, and the new timer should stop inside a Subspace without any extra change.
- A Max Hearts drop can leave stored Withered Hearts above the clamped count (see `count`). Recovery has to clear stored hearts the clamp hides, as `recover` does now, or they could come back later.
- The interval is 10 seconds whatever armor the player wears. The Electrum set bonus only slows withering.
- `WitheredHeartsGameTests` currently expects all hearts back a few ticks after recovery starts. Those tests change with this ticket.
