# 39: Brewing changes

**What to build:** Potions stack, thrown potions share a cooldown, and Weakness needs the Nether.

**Blocked by:** 01 (GameTest harness)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [ ] Potions, splash, lingering potions and water bottles stack to 16; brewing stand slots still hold one
- [ ] Splash and lingering throws by players share a 1-second cooldown; dispensers are unaffected
- [ ] Fermented spider eye: Strength to Weakness (1:30), Long Strength to Long Weakness (4:00), Strength II to Weakness; nothing on a water bottle
- [ ] GameTests cover stacking, the cooldown and recipes
