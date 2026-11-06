# Plan: 35 Alloy set bonuses

## End goal

Each Alloy's full armor set gives its own strong, defining bonus, so picking an Alloy is a real trade-off
(spec: "no Alloy is strictly best"). Closing it means a player wearing helmet, chestplate, leggings and boots of one
Alloy gets that bonus at once, loses it the moment a piece comes off, and gets credit toward a "wear every set"
challenge.

## Seams and API

New `mapleflavored.alloys.AlloySets` (public, final):

```java
/** The Alloy whose helmet, chestplate, leggings and boots the entity all wears, or empty. */
public static Optional<Alloy> fullSet(LivingEntity entity);
public static boolean wearsFullSet(Entity entity, Alloy alloy);   // false for non-living entities
/** Melee damage share that skips armor points: 0.25 when the direct attacker wears full Adamant, else 0. */
public static float armorPiercingShare(DamageSource source);
static void register();  // EQUIPMENT_CHANGE listener, called from Alloys.register()
```

The full set is the four humanoid slots (HEAD, CHEST, LEGS, FEET) through `AlloyGear.alloyOf`, so the Armored Elytra
(ticket 36) counts as a chest piece once it reports its Alloy there. Body armor isn't part of a set.

New `mapleflavored.alloys.OrichalcumSave` (public, final):

```java
/** Saves the player from a lethal hit when they wear full Orichalcum when the save is ready (Hardcore included). */
public static boolean trySave(ServerPlayer player);
public static boolean isReady(ServerPlayer player);
static void register();  // tick recharge for every online player
```

State: attachment `maple_flavored:orichalcum_save_recharge` (Integer ticks charged since the last save; absent =
ready), persistent, `copyOnDeath()`. Each server tick every online player's value goes up by 1 (worn or not); at the
new playtest game rule `maple_flavored:orichalcum_save_recharge_time` (default 36 000 = 30 min) it is removed.

New trigger `MapleTriggers.ALLOY_SET_WORN` (`maple_flavored:alloy_set_worn`, a `SimpleCriterionTrigger` with an
optional `alloy` field). `Alloy` gains a `StringRepresentable` codec.

## Bonuses and where they hook

| Bonus | Mechanism | Hook |
| --- | --- | --- |
| Steel knockback immunity | transient attribute modifiers `KNOCKBACK_RESISTANCE +1` and `EXPLOSION_KNOCKBACK_RESISTANCE +1` (both clamp at 1), added/removed on equipment change | covers `LivingEntity.knockback` and `ServerExplosion` |
| Steel Levitation immunity | `canBeAffected` returns false for Levitation; a Levitation effect present when the set completes is removed | `LivingEntityMixin` |
| Electrum fire and lava immunity | `Entity.fireImmune()` true for a living entity in full Electrum (vanilla then skips fire/lava damage and burning) | new `EntityMixin` |
| Electrum Withered Hearts | `WitheredHearts` interval is 200 ticks with full Electrum, else 100, read each tick | `WitheredHearts.advanceWithering` |
| Shakudo no slowdown | `LivingEntity.getBlockSpeedFactor` returns at least 1 (soul sand, honey); `Entity.makeStuckInBlock` cancelled for cobweb and powder snow; `tryAddFrost` skipped so freezing adds no speed penalty (vanilla's `removeFrost` runs first each tick, so an existing penalty clears). Freezing damage stays. Sliding down a honey block's side stays whole, horizontal damping included: it is the wall-grab that saves a fall, not a walking slowdown. Both run on client and server, from the worn armor, so the client predicts it without sync | `LivingEntityMixin`, `EntityMixin` |
| Adamant armor piercing | wrap `getDamageAfterArmorAbsorb`: `share*d + (1-share)*original(d)`, so armor sees the whole hit (durability and the damage-dependent formula unchanged) and the result is never below vanilla's. Protection, Resistance (magic absorb) still apply after. Melee = damage type tag `maple_flavored:adamant_set_pierces` (player_attack, mob_attack, mob_attack_no_aggro, spear, mace_smash) and a direct hit (`source.isDirect()`) from an attacker in full Adamant | `LivingEntityMixin` (@WrapMethod) |
| Orichalcum save | `@Inject(HEAD, cancellable)` of `checkTotemDeathProtection`: unless the source bypasses invulnerability, `OrichalcumSave.trySave` → health 1, Totem effects (`DeathProtection.TOTEM_OF_UNDYING.applyEffects`), entity event 35, start recharge. Runs before the hand loop, so before any held Totem or Crystal Heart. Not a death, no Postmortal | `LivingEntityMixin` |

Steel/Electrum/Shakudo/Adamant apply to any living entity wearing the set (armor stands, zombies) – the rule is
about armor, as the damage reductions are. The Orichalcum save is players-only (its timer lives on the player).

Equipment change (Fabric `ServerEntityEvents.EQUIPMENT_CHANGE`, server, fires on a mob's/player's first tick after
load too): refresh Steel modifiers, clear Levitation when Steel completes, and for a `ServerPlayer` with a full set
fire `ALLOY_SET_WORN`.

## Advancement

A custom trigger, not `minecraft:tick` with equipment predicates, so `AlloySets` stays the one definition of a full
set (the Armored Elytra joins it in ticket 36 without touching advancement data).

`maple_flavored:nether/wear_every_alloy_set`: parent `nether/smelt_alloy_ingot`, challenge, 100 XP, five criteria (one
per Alloy) all required, icon Adamant chestplate. Title "Full Metal Wardrobe", description "Wear a full set of each
Alloy".

## Invariants

- A bonus is active exactly when all four humanoid slots hold one Alloy's gear; removing any piece ends it within the
  equipment-change tick (Steel) or immediately (on-demand checks).
- The Orichalcum save works in Hardcore (Jackson). It never fires from damage that bypasses invulnerability, or while recharging; it fires
  before hand items are checked.
- Recharge progress persists through logout and death and counts only while the player is online.

## Exclusions

The Totem animation shows the
client's default item (a Totem, or a held Crystal Heart). Armored Elytra counting waits on ticket 36.

## Tests (new `AlloySetGameTests`)

- full set detection: four pieces of one Alloy is a set; three plus Netherite or mixed Alloys isn't.
- Steel: knockback from `knockback()` and an explosion is zero with a full set, non-zero with three pieces; Levitation
  refused and removed on completing the set.
- Electrum: fire and lava damage ignored, set on fire doesn't burn; Withered Hearts form at 10 s, and swapping armor
  mid-timer switches to 5 s.
- Shakudo: `getBlockSpeedFactor` on soul sand/honey is 1; cobweb/powder snow leave `stuckSpeedMultiplier` unset.
- Adamant: a full-Adamant attacker's hit on an armored target deals `0.25d + 0.75*armor(d)` and never less than without the set; ranged hits are unchanged;
  Protection still reduces.
- Orichalcum: lethal hit saved with Totem effects; a held Totem is kept; a second lethal hit while recharging uses the
  Totem; recharge completes after the game rule's time with no armor worn; death and relogging keep the timer; no save
  without the full set; a Crystal Heart in either hand is kept; damage that bypasses invulnerability (`kill`) isn't
  saved and starts no recharge.
- Shakudo freezing: a frozen entity in full Shakudo has no frost speed modifier.
- Advancement: wearing all five sets in turn grants it; four doesn't; its parent, frame and XP.

Verification: `gradlew build runGameTest` (full suite).
