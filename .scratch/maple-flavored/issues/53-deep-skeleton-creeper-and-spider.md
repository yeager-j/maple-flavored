# 53: Deep Skeleton, Deep Creeper and Deep Spider

**What to build:** The other three Deep variants. Below Y 0, natural skeletons, creepers and spiders become Deep Skeletons with Slowness arrows, Deep Creepers that explode like TNT, and Deep Spiders whose bites blind.

**Blocked by:** 52 (Deep variants and the Deep Zombie)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder, user stories 165a, 165b and 165d–165g, the Deep variants module, and the Deep variant bullet under Spec defaults.

- [ ] Registered `deep_skeleton`, `deep_creeper` and `deep_spider` entity types, each built on its base mob, with a renderer, its own texture and a spawn egg, and in every entity tag its base mob is in
- [ ] Natural spawns of skeletons, creepers and spiders below Y 0 in the Overworld become their Deep variants, with the same exceptions as the Deep Zombie. A spider jockey that spawns below Y 0 is a Deep Skeleton riding a Deep Spider
- [ ] Deep Skeleton: 30 health, double arrow damage, arrows carry a stray's Slowness, follow range 32
- [ ] Deep Creeper: 20 health, ignoring the health game rule, explosion power 4 (8 when charged), vanilla fuse
- [ ] Deep Spider: 24 health, 4 attack, its bite gives Blindness for 60 ticks; the Mobs table's spider row (jump, no fall damage) applies
- [ ] Each drops its base mob's loot table and twice its vanilla XP
- [ ] GameTests: spawn replacement for each mob and for jockeys, health and damage against the game rules, a Deep Skeleton arrow gives Slowness, a Deep Creeper's explosion is larger than a creeper's, a Deep Spider bite gives Blindness, and Bane of Arthropods hits the Deep Spider
- [ ] Hand check: all three textures in a dark cave, a Deep Skeleton shooting from beyond torchlight, and a Deep Creeper crater next to a creeper's

## Notes

- Confirm a stray's Slowness arrow (amplifier and duration) on the 26.3 jar and reuse it.
- A Deep Creeper's power has no game rule. The spec keeps 4 as a fixed number.
