# Matcha Flavoured heart system

Research notes for Maple Flavored. They describe what Matcha Flavoured does and make no design recommendations.

## Source and confidence

Primary source: the Matcha Flavoured 1.12.9 data pack and resource pack for Minecraft 26.3. Paths starting `dp/` or `rp/` are inside the extracted download. `dp/data/matcha/function/setup/scoreboard/create_scoreboards.mcfunction` sets `current_version` to 1129.

Secondary context comes from [kleiwright/matcha-flavoured](https://github.com/kleiwright/matcha-flavoured), branch `26.3`. `hpdown.mcfunction` and `current_version` on that branch match the download; other files were not diffed.

Labels:

- **Fact**: directly stated by a cited pack file. Unlabelled statements in tables and lists are facts.
- **Inference**: my reading of how pack code or vanilla Minecraft behaves. Vanilla behaviour was not checked against 26.3 code.
- **Unconfirmed**: could not be established from what I read.

Nothing was run in game. All behaviour below comes from reading files.

Units: scores are in health points, so 2 points = 1 heart. A new player's 20 points is 10 hearts.

## Summary

Each player has a `Hearts` score that sets their base `max_health`. Death removes 1 heart, or 2 on Hard, but never takes the player below a floor. On Normal and Hard the floor is the player's own `minimum_hearts`, which starts at 10 hearts and falls 1 heart with each of seven progression advancements, down to 3 hearts. Using a Crystal Heart adds 1 heart, up to a cap of 30.

## State

| Score or value | Scope | Value | Source |
| --- | --- | --- | --- |
| `Hearts` | Per player | Copied from `max_health` at player setup | `setup/scoreboard/player_setup.mcfunction` |
| `$Max Hearts` | Global | 60 | `setup/scoreboard/create_scoreboards.mcfunction` |
| `minimum_hearts` | Per player | Starts at 20 | `setup/scoreboard/player_setup.mcfunction` |
| `$Easy minimum_hearts` | Global | 20 | `create_scoreboards.mcfunction` |
| `$Normal minimum_hearts` | Global | 12, the auto-Hard threshold | `create_scoreboards.mcfunction`, `decrease_minimum_hearts.mcfunction` |
| `$Hard minimum_hearts` | Global | 6, the lowest possible floor | `create_scoreboards.mcfunction` |
| `CrystalHeartCooldown` | Per player | 20 ticks after a use | `use_crystal_heart.mcfunction` |
| `current_world_settings_difficulty` | Global | 1 Easy, 2 Normal, 3 Hard | `create_scoreboards.mcfunction` |

Function paths in this table and below are under `dp/data/matcha/function/`. Heart functions are in `mechanics/heart_container/`.

`set_max_hp.mcfunction` is the only place `Hearts` becomes real health. It clamps the score, then runs `attribute @s minecraft:max_health base set` through `set_max_hp.macro.mcfunction`.

## Heart loss on death

1. The `deaths` objective uses the `deathCount` criterion (`create_scoreboards.mcfunction`).
2. `timers/tick.mcfunction` calls `detect_death` every tick, which runs `hpdown` as any player in `@a` with `deaths` at 1 or more.
3. `hpdown` resets `deaths` to 0, subtracts from `Hearts`, then calls `set_max_hp`.

| Cached difficulty | Points lost | Hearts lost | Floor applied by `set_max_hp` |
| --- | --- | --- | --- |
| 0 to 1 (Peaceful, Easy) | 2 | 1 | `$Easy` = 20 points |
| 2 (Normal) | 2 | 1 | Player's `minimum_hearts` |
| 3 (Hard) | 4 | 2 | Player's `minimum_hearts` |

The clamp also caps `Hearts` at `$Max`. A comment in `set_max_hp.mcfunction` says the change "will likely be done as the player is on the death screen".

**Inference:** the new `max_health` survives into respawn, because the mechanic depends on it. I did not check the vanilla respawn code.

### Other heart loss: Heartbreaker

Wither can also remove hearts without a death. `effects/wither/heartbreaker.mcfunction` clears Wither, calls the same `hpdown`, plays a sound, deals 0.1 damage, and grants the `matcha:hell/wither_breaks_heart` advancement (described in `rp` as "Fail to clear the Wither effect before time runs out"). So the loss amounts and floors in the table above apply.

- It only runs on cached difficulty 2 or 3 (`effects/wither/effect.mcfunction`).
- `effects/wither/mark_player.mcfunction` does not start the timer on Easy.
- `effects/wither/set_timer.mcfunction` sets the timer to 9 with no Shakudo resistance, then 11, 12, 13 or 14 at resistance 1, 2, 3 and 4 or more.
- **Inference, traced from `effect`, `effect_normal` and `effect_hard`:** at the once-per-second rate, a base timer triggers Heartbreaker on about the 9th check on Normal and the 6th on Hard.

## How the floor changes with progress

`decrease_minimum_hearts.mcfunction` subtracts 2 from the player's `minimum_hearts`. Seven advancements in `dp/data/matcha/advancement/tutorial/` call it as a reward:

| Advancement file | Criterion |
| --- | --- |
| `obtain_copper.json` | Copper ingot in inventory |
| `obtain_iron_ingot.json` | Iron ingot in inventory |
| `obtain_diamond.json` | Diamond in inventory |
| `obtain_electrum.json` | `heart_of_the_sea` in inventory, renamed Electrum Alloy in `rp` |
| `obtain_adamant.json` | `netherite_ingot` in inventory |
| `enter_nether.json` | Change dimension to the Nether |
| `find_stronghold.json` | Be inside a stronghold |

Each trigger moves the floor down a fixed ladder, so seven triggers reach exactly the `$Hard` value:

| Triggers | `minimum_hearts` | Floor in hearts | Effect |
| --- | --- | --- | --- |
| 0 | 20 | 10 | Start |
| 1 | 18 | 9 | |
| 2 | 16 | 8 | |
| 3 | 14 | 7 | |
| 4 | 12 | 6 | Equal to `$Normal`, no flip |
| 5 | 10 | 5 | Below `$Normal`: Normal worlds become Hard |
| 6 | 8 | 4 | |
| 7 | 6 | 3 | Lowest value |

Details from the same function:

- Lowering the floor changes neither `Hearts` nor `max_health`. It only matters at the next `set_max_hp` call. It does not call `set_max_hp` itself.
- If the score is outside -20 to 999, or missing, it is first set to 20.
- A value below `$Hard` is clamped to 6 and the function returns before any message.
- Each step prints a flavour line, such as `log.kleispack.god_grows_angry_1`, to the player. The step to 10 sends its line to `@a`.
- On cached difficulty 0 or 1 (Peaceful, Easy), the "Minimum Hearts is now set to %s" line is skipped, but the flavour lines still print.
- **Inference:** the order is not enforced, even though the advancements have parents. Criteria are inventory or location checks, so vanilla can complete a child before its parent. A player can also pass several steps quickly.

### Automatic Hard mode

On cached Normal, a player falling below `$Normal` runs `mechanics/difficulty_scaling/increase_difficulty.mcfunction`. It announces to `@a`, runs `difficulty hard`, and re-caches the difficulty. That is the only function in `mechanics/difficulty_scaling/`.

`setup/first_load.mcfunction` and `setup/disallow_hard_on_first_load.mcfunction` force a world started on Hard back to Normal on first load and reload, unless it is Hardcore. The in-game text says Hard "is not meant to be played out-of-the-gate" (`error.kleispack.hard_too_early.desc`).

**Inference:** Easy worlds never auto-flip, because the check requires cached difficulty 2. `minimum_hearts` still falls on Easy, so a later switch to Normal applies the lowered floor. A comment in `set_max_hp.mcfunction` states this intent.

## Crystal Hearts

### The item

A Crystal Heart is a `minecraft:poisonous_potato` with model `matcha:heart_container`, rare rarity and a glint (`dp/data/matcha/loot_table/treasure/crystal_heart.json`). It has:

- a `consumable` component: 60 second use time, spyglass animation, no consume effects
- `death_protection`: Regeneration amplifier 10, duration 3, and Resistance amplifier 4, duration 1
- `!minecraft:food`, removing its food component

### Using one

`dp/data/matcha/advancement/mechanics/crystal_heart_used.json` fires on `minecraft:using_item`, which calls `use_crystal_heart.mcfunction`.

1. Revokes the advancement for `@a` so it can fire again.
2. Stops if `CrystalHeartCooldown` is 1 or more.
3. Stops if `Hearts` is at or above `$Max`.
4. Stops if a Totem of Undying is in the main hand (`dp/data/matcha/predicate/crystal_heart/not_holding_totem.json`). A comment says the totem would be used instead of the heart.
5. Runs `damage @s 999 minecraft:magic`, which the comment labels "Remove Heart (kill them)".
6. Adds 2 to `Hearts`.
7. Gives Regeneration 11 for 3 seconds and Resistance 5 for 1 second, using the command's amplifiers 10 and 4.
8. Calls `set_max_hp`.
9. Sets the cooldown to 20 ticks. A comment says it "MUST BE AT LEAST 20 OR IT BREAKS".

The item is consumed indirectly. **Inference:** the 999 damage is lethal, `death_protection` triggers as a totem would and consumes the held heart, leaving the player alive. No code removes the item directly. Because `using_item` fires as soon as use starts, the 60 second use time does not delay the gain.

Reaching the cap from a 20-point start takes 20 uses, each +2. A heart above the cap is rejected by step 3. A commented-out line references a future `matcha:with_songs/max_health` advancement.

**Further inferences about use, none tested:**

- Holding a heart in hand also saves the player from a lethal hit as a totem would, without adding to `Hearts`. The advancement text says "mortals were only able to use their Undying properties" (`adv.kleispack.obtain_heart.desc`). `death_protection` durations are probably ticks, so this path's effects would be far shorter than those in step 7.
- With a totem in the off hand and a heart used from the main hand, vanilla's hand order should consume the heart, since the predicate checks only the main hand.
- If `damage` deals nothing, for example in Creative or under Resistance 5, the item would not be consumed but `Hearts` would still rise. This may be why the cooldown must outlast the 1 second Resistance from step 7.
- A player at the cap who holds use for the full 60 seconds would probably consume the item with no gain, since steps 2 to 4 stop only the function.

### Guaranteed sources

Advancement rewards are once per player. `tutorial/obtain_*.json` below are under `dp/data/matcha/advancement/`.

| Advancement | Title | Hearts | Loot table |
| --- | --- | --- | --- |
| `tutorial/obtain_opal.json` | Canto I, "Defeat the Sea God" | 1 | `advancement/crystal_heart_1` |
| `tutorial/obtain_ruby.json` | Canto II | 1 | `advancement/crystal_heart_1` |
| `tutorial/obtain_amber.json` | Canto III | 1 | `advancement/crystal_heart_1` |
| `tutorial/obtain_topaz.json` | Canto IV, "Raid Tartarus" | 2 | `advancement/crystal_heart_2` |

That totals 5 Crystal Hearts per player. `tutorial/obtain_heart.json` rewards Obols, not a heart.

### Loot

Each table below is under `dp/data/minecraft/loot_table/` and references `matcha:treasure/crystal_heart`. The chance column is **inference**: my arithmetic at Luck 0, assuming a uniform 0 to 1 roll count means 50% for one roll.

| Table | Pool | Weight | Chance per chest |
| --- | --- | --- | --- |
| `chests/abandoned_mineshaft.json` | 0 to 1 rolls | 1 of 2 | 25% |
| `chests/simple_dungeon.json` | 0 to 1 rolls | 1 of 2 | 25% |
| `chests/stronghold_corridor.json` | 0 to 1 rolls | 1 of 1 | 50% |
| `chests/abbey/tower.json` | 0 to 1 rolls | 1 of 1 | 50% |
| `chests/abbey/barracks.json` | 1 roll | 33 of 99, quality 1 | 33.3% |
| `chests/ancient_city.json` | 1 roll, bonus 1 | 2 of 11, quality 1 | 18.2% |
| `chests/trial_chambers/reward_unique.json` | 1 roll | 4 of 18 | 22.2% |

Fishing: `gameplay/fishing/treasure.json` has the heart at weight 6 of 171, quality 2. `gameplay/fishing.json` has treasure at weight 5 of 100, quality 1. So the heart is about 3.5% of treasure catches and about 0.18% of catches at Luck 0 (**inference**, ignoring any other fishing conditions). The Angler's Almanac advancement displays "3.3%" (`dp/data/matcha/advancement/anglers_almanac/treasure/crystal_heart.json`). **Unconfirmed:** what that figure measures. It does not match either number.

### Structure

The Abbey room `dp/data/minecraft/structure/abbey/default/rooms/boon_heart.nbt` contains the strings `item.kleispack.crystal_heart`, `matcha:heart_container` and `particle_crystal_heart`. It is one of three equal-weight entries in `dp/data/minecraft/worldgen/template_pool/abbey/default/boon_rooms.json`, alongside `boon_checkpoint` and `boon_vault`.

**Inference:** it holds a single placed Crystal Heart item. **Unconfirmed:** the count, placement, and how often the boon pool is chosen. I scanned strings and did not fully parse the NBT.

### Crafting

`dp/data/matcha/recipe/crafting/crystal_heart.json`: 8 diamonds around 1 `turtle_scute`, making 1 heart. `rp` names `turtle_scute` "Pith"; the pack's Divine Fragment is a glinted, rare version of the same item (`dp/data/matcha/loot_table/treasure/divine_fragment.json`).

`dp/data/matcha/advancement/recipe_unlocks/divine_fragment.json` unlocks the recipe on holding `turtle_scute`, or on unlocking `matcha:crafting/divine_favour`. The recipe key is the plain item id, so **inference:** any turtle scute works. Divine Fragment sources include spawners, Evokers, Elder Guardians and Piglin Brutes, but I did not trace their chances.

### Villager trades

Files are under `dp/data/matcha/villager_trade/`. All have `max_uses` 999.

| Trade | Villager level | Gives | Wants |
| --- | --- | --- | --- |
| `librarian/1/divine_comedy.json` | Librarian 1 | 1 heart | Divine Comedy book |
| `librarian/1/paradise_lost.json` | Librarian 1 | 1 heart | Paradise Lost book |
| `cartographer/5/cheerful_clay_statue.json` | Cartographer 5 | 1 heart | Cheerful Clay Statue |
| `cartographer/5/mournful_clay_statue.json` | Cartographer 5 | 1 heart | Mournful Clay Statue |

Divine Comedy and Paradise Lost come from `chests/buried_treasure`, `chests/shipwreck_treasure` and `matcha:chests/fishing/treasure`.

**Unconfirmed:** how players obtain the clay statues. `treasure/cheerful_clay_statue` exists, but no JSON or function file in the pack references either statue's treasure table. Structure NBT was not searched for this.

Trades consume hearts too:

| Trade | Hearts spent | Other cost | Gives |
| --- | --- | --- | --- |
| `librarian/2/avesta.json`, `quran.json`, `tanakh.json` | 1 | The named book | Ofuda (blessing) |
| `librarian/3/enoch.json` | 1 | The named book | Ofuda |
| `librarian/4/solomon.json` | 2 | The named book | Ofuda |
| `librarian/5/crystal_heart.json` | 2 | 1 Echo Shard | 1 Divine Fragment |

### Not a heart source

- A search of the pack for `Hearts` writes finds only `hpdown`, `use_crystal_heart`, `set_max_hp` and `player_setup` for real players. `clear_everything.mcfunction` only sets unused fake-player entries.
- Cake and one favourite-food item grant Health Boost (`mechanics/cake_eaten.mcfunction`, `favorite_food/food_trades/food_33.mcfunction`). These are temporary effects, not `Hearts`.
- `matcha_item/item_modifier/modify/divine_comedy.json`, `paradise_lost.json` and both statue modifiers contain Crystal Heart components. **Inference:** this is inert. Their triggers compare `item_name` to the string `matcha:heart_container`, which no real item uses. **Unconfirmed:** not tested in game.

## Edge cases

### Keep Inventory

- `setup/gamerules.mcfunction` sets `keep_inventory true`, along with `natural_health_regeneration false` and others.
- `setup/first_load.mcfunction` runs that only once per world, gated by the `gamerules_setup` score. **Inference:** a player can change it afterwards and the pack will not reset it.
- A search of the pack finds `keep_inventory` only in that one function. The heart functions never read it, so heart loss is the same whether it is on or off.
- **Inference:** with it off, held hearts drop on death like other items. The pack has no special handling.
- The repo's `changelog.md` notes that on Purpur servers `keep_inventory` is per dimension and must be set by hand. I did not establish which version that entry belongs to, or test it.

### Multiplayer

- `Hearts`, `minimum_hearts`, cooldown and advancements are per player.
- `setup/player_update_check_loop.mcfunction` runs every second and sets up any player whose `version_number` is behind. Its comments say this exists for multiplayer, because joining does not reload the world.
- Difficulty is global. **Inference from `decrease_minimum_hearts`:** the first player on Normal to hit `minimum_hearts` 10 moves the whole world to Hard, so all players then lose 2 hearts per death, while each keeps their own floor. A newly joined player keeps a floor of 10 hearts.
- If admins return the world to Normal and reload, **inference:** the next step by any player already below 12 flips it to Hard again.
- `use_crystal_heart` revokes `crystal_heart_used` for `@a`, not `@s`. **Inference:** this is harmless, since the advancement only tracks an in-progress use, but I did not test simultaneous use.
- **Inference:** a player who dies and disconnects before the next tick keeps their `deaths` score and is processed once they are back in `@a`.

### Difficulty changes

- Heart logic reads a cached score, set at load and in `increase_difficulty`. **Inference:** changing difficulty without reloading leaves heart logic on the old value. The in-game disclaimer `log.kleispack.difficulty_disclaimer` tells players to reload.
- On Easy, the clamp raises `Hearts` to 20 only when `set_max_hp` runs. **Inference:** a player at 14 points moved to Easy stays there until their next death or heart use, then jumps to 20. A comment there says "never goes below 10".
- A player moved from Easy back to Normal is floored by their own lowered `minimum_hearts`, not 20. Hearts at 20 can then fall.

### Hardcore

`setup/disallow_hard_on_first_load.mcfunction` aborts its Hard warning on Hardcore. `changelog.md` records "Hard mode flipper no longer appears in hardcore". **Unconfirmed:** how hearts behave in Hardcore beyond this. **Inference:** the cached difficulty there reads 3.

### Version updates

`setup/update_this_player.mcfunction` re-runs `player_setup` when a player's version is behind, so `Hearts` is re-copied from `max_health`. `minimum_hearts` is kept if at least `$Hard`. **Inference:** `attribute get` returns the total including modifiers, so updating during Health Boost could inflate `Hearts`. Not tested.

### Developer helpers

`clear_me.mcfunction` revokes all advancements but leaves `minimum_hearts` alone. **Inference:** re-earning the seven advancements would lower the floor again, though it is clamped at 6. These helpers are not called by normal play as far as I found.

## Conflicts between the repo and the release

- `my_current_and_future_plans.md` marks "Paradise Lost and Divine Comedy no longer giving Hearts" as done, but both 1.12.9 trades in the table above give a heart. **Unconfirmed:** whether the plan meant something else, such as a loot change.
- The same file says "Heart loss being dynamic (ie increases with difficulty and hearts)" was pushed to a later update. In 1.12.9 loss varies with difficulty only.
- The same file says Crystal Hearts are "too hard to loose late in the game" and describes a planned Wither Fight attack removing a heart without death. Heartbreaker implements a form of this through the Wither effect.
- Open issue [#477](https://github.com/kleiwright/matcha-flavoured/issues/477), created 2026-10-18, is titled "If on hard, you can dip below the minimum hearts". The body gives no cause. **Unconfirmed:** I found no path in `set_max_hp` that allows this, since it always clamps to `minimum_hearts` on difficulty 2 or above, but the bug may be outside what I traced or need runtime conditions.

## Not checked

- No behaviour was tested in game, and vanilla 26.3 code was not read.
- `generate_item_update_files.py` and other repo tooling were not read.
- The chance of finding Abbey structures, boon rooms, and each gem were not traced.
- Only `hpdown` and `current_version` were compared between the release and branch `26.3`.
