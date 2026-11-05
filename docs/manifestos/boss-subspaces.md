# Bosses need subspaces

Jackson's opinion on bosses, written down while working out [enchanting](enchanting.md). It's a summary of intent rather than a quote. Where a resolved ticket on the design map differs, the ticket wins. [Mob changes](../../.scratch/maple-flavored-design/issues/05-mob-changes.md) adopted this and replaced parts of it, including how a Warden summons and where Withered Hearts recover.

## The problem

Bosses can be escaped too easily. The Warden is the clearest case: it's terrifying on foot and useless if you can fly. If you summon one by accident with an Elytra and rockets, you fly to the other side of the Ancient City and wait it out.

## The idea

When a boss appears, it opens a subspace: an area that applies an effect. Each boss's subspace shuts down the cheapest way to avoid its fight, so the fight it was designed for happens. Players can enter and leave. Vanilla already does a small version of this: the Elder Guardian's Mining Fatigue stops you from digging into an Ocean Monument.

This matters for enchanting. Grabbing the Ancient City's book spawns a Warden, and without a subspace an Elytra makes that encounter trivial.

Subspaces start with the Warden. The Wither is next, and other bosses come later, once their own cheeses are identified.

## The Warden's subspace

- **Where:** a fixed sphere centred where the Warden spawns, with a radius of 100 blocks. It doesn't move with the Warden. (Ticket 26 shrank the radius to 50 blocks.)
- **Effect:** Darkness and Grounded. Grounded stops Elytra from working (so Firework Rockets can't boost either), stops Riptide and Zephyr's double jump, and greatly increases Ender Pearl gravity. Pearls fall sharply and travel much shorter distances, but their teleports still work. The gravity multiplier needs playtesting.
- **Escape:** there is no barrier. Sneaking, hiding and finding a physical route out are valid ways to survive. Short pearl throws can still help you reach nearby cover or cross a gap.
- **Ending:** each subspace ends when the Warden that opened it goes away, which in vanilla happens after a minute without being disturbed. You can leave earlier if you reach the edge.
- **The Ancient City's book:** a Warden spawned by grabbing it is angry from the start, which makes it a bit tougher.

Other escapes cost the player more than they'd gain. Pillaring makes noise unless it's done with wool, and the Warden's sonic boom and sniffing both reach 20 blocks vertically. Ender Pearls and Chorus Fruit make sound.

### What it looks like

When the Warden spawns, sculk spreads from it to cover the ground of the subspace, and the subspace gets a sculk skybox. The spreading sculk can grow new sensors, catalysts and shriekers. (Ticket 26 changed this: a weak patch spreads only around where the Warden spawns, only sensors and shriekers grow, as from a vanilla catalyst, and a sculk shell marks the edge with a sculk-dark sky inside.) Grown shriekers can summon Wardens while the subspace is active, and are set to `can_summon: false` when it ends, so the city doesn't escalate after each fight.

### How the Warden finds you

A vanilla Warden is aware of every entity in a 50 by 50 box around itself. It slowly pathfinds towards the closest one, preferring players, unless it detects a vibration within 15 blocks. Pathfinding towards a player isn't the same as knowing where they are: sniffing is what raises its anger.

A Warden is aware of everything inside its subspace, so it can always pathfind towards you wherever you hide. Vanilla's box reaches only about 25 blocks from the Warden, and two points in a 100 block radius subspace can be 200 blocks apart. With this rule, the subspace's size is chosen for how much room there is to sneak and escape. Anger still comes only from sniffing.

### Multiple Wardens

Every Warden opens its own subspace, centred where it spawns, including when it spawns inside another subspace. Each subspace lasts until its own Warden goes away.

Subspaces can overlap. There is no barrier to trap players, so no displacement or overlap-based summon failure is needed. Grounded does not become stronger where subspaces overlap.

Grabbing the Ancient City's book always makes an angry Warden, even if one is already within 24 blocks. This skips vanilla's nearby-Warden check. Overlapping subspaces do not prevent the book from being taken. Summoning positions follow [Mob changes](../../.scratch/maple-flavored-design/issues/05-mob-changes.md).

### Logging out

A player who logs out inside the subspace could leave the Warden with no one to hear, ending the subspace. That's no worse than switching to Creative or teleporting away, so it isn't solved.

## The Wither's subspace

The Wither's subspace has no wall, and you can come and go. Running away isn't how players cheese the Wither, so the subspace doesn't need to trap anyone. Its job is to block recovery: Withered Hearts don't return while you're inside it.

The Wither's real cheese is trapping it under bedrock so it can't move. To stop that, the Wither is immune to suffocation and passes through bedrock and obsidian, the blocks it can't break. Everything else it already destroys. Full noclip, like the Ender Dragon's, would let it hide inside stone where nothing can reach it.

- **Where:** a fixed area centred where the Wither is summoned, with a radius of 100 blocks.
- **The Wither stays near the centre.** Its AI keeps it within 50 blocks of the centre. The Wither is very mobile, so this leaves room to run around and fight it.
- **No automatic Wither effect.** The subspace only blocks heart recovery. The Wither's skulls already apply the effect.
- **Overlap is harmless.** Two Wither subspaces can overlap without trapping anyone.

Below half health, the Wither's armor stops arrows and other projectiles, so fighting from outside the subspace can't finish it: at some point you have to go in and swing a sword. Stepping out lets your hearts recover, but the Wither heals while you're gone, so it's a trade rather than a cheese. In the Nether, hearts don't recover anyway, so the subspace matters most everywhere else.
