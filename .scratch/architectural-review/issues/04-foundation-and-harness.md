# 04: Foundation and harness

**What to fix:** The architecture findings for the mod's wiring, its package structure, the shared GameTest harness and CI, plus the findings that cut across every feature. Each finding stands alone unless it says otherwise.

**Blocked by:** None

**Status:** ready-for-human

**Priority:** P2 (Medium)

**Estimate:** L

**Source:** Architectural Review 11/9, the foundation and harness reviewer, with cross-cutting findings merged in from the feature reviewers.

Related: ticket 03 holds this slice's P1.

Paths: `main/` is `src/main/java/mapleflavored/`, `mixin/` is `src/main/java/mapleflavored/mixin/`, `client/` is `src/client/java/mapleflavored/client/`, `test/` is `src/gametest/java/mapleflavored/gametest/`, and `data/` is `src/main/resources/data/`.

## Findings

### 1. Feature packages import accessors from `mixin`, closing one large cycle (P2)

**Principle:** Parnas, hide decisions and not files. "It's only an import" does not excuse a dependency.

Seven feature packages import accessor or invoker interfaces from the flat `mixin` package, which itself imports 24 feature packages. Counting it, 17 of 31 packages are one import cycle. The 14 accessor and invoker files import nothing from the mod, so they could sit in a leaf package. Nothing polices dependency direction.

The other cycles each have their own finding: `hearts`, `alloys` and `items` in ticket 12, `spawners` and `structurebooks` in ticket 13, `advancements` and `alloys` in ticket 07, and `structurebooks` and `strongholds` in ticket 08.

- The accessors: `mixin/AbstractArrowAccessor.java`, `BaseContainerBlockEntityInvoker.java`, `BaseSpawnerAccessor.java`, `BrushableBlockEntityAccessor.java`, `CreeperAccessor.java`, `EnderDragonAccessor.java`, `EnderDragonFightAccessor.java`, `FeaturePoolElementAccessor.java`, `ListPoolElementAccessor.java`, `LootItemAccessor.java`, `MineShaftCorridorAccessor.java`, `SinglePoolElementAccessor.java`, `StructurePieceInvoker.java`, `WardenSpawnTrackerAccessor.java`

### 2. Mock-player knowledge is split, and `survivalPlayer` means two things (P2)

**Principle:** One authority. Names tell the truth.

`MockPlayers.survivalPlayer` builds its own `ServerPlayer` because vanilla's mock "always reports Creative" (`test/MockPlayers.java:79-91`). Five other `survivalPlayer` helpers call `helper.makeMockServerPlayerInLevel()` and then `setGameMode(SURVIVAL)`, which by that Javadoc still reports Creative. Tests can pass against a player that is not what its name says, and production code does branch on `isCreative()` (`main/alloys/AlloyMining.java:118`, `main/subspaces/WardenSubspaces.java:101`).

- The weaker copies, with 28 call sites: `test/AlloyGearGameTests.java:372-378`, `test/AnvilGameTests.java:391-397`, `test/StructureBookGameTests.java:482-488`, `test/WorkstationGameTests.java:162-168`, `test/EnchantingTables.java:110-117`
- Vanilla's mock is created directly 26 times in 17 test files, without the `finishLoading` step that `test/MockPlayers.java:196-199` says a player needs before it can take damage.
- Other fidelity gaps are patched elsewhere: ticking in `test/SubspaceArena.java:85-93` plus 28 hand-written `player.doTick()` calls, and post-teleport invulnerability in `test/SubspaceArena.java:181-184` and `test/RemnantsGameTests.java:462-465`.
- `MockPlayers` has five factories with three near-identical bodies (`test/MockPlayers.java:44-109`).

Not verified: "vanilla's mock reports Creative" comes from the repo's Javadoc, not from decompiled 26.3 code.

### 3. The CI gate never starts a client, checks the generators or checks the registration lists (P2)

**Principle:** A gate that has always been green proves nothing. One authority with explicit derivations.

- `.github/workflows/build.yml:23-24` runs `./gradlew build`, which attaches only the server `gameTest` run to `check`. The reviewer confirmed that on Loom 1.19.4; the build resolves `1.19-SNAPSHOT`.
- The four client GameTests never run in CI (`src/gametest/resources/fabric.mod.json:64-69`), and none asserts anything. They take screenshots for checking by eye: `test/client/ReinforcementClientGameTest.java:8-47`, `test/client/PortalTrimClientGameTest.java:13-14`, `test/client/BloodMoonSkyClientGameTest.java:19-45` and `test/client/ArmoredElytraClientGameTest.java`.
- So no client starts under automation. The 8 client mixins and the 16 registrations in `client/MapleFlavoredClient.java:27-42` are never executed.
- The 449 files under `src/main/generated` are committed, and nothing reruns datagen and diffs the result.
- A GameTest class missing from `src/gametest/resources/fabric.mod.json:9-63`, or a mixin missing from `src/main/resources/maple_flavored.mixins.json:5-96`, is silently inert. All three lists match the source today; there is no gate.

### 4. "Runs alone in its batch" is 41 identical JSON files (P2)

**Principle:** Single Choice. Promote a normative comment to enforcement.

A test that changes server-wide state gets a private batch by naming a unique test environment. 41 of the 65 files in `src/gametest/resources/data/maple_flavored_gametest/test_environment/` are byte-identical `all_of [normal]` wrappers. `restoring_peaceful.json` is one of them: it sets difficulty normal.

`SubspaceArena.cleared` is documented as "for a test running alone in its batch" (`test/SubspaceArena.java:114-124`). It logs out every player, discards every entity in range and clears every Subspace (`:130-145`). Its 43 callers each have a unique environment by convention only. A caller on the shared `normal` environment would disrupt its neighbours without failing itself.

### 5. Build inputs aren't reproducible or portable (P3)

**Principle:** Home state according to its lifetime. DRY.

- `gradle.properties:13` sets `loom_version=1.19-SNAPSHOT`, a moving target.
- `gradle.properties:7` commits one developer's absolute JDK path, which CI overrides (`.github/workflows/build.yml:22-24`) and `AGENTS.md` has to explain.
- Java 25 is stated three times (`build.gradle:63`, `:67`, `:68`).
- Loader and Minecraft versions are in `gradle.properties:11-12` and again in `src/main/resources/fabric.mod.json:23-24`; only `version` is expanded (`build.gradle:53-60`).
- `src/main/resources/fabric.mod.json:26` accepts any `fabric-api`, while the code is built against 0.165.0.

### 6. The entrypoints are an incomplete, uneven manifest (P3)

**Principle:** Small interfaces. Cohesion.

`main/MapleFlavored.java:43-66` lists 24 calls. Eleven packages never appear because only mixins drive them. `loot` needs four calls (`:50-53`) where `alloys`, `structurebooks` and `subspaces` each expose one. `client/MapleFlavoredClient.java:31-32` registers two menu screens inline while the other 14 lines call a feature's own `register()`.

### 7. A compatibility decision lives only in a test mixin (P3)

**Principle:** Don't disguise a workaround as a solution.

`test/mixin/GameTestServerMixin.java:10-25` filters the Trade Rebalance pack out of the test server. Its comment says a world with that pack can't load and that Maple doesn't support it. Nothing in `src/main`, `README.md` or `docs/project-overview.md` guards against it or tells a player. The filter is a deny-list of one, so the test server still enables every other available pack, not the default set players run.

### 8. Shared test helpers are redeclared per class (P3)

**Principle:** DRY. The deletion test.

- `isDone(helper, player, advancement)` has 12 copies, for example `test/BossRewardGameTests.java:247-253`, `test/WitherGameTests.java:307-313`, `test/HeartFloorGameTests.java:240-250`, `test/WitheredHeartsGameTests.java:300-306`, `test/CrystalHeartGameTests.java:323-329` and `test/RemnantsGameTests.java:524-530`.
- `enchantment`, `book` and `damaged` are private copies in `test/AnvilGameTests.java:391-432`, `test/WorkstationGameTests.java:162-181` and `test/TradeGameTests.java:342-346`, beside the shared `test/EnchantingTables.java:136-143`.
- `rollMany` is written three times, `assertRate` twice and `RATE_TOLERANCE = 4.0` three times, beside the shared `test/LootRolls.java:23-42`: `test/StructureChestGameTests.java:49`, `:95-110`, `:128-133`; `test/BookLootGameTests.java:44`, `:209-218`, `:249-254`; `test/DiamondShardGameTests.java:263-273`.
- Marked-mob killing is written in `test/SpawnerGameTests.java:358-365` and `test/DungeonGameTests.java:250-260`.
- `test/SmokeGameTests.java:14-27` can no longer fail independently of the other classes, and its `assertValueEqual` arguments are in the opposite order to `test/MockPlayers.java:138-139`.
- `MockPlayers` imports `hearts.Hearts` for two feature helpers (`test/MockPlayers.java:9`, `:31-41`, `:137-140`).

### 9. Tuning values sit outside `MapleGameRules` in every feature (P3)

**Principle:** The project convention that tuning values are playtest game rules. One authority.

`AGENTS.md` asks only for an offer when a value is tweaked, so a constant is not a breach. The cost is that each number is also written into prose, which raises the price of converting it, and some prose has already drifted.

- **Bosses:** `main/grounded/Grounded.java:40-50` (pearl gravity, which its comment calls a playtest value), `main/subspaces/WardenSubspaces.java:33-36`, `main/wardensummoning/WardenSummoning.java:41-43`, `main/bosses/BossKillCredit.java:26-27`, `:41`
- **Zephyr:** the level count is in `data/maple_flavored/enchantment/zephyr.json` (`max_level`) while the per-level behaviour is two positional arrays in `main/zephyr/Zephyr.java:19-21`, `:27-28`, and `:129-131` clamps silently if they diverge.
- **Workstations:** `main/anvil/AnvilResults.java:23-24` and `main/trades/BookTrade.java:15-16` hold two 50s that a comment says must match.
- **Hearts:** `main/hearts/Hearts.java:34-39`, `main/hearts/WitheredHearts.java:20-24`, `:28-31`, `main/remnants/Remnant.java:23-24`, `:53-58`, `main/items/CrystalHeartItem.java:17`
- **Structure books:** `main/structurebooks/ShriekerKeepers.java:41-49`, `main/echoshard/ShriekerPulses.java:44-46`, `main/structurebooks/PiglinAnger.java:41-46`, `main/structurebooks/IllagerAlert.java:46-50`
- **Strongholds and Blood Moons:** `main/bloodmoons/Restoring.java:50-54`, `main/strongholds/StrongholdIllagers.java:53`, `:101`, `main/strongholds/StrongholdRouteGoal.java:24-27`. `CORRIDOR_CHANCE` must give the same answer for every chunk of a piece, so it can't simply become a rule.
- **Mobs and spawners:** `main/spawners/SpawnerClearing.java:22-24`, `main/deepvariants/DeepVariants.java:52`, `:54`, `main/deepvariants/DeepSkeleton.java:23`, `:25`, `main/deepvariants/DeepSpider.java:14`, `main/loot/StructureChestRolls.java:46-65`, `main/mobs/CommonMobs.java:29`
- **Enchanting:** `main/enchanting/EnchantmentSlots.java:14`, `main/enchanting/Library.java:36`, `main/enchanting/AppleEffect.java:28-31`
- **Alloys:** `main/alloys/AlloyDamage.java:18-20`, `main/alloys/AlloySets.java:41`, `main/alloys/OrichalcumDrops.java:39`, `main/alloys/ArmoredElytraFlight.java:14-15`, `main/alloys/AlloyFurnaceBlockEntity.java:38`, `main/alloys/AlloyMining.java:42-43`
- **Drifted prose:** the `WITHERED_HEART_RECOVERED` Javadoc in `main/advancements/MapleTriggers.java` says a player's Withered Hearts "all returned", but it fires per heart (`main/hearts/WitheredHearts.java:147`). `main/hearts/WitheredHearts.java:37` says the client draws "darkened hearts"; it draws hollow ones.

### 10. There's no shared way to sync a game rule to the client (P3)

**Principle:** One authority with explicit derivations. Two adapters are the evidence a shared seam needs.

`AlloyMining` hand-rolls a level-attachment mirror of three rules (`main/alloys/AlloyMining.java:36-39`, `:107-115`, `:130-143`), and `WitherSubspaces` does the same for one (`main/subspaces/WitherSubspaces.java:37`, `:57`, `:106`). Several values can't become rules without a third copy, because the client evaluates them:

- Armored Elytra drag and boost (`main/alloys/ArmoredElytraFlight.java:14-15`)
- the Cleared requirement (ticket 13 finding 2)
- the enchanting verdicts (`main/enchanting/Checklist.java:92`; `client/enchanting/EnchantingTableScreen.java:143`, `:274`, `:323`)
- pearl gravity (`main/grounded/Grounded.java:40-50`)

`test/AlloyMiningGameTests.java:162-182` checks only the server, though client sync is the reason the attachment exists. Not verified: that the level attachments actually reach the client.

## Checklist

- [x] 1. The accessor and invoker interfaces live in a package that imports nothing from the mod, and no feature package imports `mapleflavored.mixin`.
- [x] 2. One `survivalPlayer` exists, in `MockPlayers`, and no test creates vanilla's mock where it needs a Survival or damageable player.
- [ ] 3. CI fails when committed datagen output differs from a fresh run, and when a GameTest class or mixin is missing from its list. A decision is recorded on whether CI starts a client.
- [x] 4. A test declares that it runs alone in its batch without a copied environment file, and `SubspaceArena.cleared` fails when its caller shares a batch.
- [x] 5. Loom is pinned to a release, the JDK path is out of the committed properties, and each version is stated once.
- [x] 6. Each feature exposes one registration call per side, and the entrypoints list every feature that registers anything.
- [x] 7. The Trade Rebalance decision is stated where a player or the mod's metadata can see it.
- [x] 8. `isDone`, the enchantment and book helpers and the loot roll helpers each exist once, and `SmokeGameTests` either checks something the others don't or is removed.
- [ ] 9. The two drifted comments are corrected, Zephyr's levels have one authority, and Jackson has marked which of the listed values become game rules.
- [x] 10. One helper syncs a game rule to the client, `AlloyMining` and `WitherSubspaces` use it, and a test shows a synced value reaching a client-side read.

## Left for Jackson

Boxes 3 and 9 stay open only for the decisions that are Jackson's. Everything else in them is done.

- **3. Whether CI starts a client.** The datagen gate and the list gate (`gradlew checkWiring`) are in. No decision is recorded yet on running `runClientGameTest` in CI.
- **9. Which listed values become game rules.** The two comments are corrected and Zephyr's levels are one list in `Zephyr.java`, with a GameTest holding `max_level` in `zephyr.json` to it. No value was converted.
