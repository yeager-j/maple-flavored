# Plan: ticket 30, Elder Guardian, Warden and Dragon rewards

Repo: C:\Users\jackson\Developer\maple-flavored (Fabric mod, MC 26.3). Ticket: `.scratch/maple-flavored/issues/30-elder-guardian-warden-dragon-rewards.md`. Spec: `.scratch/maple-flavored/spec.md` (Milestones and advancements; Loot). Design: `.scratch/maple-flavored-design/issues/17-advancements.md`. Closest precedent: ticket 29's Kill the Wither (`WitherSubspaces.creditKill`, `advancement/nether/kill_wither.json`, `WitherGameTests`).

## End goal

The four boss Crystal Hearts exist (Wither already done): each boss pays one Crystal Heart, once per player, through an advancement whose credit is shared by the players who were actually in the fight. Elder Guardians and the Dragon also pay repeatable materials (2 Pith; a Zephyr I book).

## Outcomes

1. Elder Guardian loot gains a pool: 2 `maple_flavored:pith`, always (no killed_by_player, no Looting), like Evoker's Pith pool. Override `data/minecraft/loot_table/entities/elder_guardian.json` copying vanilla's pools verbatim.
2. Ender Dragon loot: new `data/minecraft/loot_table/entities/ender_dragon.json` (vanilla's is empty) with a `minecraft:book` + `set_enchantments {maple_flavored:zephyr: 1}` entry, same shape as wither.json. Drops in `LivingEntity.die` at the death spot (vanilla calls dropAllDeathLoot there; `EnderDragon.kill()` (/kill) skips die, so /kill drops nothing — same as vanilla loot).
3. `maple_flavored:adventure/kill_elder_guardian` (parent `minecraft:adventure/kill_a_mob`, goal, reward loot `maple_flavored:advancement_reward/crystal_heart`), trigger `maple_flavored:elder_guardian_killed`, fired on any Elder Guardian death for every non-spectator player in the same level within 64 blocks (3D, `distanceToSqr <= 64²`, measured from the guardian's position).
4. `maple_flavored:adventure/survive_warden` (parent `minecraft:adventure/avoid_vibration` = Sneak 100, goal, Crystal Heart), trigger `maple_flavored:warden_survived`, fired for every non-spectator player inside the Warden's Subspace when (a) it dies or (b) it digs down. Not for a discard by Peaceful, /kill? (/kill is death → credited, same as Wither), commands or any other removal.
5. Free the End (`data/minecraft/advancement/end/kill_dragon.json` override): keep vanilla's `killed_dragon` criterion, add `ender_dragon_killed` (trigger `maple_flavored:ender_dragon_killed`) as an OR alternative (`requirements: [["killed_dragon","ender_dragon_killed"]]`), add `rewards.loot` crystal_heart. The new trigger fires on the dragon's death for every non-spectator player that its `EnderDragonFight`'s `validPlayer` predicate accepts (alive, within 192 blocks of origin+128y — the fight's boss-bar range). The design doc says "Free the End uses vanilla's dragon-fight player range"; vanilla actually credits only the killer, so this realizes the design's intent of shared credit. A dragon with no fight (summoned outside the End) shares nothing; its killer still gets vanilla credit.
6. Lang entries for the two new advancements.

## Seams and API

- `MapleTriggers`: three new `PlayerTrigger`s: `ELDER_GUARDIAN_KILLED`, `WARDEN_SURVIVED`, `ENDER_DRAGON_KILLED`.
- `Subspaces` (package-private helpers, moved out of `WitherSubspaces` so Warden and Wither share them):
  - `static void triggerForPlayersInside(ServerLevel level, Subspace s, PlayerTrigger t)` — non-spectators inside.
  - `static boolean bossIsDying(MinecraftServer server, Subspace s)` — finds the boss by UUID in any level, `LivingEntity.isDeadOrDying()`.
  `WitherSubspaces` keeps its public `triggerForPlayersInside(WitherBoss, PlayerTrigger)` (used by WitherPassage) delegating.
- `WardenSubspaces`:
  - registers `Subspaces.ENDED` listener: WARDEN kind and `bossIsDying` → credit WARDEN_SURVIVED (same timing as Wither: ENDED fires from AFTER_DEATH while the dying boss is still in its level).
  - `public static void creditDiggingDown(Warden warden)` — for each server level, the Subspace owned by the warden's UUID → trigger WARDEN_SURVIVED. Called by a new `DiggingMixin` on `net.minecraft.world.entity.ai.behavior.warden.Digging.stop`, `@Inject` at `INVOKE` of `Warden.remove(RemovalReason)` (only reached when removal reason is null, i.e. the actual dig-down). Runs before removal, so the Subspace is still open; the existing ENTITY_UNLOAD path then ends it. Digging.stop's remove is the only dig-down removal in vanilla (also covers the in-liquid "agitated" immediate stop, which is vanilla's dig-away too).
- New package `mapleflavored.bosses`, class `BossKillCredit` (register from `MapleFlavored.onInitialize`): one `ServerLivingEntityEvents.AFTER_DEATH` listener:
  - `ElderGuardian` → players within 64 blocks get ELDER_GUARDIAN_KILLED. Constant `ELDER_GUARDIAN_CREDIT_RANGE = 64`.
  - `EnderDragon` with a fight → `level.getPlayers(fightAccessor.maple$validPlayer())`, minus spectators, get ENDER_DRAGON_KILLED.
- Mixin accessors: `EnderDragonAccessor` (`@Accessor("dragonFight") @Nullable EnderDragonFight maple$dragonFight()`), `EnderDragonFightAccessor` (`@Accessor("validPlayer") Predicate<Entity> maple$validPlayer()`). `validPlayer` is set in `init`; a fight that's never been init'd isn't attached to a dragon in practice.

Why the dragon credit uses its own fight (accessor) rather than `level.getDragonFight()`: the fight a dragon belongs to is its field; it's also the only way to test outside the End.

## Invariants

- Each reward is advancement loot, so a player gets each Crystal Heart once (advancements complete once).
- Spectators are never credited (as Kill the Wither).
- Warden: a discard (Peaceful, `/kill`? no — /kill is a death; plain `discard()`, dimension change, unload) never credits; only death or Digging.stop.
- No Pith/books from advancements; Pith is loot data.

## Tests (GameTests, each in its own environment batch, in `SubspaceArena.cleared`)

New file `BossRewardGameTests` (Elder + Dragon) and additions to `SubspaceGameTests` or a new `WardenSurvivalGameTests`:
1. `elderGuardianDropsTwoPith` — kill an elder guardian, drops contain exactly 2 Pith.
2. `killingAnElderGuardianCreditsEveryPlayerWithin64Blocks` — players at 64 (credited, Crystal Heart in inventory), 65 (not), spectator at 10 (not).
3. `theDragonDropsAZephyrBookEveryKill` — kill dragon by explosion damage; one enchanted book with only Zephyr I.
4. `killingTheDragonCreditsFreeTheEndInItsFightsRange` — `EnderDragonFight.createDefault()`, `init(level, seed, origin)` with origin placed so player A is ~186 and B ~198 blocks from origin+128y; `dragon.setDragonFight(fight)`; explosion damage (no player kill credit) → A has Free the End + Crystal Heart, B doesn't. Then a fightless dragon killed by explosion credits nobody. Discard dragons right after (avoid setDragonKilled portal at tick 200).
5. `survivingAWardenCreditsPlayersInsideWhenItDies` — still Warden: `discard()` credits nobody; second still Warden `kill(level)` → inside players credited, outside (51) not.
6. `survivingAWardenCreditsPlayersInsideWhenItDigsDown` — penned real Warden, shortened dig cooldown, players inside at 40 and outside at 51 → after it digs, inside credited, outside not (pattern of `aTargetKnownOnlyThroughTheSubspaceDoesntKeepTheWardenFromDiggingDown`).
Add a generic `SubspaceArena.still(EntityType<E>, Vec3)` (noAi + chunks loaded); `stillWither` delegates.

## Checks

`gradlew build` + gametest task (`runGametest` — check build.gradle), full suite green. Then LARP review.

## Hand check (left in ticket)

Kill an Elder Guardian with a friend at ~60 blocks; survive a Warden until it digs; kill the Dragon with a friend on the island; see books/Pith drop (note: the Dragon's book drops where it dies, so a kill over the void loses it — same as any drop).

## Least confident

- Whether shared Free the End credit is wanted (design says yes in prose; ticket bullet is silent). Going with design.
- Fabric AFTER_DEATH firing for the dragon's die() (handleKillingBlow is overridden and doesn't set `dead`). Test 3/4 will prove it.

## Revision (Jackson, 2026-11-05, after PR review)

- No new Survive the Warden advancement: vanilla Sneak 100 (`minecraft:adventure/avoid_vibration`) is overridden instead. Its criterion becomes `maple_flavored:warden_survived`, its frame a goal, and it rewards a Crystal Heart. The title stays, and the description is new.
- Credit goes to every non-spectator player within the `warden_survival_range` game rule (default 50, applies at once) of the Warden, not to everyone inside its Subspace, when it dies or digs down. It moves into `BossKillCredit`: on death through `AFTER_DEATH`, and on digging down through `DiggingMixin`. The Subspace helper refactor is reverted, since nothing shares it any more.
