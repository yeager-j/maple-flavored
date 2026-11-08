# 58: Deep Core look and structure template

**What to build:** The Deep Core block's textures for each level, and the small structure built around it.

**Blocked by:** 55 (Deep Core generation)

**Status:** ready-for-human

Spec: the Maple Flavored spec in this folder, user story 193f and the Deep Cores module.

- [ ] A block model and textures for levels I to V, readable in a dark cave without giving off light
- [ ] The decoration around the core, as a template or a code-built piece, that sits on a sloped cave floor
- [ ] Any reused asset listed in `CREDITS.md`

## Notes

- Jackson plans to design the template. A template needs either a `TemplateStructurePiece` with its own piece type, or a call to `JigsawPlacement.addPieces` with a one-piece pool (`docs/research/vanilla-cave-structure-placement.md`, section 5).
