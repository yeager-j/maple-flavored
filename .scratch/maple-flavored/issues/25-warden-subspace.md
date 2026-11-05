# 25: Warden Subspace

**What to build:** Every Warden opens a fixed Subspace with a 100-block radius where it spawns. Players inside get Darkness and Grounded, and the Warden knows where they are. This ticket builds the shared Subspace module.

**Blocked by:** 24 (Grounded effect), 09 (Remnants)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [x] Subspace module: fixed sphere with centre, radius, dimension, boss and kind; a query for Subspaces containing a position; start and end events; record kept in world data
- [x] Ends when its boss dies, digs down or is removed for good, never on chunk unload; player rules keep working while the boss is unloaded; boss-driven work pauses
- [x] Players inside get Darkness and Grounded; overlap is fine and Grounded doesn't stack; no barrier
- [x] The Warden is aware of every entity inside and pathfinds to the nearest player; anger and sniffing stay vanilla
- [x] Death inside respawns normally; a Remnant from a collectable death spot stays put, while an uncollectable one still goes to safe ground
- [x] Subspaces sync to clients
- [x] GameTests cover entering and leaving, two overlapping Subspaces, unloading the Warden and Remnant placement inside
- [ ] Hand check: Darkness and Grounded inside and wearing off past 100 blocks, Elytra blocked, the Subspace surviving an unload and a save and reopen, and the client holding it after joining, changing dimension and respawning into another dimension, then losing it when the Warden dies
