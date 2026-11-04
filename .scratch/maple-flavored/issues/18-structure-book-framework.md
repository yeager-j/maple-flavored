# 18: Structure book framework, with Mineshafts

**What to build:** Each structure holds one guaranteed level I book in a fixed, visible spot. This ticket builds the book block and placement, proven end to end with the Mineshaft's Efficiency book.

**Blocked by:** 01 (GameTest harness)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [x] A registered book block with a block entity shows a floating level I book and holds its enchantment
- [x] It can't be mined, blown up, pushed or replaced; using it gives the book and removes the block, as one server action with a hook for wake events
- [x] With a full inventory the action is refused, with nothing changed and no wake event
- [x] Starting approach (open to experiment): a per-structure spot rule picks the spot when pieces are planned; the spot and enchantment are saved with the structure; the book is placed when that chunk is built
- [x] Exactly one book per Mineshaft
- [x] Add the structure book to the glossary
- [x] GameTests place a Mineshaft and take its book, including with a full inventory
- [ ] Hand check: the floating book
