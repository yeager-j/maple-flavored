# The role of XP

Type: grilling
Status: resolved
Blocked by: 10

## Question

What should XP be spent on, given the enchanting decision, and how does it interact with farms and repair?

## Answer

XP is your standing as an enchanter: level sets your Enchantment Slots, and book combining is its only real cost. Death now puts all of it at risk, but you can always go back for it. Terms are in `GLOSSARY.md`. Numbers are starting values that balance work can change. The rules are fixed.

### What XP does

- Enchantment Slots keep growing every 30 levels with no cap. A sword with seven useful enchantments needs level 210, or 166,545 points.
- Combining two books costs 50 points × the resulting level: 100 for level II and 250 for level V. Building a level V book from level I takes 2,050 points, and a Full Library takes 27,100, about the XP to reach level 90.
- Renaming costs a flat 10 points. Nothing else spends XP.
- Repairs cost no XP, because crafting a new item doesn't either. This changes [Enchanting](10-enchanting.md).
- The anvil gives no warning when a cost would drop you below a slot threshold.
- All XP sources and Mending stay vanilla. Bottle o' Enchanting trades stay with trading economy details.

### Loopholes closed

- The grindstone gives no XP from gear or books. Otherwise saving and grinding would turn lapis into XP.
- The crafting grid and grindstone can't combine damaged items.
- The anvil can't apply a book to gear, so the table is the only way to enchant.

### Remnants

Dying drops all your XP as a Remnant, in exact points, in place of vanilla's partial drop. Inventory stays, and this adds to the heart cost in [Hearts and death](03-hearts-and-death.md). Stakes grow with progress: hearts and the Heart Floor matter most early, and a Remnant matters most late.

- **Owner only:** only you can see or collect it, so it can't be stolen or used to hand XP to a friend. It shows as a glow at its location.
- **Lasting:** it can't be destroyed and doesn't despawn, including while you're offline or its chunk is unloaded.
- **Collecting:** walking over it returns everything at once, not as orbs, so Mending can't eat it.
- **Saves:** a Crystal Heart or Totem save isn't a death, so nothing drops.
- **Placement:** it stays where you died, or appears at your last solid ground if you died in the void, in lava or anywhere else it couldn't be collected. Inside a Warden's Sealed Subspace it stays put, so you can collect it once the Subspace ends.
- **Tracking:** vanilla's Recovery Compass is the only tracker and points to your most recent death.

### Dying again

You can hold any number of Remnants. Each death cuts every uncollected Remnant by 20% of its current amount, whether or not that death leaves a new one, so dying at 0 XP still counts. A Remnant disappears only when it rounds down to nothing.

| Deaths after the first | Points left from level 150 | About the level that gets you |
| --- | --- | --- |
| 0 | 79,095 | 150 |
| 1 | 63,276 | 136 |
| 2 | 50,621 | 124 |
| 3 | 40,497 | 112 |
| 5 | 25,918 | 92 |

None of this has been tested in a world.

## Comments

From [the enchanting manifesto](../../../docs/manifestos/enchanting.md): level sets how many enchantments an item can hold, 1 at level 30 and 1 more every 30 levels. Enchanting is paid with one ingot of the item's material per session and one lapis per enchantment, not XP. The table has no XP upgrades. Anvils keep their XP costs, which now also lower a player's enchantment slots.

From [Getting villagers and removing villages](08-villagers-and-villages.md):

- Enchanted golden apple effects each take an Enchantment Slot, so the first apple, and with it the first cured villager, needs level 30.
