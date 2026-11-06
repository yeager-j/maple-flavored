# Structures

Type: grilling
Status: resolved
Blocked by: 06, 08, 13

## Question

Which structures do we add or change, starting with the Abbey, and what does each give the player?

## Answer

Structures stay vanilla apart from their books, a few loot changes, dungeon frequency and how spawners fight back. No new structures are added. Terms are in `GLOSSARY.md`. Numbers are starting values that balance work can change. Research: [Matcha Flavoured's structures](../../../docs/research/matcha-structures.md).

### Spawners

Placing a torch no longer wins a dungeon. Every spawner works like a small trial spawner:

- It ignores light, for every mob it spawns.
- It's unbreakable and immune to explosions until it's **Cleared**: 12 of the mobs it spawned have died, plus 2 for each additional player. Any death counts, whatever killed it, so traps and lava work.
- An additional player is anyone who has come within its 16 block activation range since its count began. The requirement never goes down, so leaving doesn't lower it.
- The count is shared, a Cleared spawner stays Cleared, and the spawner shows its progress, for example with particles or cracks.
- Once Cleared, it breaks as normal and drops its 1 Pith. Keeping it for a farm is fine.
- Trial spawners stay vanilla, including their 75 second break time.

### Dungeons

Dungeons generate at Matcha Flavoured's frequency, so early Pith isn't too rare: 15 attempts per chunk above Y 0 and 8 below, against vanilla's 10 and 4. Each keeps a single vanilla spawner.

### Books

- The Nether Fortress's book is locked, visibly caged or chained, until every Blaze spawner that generated in that fortress is Cleared. Clearing is enough, so a blaze farm can stay. A fortress with no Blaze spawners has its book unlocked from the start. Taking it wakes nothing.
- No other structure wakes when its book is taken beyond those [Enchanting](10-enchanting.md) named: the Bastion, Stronghold and Woodland Mansion. The Ancient City's book is locked behind keeper shriekers instead (see Enchanting).
- Stronghold Illagers are Restored by a [Blood Moon](15-blood-moons.md) only where the stone bricks family is still beneath their spot.

### Crystal Hearts and Pith in chests

Crystal Hearts appear only in chests guarded by a real threat, and Pith appears more widely at low odds. Each entry is a separate roll per chest giving 1 item. These are half of Matcha's rates or less.

| Chest | Crystal Heart | Pith |
| --- | --- | --- |
| Ancient City | 9% | 18% |
| Stronghold corridor | 12.5% | 6% |
| Bastion treasure | 25% | 5% |
| Other Bastion chests | none | 5% |
| Woodland Mansion | 12.5% | none |
| Dungeon | none | 5% |
| Mineshaft | none | 5% |
| Ominous vault, per opening | none | 10% |
| End City | 15% | 50%, 1 or 2 |

End City odds come from [Storage and End City rewards](13-storage-and-end-rewards.md). Matcha's Ancient City entry is written to give 1 to 3 Pith but is malformed, and in Jackson's play it always gave 3. Ours gives 1.

### Villages

Village chests stay vanilla apart from what's already ruled out: diamonds, obsidian and iron gear are dropped, not replaced, stone tools become copper, and enchanted golden apples become plain ones. An abandoned village should feel picked over.

### Vanilla facts (26.3 jar)

- A trial spawner has hardness 50, needs no particular tool and isn't in any tool's mineable tag, so every tool breaks it in 75 seconds. Haste helps. From memory, Efficiency doesn't.
- A normal spawner has hardness 5 and needs a pickaxe.
- Trial spawners default to 6 mobs, 2 at a time, plus 2 mobs and 1 at a time per extra player. Ominous spider, cave spider, silverfish and slime spawners use 12, and breeze spawners 2 (4 ominous), plus 1 per extra player.

None of this has been tested in a world.

## Comments

From Hearts and death: Ancient Cities should probably be a rich reward because of the Warden, but a cheap Warden switch (a piston clock, an arrow and spawn-proofing) disables Warden spawning, so each city can give 3-4 Crystal Hearts and a dozen Pith at Matcha's odds for about 10 minutes of setup. Vanilla's Ancient City loot also has little of interest. Decide Crystal Heart and Pith odds per structure, starting from Matcha's, with this in mind.

From [the enchanting manifesto](../../../docs/manifestos/enchanting.md):

- Every structure holds one floating book, and grabbing it wakes the structure's danger. The sources are in the Enchantment Sources spreadsheet linked from the manifesto.
- Strongholds might be filled with Illagers, giving Sharpness and Protection a home outside raids. Consider adding Strongholds to the Blood Moon list if so.

From [Getting villagers and removing villages](08-villagers-and-villages.md):

- Villages are all abandoned, at spacing 80 and separation 50. Hay bales and brewing stands are removed. Crops, beds, bells and the other workstations stay. Village chests never hold diamonds, obsidian or iron gear: decide their exact contents.
- The igloo basement's villager becomes a zombie villager.
- Every enchanted golden apple in a loot table becomes a plain golden apple.

From [Mob changes](05-mob-changes.md): the Warden switch is closed by the new summoning rules, so Ancient City rewards don't need to allow for it. Warden Territory uses the Ancient City's bounds.
