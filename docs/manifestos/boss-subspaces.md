# Bosses need subspaces

Jackson's opinion on bosses, written down while working out [enchanting](enchanting.md). It's a summary of intent rather than a quote. Where a resolved ticket on the design map differs, the ticket wins. [Mob changes](../../.scratch/maple-flavored-design/issues/05-mob-changes.md) adopted this and replaced parts of it, including how a Warden summons and where Withered Hearts recover.

## The problem

Bosses can be escaped too easily. The Warden is the clearest case: it's terrifying on foot and useless if you can fly. If you summon one by accident with an Elytra and rockets, you fly to the other side of the Ancient City and wait it out.

## The idea

When a boss appears, it opens a subspace: an area you can't leave, which applies an effect. Each boss's subspace shuts down the cheapest way to avoid its fight, so the fight it was designed for happens. Vanilla already does a small version of this: the Elder Guardian's Mining Fatigue stops you from digging into an Ocean Monument.

This matters for enchanting. Grabbing the Ancient City's book spawns a Warden, and without a subspace an Elytra makes that encounter trivial.

Subspaces start with the Warden. The Wither is next, and other bosses come later, once their own cheeses are identified.

## The Warden's subspace

- **Where:** a fixed area centred where the Warden spawns, with a radius of about 50 blocks. It doesn't move with the Warden.
- **Effect:** Darkness and gravity. Gravity stops you from flying away, so you have to deal with the Warden by sneaking and hiding.
- **Barrier:** a physical wall like a world border. Players can't enter or exit, and you can't dig out.
- **Ending:** the subspace ends when its Wardens go away, which in vanilla happens after a minute without being disturbed. The win condition is staying unheard for a minute.
- **The Ancient City's book:** a Warden spawned by grabbing it is angry from the start, which makes it a bit tougher.

Other escapes cost the player more than they'd gain. Pillaring makes noise unless it's done with wool, and the Warden's sonic boom and sniffing both reach 20 blocks vertically. Ender Pearls and Chorus Fruit make sound.

### What it looks like

When the Warden spawns, sculk spreads from it to cover the ground of the subspace, and the subspace gets a sculk skybox. The spreading sculk can grow new sensors, catalysts and shriekers. Grown shriekers can summon Wardens while the subspace is active, and are set to `can_summon: false` when it ends, so the city doesn't escalate after each fight.

### How the Warden finds you

A vanilla Warden is aware of every entity in a 50 by 50 box around itself. It slowly pathfinds towards the closest one, preferring players, unless it detects a vibration within 15 blocks. Pathfinding towards a player isn't the same as knowing where they are: sniffing is what raises its anger.

A Warden is aware of everything inside its subspace, so it can always pathfind towards you wherever you hide. Vanilla's box reaches only about 25 blocks from the Warden, and two points in a 50 block subspace can be 100 blocks apart. With this rule, the subspace's size is chosen for how much room there is to sneak. Anger still comes only from sniffing.

### Multiple Wardens

A shrieker tries to spawn a Warden in an 11 by 13 box, and only if no other Warden is within 24 blocks. A 50 block subspace is much wider than that, so a shrieker far enough from the current Warden can still summon a second one.

A subspace is a sphere for its barrier and skybox, but a cylinder from bedrock to the build limit for spawning rules. Two subspaces count as overlapping if their circles overlap, which keeps the rules simple and stops subspaces stacking vertically. Spheres can't overlap if their cylinders don't, so the invariant holds.

Subspaces never overlap. Overlapping subspaces would trap players between two barriers, so this is an invariant, not a balance choice.

- **Triggered inside a subspace's sphere:** the new Warden spawns inside that subspace and joins it.
- **Triggered outside, with no overlap:** the Warden opens its own subspace as normal.
- **Triggered outside, where its subspace would overlap:** the new subspace is pushed away from the existing one until the two touch without overlapping. The shrieker that set it off ends up inside the new subspace, so the consequence lands on whoever triggered it. Only the subspace moves: the Warden still spawns in vanilla's box near the shrieker, limited to spots inside its own subspace. It doesn't need to be at the centre, since it's aware of the whole subspace, and a pushed centre may be inside stone, over lava or in open air.
- **Grabbing the book always makes a Warden**, even if one is already within 24 blocks. This skips vanilla's check, and the same overlap rules apply. If no valid subspace could open, the book can't be taken until one can, so taking it always traps you.
- **No valid subspace:** the shriek fails to summon. This covers a shrieker inside a subspace's cylinder but outside its sphere, where no pushed subspace could contain it, and a cluster of subspaces where no position touches without overlapping. It extends vanilla's rule that no Warden emerges with another one nearby.

### Logging out

A player who logs out while trapped could leave the Warden with no one to hear, ending the subspace. That's no worse than switching to Creative or teleporting away, so it isn't solved.

## The Wither's subspace

The Wither's subspace is an open barrier: there's no wall, and you can come and go. Running away isn't how players cheese the Wither, so the subspace doesn't need to trap anyone. Its job is to block recovery: Withered Hearts don't return while you're inside it.

The Wither's real cheese is trapping it under bedrock so it can't move. To stop that, the Wither is immune to suffocation and passes through bedrock and obsidian, the blocks it can't break. Everything else it already destroys. Full noclip, like the Ender Dragon's, would let it hide inside stone where nothing can reach it.

- **Where:** a fixed area centred where the Wither is summoned, with a radius of 100 blocks.
- **The Wither stays near the centre.** Its AI keeps it within 50 blocks of the centre. The Wither is very mobile, so this leaves room to run around and fight it.
- **No automatic Wither effect.** The subspace only blocks heart recovery. The Wither's skulls already apply the effect.
- **Overlap is harmless.** An open subspace has no wall, so two Wither subspaces can overlap without trapping anyone.

Below half health, the Wither's armor stops arrows and other projectiles, so fighting from outside the subspace can't finish it: at some point you have to go in and swing a sword. Stepping out lets your hearts recover, but the Wither heals while you're gone, so it's a trade rather than a cheese. In the Nether, hearts don't recover anyway, so the subspace matters most everywhere else.
