# Mob changes

Type: grilling
Status: resolved
Blocked by: 04

## Question

Which mob changes do we include, such as fast zombies and lower health on skeletons and baby zombies, and with what numbers given the heart and healing systems?

## Answer

Common mobs are made more dangerous on the surface without becoming a slog, each change needing its own reason, and bosses get Subspaces that shut down their cheapest escape. Terms are in `GLOSSARY.md`. Numbers are starting values that balance work can change. The rules are fixed. Common mob numbers come from Matcha Flavoured 1.12.9, with credit.

### Common mobs

These apply to every mob of the type, on spawn. Anything not listed stays vanilla.

| Mob | Easy | Normal | Hard |
| --- | --- | --- | --- |
| Adult zombie | vanilla | 10 health, 0.34 speed, steps up 1 block | 15 health, 0.38 speed, steps up 1 block, 5 damage, 0.6 jump |
| Baby zombie, husk, drowned and zombie villager | 4 health | 4 health | 7 health |
| Skeleton, stray, bogged and parched | 10 health | 10 health | 12 health |
| Creeper | 14 health | 16 health | 18 health |
| Cave spider | 2 health, 0.35 speed | 4 health, 0.40 speed | 6 health, 0.42 speed |
| Silverfish | 2 health | 2 health | 4 health |
| Spider | vanilla | 0.85 jump, no fall damage | 0.85 jump, no fall damage |
| Adult husk | vanilla | 30 health, 0.25 speed, 6 damage, 12 armor, 50 follow range, 0.9 knockback resistance | 40 health, 0.265 speed, 8 damage, 14 armor, 60 follow range, full knockback resistance |

- Adult zombie villagers stay vanilla, so curing one isn't a chase.
- Skeletons get enchanted bows only through vanilla's local difficulty.
- Husks spawn only where vanilla spawns them.

### Spawning

Creepers don't spawn naturally at Y 63 or above in the Overworld, with no exceptions for structures or caves. Spawners aren't affected. All other spawn rules stay vanilla.

### Gear

Mobs can carry enchantments but can't be a source of them.

- Gear a mob spawned with keeps vanilla's drop chance and drops with its enchantments removed.
- Gear a mob picked up always drops exactly as it was.

### Subspaces

The Warden and Wither Subspaces follow [the boss subspaces manifesto](../../../docs/manifestos/boss-subspaces.md), including sizes and overlap rules, except where this answer differs. Neither has a barrier: players can enter and leave. Every Warden opens its own fixed sphere with a 100 block radius centred where it spawns, even inside another Subspace. Each Subspace ends when its own Warden goes away. Subspaces can overlap without shifting their centres or blocking summons, and Grounded does not stack with itself. The Elder Guardian and Ender Dragon get none unless playtesting finds a cheap way to avoid their fights.

Inside the Warden's Subspace:

- Players are Grounded: horizontal glide drag is multiplied by 0.97 per tick and Firework Rocket boost is reduced by 95%. Both stack with the Armored Elytra's penalty.
- Grounded greatly increases Ender Pearl gravity, sharply shortening throws. Pearls still teleport on landing and remain useful for reaching nearby cover or crossing gaps. The exact gravity multiplier needs playtesting.
- Riptide and Zephyr's double jump don't work. Levitation, Slow Falling and Chorus Fruit still work, and nothing blocks crossing the Subspace's edge.
- Players can escape by leaving the active Subspaces, or sneak and hide until their Wardens go away. Each Subspace ends independently when its own Warden goes away.
- Dying costs hearts and respawns you as normal, even at a bed or anchor inside the Subspace.

### Summoning a Warden

This replaces the vanilla rule and closes the Warden switch, where a spawn-proofed shrieker keeps failing to summon while its cooldown locks out the others.

- A shriek counts only for players in Warden Territory. Others get no warning level or cooldown, so a chunk-loaded switch does nothing to a player elsewhere.
- The Warden spawns 5 to 15 blocks from the player who caused the shriek, or from the nearest player in Warden Territory if they aren't there. A friend's arrow can therefore summon on someone else in Warden Territory.
- With no room there, the search widens to other spots within the Subspace, up to 50 blocks from that player. This spawn search distance is separate from the Subspace's 100 block radius. If that fails too, the cooldown is cleared.
- If nobody is in Warden Territory when the summon happens, it's cancelled.

### Withered Heart recovery

This changes [Hearts and death](03-hearts-and-death.md). Withered Hearts return anywhere except the Nether when the player doesn't have the Wither effect, so the End allows recovery, which matters for PvP. Inside the Wither's Subspace they never return, in any dimension. No other enemy blocks recovery, and the 5 second interval stays.

### Vanilla facts (26.3 jar)

- A shriek checks a 10 second cooldown shared by players within 16 blocks, and always adds the player who caused it, wherever they are.
- Shriekers also generate in the Deep Dark outside Ancient Cities and can summon Wardens, though less often than in cities, so Warden Territory includes the biome. This comes from Jackson's play experience rather than the code.
- None of this has been tested in a world, and the Grounded numbers need playtesting.

## Comments

From Hearts and death:

- Should Warden spawn rules change so that a Warden switch can't trivially disable the Ancient City's danger? See Structures.
- Which enemies project an aura that blocks Withered Heart recovery? Tune the Wither effect's 5 second interval here too.

From [the boss subspaces manifesto](../../../docs/manifestos/boss-subspaces.md):

- When a boss appears, it opens a Subspace that shuts down the cheapest way to avoid its fight. The Warden's applies Darkness and Grounded in a 100 block radius, with no barrier. Grounded weakens flight and greatly increases Ender Pearl gravity, keeping short teleports useful while making escape a journey through the area.
