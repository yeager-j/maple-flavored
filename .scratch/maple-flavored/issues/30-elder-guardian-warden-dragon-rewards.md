# 30: Elder Guardian, Warden and Dragon rewards

**What to build:** Bosses give a one-time Crystal Heart through advancements, and repeatable materials.

**Blocked by:** 07 (Crystal Heart, Pith and Netherite recipe), 12 (Zephyr), 25 (Warden Subspace)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [x] Elder Guardians drop 2 Pith
- [x] Kill an Elder Guardian (Adventure goal, under Monster Hunter), Crystal Heart, to every player within 64 blocks
- [x] Survive the Warden (Adventure goal, under Sneak 100), Crystal Heart, to every player inside its Subspace when it digs down or dies
- [x] Free the End also gives a Crystal Heart
- [x] The Ender Dragon drops a level I Zephyr book every kill
- [x] GameTests cover each reward and its player range
- [ ] Hand check: kill an Elder Guardian with a second player about 60 blocks away and one past 64, and confirm 2 Pith drop and only the two within 64 get Monumental with a Crystal Heart; stay inside a Warden's Subspace until it digs back down (and again until one is killed) and confirm Lived to Tell with a Crystal Heart, but not for a player outside it; kill the Ender Dragon with a second player on the island, and confirm both get Free the End with a Crystal Heart and a Zephyr I book drops where the Dragon dies

Free the End goes to every non-spectator player the dragon fight counts as taking part (alive and within 192 blocks of the point 128 blocks above the centre of the End, the range that sees its boss bar), as well as to the killer through vanilla's own criterion. Vanilla gives it only to the killer; this follows the advancements decision's "Free the End uses vanilla's dragon-fight player range". A dragon outside a fight, such as one summoned in the Overworld, credits only its killer. Kill an Elder Guardian and Survive the Warden skip spectators too. The Dragon's book drops where the Dragon dies, so one killed over the void loses it, like any drop.

## Notes

- From 25 (Warden Subspace): `Subspaces.ENDED` fires for every ending, whatever caused it: death, digging down, Peaceful, or any other removal for good. Survive the Warden needs only death and digging down, so this ticket must tell those apart itself. Digging down removes the Warden with `DISCARDED` from `Digging.stop`, and so does any other discard.
