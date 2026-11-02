# Bosses need domains

Jackson's opinion on bosses, written down while working out [enchanting](enchanting.md). It's a summary of intent rather than a quote. Where a resolved ticket on the design map differs, the ticket wins. "Domain" is a working name, not yet a glossary term.

## The problem

Bosses can be escaped too easily. The Warden is the clearest case: it's terrifying on foot and useless if you can fly. If you summon one by accident with an Elytra and rockets, you fly to the other side of the Ancient City and wait it out.

## The idea

When a boss appears, it opens a domain: an area you can't leave, which applies an effect. The Warden's domain applies Darkness and gravity, so you can't fly away. You have to deal with it by sneaking and hiding, which is what the Warden is for.

The point isn't the domain itself. Each boss's domain shuts down the cheapest way to avoid its fight, so the fight it was designed for happens. Vanilla already does a small version of this: the Elder Guardian's Mining Fatigue stops you from digging into an Ocean Monument.

This matters for enchanting. Grabbing the Ancient City's book spawns a Warden, and without a domain an Elytra makes that encounter trivial.

## Open questions

### What the Warden's domain blocks

Gravity rules out Elytra gliding and firework boosts. Ender Pearls and Chorus Fruit are also escapes, so they either fail inside the domain or can't land outside it. Pillaring up and digging out are the other common cheeses.

### How it ends

You can't leave, so the domain needs an end that stealth can reach. A vanilla Warden digs back down after about 60 seconds without being disturbed. Ending the domain then makes the win condition "stay unheard for a minute", which is a real stealth game. Dying also ends it, at the cost of a heart.

### Where it is

A domain fixed where the Warden appeared is easier to read than one that moves with it. Its size could be a radius or the structure's bounds, and Ancient Cities are large. The boundary has to be visible, perhaps as spreading sculk.

### Multiplayer

The domain could trap everyone inside it. Letting players outside walk in but not out would allow rescues.

### Other bosses

Each boss needs its own cheese identified before it gets a domain. The Wither is often fought under bedrock, and the Ender Dragon can be killed with beds. Some bosses may need no domain at all.

### Relation to auras

The Hearts and death ticket already has enemy auras that block Withered Heart recovery. A domain may be the same mechanic with a boundary, so the two should be designed together in the Mob changes ticket.
