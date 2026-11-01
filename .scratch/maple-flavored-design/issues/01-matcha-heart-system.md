# What Matcha's heart system does

Type: research
Status: resolved

## Question

How does Matcha Flavoured's heart system work exactly? Cover heart loss on death, how the minimum health floor changes with progress, every Crystal Heart and other heart source, the numbers involved, and edge cases such as Keep Inventory and multiplayer.

## Answer

From reading Matcha 1.12.9's files, not from playing it. Full findings: [docs/research/matcha-heart-system.md](../../../docs/research/matcha-heart-system.md).

- Death removes 1 heart (2 on Hard), clamped to a floor. The Heartbreaker Wither timer does the same without a death.
- The floor is fixed at 10 hearts on Easy. On Normal and Hard it's a per-player score that starts at 10 and falls by 1 per seven progression advancements, down to 3. The fifth step also switches a Normal world to Hard, for everyone.
- A Crystal Heart gives +1 heart, up to a cap of 30, with a 1 second cooldown.
- Sources are gem advancements (5 in total), seven chest tables, fishing, an Abbey room, crafting and four trades. Other trades spend hearts.
- Keep Inventory is set once per world, and the heart code never reads it.

Unconfirmed: clay statue sources, the Almanac's 3.3%, Abbey room details and Hardcore behaviour.
