# Brewing

Type: grilling
Status: resolved
Blocked by: 04

## Question

What do we change about brewing, and what role does it hold beside healing and food?

## Answer

Brewing stays vanilla apart from potion stacking, a throw cooldown and the Weakness recipe. The enchanted golden apple covers effects brewing can't give, gated by Library Progress and Enchantment Slots. Numbers are starting values that balance work can change. Vanilla numbers are from memory and not checked against the 26.3 jar.

### Potions

- Potions, splash potions, lingering potions and water bottles stack to 16, matching honey bottles.
- Throwing a splash or lingering potion starts a 1 second cooldown shared by both kinds, like an Ender Pearl's, so switching stacks doesn't get around it. Drinkable potions have no cooldown, since drinking already takes 1.6 seconds.
- The cooldown applies only to players, so dispensers throw potions as fast as in vanilla.
- Each bottle slot in a brewing stand still holds one bottle, and hoppers fill them one at a time, so brewing throughput stays vanilla.
- Weakness is brewed only from Strength, as [Getting villagers and removing villages](08-villagers-and-villages.md) decided. All other recipes stay vanilla.

### Enchanted golden apple effects

The apple offers vanilla's four effects at vanilla's strengths. Each unlocks at a Library Progress threshold, and each takes an Enchantment Slot, so an apple with all four needs level 120.

| Effect | Strength and duration | Unlocks at |
| --- | --- | --- |
| Regeneration II | 20 seconds | 15% |
| Fire Resistance | 5 minutes | 30% |
| Absorption IV | 2 minutes | 50% |
| Resistance | 5 minutes | 75% |

Absorption and Resistance aren't brewable, so the apple doesn't crowd out potions. A gold farm makes apples renewable, but slots, lapis, a block of gold and the Library thresholds keep strong apples late. Fire Resistance overlaps with its potion, but it's cheap and vanilla already gives it.

## Comments

From [Getting villagers and removing villages](08-villagers-and-villages.md):

- Weakness is brewed only by adding a fermented spider eye to Strength: Strength gives Weakness, Long Strength gives Long Weakness, and Strength II gives Weakness. A fermented spider eye on a water bottle does nothing.
- Village brewing stands are removed.

## Comments

From [Healing and food](04-healing-and-food.md), which kept vanilla food. These came from [Getting villagers and removing villages](08-villagers-and-villages.md):

- An enchanted golden apple is made at the enchanting table and carries effects chosen like enchantments, one Enchantment Slot each, at 1 lapis per effect plus a block of gold on the first save. Regeneration II unlocks at 15% Library Progress. Decide the other effects (Fire Resistance, Absorption and Resistance were suggested, maybe more), their strengths and durations, and when each unlocks. The default is a Library Progress threshold per effect. The alternative is tying an effect to a shelved enchantment, such as Fire Resistance to Fire Protection IV.
- With a gold farm, these apples are renewable, so weigh them against brewing, as the Matcha manifesto warns.
