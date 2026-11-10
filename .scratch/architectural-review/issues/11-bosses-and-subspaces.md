# 11: Bosses and Subspaces

**What to fix:** The architecture findings for the Warden and Wither Subspaces, Warden summoning, boss kill credit, the Grounded state and Zephyr. Each finding stands alone unless it says otherwise.

**Blocked by:** None

**Status:** needs-triage

**Priority:** P2 (Medium)

**Estimate:** M

**Source:** Architectural Review 11/9, the bosses and Subspaces reviewer. Nothing was built or run; the Fabric attachment storage type in finding 3 was confirmed with `javap` on the Fabric API jar.

What held up: all sixteen mixins pass vanilla's call into a domain module, `Subspaces` is a deep module with one level attachment as its authority, `Zephyr.canJump` is one predicate used by client and server, and Blight is owned entirely by its JSON.

Other tickets hold findings from this slice: ticket 10 finding 1 covers `Subspace.Kind`'s throwing codec, ticket 04 finding 8 covers the copied `isDone` helper, and ticket 04 finding 9 covers the tuning constants and Zephyr's two authorities.

Paths: `main/` is `src/main/java/mapleflavored/`, `mixin/` is `src/main/java/mapleflavored/mixin/`, `client/` is `src/client/java/mapleflavored/client/`, and `test/` is `src/gametest/java/mapleflavored/gametest/`.

## Findings

### 1. `Grounded` doesn't own who can be Grounded or how it is topped up (P2)

**Principle:** Single Choice (Meyer). Put rules with the context that can enforce them.

`Grounded.keepGrounded` is the single entry point for Grounding a player, but both callers apply the same exemption guard before calling it, and the top-up algorithm and its two constants are copied for Darkness. A third source of Grounded would have to copy the guard, or would silently Ground creative players.

- `main/subspaces/WardenSubspaces.java:101` and `main/structurebooks/AncientCityGrounding.java:32` hold the identical guard `!player.isAlive() || player.isCreative() || player.isSpectator()`. `main/structurebooks/AncientCityGrounding.java:16-17` ties them together only in prose.
- `main/grounded/Grounded.java:37-38`, `:64-70` and `main/subspaces/WardenSubspaces.java:35-36`, `:114-120` both define `EFFECT_TICKS = 60`, `TOP_UP_TICKS = 40` and the same `endsWithin(TOP_UP_TICKS - 1)` branch.

### 2. Boss kill credit has two homes, and `Subspaces.ENDED` discards why a Subspace ended (P2)

**Principle:** Single Choice. Parse, don't validate: keep the evidence at the boundary.

The Elder Guardian, Warden and Dragon are credited in `BossKillCredit`. The Wither is credited inside `WitherSubspaces` through the `ENDED` event. `ENDED` fires "whatever ended it" with no cause, so `creditKill` works the cause out again by searching every level for the boss and asking `isDeadOrDying()`. The cause was known at the three hooks in `Subspaces.register` and thrown away.

The split hides an ordering constraint: `BossKillCredit` can't credit the Wither from its own `AFTER_DEATH` listener, because `Subspaces`' listener is registered first and has already removed the Subspace.

- `main/subspaces/Subspaces.java:50-55`, `:61-72`, `:129-137`: `ENDED` carries no cause.
- `main/subspaces/WitherSubspaces.java:139-156`: `creditKill` and `isDying` re-derive it.
- `main/bosses/BossKillCredit.java:20-24`: the Javadoc carves out "advancements that don't come from a Subspace".
- `main/bosses/BossKillCredit.java:72-76` and `main/subspaces/WitherSubspaces.java:143-147`: two copies of the "every non-spectator player gets the trigger" loop.
- `main/MapleFlavored.java:60`, `:62`: the registration order the design depends on.

### 3. The chunk mesher reads level attachments from worker threads (P2)

**Principle:** One authority with explicit derivations. Concurrency constraints belong in the interface.

`WitherSubspaceLight.isInside` says in its own comment that the chunk mesher calls it from its own threads. It reads `Subspaces.inLevel(level)` and then `WitherSubspaces.lightLevel(level)`, both `level.getAttachedOrElse` calls. Fabric stores attachments in a plain `IdentityHashMap`, and the client thread mutates it whenever a level attachment syncs. A read that races a resize can miss the key and fall back to the default. That section is then baked with the wrong light, and nothing re-dirties it. The effect is cosmetic and rare, but it is a real data race.

The class already keeps a main-thread projection of the same facts for change detection, so the mesher reads a different representation from the one the redraw logic tracks.

- `client/subspaces/WitherSubspaceLight.java:24-26`, `:40-55`, `:57-70`, `:74-85`
- `client/mixin/LightCoordsUtilMixin.java:22-24`
- `client/mixin/EntityRendererMixin.java:17-25`
- `main/subspaces/Subspaces.java:83-85`
- `main/subspaces/WitherSubspaces.java:56-58`

### 4. `Subspace.Kind` is a tag every consumer re-checks (P3)

**Principle:** Single Choice. Deep modules (Ousterhout).

`Subspaces` exposes only untyped queries and events, so each consumer filters by kind itself: eight checks in six files. "Inside a Wither's Subspace" is implemented on the server and again on the client, and "open one Subspace per boss unless it already owns one" is written twice. With two kinds, a per-kind strategy object would be premature; kind-typed queries would be enough.

- Kind checks: `main/subspaces/WardenSubspaces.java:108-111`; `main/subspaces/WitherSubspaces.java:62`, `:140`; `main/subspaces/WardenSculk.java:60`; `client/subspaces/WitherSubspaceLight.java:66`, `:91`; `client/subspaces/SubspaceShellRenderer.java:58`; `client/subspaces/SculkSky.java:42-43`
- Duplicate open logic: `main/subspaces/WardenSubspaces.java:92-97` and `main/subspaces/WitherSubspaces.java:120-126`

### 5. `Subspaces.ATTACHMENT` is public only so tests can write it (P3)

**Principle:** Let the interface be the test surface. Promote a normative comment to enforcement.

The attachment's Javadoc says "only this module writes them", but that is prose. `SubspaceArena` clears it directly in setup and teardown, which skips `ENDED` and its listeners, and `SubspaceGameTests.reloadSubspaces` round-trips it by hand. The tests need this because the module has no way to end an orphaned Subspace, whose boss was unloaded and never returns. Production has the same gap; Peaceful is the only escape.

- `main/subspaces/Subspaces.java:32-41`
- `test/SubspaceArena.java:142`, `:467`
- `test/SubspaceGameTests.java:337-343`

### 6. "An Ancient City's bounds" is implemented twice and checked every tick (P3)

**Principle:** DRY. Information hiding (Parnas).

`WardenTerritory.contains` holds the rule that a city's bounds are its whole bounding box. `AncientCityGrounding` repeats the same lookup line for line, because it also needs the `StructureStart` and `WardenTerritory` returns only a boolean. A comment ("as for Warden Territory") is the only link. `AncientCityGrounding` also runs a registry lookup and a structure query per player, per tick, in every dimension, though Grounded lasts 60 ticks and tops up at 40.

- `main/wardensummoning/WardenTerritory.java:19-25`
- `main/structurebooks/AncientCityGrounding.java:24-28`, `:34-41`

### 7. Vanilla Warden rules are copied instead of reused (P3)

**Principle:** DRY: a second authority for vanilla's rule.

- `WardenSummoning.warn` re-implements `WardenSpawnTracker.tryWarn` in full to add one filter. It copies the 48-block Warden box, the 16-block radius, the cooldown rule and the raise-and-copy steps, and needs four private-member accessors. The tests pin warning levels and cooldowns, but not the 16 and 48.
- `extendAwareness` merges vanilla-sensed and Subspace-sensed entities into one memory, so `disturbFromSniff` reconstructs "was this inside vanilla's box?" from a hand-copied sensor box. A test does pin that box at 24.68 blocks.

Both are defensible given what mixins allow, and the comments label the copies. The cost is silent drift on a Minecraft update.

- `main/wardensummoning/WardenSummoning.java:37-39`, `:69-87`, `:124-156`
- `mixin/WardenSpawnTrackerAccessor.java:10-26`
- `main/subspaces/WardenSubspaces.java:56-69`, `:75-88`, `:133-138`

### 8. Two Subspace tests can't go red, and two depend on random sculk growth (P3)

**Principle:** Prove a test can go red. Isolate tests by construction.

- `aRemnantInsideStaysWhereItsOwnerDiedUnlessItCantBeCollectedThere` and `deathInsideRespawnsAtARespawnPointInside` assert Remnant and respawn behaviour with a Warden present. No production code connects Remnants or respawning to Subspaces, so nothing can make them fail. Each costs its own batch and arena (`test/SubspaceGameTests.java:182-230`).
- `ShriekerGrowth` kills and respawns Wardens every 400 ticks until random sculk growth happens to produce a shrieker, under default rules with `maxTicks = 20000` (`test/SubspaceSculkGameTests.java:123`, `:162`, `:230-269`). The environment could raise the patch charge and radius rules to make growth near-certain (`src/gametest/resources/data/maple_flavored_gametest/test_environment/subspace_sculk_overlap.json`).

## Checklist

- [ ] 1. `Grounded` decides who is exempt, and one top-up helper serves both Grounded and Darkness.
- [ ] 2. `ENDED` says why a Subspace ended, or one module credits every boss kill. The spectator rule is decided once, and no behaviour depends on listener registration order.
- [ ] 3. The mesher reads one immutable snapshot published from the client tick, and the redraw logic compares against the same snapshot.
- [ ] 4. `Subspaces` offers kind-typed queries and one open-if-new call, and consumers no longer filter by kind.
- [ ] 5. `Subspaces` can end orphaned Subspaces and fires `ENDED` when it does, tests use that, and the attachment isn't public.
- [ ] 6. `WardenTerritory` returns the Ancient City at a position, both callers use it, and Grounding doesn't query structures every tick.
- [ ] 7. Tests pin the copied 16 and 48, or `warn` reuses vanilla's method.
- [ ] 8. The two Remnant and respawn tests are removed or given a production link that can break them, and the sculk growth tests run under rules that make growth certain.
