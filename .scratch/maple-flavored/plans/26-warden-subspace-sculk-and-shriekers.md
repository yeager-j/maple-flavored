# Plan: 26 Warden Subspace sculk and shriekers

## Context / end goal

Ticket 25 gave every Warden a fixed Subspace with Darkness and Grounded. A player couldn't see it, though: nothing marked where it started. This ticket makes the Subspace visible without making the city more dangerous after each fight:

- A weak patch of sculk spreads where the Warden appears. It follows vanilla's catalyst rules: only blocks a catalyst can replace, plus vanilla veins and sensor and shrieker growths.
- Shriekers the patch grows can summon while their Subspace lasts. They're silenced when it ends, even if they were unloaded then. Each Subspace silences only its own shriekers.
- A translucent sculk shell marks the sphere's edge, from inside and outside. The sky, fog and clouds inside turn sculk-dark.

Jackson's calls (2026-11-05):
- **Shell plus sky.** A recoloured sky can't show the border from outside, hence the shell. Its texture is Jackson's recolour of vanilla's End sky.
- **Vanilla growths only.** Sensors and shriekers grow; catalysts don't.
- **Radius.** The Warden Subspace radius dropped from 100 to **50**.
- **A weak patch, not an expanding front.** He first chose sculk covering the whole Subspace from an expanding front. On seeing it, he found that spreading over ground that already had sculk packed it with sensors and shriekers, so he replaced it with a weak patch at the spawn point, about 5 blocks across.
- **Grown shriekers keep this ticket's rule**: they can summon until their Subspace ends.
- **Playtest game rules.** The Subspace radius, the patch radius and the patch charge are game rules.

## Decisions

1. **Subspace identity.** `Subspace` gains `UUID id`, generated in `open`.
   - Boss and centre together can repeat: Peaceful ends a Subspace, then the same saved Warden reloads where it stood. The id doesn't repeat.
   - Shrieker records key on it.
   - There's no path for records saved before ids existed, since the spec doesn't upgrade existing worlds.
2. **Game rules** (`MapleGameRules`, category Mobs). Each applies to Subspaces that open after it changes:
   - `maple_flavored:warden_subspace_radius`, default 50.
   - `maple_flavored:warden_sculk_patch_radius`, default 5.
   - `maple_flavored:warden_sculk_patch_charge`, default 150, where 0 spreads none.
3. **The patch** (`WardenSculk`).
   - **Seeding.** On `Subspaces.STARTED` for a Warden Subspace, it seeds one `SubspaceSculkSpreader` at the Warden's feet block, as a catalyst would for a mob dying there, with the charge from the rule.
   - **Ticking.** `END_LEVEL_TICK` spreads each patch while its Warden is a living entity in that level. Its origin must be ticking. A patch is dropped once it's empty or its Subspace has ended.
   - **Storage.** Patches live in a non-persistent level attachment, so a reload leaves a patch as far as it got.
4. **Staying in the patch.** `SubspaceSculkSpreader extends SculkSpreader` copies `createLevelSpreader()`'s parameters.
   - **Why a subclass.** It's how the two mixins tell a Subspace's sculk from vanilla's, so inheritance is only a tag.
   - **The guard.** `SculkSpreaderChargeCursorMixin` wraps `canMoveToPos` in `ChargeCursor.getValidMovementPos`. A Subspace cursor may move only where vanilla allows, within the patch radius of the Subspace's centre, and into a ticking chunk.
   - **How far writes reach.** Veins and growths land at most √5 blocks beyond a cursor. Vanilla's knock-on block updates stay vanilla.
   - **The cap.** The patch radius is capped at the Subspace radius minus 3, so the patch stays inside its Subspace however the rules are set, and cleanup's scan always covers its shriekers.
5. **Grown shriekers** (`GrownShriekers`).
   - **Recording.** `SculkBlockMixin` wraps the growth `setBlockAndUpdate` in `SculkBlock.attemptUseCharge`. A shrieker grown by a Subspace's patch is placed with `CAN_SUMMON` true and gets a persistent block entity attachment `maple_flavored:grown_in_subspace` holding the Subspace's id.
   - **Silencing at the end.** On `Subspaces.ENDED`, every loaded chunk over the sphere is scanned. Each shrieker recording that id is silenced: `CAN_SUMMON` goes false and the attachment is removed.
   - **Silencing on load.** On `ServerChunkEvents.FULL_CHUNK_STATUS_CHANGE` to full, stale shriekers in the chunk join a per-level pending set. Each `END_LEVEL_TICK` silences the pending ones whose chunk is ready. A chunk leaving full drops its entries.
   - **Why that event.** A chunk still held in memory goes back to full without `setBlockEntity` or `CHUNK_LOAD` firing, so `BLOCK_ENTITY_LOAD` and `CHUNK_LOAD` both missed it. GameTests showed this.
6. **Client: sky.** `ClientLevelMixin` injects at the return of `addEnvironmentAttributeLayers`. `SculkSky` adds positional layers for `SKY_COLOR`, `FOG_COLOR`, `CLOUD_COLOR` and `SUNRISE_SUNSET_COLOR` that blend in over the last 8 blocks inside a Warden Subspace.
7. **Client: shell.** `SubspaceShellRenderer` submits each Warden Subspace on `LevelRenderEvents.COLLECT_SUBMITS` as a 64×32 latitude–longitude sphere.
   - It uses `RenderTypes.entityTranslucentEmissive`, which blends translucently, doesn't cull and doesn't write depth.
   - A sphere lying wholly beyond the render distance isn't submitted.
   - The texture repeats a whole number of times around the sphere and drifts slowly. A faint vertex alpha makes the opaque texture translucent.
8. **API.** `Subspaces.inLevel(Level)` is public for the shell. `isActive(Level, UUID)` is package-private.

## Tests

- **Ticket 25's `SubspaceGameTests`** now run against the new arena and radius:
  - They use the extracted `SubspaceArena`: 160 blocks up, with smooth stone floors sculk can't replace.
  - Positions are rescaled for radius 50.
  - Each opened record's fields are checked, and its id stays the same through persistence and reload. A second Warden's opening gets a different id.
  - The near burrow test was flaky on main too, failing 2 of 4 runs there. There were three causes:
    - `Mob.finalizeSpawn` gives every mob a random follow-range bonus of up to ±11.5%. That moves vanilla's awareness box, which the range gate matches, past the test's 24.68-block player. The arena's test Wardens now lose that bonus.
    - A summoned Warden's first sniff can end close to tick 200, so the shortened dig cooldown went from 200 to 600 ticks.
    - Killed Wardens' experience orbs, left from an earlier batch, landed by the next Warden and reset its dig timer through the vibration. The arena now clears every non-player entity, not only mobs.
- **`SubspaceSculkGameTests`**:
  - **Limits.** After 400 ticks, sculk has spread, nothing sculk-family lies beyond 5 + √5 blocks, and deepslate bricks inside the patch are unchanged.
  - **Small Subspace.** With game rules setting a 6-block Subspace and a 32-block patch, the sculk stays inside the Subspace.
  - **Pausing.** Sculk doesn't change while the Warden is unloaded, grows again once it reloads, and stops once the Subspace ends.
  - **Overlap.** Two Wardens 30 blocks apart, so each Subspace covers both patches. Ending the first silences only the first's grown shrieker, and a hand-placed shrieker stays able to summon.
  - **Unloaded shrieker, through a real chunk unload.** The Warden opens its Subspace 100 blocks out, then moves into the test's always-loaded chunk. The shrieker stays able to summon after its chunk unloads and reloads while the Subspace lasts. Once the Subspace ends while the chunk is unloaded, the shrieker is silenced when the chunk loads.
  - **Retrying for shriekers.** Only some patches grow a shrieker, so `ShriekerGrowth` retries with a fresh floor and Warden every 400 ticks until one does.

## Hand check (kept on the ticket)

- `/summon minecraft:warden` on stone and in a Deep Dark cave. A small sculk patch spreads around its feet and city bricks stay clean.
- The shell shows from outside as a dome, from inside overhead, and as a sheet through caves. It blends correctly against sky, water and stained glass, with Fabulous graphics on and off.
- The sky, fog and clouds turn sculk-dark inside and blend back at the edge.
- Kill the Warden: the shell and sky go, and any grown shrieker shows `can_summon=false` in F3.
- `/gamerule maple_flavored:warden_subspace_radius 30` changes the next Warden's shell. The patch rules change the next patch.

## Exclusions

- Who a shriek warns and where a summoned Warden spawns are ticket 27.
- Wither Subspaces get no sculk, shell or sky, and their radius stays 100, for ticket 29.
- Sculk already spread stays after a Subspace ends, as vanilla sculk does.
