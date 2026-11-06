# 30: Elder Guardian, Warden and Dragon rewards

**What to build:** Bosses give a one-time Crystal Heart through advancements, and repeatable materials.

**Blocked by:** 07 (Crystal Heart, Pith and Netherite recipe), 12 (Zephyr), 25 (Warden Subspace)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [x] Elder Guardians drop 2 Pith
- [x] Kill an Elder Guardian (Adventure goal, under Monster Hunter), Crystal Heart, to every player within 64 blocks
- [x] Sneak 100 becomes Survive the Warden (now an Adventure goal), Crystal Heart, to every player within 50 blocks of the Warden when it digs down or dies (Jackson's change on 2026-11-05, replacing a new advancement under Sneak 100 for every player inside its Subspace)
- [x] Playtest game rule for the Warden survival range (default 50, applies at once)
- [x] Free the End also gives a Crystal Heart
- [x] The Ender Dragon drops a level I Zephyr book every kill
- [x] GameTests cover each reward and its player range
- [ ] Hand check: kill an Elder Guardian with a second player about 60 blocks away and one past 64, and confirm 2 Pith drop and only the two within 64 get Monumental with a Crystal Heart; stay within 50 blocks of a Warden until it digs back down (and again until one is killed) and confirm Sneak 100, now a goal, with a Crystal Heart, but not for a player farther away, and that sneaking past a sculk sensor no longer grants it; kill the Ender Dragon with a second player on the island, and confirm both get Free the End with a Crystal Heart and a Zephyr I book drops where the Dragon dies

Free the End goes to every non-spectator player the dragon fight counts as taking part (alive and within 192 blocks of the point 128 blocks above the centre of the End, the range that sees its boss bar), as well as to the killer through vanilla's own criterion. Vanilla gives it only to the killer; this follows the advancements decision's "Free the End uses vanilla's dragon-fight player range". A dragon outside a fight, such as one summoned in the Overworld, credits only its killer. Kill an Elder Guardian and Sneak 100 skip spectators too. The Dragon's book drops where the Dragon dies, so one killed over the void loses it, like any drop.

## Notes

- From 25 (Warden Subspace): `Subspaces.ENDED` fires for every ending, whatever caused it: death, digging down, Peaceful, or any other removal for good. Survive the Warden needs only death and digging down, so this ticket must tell those apart itself. Digging down removes the Warden with `DISCARDED` from `Digging.stop`, and so does any other discard.
