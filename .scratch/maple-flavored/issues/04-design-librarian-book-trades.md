# 04: Design: Librarian book trades

**What to build:** Decide which enchanted books Librarians buy, at which levels and for how many emeralds, replacing their book sales. Run it as a grilling ticket on the design map, then record the answer in the spec.

**Blocked by:** None (can start immediately)

**Status:** resolved

Spec: the Maple Flavored spec in this folder.

- [x] The trades per Librarian level are decided
- [x] The answer doesn't make books or emeralds farmable in a way that skips exploration
- [x] The spec is updated with the decision

## Comments

Resolved in [Librarian book trades](../../maple-flavored-design/issues/20-librarian-book-trades.md) on the design map. Every Novice Librarian buys any enchanted book and pays per find: 4 emeralds, 10 villager XP and 50 player XP points. Levels 2 to 4 lose their book slot. The trade never makes books, and uses and restocks cap its rate. The spec's Trades and bartering line and story 72 now carry the rule. Its "Design tasks before the build" section is gone, since this was the last task. Checked against the 26.3 jar: a data trade can take any enchanted book, but pricing by level needs one mixin on `MerchantContainer.updateSellItem`.
