# 55: Deep Core generation

**What to build:** Deep Cores generate in large open caves below Y 0. This ticket adds the structure type with its cave finder, the core block and block entity with a level, the loaded-core index, and a command for checking placement.

**Blocked by:** 01 (GameTest harness)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder, user stories 193a, 193f and 193m, and the Deep Cores module's Placement, Level and block bullets and its command.

- [ ] A registered `maple_flavored:deep_core` structure type and piece type, with `worldgen/structure/deep_core.json`, `worldgen/structure_set/deep_cores.json` (`random_spread`, spacing 8, a unique salt) and a biome tag of every Overworld biome except `deep_dark`
- [ ] The cell scan:
  - It reads every chunk centre in the attempt's 8 × 8 cell, at X and Z multiples of 4, with `getBaseColumn`.
  - A column needs an air run of at least 14 on a solid floor between Y −52 and −4. Water and lava break a run.
  - At least 3 of 4 probes at ±12 blocks must pass, with air from floor + 2 to + 4 and a floor within 10.
  - The tallest run wins.
  - All thresholds are read from the structure data.
- [ ] A second Deep Core in a cell when it sits more than 35 blocks above or below the first. None within 25 blocks of another in 3D, including across neighbouring cells
- [ ] The Deep Dark ring and the Ancient City potential-site guard
- [ ] Terrain adaptation `none`, step `top_layer_modification`. The piece checks the real floor when built and moves up to a few blocks to find it
- [ ] A registered Deep Core block and block entity: unbreakable, blast-proof, not pushable or replaceable, no light, and a `level` state from 1 to 5. The level comes from the depth bands in structure data
- [ ] A loaded-core index, kept from block entity load and unload, answering which cores' zones contain a position without loading chunks
- [ ] `/deepcores [count] [radius]` lists the nearest Deep Cores with clickable teleports and the cores per km² in that radius
- [ ] Deep Cores are described in `docs/project-overview.md` (the glossary entry already exists)
- [ ] GameTests:
  - the cave finder on made-up columns: a big cave passes, and a narrow shaft, a flooded cave, a lava floor and a Deep Dark column fail
  - the spacing rule
  - a core placed directly in the test world gets the right level and resists mining, explosions and pistons
  - the index finds it
- [ ] Hand check: in a fresh world, `/deepcores` shows about 20 per km², and visiting a few shows them in big caves

## Notes

- Read `docs/research/vanilla-cave-structure-placement.md` first. Sections 1 to 5 cover the code facts: what `getBaseColumn` sees, the placement hook, the runtime lookup and registration. Section 6 covers the measured settings.
- A throwaway prototype of the finder, the marker structure and the `/cavecores` command was built in another session's scratchpad, at `C:\Users\jackson\AppData\Local\Temp\claude\01ca97b58b7d174e\42396275-d0d9-42bf-8c1c-ef6450d53406\scratchpad\proto`. It's in a temp folder and may be gone. Use it as a reference if it's still there, but don't copy it in as it is.
- `/place structure` never records starts and never passes the cave check in a flat world, so tests need a way to build a core at a given position without the check.
- The neighbour spacing check must stay deterministic and thread-safe, using only `context.random()` and no shared state. Cache per cell if a neighbour check would otherwise rescan.
- The block's texture and the structure's decoration are ticket 58. Use a placeholder here.
