# Plan: ticket 34, Alloy combat and loot behavior

Repo `C:\Users\jackson\Developer\maple-flavored` (Fabric, MC 26.3). Ticket `.scratch/maple-flavored/issues/34-alloy-combat-and-loot-behavior.md`, parent 33 (done). Spec: "Materials and Alloys". Design: `.scratch/maple-flavored-design/issues/06-material-roles-and-alloys.md`. Branch `34-alloy-combat-and-loot-behavior`.

## End goal

Ticket 33 gave every Alloy its plain-number traits. Orichalcum still does nothing over Netherite, and Steel and Electrum armor lack their defensive identity. This ticket gives them the traits that need code, so Steel is the explosion and Warden armor, Electrum the Nether armor that Piglins accept as gold, and Orichalcum the wealth and peace metal: more drops, and Illagers that leave you alone until you, a raid or a book (ticket 44) start a fight. Ticket 44 (Mansion book alert) and 45 (Stronghold) build on the alert API made here; 35 (set bonuses) reuses the piece count.

## Decisions from Jackson (this session)

- Reductions stack **additively** per piece: 4 Steel = 40% less, 4 Electrum = 60% less fire.
- The drop bonus applies to **counts and chances**: Fortune and Looting count functions, and Looting chance conditions.
- Hostility after an attack or alert is **timed, like Piglins**, as a playtest game rule.

## Behavior

**Damage reductions.** After armor, Resistance and Protection (`LivingEntity.getDamageAfterMagicAbsorb`'s return, every path, so sonic boom, which bypasses armor and enchantments, is still reduced), damage is multiplied by `max(0, 1 − Σ pieces × percent)`. Pieces are the Alloy gear worn in the entity's armor slots (`EquipmentSlotGroup.ARMOR`: head, chest, legs, feet, body), so a horse in Steel horse armor takes 10% less explosion damage and armored mobs benefit too. Damage types come from three new damage type tags, so data is the one authority:
- `maple_flavored:reduced_by_steel_armor`: `minecraft:sonic_boom`, `#minecraft:is_explosion` (explosions, player explosions, fireworks, bad respawn point). 10% per piece.
- `maple_flavored:reduced_by_electrum_armor`: `#minecraft:is_fire`. 15% per piece.
- `maple_flavored:reduced_by_orichalcum_armor`: `minecraft:magic`, `minecraft:indirect_magic`. 10% per piece.

**Piglins.** Electrum helmet, chestplate, leggings and boots go in `minecraft:piglin_safe_armor` (datagen, `AlloyGearTags`). Vanilla's `PiglinAi.isWearingSafeArmor` then does everything: Piglins ignore the wearer, and still anger at chests, gold mining and attacks, as with gold.

**Illager neutrality.** An Illager (`AbstractIllager`: Vindicator, Pillager, Evoker, Illusioner) can't attack a player who wears at least one Orichalcum armor piece, unless:
- the Illager is in an active raid (`Raider.hasActiveRaid()`), covering raid waves and Illagers that join one, or
- the Illager is **angry at that player**: a per-Illager map of player UUID → game time the anger ends, saved with the Illager (Fabric attachment).

The check is a `@ModifyReturnValue` on `Mob.canAttack` (in `MobMixin`), which vanilla consults when picking a target (`TargetingConditions`), each tick to keep one (`TargetGoal.canContinueToUse`), and in `Mob.getTarget`/`setTarget` (`asValidTarget`), so vanilla's alert-others path is covered too. A Vex resolves to its Illager owner, since Vexes also find players on their own (plan review). So neutral Illagers and their Vexes never target the player, and drop a target the moment the player puts Orichalcum on.

Anger: when an Illager is hurt by damage whose responsible entity (`source.getEntity()`) is a player (melee, arrows, tridents; a killing blow counts), that Illager and every Illager within 16 blocks of it that has line of sight to it become angry at that player for `orichalcum_illager_anger_time` ticks (playtest game rule, default 600, 30 s like a Piglin's gold anger; range 1–72 000; applies to anger started after a change). Hitting again renews it. Anger at a player ends with its time; it isn't cleared by death, but a dead player is no target anyway. Hooked at `Raider.hurtServer`'s return (Illagers' nearest override) when it returns true. Vanilla's `HurtByTargetGoal` then retaliates and alerts, as before.

Book alert (ticket 44 and 45 call it): `IllagerNeutrality.anger(ServerLevel, AbstractIllager, ServerPlayer)` sets the same anger for the game rule's time. 44 can widen it if a book alert needs a longer anger, and decides the targeting.

**Orichalcum drop bonus.** When the drop's source holds Orichalcum gear (Fortune: the loot context's `tool`; Looting: the `attacking_entity`'s main hand), on loot functions and conditions whose enchantment is Fortune or Looting, at any level including 0:
- `minecraft:apply_bonus` (Fortune, every formula) and `minecraft:enchanted_count_increase` (Looting): add half the count the drop had before the function, rounded randomly (`floor(0.5 × before + random)`), after vanilla's own bonus. Diamond ore: 1 → 1.5 on average; with Fortune III, 2.2 → 2.7. A count function's `limit` still caps the result.
- `minecraft:random_chance_with_enchanted_bonus` (Looting): add half the unenchanted chance to the chance vanilla works out. Wither skeleton skull: 2.5% → 3.75%, plus 1% per Looting level.
- Never for drops in the new item tag `maple_flavored:no_orichalcum_bonus` (`maple_flavored:ancient_ember`, `maple_flavored:pith`, `minecraft:enchanted_book`). Count functions see the stack. A chance condition doesn't, so the pool or entry that holds it says: a pool whose entries are all items in the tag (or empty), or an item entry in the tag, tests its conditions with the bonus off. That covers the Brute's Ember pool today and Illager book pools (ticket 46) that follow the same shape.
- Not changed: mob equipment drop chances, which Looting raises through an enchantment effect, not loot tables.

## Seams and API

```java
// mapleflavored.alloys
public final class AlloyGear {
    public static Optional<Alloy> alloyOf(ItemInstance item);          // widened from ItemStack; loot's tool is an ItemInstance
    /** How many pieces of the Alloy's gear the entity wears in its armor slots, including mob body armor. */
    public static int piecesWorn(LivingEntity entity, Alloy alloy);    // reused by 35's set bonuses
}

/** Steel, Electrum and Orichalcum armor's damage reductions. */
public final class AlloyDamage {
    /** {@code damage} after the entity's Alloy armor reductions for the source's damage type. */
    public static float afterAlloyArmor(LivingEntity entity, DamageSource source, float damage);
}

/** Illagers leave Orichalcum wearers alone until a raid, an attack or a book alert. */
public final class IllagerNeutrality {
    public static boolean isNeutralTo(Mob attacker, LivingEntity target);   // from MobMixin; an Illager or an Illager's Vex
    public static void wasHurt(ServerLevel level, AbstractIllager illager, DamageSource source); // from RaiderMixin
    public static void anger(ServerLevel level, AbstractIllager illager, ServerPlayer player); // ticket 44/45
}

/** Orichalcum tools' +50% to drops Fortune and Looting affect. */
public final class OrichalcumDrops {
    public static ItemStack addCountBonus(ItemStack result, int countBefore, Holder<Enchantment> enchantment, LootContext context); // count mixins
    public static float addChanceBonus(float chance, float unenchantedChance, Holder<Enchantment> enchantment, LootContext context); // condition mixin
    /** Runs a pool's or entry's condition test with the bonus off when every item it can give is excluded. */
    public static boolean testWithout(BooleanSupplier test);  // scoped ThreadLocal depth counter
}
```

The four classes are registered (`IllagerNeutrality`'s attachment, `AlloyDamage`'s tags) from `Alloys.register()`. All the decisions (which pieces, which damage types, what counts as angry, which drops) live in them; the mixins only route.

## Files

- New `alloys/AlloyDamage.java`, `alloys/IllagerNeutrality.java`, `alloys/OrichalcumDrops.java`.
- `alloys/AlloyGear.java`: `alloyOf(ItemInstance)`, `piecesWorn`.
- `alloys/Alloys.java`: register them.
- `gamerules/MapleGameRules.java`: `ORICHALCUM_ILLAGER_ANGER_TIME`; lang name and description.
- Mixins (`maple_flavored.mixins.json`): `LivingEntityMixin` (+ `getDamageAfterMagicAbsorb` return), `MobMixin` (+ `canAttack`), new `LootItemAccessor`, `RaiderMixin` (`hurtServer`), `ApplyBonusCountMixin` and `EnchantedCountIncreaseFunctionMixin` (`@WrapMethod run`; the latter shadows `limit`), `LootItemRandomChanceWithEnchantedBonusConditionMixin` (`@ModifyVariable` on the chance), `LootPoolMixin` (wrap the pool condition test), `LootPoolEntryContainerMixin` (wrap `canRun` for `LootItem` entries).
- Data: `data/maple_flavored/tags/damage_type/reduced_by_{steel,electrum,orichalcum}_armor.json`, `data/maple_flavored/tags/item/no_orichalcum_bonus.json`; datagen `AlloyGearTags` adds Electrum's four pieces to `minecraft:piglin_safe_armor`, then regenerate.
- GameTests: new `AlloyCombatGameTests.java`, `OrichalcumDropsGameTests.java`, registered in `src/gametest/resources/fabric.mod.json`; fixture loot tables under `src/gametest/resources/data/maple_flavored_gametest/loot_table/`.
- Docs: ticket checkboxes and status; spec's Illager neutrality line (timed anger, the game rule) and drop-bonus line (counts and chances, how much); GLOSSARY entries "Illager neutrality" and "Orichalcum drop bonus".

## Invariants

- A reduction never makes damage negative; percent sums cap at 100%.
- Neutrality never applies during an active raid or to a non-player target; it needs at least one Orichalcum humanoid or body armor piece.
- Anger is per Illager and per player: angering one Illager at Alice leaves it neutral to Bob.
- The bonus applies only with Orichalcum held, only to Fortune or Looting functions and conditions, and never to excluded items.
- Vanilla behavior is unchanged without Alloy gear.

## GameTests (acceptance matrix)

Reductions, each against a Netherite-armored control on a mock player, with exact numbers (damage applied straight with `hurtServer` and a type, no armor bypass ambiguity, health compared):
- 4 Steel: explosion 40% less, sonic boom 40% less; fire unchanged. 1 Steel piece: 10% less explosion.
- 4 Electrum: `in_fire`/`lava` 60% less; magic unchanged.
- 4 Orichalcum: magic and indirect magic 40% less; explosion unchanged.
- Steel horse armor on a horse: 10% less explosion.

Piglins: a Piglin's sensor ignores a player in Electrum boots (`isWearingSafeArmor` true and no attack target after sensing), but targets one in Netherite boots.

Illagers, each with a Vindicator next to a survival mock player:
- Netherite armor: the Vindicator targets the player. One Orichalcum piece: it doesn't.
- Orichalcum and the player hits it: it targets the player; a second Vindicator in sight within 16 blocks does too; one behind a wall doesn't.
- Anger ends after the game rule's time (set to a few ticks) and the Vindicator drops the player.
- A Vindicator in an active raid targets an Orichalcum wearer.
- `anger(...)` makes it target an Orichalcum wearer, and leaves it neutral to a second Orichalcum player.

Drops (fixture loot tables, run with a built `LootParams`):
- `apply_bonus ore_drops` on a count-10 item, tool an Orichalcum pickaxe without Fortune: 15. Netherite: 10.
- `enchanted_count_increase` (Looting) on a count-10 item, killer holds an Orichalcum sword: 15; with the limit 12: 12.
- A count-10 Ancient Ember with `apply_bonus`: 10.
- `random_chance_with_enchanted_bonus` unenchanted 0.7, item stone: passes all 200 rolls with Orichalcum; with Netherite, some fail. Same with the item an Ancient Ember: some fail (the pool exclusion), and the same as an entry condition.
- The real Piglin Brute table: Ember chance not raised (same Ember-pool check via exclusion, covered above), so no separate test.

Verification: `runDatagen`, `build`, `runGameTest` (full suite), JAVA_HOME from `gradle.properties`.

## After plan review

Added: Vex coverage; Fortune III and Looting III cases checking the bonus is half the count *before* the enchantment (same seed, Orichalcum = Netherite + 5); unsaturated chance rates (20% → 30%, Looting 40% → 50%); all three excluded items; a boosted pool right after an excluded one (no leak); an Illager dropping a target who puts Orichalcum on. The exclusion's supported shape (excluded drop's chance on an excluded-only pool or its own entry) is documented in `OrichalcumDrops`, the spec and ticket 46. Negative controls: each mechanism disabled in turn turned its tests red.

## Hand check (left on the ticket)

Illagers in a Woodland Mansion with one Orichalcum piece; hitting one with others around; a Piglin barter wearing Electrum; creeper damage in full Steel.
