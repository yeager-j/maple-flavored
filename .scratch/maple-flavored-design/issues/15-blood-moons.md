# Blood moons

Type: grilling
Status: resolved
Blocked by: 03, 06

## Question

Should rare, periodic blood moons make mobs that don't normally respawn come back, as in Breath of the Wild, so that sources like Piglin Brutes' Ancient Embers and Pith become renewable? If so, what do they respawn, how often and where, how are they announced, and how do they fit a shared multiplayer world and farms?

## Answer

Yes. A Blood Moon Restores structures' mobs, making Ancient Embers and Pith slowly renewable. Terms are in `GLOSSARY.md`. Numbers are starting values that balance work can change.

### What is Restored

Blood Moons Restore mobs, never blocks or loot.

- Bastions: Piglins, Piglin Brutes and Hoglins
- Ocean Monuments: Elder Guardians
- End cities: Shulkers
- Woodland mansions: Vindicators and Evokers
- Strongholds: their Illagers, added by the Enchanting ticket

Allays, chests and broken spawners don't return. Diamonds stay non-renewable, so Crystal Heart crafting keeps its limit, and a broken spawner's 1 Pith stays a one-time reward.

### Where mobs return

Each mob returns only at its structure's original spot, as the same mob that spot first rolled, so a bastion's Brute count is a fixed trait. Placing blocks elsewhere attracts nothing.

The spot must still be clear, and the right block must still be there:

| Mob | Condition at the spot |
| --- | --- |
| Bastion mobs | Blackstone family or basalt beneath |
| Elder Guardian | In water, with prismarine family beneath |
| Shulker | Attached to purpur family or end stone bricks |
| Mansion illagers | Birch planks, cobblestone or smooth stone slab beneath |
| Stronghold illagers | Stone bricks family beneath (proposed) |

So tearing down a bastion or draining a monument ends its Restoring. Restoring never replaces a block, and no mob appears within 32 blocks of an online player. A base inside an intact bastion can still get visitors.

### How Restoring works

A structure is Restored when its chunks next load after a Blood Moon, topped up to its original population. Missed Blood Moons don't stack, untouched structures are unaffected, and nothing loads far-off chunks on the night.

### The night

A Blood Moon falls on every 4th full moon, every 32 in-game days, on a fixed schedule. It's a restock, not a siege: red moon and sky, one chat message in every dimension, sleeping allowed and no extra spawns. Other effects of a Blood Moon can be proposed in the grilling ticket they belong to.

### Farms and supply

Ancient Embers stay killed-by-player, so Alloy ingots still can't be fully automated. A monument's 3 Elder Guardians give 6 Pith per Blood Moon, or 1.5 Netherite Ingots.

### Vanilla facts (26.3 jar)

- Bastion mob spots are 144 jigsaws across the templates, rolled from weighted pools: a general piglin jigsaw gives a Brute 10% of the time and a melee jigsaw 6 in 7. The `melee_piglin` template holds a Brute.
- End cities use `Sentry` markers, and mansions use `Warrior`, `Mage` and `Group of Allays` markers.
- Elder Guardians aren't in templates: 3 hardcoded `spawnElder` calls set their spots.
- Saved structure data keeps each piece's template and position, so spots should be recoverable. This was read from the jar, not tested in a world.

## Comments

From [the enchanting manifesto](../../../docs/manifestos/enchanting.md): Strongholds are filled with Illagers that drop enchanted books, and Blood Moons Restore them like mansion Illagers. The stone bricks condition is a proposal to confirm.
