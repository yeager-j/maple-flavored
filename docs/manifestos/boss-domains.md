# Bosses need domains

Jackson's opinion on bosses, written down while working out [enchanting](enchanting.md). It's a summary of intent rather than a quote. Where a resolved ticket on the design map differs, the ticket wins. "Domain" is a working name, not yet a glossary term.

## The problem

Bosses can be escaped too easily. The Warden is the clearest case: it's terrifying on foot and useless if you can fly. If you summon one by accident with an Elytra and rockets, you fly to the other side of the Ancient City and wait it out.

## The idea

When a boss appears, it opens a domain: an area you can't leave, which applies an effect. Each boss's domain shuts down the cheapest way to avoid its fight, so the fight it was designed for happens. Vanilla already does a small version of this: the Elder Guardian's Mining Fatigue stops you from digging into an Ocean Monument.

This matters for enchanting. Grabbing the Ancient City's book spawns a Warden, and without a domain an Elytra makes that encounter trivial.

Domains start with the Warden. Other bosses come later, once their own cheeses are identified.

## The Warden's domain

- **Where:** a fixed area centred where the Warden spawns. It doesn't move with the Warden.
- **Effect:** Darkness and gravity. Gravity stops you from flying away, so you have to deal with the Warden by sneaking and hiding.
- **Barrier:** a physical wall like a world border. Players can't enter or exit, and you can't dig out.
- **Ending:** the domain ends when the Warden goes away, which in vanilla happens after a minute without being disturbed. The win condition is staying unheard for a minute.
- **The Ancient City's book:** a Warden spawned by grabbing it is angry from the start, which makes it a bit tougher.

Other escapes cost the player more than they'd gain. Pillaring makes noise unless it's done with wool, and the Warden's sonic boom and sniffing both reach 20 blocks vertically. Ender Pearls and Chorus Fruit make sound.

### What it looks like

When the Warden spawns, sculk spreads from it to cover the ground of the domain, and the domain gets a sculk skybox. The spreading sculk can grow new sensors, catalysts and shriekers.

## Relation to auras

The Hearts and death ticket already has enemy auras that block Withered Heart recovery. A domain is the same mechanic with a boundary, so the two are designed together in the Mob changes ticket.

## Open questions

### Multiple Wardens

Shriekers can summon another Warden while a domain is active, especially with the domain growing new ones. Domains could then overlap, trapping players between two barriers. A possible rule set:

- **One domain at a time in an area.** No Warden can spawn where its domain would overlap an active one. Vanilla already refuses to warn from a shrieker when a Warden is within about 48 blocks. This needs confirming in 26.3's code.
- **Shrieks inside a domain call the current Warden.** Rather than summoning a second, a shriek draws the existing Warden to it and resets its one-minute timer. The shriekers that the domain grows make the arena more dangerous the longer you're noisy.
- **The book never makes a second Warden.** Grabbing it while a Warden is already nearby angers that Warden and opens its domain instead of spawning a new one.

### Shriekers grown by the domain

In vanilla, shriekers grown by a sculk catalyst can't summon Wardens. Only the ones generated with the Ancient City can. If the domain's shriekers can summon, every Warden makes the city denser with shriekers, and one domain ending can set off the next. Following vanilla's rule keeps them dangerous inside the domain without making the city escalate. Repeated summons would also become a way to farm sculk, which design rule 2 allows.

### Logging out

In multiplayer, a player who logs out while trapped leaves the Warden with no one to hear. If others keep the area loaded, the Warden goes away, the domain ends, and the player logs back in safe. The domain may need to wait for its trapped players.

### Shape

The barrier's shape is open: a cylinder from bedrock to the build limit, a sphere, or a box. Underground, the sculk skybox is mostly how the barrier looks from inside.
