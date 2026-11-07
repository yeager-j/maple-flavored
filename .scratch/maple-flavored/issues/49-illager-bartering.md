# 49: Illager bartering

**What to build:** A player wearing any Orichalcum armor can hand a neutral Pillager, Vindicator or Evoker an emerald and get a barter back, as Piglins do with gold. It's an easter egg: no docs mention it, and only the hidden Switching Sides advancement hints at it.

**Blocked by:** 34 (Alloy combat and loot behavior)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder, user stories 149a–149c and 211b, and the Illager bartering bullets under Materials and Alloys.

- [ ] Right-clicking a neutral Pillager, Vindicator or Evoker with an emerald, while wearing any Orichalcum piece, hands it one emerald
- [ ] The Illager holds the emerald in its offhand and stands looking at the player for 120 ticks, then throws one roll of its barter loot table toward the player
- [ ] Pillager Captains use the Pillager table; Illusioners, Vexes, Ravagers and Witches don't barter
- [ ] An Illager that is already bartering, angry at the player or in an active raid refuses the emerald, and the emerald stays in the player's hand
- [ ] Hurting or angering the Illager mid-barter stops the barter and drops the emerald back on the ground
- [ ] Illagers ignore emeralds on the ground
- [ ] Barter loot tables `maple_flavored:gameplay/illager_bartering/{pillager,vindicator,evoker}` hold the spec's items and weights (the shared Mansion pool plus each Illager's own entries, in one weighted pool), with plain gear at full durability, and the Orichalcum drop bonus doesn't apply
- [ ] Hidden task Switching Sides ("Barter with an Illager") in the Adventure tab under Voluntary Exile, with no reward, granted on the first finished barter
- [ ] Nothing in `docs/project-overview.md`, the README or `GLOSSARY.md` mentions bartering with Illagers
- [ ] GameTests cover: a barter with and without Orichalcum, refusal while angry and in a raid, the interrupted barter returning the emerald, each table's items, and the advancement

## Notes

- `alloys/IllagerNeutrality.isNeutral` already decides whether an Illager leaves a player alone. Use it as the single test for whether an Illager will take the emerald, so a raid, an attack or a book alert blocks bartering without extra rules.
- `AbstractIllager` has no `mobInteract` of its own. A mixin on `Mob.interact` (there's already a `MobMixin`) can catch the emerald hand-over.
- Vanilla's Piglin barter (`PiglinAi`, `LootContextParamSets.PIGLIN_BARTER`, the `gameplay/piglin_bartering` table) is the model for the loot context and for throwing the result toward the player.
- A Pillager or Evoker's goals may move it during the 120 ticks. Hold it in place, as a Piglin admiring gold stops to look.
- The shared pool repeats in all three tables. A loot table can't reference a shared pool inside one weighted pool's entries, so either duplicate the entries in each file (keep them identical; a test can compare them) or merge the shared entries in code when the tables load.
- Item ideas: the shared pool is the Woodland Mansion's chest loot, the Poppies tell the story of the village's missing iron golems, the red wool is the Evoker's blue sheep trick, and the "Johnny" name tag points at the Vindicator name easter egg.
