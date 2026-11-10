# 03: Off-grid test sites have no owner

**What to fix:** GameTests that work outside their test structure each pick world coordinates by hand, force chunks by hand, and disagree about who releases them. Isolation between tests rests on authors reading each other's constants, and it has already slipped once. Give the harness one type that hands out sites and owns forcing, waiting and release.

**Blocked by:** None

**Status:** needs-triage

**Priority:** P1 (High)

**Estimate:** M

**Source:** Architectural Review 11/9, the foundation and harness reviewer. The collision below was confirmed by reading the code during the merge.

Paths: `test/` is `src/gametest/java/mapleflavored/gametest/`.

## Principle

Isolate parallel tests by construction, with unique data and tracked cleanup. Single Choice (Meyer). Deep modules (Ousterhout): `ForcedChunks` hides only the wait.

## What the review found

- **Sites are chosen by hand.** `CHUNKS_APART = 40` is declared in 10 test classes, and `FAR_CHUNK` in 10 with hand-picked values (2000, -2000, 3000, 4000, 5000, 6000, 7000, 8000, -9000). `farChunk` exists in 7 classes with different signatures and quadrant conventions.
  - `test/AncientCityBookGameTests.java:64-65`, `:631-632`
  - `test/BastionBookGameTests.java:63-64`, `:491-492`
  - `test/FortressBookGameTests.java:58-59`, `:296-297`
  - `test/StrongholdGameTests.java:68-69`, `:618-619`
  - `test/OrdinaryStructureBookGameTests.java:58-59`, `:453-454`
- **The convention is a comment.** "Searches a line of chunks running away from the other tests' areas" (`test/OrdinaryStructureBookGameTests.java:370-376`). The same function is copied with its sign flipped at `test/AbandonedVillageGameTests.java:293-298`.
- **Two tests share a site today.** In `test/BloodMoonRestoringGameTests.java`, line 440 places a Bastion at `new ChunkPos(-2_600, 2_600)`, which is block (-41,600, 41,600), and line 451 generates a Woodland Mansion at `new BlockPos(-41_600, 100, 41_600)`. The intended series is 2600, 2700, 2800 chunks (lines 464 and 484); mixing chunk and block units hid the overlap. It looks harmless now because each test filters mobs by type and finishes in its first tick. Whether the two structures overlap vertically was not checked.
- **Forcing is uneven.** Chunks are forced at 10 sites, and only 4 wait through `ForcedChunks.awaitEntityTicking`. That method's own Javadoc says un-awaited forced chunks make entity behaviour depend on the machine (`test/ForcedChunks.java:23-28`).
- **Release is decided three ways.**
  - "The runner releases every forced chunk as each batch ends", so the arena never releases them (`test/SubspaceArena.java:409-412`).
  - The same reliance at `test/ShriekerPulseGameTests.java:43-47`.
  - Release by hand, on the success path only, at `test/StrongholdGameTests.java:361`, `:493`, `test/DeepVariantGameTests.java:647-654`, `test/BloodMoonRestoringGameTests.java:640-652` and `test/MansionBookGameTests.java:325-327`.

Not verified: that vanilla's GameTest runner releases forced chunks at batch end, and whether any un-awaited site is actually flaky. Both come from the repo's own comments.

## Checklist

- [ ] One harness type hands out non-overlapping off-grid sites, so no test names a far coordinate.
- [ ] That type forces the site's chunks, waits until entities tick there, and releases them on success and on failure.
- [ ] `CHUNKS_APART`, `FAR_CHUNK` and `farChunk` no longer appear in test classes.
- [ ] The Bastion and Woodland Mansion in `BloodMoonRestoringGameTests` no longer share a site.
- [ ] Whether the runner releases forced chunks at batch end is checked against the 26.3 sources, and the answer is written into the harness type's Javadoc.
