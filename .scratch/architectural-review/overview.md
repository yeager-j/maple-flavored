# Architectural Review 11/9

A whole-repository architecture review, run on 2026-11-09 against `main` at `282567d`. Ten reviewers each read one slice of the source against the `engineering-principles` rubric. Their 102 findings merged into 86, filed here as 13 tickets: one per P1, and one per slice holding that slice's P2 and P3 findings as a checklist.

Label: Health Check.

## Verdict

The code structure is healthy, state that is saved in two places is the weak point, and the test harness is the weakest part. No reviewer found a P0, and nothing is known to be broken in normal play.

What held up in every slice:

- Mixins are thin. All 90 delegate to feature code, with the few exceptions the tickets name.
- Features have real owners. `Subspaces`, `Restoring`, `StructureBookRules`, `Library` and `ShriekerPulses` were each judged deep, single-authority modules.
- Tests mostly restate the spec independently and use paired controls.

## Findings by priority

| Priority | Count |
| --- | --- |
| P0 | 0 |
| P1 | 3 |
| P2 | 30 |
| P3 | 53 |

## Tickets

| # | Ticket | Priority | Findings |
| --- | --- | --- | --- |
| 01 | [Book locks can't be rebuilt after a crash](issues/01-book-locks-reconcile.md) | P1 | 1 |
| 02 | [Restore spots can't be rebuilt after a crash](issues/02-restore-spots-reconcile.md) | P1 | 1 |
| 03 | [Off-grid test sites have no owner](issues/03-test-site-allocation.md) | P1 | 1 |
| 04 | [Foundation and harness](issues/04-foundation-and-harness.md) | P2 | 4 P2, 6 P3 |
| 05 | [Enchanting and Library](issues/05-enchanting-and-library.md) | P2 | 4 P2, 3 P3 |
| 06 | [Workstations and villagers](issues/06-workstations-and-villagers.md) | P2 | 3 P2, 4 P3 |
| 07 | [Alloys](issues/07-alloys.md) | P2 | 3 P2, 3 P3 |
| 08 | [Structure book placement](issues/08-structure-book-placement.md) | P2 | 2 P2, 7 P3 |
| 09 | [Structure book locks](issues/09-structure-book-locks.md) | P2 | 2 P2, 4 P3 |
| 10 | [Strongholds and Blood Moons](issues/10-strongholds-and-blood-moons.md) | P2 | 3 P2, 7 P3 |
| 11 | [Bosses and Subspaces](issues/11-bosses-and-subspaces.md) | P2 | 3 P2, 5 P3 |
| 12 | [Hearts, death and Remnants](issues/12-hearts-death-and-remnants.md) | P2 | 2 P2, 9 P3 |
| 13 | [Mobs, spawners and loot](issues/13-mobs-spawners-and-loot.md) | P2 | 4 P2, 5 P3 |

## Themes

- **Saved state with two authorities.** Book locks, Restore spots and taken books each copy facts the world owns into level saved data, which saves on a different schedule from chunks. Tickets 01, 02 and 08.
- **Decisions discarded and re-derived.** A lock's kind and keepers, the anvil's refusal reason and a Subspace's reason for ending are each known at one point, thrown away, and worked out again later. Tickets 06, 08, 09 and 11.
- **Client and server deciding the same thing separately.** The enchanting screen, the anvil screen and the spawner forcefield each re-run a server rule. Tickets 05, 06 and 13.
- **Rules split between JSON and Java.** The book trade, per-Alloy data, curse removal and the loot substitution rule each have two authors. Tickets 05, 06, 07 and 13.
- **Package cycles.** Counting the flat `mixin` package, 17 of 31 packages form one import cycle, each closed by one small import. Tickets 04, 07, 08, 12 and 13.
- **Persisted enums that throw.** Four enums decode with `valueOf`, and for Restore spots one unknown name loses the file. Ticket 10.
- **The test harness.** Hand-picked world sites, two meanings of `survivalPlayer`, 41 identical test environments, and a CI gate that never starts a client. Tickets 03 and 04.
- **Tuning values.** They sit outside `MapleGameRules` in every feature, and there is no shared way to sync a rule to the client. Ticket 04.

## How to read a ticket

- Each finding names the principle it breaks and cites the code. Line numbers are as of `282567d`.
- Each ticket's checklist states the observable condition that closes each finding, so a slice ticket can be worked across several pull requests.
- A finding raised by more than one reviewer lives in the ticket that owns the fix, and the other tickets point to it.
- Nothing was built or run for the review. Where a claim comes from reading code and was not confirmed by running it, the finding says so.

## Not recorded

- **"Some rules live only in a mixin"** was reported as one finding. It is split across tickets 08, 10 and 13. Its remaining examples are not recorded: the zombie villager cure item, Keep Inventory's default, Illager goal priorities and the Wither mixin's call order. Each is a one-line rule, and the reviewers who own those mixins judged them thin.
- **The Wither leash exceeding the Subspace radius** and **`Library.offers()` not clamping to an enchantment's maximum level** were listed by their reviewers as notes, not findings.
