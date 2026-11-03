# 14: Library and checklist table

**What to build:** Enchanted books shelved in Chiseled Bookshelves around an enchanting table form a shared Library. The table becomes a checklist: insert an item, pick enchantments and Save. Level sets Enchantment Slots.

**Blocked by:** 11 (Material units, repairs and anvil rules)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [ ] The Library is the books in Chiseled Bookshelves at vanilla's bookshelf positions; the table still needs 15 bookshelves of either kind; books aren't used up
- [ ] The table offers each shelved enchantment at its highest shelved level; the random offers are gone
- [ ] Only enchantable gear is accepted; plain and enchanted books are refused (golden apples come later)
- [ ] The checklist pre-selects the item's enchantments. Save needs at least one choice; every selected enchantment offered by the current Library at that level or higher, including ones already on the item; valid and compatible choices; and a count that fits the player's Enchantment Slots: 1 per 30 levels, no cap, plus 1 for gold tools, weapons and armor. A refused save changes nothing
- [ ] Save costs 1 lapis per selected enchantment, plus 1 material unit on the first save of an unenchanted item, stored on the item as a deposit
- [ ] The grindstone strips all enchantments and returns the deposit, never the lapis
- [ ] Saving fires vanilla's enchanted item trigger. Advancement: Shelve an enchanted book in a Library (Story, under Diamonds!); Enchanter moves under it
- [ ] Enchanted book item entities resist fire and explosions
- [ ] GameTests drive the table menu with mock players at different levels, including a shared Library, a refused over-limit save and a save after a book was removed
- [ ] Hand check: the checklist screen
