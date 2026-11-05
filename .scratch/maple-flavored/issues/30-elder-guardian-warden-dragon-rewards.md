# 30: Elder Guardian, Warden and Dragon rewards

**What to build:** Bosses give a one-time Crystal Heart through advancements, and repeatable materials.

**Blocked by:** 07 (Crystal Heart, Pith and Netherite recipe), 12 (Zephyr), 25 (Warden Subspace)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [ ] Elder Guardians drop 2 Pith
- [ ] Kill an Elder Guardian (Adventure goal, under Monster Hunter), Crystal Heart, to every player within 64 blocks
- [ ] Survive the Warden (Adventure goal, under Sneak 100), Crystal Heart, to every player inside its Subspace when it digs down or dies
- [ ] Free the End also gives a Crystal Heart
- [ ] The Ender Dragon drops a level I Zephyr book every kill
- [ ] GameTests cover each reward and its player range

## Notes

- From 25 (Warden Subspace): `Subspaces.ENDED` fires for every ending, whatever caused it: death, digging down, Peaceful, or any other removal for good. Survive the Warden needs only death and digging down, so this ticket must tell those apart itself. Digging down removes the Warden with `DISCARDED` from `Digging.stop`, and so does any other discard.
