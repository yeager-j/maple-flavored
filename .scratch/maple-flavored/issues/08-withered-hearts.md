# 08: Withered Hearts

**What to build:** Under the Wither effect, hearts become Withered Hearts: temporarily unusable, without changing Max Hearts. They return once the player leaves the Nether and the effect ends.

**Blocked by:** 05 (Max Hearts and death cost)

**Status:** done

Spec: the Maple Flavored spec in this folder.

- [x] Under the Wither effect, one heart withers every 5 seconds, down to 1 usable heart; Max Hearts don't change
- [x] The timer starts with the effect and resets whenever it ends, so one short exposure withers nothing; timer progress is saved through logout
- [x] All return at once when the player has no Wither effect and is outside the Nether (the Wither Subspace rule comes later)
- [x] Death clears them; logging out doesn't
- [x] The count syncs to the client and shows as darkened hearts
- [x] Advancement: Get a Withered Heart back (Nether, under A Terrible Fortress)
- [x] GameTests cover withering, the timer reset, the minimum, recovery in each dimension and death
- [ ] Hand check: darkened hearts
