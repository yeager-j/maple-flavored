# 07: Alloys

**What to fix:** The architecture findings for the five Alloys, Alloy gear, full-set bonuses, the Alloy Furnace, Armored Elytra and Alloy datagen. Each finding stands alone unless it says otherwise.

**Blocked by:** None

**Status:** done

**Priority:** P2 (Medium)

**Estimate:** M

**Source:** Architectural Review 11/9, the alloys reviewer. Nothing was built or run.

What held up: the vanilla Netherite numbers copied into `GearKind` and the hand-written `NETHERITE_TAGS` map are each guarded by a test against the real items, `AlloyFurnaceSlots` is a real single choice, and the server GameTests use paired Netherite-versus-Alloy controls.

Other tickets hold three findings from this slice: the hunt deadline stored in `IllagerNeutrality` is in ticket 09 finding 2, the Alloy balance constants are in ticket 04 finding 9, and the hand-rolled game rule mirror is in ticket 04 finding 10.

Paths: `al/` is `src/main/java/mapleflavored/alloys/`, `mixin/` is `src/main/java/mapleflavored/mixin/`, `client/` is `src/client/java/mapleflavored/client/`, `test/` is `src/gametest/java/mapleflavored/gametest/`, and `data/` is `src/main/resources/data/`.

## Findings

### 1. Which Alloy grants which full-set bonus is decided in four mixins and `hearts` (P2)

**Principle:** Single Choice (Meyer). Information hiding (Parnas): a mixin should be blind to the rule.

`AlloySets` hides the Alloy for only three bonuses. For the rest, the caller picks the Alloy constant and often holds the policy too. The Javadoc list at `al/AlloySets.java:29-35` is the only index of set bonuses, and nothing enforces it.

- **`AlloySets` owns the rule:** Adamant piercing share (`al/AlloySets.java:73-77`), Shakudo cobweb and powder snow (`:80-84`), Steel knockback (`:93-101`).
- **The caller owns the rule:**
  - `mixin/EntityMixin.java:20-23`: Electrum fire immunity.
  - `mixin/EntityMixin.java:25-30`: Shakudo jump, including the `Math.max(jumpFactor, 1.0F)` policy.
  - `mixin/LivingEntityMixin.java:54-59`, `:62-65`: Shakudo block speed and frost.
  - `mixin/PowderSnowBlockMixin.java:17-20`: Shakudo powder-snow walking.
  - `src/main/java/mapleflavored/hearts/WitheredHearts.java:29`, `:110-112`: the Electrum withering interval.
- **One bonus in two files:** Steel's Levitation immunity is removal on equip at `al/AlloySets.java:101` plus refusal at `mixin/LivingEntityMixin.java:49-52`.
- **Formula and preconditions in the mixin:**
  - `mixin/LivingEntityMixin.java:69-75` holds the Adamant blend formula.
  - `mixin/LivingEntityMixin.java:78-83` enforces a precondition that `al/OrichalcumSave.java:36-39` states only in prose.
  - `mixin/BlockBehaviourMixin.java:19-22` holds the bedrock branch in front of two `AlloyMining` calls.

`ArmoredElytraFlight`, `AlloyDamage`, `IllagerNeutrality` and `OrichalcumDrops` already show the thin pattern: the mixin passes vanilla's value in and gets the answer back.

### 2. Hand-written per-Alloy data mirrors the `Alloy` enum beside a generator that already enumerates it (P2)

**Principle:** DRY (Hunt and Thomas): one authority, explicit derivations.

The enum (`al/Alloy.java:15-25`) and `AlloyGear` are the authority for which Alloys, ingots, blocks and Armored Elytras exist. The generator covers only gear models, smithing recipes and gear tags (`client/datagen/MapleDataGenerator.java:6-15`). The rest is copied by hand, and the boundary follows no rule. A sixth Alloy is about twenty hand edits.

- **Hand-copied lists of the five Alloys:** `data/maple_flavored/tags/item/alloy_ingots.json`, `data/maple_flavored/tags/block/alloy_blocks.json`, `data/maple_flavored/tags/item/armored_elytras.json`, the five criteria in `data/maple_flavored/advancement/nether/wear_every_alloy_set.json`, and per Alloy an alloying recipe, block recipes, a loot table, a blockstate and an equipment JSON.
- **Electrum's tag traits straddle the boundary:** Piglin-safe armor is generated (`client/datagen/AlloyGearTags.java:49`, `:71-75`), while the extra Enchantment Slot is a hand list of the same pieces (`data/maple_flavored/tags/item/extra_enchantment_slot.json`).
- **Three lists of Armored Elytras:** `al/AlloyGear.java:41`, `:78-80`, the generated tags (`client/datagen/AlloyGearTags.java:44-46`, `:65-69`) and the hand-written JSON.
- **Java trusts the copies:** `al/AlloyBeacons.java:16`, `:48` reads `#alloy_blocks`, not `Alloy.block()`. `src/main/java/mapleflavored/materialunits/MaterialUnits.java:47`, `:99` reads `#alloy_ingots`.
- **Smaller copies:** the item id scheme is built at `al/AlloyGear.java:134`, `:140` and again at `client/datagen/AlloyGearRecipes.java:45-49`. `client/datagen/AlloyGearModels.java:26-41` lists kinds by hand, so a new `GearKind` silently gets no model.
- **Uneven gates:** `armored_elytras` and `extra_enchantment_slot` are checked for one Alloy each (`test/ArmoredElytraGameTests.java:306`, `:230-232`).

### 3. `PortalTrim` is a prototype shipped in the client path with no consumer (P2)

**Principle:** Proportion: model only what somebody uses. A test that can't go red.

The feature is switched off by an empty set, yet its machinery runs for every client.

- `client/alloys/PortalTrim.java:24-29`: `ALLOYS = Set.of()`, "None yet; it's meant for a future End Alloy".
- A render pipeline and shader pair are registered at client start (`client/alloys/PortalTrim.java:31-47`, called from `client/MapleFlavoredClient.java:38`).
- An inject fires on every equipment layer (`client/mixin/EquipmentLayerRendererMixin.java:71-86`), and five `alloy_trim_mask.png` textures ship.
- `docs/project-overview.md` reserves End content for a separate mod, but `ALLOYS` is private and immutable, so that mod couldn't opt in.
- `test/client/PortalTrimClientGameTest.java:13-14` screenshots Adamant, which isn't in `ALLOYS`, and asserts nothing.

### 4. The Orichalcum exclusion for Looting chances is decided where the drop is unknown (P3)

**Principle:** Put rules with the context that can enforce them.

A chance condition can't see the item it guards, so the exclusion is inferred from pool shape through a thread-local depth counter, and the class documents a hole: on a pool shared with other items, the excluded drop would get the bonus. The reviewer checked the six Looting-affected entity tables and none leaks today. Nothing scans loot tables for the forbidden shape, and only the Brute's Ember is pinned against the real table.

- `al/OrichalcumDrops.java:33-35`, `:41-43`, `:95-114`
- `mixin/LootPoolMixin.java:23-30`, `mixin/LootPoolEntryContainerMixin.java:13-18`
- `test/OrichalcumDropsGameTests.java:162-170`

### 5. `AlloyGear` keeps its parsed `Piece` private, so callers re-join two Optionals (P3)

**Principle:** Parse, don't validate (King).

`pieceOf` already produces the refined value (`al/AlloyGear.java:182-187`), but the public surface is `alloyOf` and `kindOf` (`:83-90`), each a separate lookup.

- The repeated join: `al/AlloyMining.java:78-79`, `:94-98`, `:100-104` and `al/OrichalcumDrops.java:116-122`. "Tool traits apply to held kinds only" is decided twice.
- The four set pieces are private at `al/AlloySets.java:40` and re-listed at `client/datagen/AlloyGearTags.java:49`, `test/AlloySetGameTests.java:58-60`, `test/AlloyCombatGameTests.java:40-41`, `:282-289`, `test/AlloyGearGameTests.java:337` and `test/ArmoredElytraGameTests.java:75`.

### 6. `advancements` and `alloys` depend on each other, and touching `Alloy` registers content (P3)

**Principle:** Cohesion: things that change together live together.

- `src/main/java/mapleflavored/advancements/AlloySetWornTrigger.java:6`, `:25-29` imports `alloys.Alloy`, while `al/AlloySets.java:6`, `:103`, `AlloyMining` and `AlloyBeacons` import `advancements.MapleTriggers`. The trigger's only parameter type and only firing site are in `alloys`.
- The enum constructor registers items and blocks (`al/Alloy.java:39-53`), and `AlloyGear`'s static block registers 65 items (`al/AlloyGear.java:44-62`). The "call once from mod init" contracts are prose; real registration happens on first class touch. It works because `src/main/java/mapleflavored/MapleFlavored.java:43-47` fixes the order.
- The trigger's Javadoc says it fires on "the last piece", but `al/AlloySets.java:93-104` fires on every set-slot change while a set is worn. That is harmless, because criteria are idempotent.

## Checklist

- [x] 1. `AlloySets` answers each full-set bonus as a question (fire immune, refuses an effect, jump factor and so on), and no mixin or other package names an Alloy constant for a set bonus.
- [x] 2. Everything that is a function of `Alloy.values()` is generated, or one test asserts each hand-written tag and advancement equals the enum's set.
- [x] 3. `PortalTrim` either has a real consumer and an asserting test, or is out of the shipped client path.
- [x] 4. A GameTest walks every loot table and fails on an excluded item in a mixed pool with an enchanted-bonus chance.
- [x] 5. `AlloyGear` exposes the parsed piece, and the set pieces are listed once.
- [x] 6. `AlloySetWornTrigger` lives with `alloys`, and Alloy items and blocks are registered by an explicit call, not by class loading.

## Comments

Closed in one pull request. What each box means in the code:

1. Each bonus is a question on `AlloySets` (`isFireImmune`, `refusesEffect`, `blockJumpFactor`, `blockSpeedFactor`, `ignoresFrost`, `walksOnPowderSnow`, `witheringInterval`, `damageAfterArmor`), and `wearsFullSet` is no longer public. `OrichalcumSave.trySave` checks the damage itself, and `AlloyMining.bedrockProgress` holds the bedrock branch.
2. Datagen now writes the Alloy tags, `extra_enchantment_slot`, the `wear_every_alloy_set` advancement, the block and ingot models, the equipment assets, the block recipes and the block loot. Still by hand, because the enum can't derive them: what each Alloy is alloyed from (`recipe/<alloy>_ingot.json`, gated by `eachStorageBlockAndANetheriteIngotMakeItsAlloy`), textures and names.
3. `PortalTrim` is removed with its shaders, mixin hook, mask textures and test. Jackson to confirm; `git checkout 1c1c21c -- <path>` restores any of it.
4. `noLootTableSharesAnEnchantedChanceWithAnExcludedDrop` reads every loot table through `ExcludedDropScan`.
5. `AlloyGear.pieceOf` and `AlloyGear.Piece` are public, and `AlloySets.PIECES` is the one list of set pieces.
6. `AlloySetWornTrigger` is in `alloys`, registered by `AlloySets.register()`. `Alloy.register()` and `AlloyGear.register()` register the items and blocks. `Alloys`' own constants (the Ember and the Alloy Furnace) still register when the class loads, as every other registry holder in the mod does; ticket 12 owns the shared helper.
