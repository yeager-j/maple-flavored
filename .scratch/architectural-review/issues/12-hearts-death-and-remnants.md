# 12: Hearts, death and Remnants

**What to fix:** The architecture findings for Max Hearts, the Heart Floor and Milestones, Withered Hearts, Crystal Hearts, experience points and Remnants. Each finding stands alone unless it says otherwise.

**Blocked by:** None

**Status:** done

**Priority:** P2 (Medium)

**Estimate:** M

**Source:** Architectural Review 11/9, the hearts, death and Remnants reviewer. Nothing was built or run.

What held up: Keep XP is decided once, Remnant decay is derived from the owner's death statistic and not stored, the max health modifier is an explicit persisted projection, and `PendingRemnants` staging is justified by the entity-loading hazard.

Other tickets hold findings from this slice: ticket 04 finding 8 covers the copied `isDone` helper, and ticket 04 finding 9 covers the tuning constants and the two drifted comments.

Paths: `main/` is `src/main/java/mapleflavored/`, `mixin/` is `src/main/java/mapleflavored/mixin/`, `client/` is `src/client/java/mapleflavored/client/`, and `test/` is `src/gametest/java/mapleflavored/gametest/`.

## Findings

### 1. Remnant XP bypasses `ExperiencePoints`, and the tests can't see the difference (P2)

**Principle:** One authority per piece of knowledge. Deep modules. The interface as the test surface.

`ExperiencePoints` presents itself as "the unit every Maple Flavored XP cost uses" and says vanilla's `totalExperience` isn't trusted. Only the anvil uses it for both read and write. Remnants read through it but write XP three other ways:

- `Remnants.takeAllXp` zeroes the three raw vanilla fields.
- `Remnants.copyXp` copies the three raw fields.
- `Remnant.collect` calls vanilla's `giveExperiencePoints`, which accumulates in a float level by level and also raises the player's score.

The design promises a Remnant "in exact points" (`.scratch/maple-flavored-design/issues/12-role-of-xp.md:32`). The exact path exists, but the largest XP transfer in the mod doesn't use it, because the module lacks `add` and `clear`. The Remnant tests assert on `totalExperience`, which can't observe level or progress, so a drift at the level 30 Enchantment Slot threshold would stay green.

Not verified: that such a drift occurs. It is a risk inferred from vanilla's float loop.

- `main/experience/ExperiencePoints.java:6`, `:14-18`, `:30-38`
- `main/remnants/Remnants.java:82-84`, `:116-126`
- `main/remnants/Remnant.java:151-155`
- `mixin/AnvilMenuMixin.java:86`, the one caller that pays through `set`
- `test/RemnantsGameTests.java:47`, `:70`, `:86`, `:112-116`

### 2. `hearts`, `alloys` and `items` form a cycle, and the Crystal Heart is split across two packages (P2)

**Principle:** Hide decisions, not files (Parnas). Cohesion.

The cycle:

- `hearts.WitheredHearts` imports `alloys.AlloySets` for the Electrum interval (`main/hearts/WitheredHearts.java:6-7`, `:110-112`).
- `alloys.Alloys` imports `items.MapleItems` to place the Ancient Ember after Pith in the creative tab (`main/alloys/Alloys.java:8`, `:107`).
- `items.CrystalHeartItem` imports `hearts.Hearts` (`main/items/CrystalHeartItem.java:3`, `:29`, `:37`, `:40`).

The avoidable edge is `items` to `hearts`. The Crystal Heart is a Hearts concept, but it lives in `items.CrystalHeartItem`, `items.CrystalHeartUsedPayload`, `MapleItems` (`main/items/MapleItems.java:30-50`, `:63`), `Hearts` (`atMaxHeartsCap` and `addMaxHeart`, public only to serve the item, at `main/hearts/Hearts.java:88-91`, `:122-128`) and `client/items/CrystalHeartActivation`. Every other feature keeps its items in its own package. Moving the Crystal Heart into `hearts` would make `items` a leaf.

The `hearts` to `alloys` edge matches how every other set trait is consumed, so the reviewer would leave it. There is no init-order hazard today.

### 3. `MapleItems` is named like the item registry but holds three items (P3)

**Principle:** Names tell the truth. DRY.

`MapleTriggers` and `MapleGameRules` really are central registries. `MapleItems` is the Crystal Heart, Pith and the Diamond Shard, from three features. Its registration helper is private, and the rule "an item's properties must carry the key it is registered under" is re-implemented three more times.

- `main/items/MapleItems.java:25`, `:70-74`
- `main/alloys/Alloys.java:118-122`
- `main/deepvariants/DeepVariants.java:230-234`
- `main/reinforcement/Reinforcement.java:135-149`

### 4. The Withered Hearts invariant is enforced on read, not at the write that can break it (P3)

**Principle:** Put rules with the context that can enforce them. Single Choice.

"Withered Hearts are at most Max Hearts minus 1" can be broken only by `Hearts.setMaxHearts` lowering Max Hearts. Instead of clamping there, the code carries three mechanisms: `count()` clamps on every read, `canRecover` deliberately reads the raw value, and `clearHiddenHearts` writes the clamp back only on ticks where recovery is allowed.

So "hidden" hearts are dropped in the Overworld but kept in the Nether, inside a Wither's Subspace, or under the Wither effect. A later rise in Max Hearts there brings them back, which the comment at `:121` says is prevented. The "at least 1 usable heart" rule is also decided twice (`:66`, `:104`), and the attachment has two write paths.

- `main/hearts/WitheredHearts.java:64-69`, `:104-107`, `:114-127`, `:145-146`, `:150-156`
- `main/hearts/Hearts.java:168-183`
- `test/WitheredHeartsGameTests.java:196-218`, which covers only the Overworld case

### 5. The valid Max Hearts range has no single home (P3)

**Principle:** Parse, don't validate. Invariants live with the model.

- The upper bound is `Hearts.MAX_HEARTS_CAP`; the lower bound is `MaxHeartsCommand.MIN_MAX_HEARTS`.
- `Hearts.setMaxHearts(int)` accepts any int, and the persistent codec is an unbounded `Codec.INT`.
- The `set` subcommand validates twice.
- For one Crystal Heart the cap is checked three times, and the boolean `addMaxHeart` returns is discarded by its only production caller.

Citations:

- `main/hearts/MaxHeartsCommand.java:20`, `:41`, `:78-80`
- `main/hearts/Hearts.java:35`, `:45-53`, `:122-128`, `:168-173`
- `main/items/CrystalHeartItem.java:29`, `:37`, `:40`
- `main/hearts/WitheredHearts.java:31`, `:66`

### 6. What death costs under the world's rules is decided piecemeal (P3)

**Principle:** Single Choice.

Hardcore is checked in two places and difficulty in two. `heartFloor(player)` can't say "no floor applies", so in Hardcore it still returns a number and every consumer must remember its own exemption.

- `main/hearts/Hearts.java:104`, `:131`, `:134`, `:153-159`, `:161-166`

### 7. The Heart Floor announcement exports a before-and-after protocol to two mixins (P3)

**Principle:** Ordering is part of the interface (Ousterhout). Thin mixins.

`announceHeartFloorDrop(player, previousHeartFloor)` requires callers to read the floor, run the change, then call back. Both mixins re-implement that; `MinecraftServerMixin` adds a parallel array and loop. A third input to the floor needs a third hand-written wrapper. The difficulty-switch path has no test.

- `main/hearts/Hearts.java:98-115`
- `mixin/PlayerAdvancementsMixin.java:18-26`
- `mixin/MinecraftServerMixin.java:20-38`

### 8. `STARTING_MAX_HEARTS` means two things, and "unknown" on a client reads as 10 (P3)

**Principle:** Names tell the truth. Empty and absent conflated.

The constant is both a new player's Max Hearts and vanilla's base max health in hearts (`main/hearts/Hearts.java:179`). They are equal today. Changing the starting value, an obvious playtest rule, would silently leave max health at 20. On a client, `maxHearts` and `witheredHearts` return 10 and 0 for any player whose data was never synced, so a spectated player shows no Withered Hearts.

- `main/hearts/Hearts.java:34`, `:49`, `:73-76`, `:149`, `:179`
- `client/mixin/HudMixin.java:48-50`, `:81`

### 9. The Milestone count is known in three places (P3)

**Principle:** DRY. Cohesion.

The seven-entry list, `BASE_HEART_FLOOR = 10` and `LOWEST_HEART_FLOOR = 3` must agree, and the `Math.max` clamp would silently swallow an eighth Milestone. `Milestones` sits in `advancements`, but only `Hearts.heartFloor` calls it. The guard against a missing Milestone id is a test holding a third hand-copied list.

- `main/advancements/Milestones.java:11-19`, `:24`, `:30-32`
- `main/hearts/Hearts.java:36-37`, `:158`
- `test/HeartFloorGameTests.java:26-35`

### 10. `HudMixin` isn't thin (P3)

**Principle:** The project's mixin convention. Cohesion.

The mixin owns the hollow-heart pixel mask, the blit loop and a reconstruction of vanilla's heart sprite ids. Peer client mixins delegate to a feature class, as `SkyRendererMixin` does to `BloodMoonSky`.

- `client/mixin/HudMixin.java:27-40`, `:90-115`
- `client/mixin/SkyRendererMixin.java:23-37`, the counterexample

### 11. Test gaps (P3)

**Principle:** Verification proportional to risk.

- No test covers Hardcore: "Hardcore retains its death rule" and the suppressed announcement.
- No test covers the difficulty-switch announcement or the `MaxHeartsLostPayload` count (`test/HeartFloorGameTests.java:185`).
- `ExperiencePoints` is a pure, high-value rule tested only indirectly at levels 16, 31 and 40. Its three-piece curve and overflow clamps would suit a round-trip sweep (`main/experience/ExperiencePoints.java:8-9`, `:40-54`). The reviewer checked the closed forms by hand and found them correct.

## Checklist

- [x] 1. `ExperiencePoints` can add and clear points, Remnants write XP only through it, and a test collects a Remnant across the level 30 boundary and asserts level and progress.
- [x] 2. The Crystal Heart lives in `hearts`, and `items` imports no feature package.
- [x] 3. One registration helper exists, and `MapleItems` is either the item registry or named for what it holds.
- [x] 4. Lowering Max Hearts clamps Withered Hearts where it happens, and a test shows hidden hearts don't return in the Nether.
- [x] 5. One type or function owns the 1 to 30 range, and the Crystal Heart checks the cap once.
- [x] 6. One resolved value says what a death costs, including "no floor" for Hardcore.
- [x] 7. `Hearts` has one entry point that runs a change and announces a Heart Floor drop, and both mixins call it.
- [x] 8. The max health bonus is derived from the attribute's base value, and a client can tell "not synced" from 10.
- [x] 9. The Heart Floor range is derived from the Milestone list, and `Milestones` lives with `hearts`.
- [x] 10. A `client/hearts` class owns Withered Heart drawing, and `HudMixin` forwards to it.
- [x] 11. Hardcore, the difficulty switch and an `ExperiencePoints` round trip are tested.

## Comments

Closed in one pull request. What each box means in the code:

1. `ExperiencePoints` has `add`, `clear` and `copy`. A Remnant is collected with `add`, death takes XP with `clear`, and Keep XP carries it to the respawned player with `copy`. Collecting no longer raises the player's score, which vanilla's `giveExperiencePoints` did. `collectingARemnantLandsExactlyOnLevelThirty` asserts level and progress, and the Remnant tests read points from level and progress. The drift the finding inferred is real: a player holding 10 points who collected 1,385 ended at level 29, and that test fails when `collect` is put back on `giveExperiencePoints`.
2. `CrystalHeartItem` and `CrystalHeartUsedPayload` are in `hearts`, the item is `Hearts.CRYSTAL_HEART`, and `CrystalHeartActivation` is in `client/hearts`. `items` imports only `MapleFlavored`.
3. `MapleItems.register` is public and every item goes through it: the Alloy items, the spawn eggs, the Reinforcement template and the Crystal Heart. `MapleItems` keeps its name as that registry, and still holds Pith and the Diamond Shard, which no one feature owns.
4. `Hearts.setMaxHearts` calls `WitheredHearts.keepWithin`, so the count is clamped at the one write that can break it. The read-side clamp, `clearHiddenHearts` and the second write path are gone. `witheredHeartsDroppedByLowerMaxHeartsNeverReturnInTheNether` covers the Nether and the Wither effect.
5. `Hearts.setMaxHearts` clamps to 1 to 30 and returns the result, and the saved value is range-checked on load. `/maxhearts set` now takes any number and stops at the nearest end, as `add` already did. The Crystal Heart is refused before use starts, and on finishing asks once, through `addMaxHeart`, whose answer it now uses.
6. `DeathCost.of(player)` resolves Hardcore, difficulty, Milestones and Keep Hearts once. It is empty in Hardcore, so `Hearts.heartFloor` returns an empty `OptionalInt` there.
7. `Hearts.announcingHeartFloorDrop(player, change)` and `announcingHeartFloorDrops(server, change)` run the change and announce. Both mixins are one call.
8. The max health modifier is usable hearts times 2 less the attribute's base value. A server stores Max Hearts for every player on join, so `Hearts.known(player)` is empty on a client exactly when that player's hearts were never sent. `maxHearts`, `witheredHearts` and `usableHearts` now take a `ServerPlayer`.
9. `Milestones` is in `hearts`. The floor is 10 less the Milestones held, so the lowest floor comes from the list's length, with no second constant and no clamp.
10. `client/hearts/WitheredHeartsHud` owns the hollow heart, and `HudMixin` forwards three calls to it.
11. `hardcoreHasNoHeartFloorAndAnnouncesNone`, `deathInHardcoreCostsNoMaxHearts`, `chatAnnouncesTheHeartFloorADifficultySwitchLowers` and `ExperiencePointsGameTests`. `WitheredHeartsClientGameTest` checks what the client is sent and takes a shot of the health bar.

Not done: no test reads the `MaxHeartsLostPayload` count. Mock players never register a channel, so the server sends them no payload.
