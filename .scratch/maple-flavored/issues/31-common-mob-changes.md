# 31: Common mob changes

**What to build:** Common mobs become faster and more fragile by difficulty, and creepers stay below sea level.

**Blocked by:** 01 (GameTest harness)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [ ] The mob table in the spec applies on spawn by difficulty; adult zombie villagers stay vanilla
- [ ] Creepers never spawn naturally at Y 63 or above in the Overworld; spawners are unaffected
- [ ] Credit Matcha Flavoured for the numbers
- [ ] GameTests spawn each mob on each difficulty and check attributes; a creeper spawn check above and below Y 63
