# Bosses need domains

Jackson's opinion on bosses, written down while working out [enchanting](enchanting.md). It's a summary of intent rather than a quote. Where a resolved ticket on the design map differs, the ticket wins. "Domain" is a working name, not yet a glossary term.

## The problem

Bosses can be escaped too easily. The Warden is the clearest case: it's terrifying on foot and useless if you can fly. If you summon one by accident with an Elytra and rockets, you fly to the other side of the Ancient City and wait it out.

## The idea

When a boss appears, it opens a domain: an area you can't leave, which applies an effect. Each boss's domain shuts down the cheapest way to avoid its fight, so the fight it was designed for happens. Vanilla already does a small version of this: the Elder Guardian's Mining Fatigue stops you from digging into an Ocean Monument.

This matters for enchanting. Grabbing the Ancient City's book spawns a Warden, and without a domain an Elytra makes that encounter trivial.

Domains start with the Warden. Other bosses come later, once their own cheeses are identified.

## The Warden's domain

- **Where:** a fixed area centred where the Warden spawns, with a radius of about 50 blocks. It doesn't move with the Warden. The size is meant to keep the Warden always able to pathfind to the player (see open questions).
- **Effect:** Darkness and gravity. Gravity stops you from flying away, so you have to deal with the Warden by sneaking and hiding.
- **Barrier:** a physical wall like a world border. Players can't enter or exit, and you can't dig out.
- **Ending:** the domain ends when its Wardens go away, which in vanilla happens after a minute without being disturbed. The win condition is staying unheard for a minute.
- **The Ancient City's book:** a Warden spawned by grabbing it is angry from the start, which makes it a bit tougher.

Other escapes cost the player more than they'd gain. Pillaring makes noise unless it's done with wool, and the Warden's sonic boom and sniffing both reach 20 blocks vertically. Ender Pearls and Chorus Fruit make sound.

### What it looks like

When the Warden spawns, sculk spreads from it to cover the ground of the domain, and the domain gets a sculk skybox. The spreading sculk can grow new sensors, catalysts and shriekers. Grown shriekers can summon Wardens while the domain is active, and are set to `can_summon: false` when it ends, so the city doesn't escalate after each fight.

### How the Warden finds you

A vanilla Warden is aware of every entity in a 50 by 50 box around itself. It slowly pathfinds towards the closest one, preferring players, unless it detects a vibration within 15 blocks. Pathfinding towards a player isn't the same as knowing where they are: sniffing is what raises its anger.

### Multiple Wardens

A shrieker tries to spawn a Warden in an 11 by 13 box, and only if no other Warden is within 24 blocks. A 50 block domain is much wider than that, so a shrieker far enough from the current Warden can still summon a second one inside the domain.

- **A Warden spawned near an active domain joins it.** If a shrieker's spawn box overlaps an active domain, the new Warden spawns inside the existing domain rather than opening a second one.
- **Grabbing the book always makes a Warden**, even if one is already within 24 blocks. This skips vanilla's check.

### Logging out

A player who logs out while trapped could leave the Warden with no one to hear, ending the domain. That's no worse than switching to Creative or teleporting away, so it isn't solved.

## Relation to auras

The Hearts and death ticket already has enemy auras that block Withered Heart recovery. A domain is the same mechanic with a boundary, so the two are designed together in the Mob changes ticket.

## Open questions

### Domain size and the Warden's awareness

Vanilla's awareness box is 50 blocks across, so it reaches about 25 blocks from the Warden in each direction. The domain is centred on the spawn point, not on the Warden, so a Warden at one edge and a player at the other can be a full diameter apart. A 50 block radius allows up to 100 blocks between them, which is outside the box. The options:

- **Make the Warden aware of everything in its domain.** Then the domain's size can be chosen for how much room there is to sneak, rather than to fit vanilla's box. Anger still comes only from sniffing.
- **Shrink the domain** until its diameter fits the box. That's a radius of about 12 blocks, which is a small, intense arena.

The box is also about 25 blocks tall, so the domain's height matters as well.

### Domains that would overlap

The joining rule checks the shrieker's spawn box, but a new Warden opens a domain much larger than that box. A shrieker just outside an active domain could still open a second domain that overlaps the first, trapping players between two barriers. The same goes for grabbing a book near an active domain. Checking whether the new domain would overlap, rather than the spawn box, closes this: any Warden whose domain would overlap an active one joins it.

### Shape

The barrier's shape is open: a cylinder, a sphere, or a box. Underground, the sculk skybox is mostly how the barrier looks from inside.
