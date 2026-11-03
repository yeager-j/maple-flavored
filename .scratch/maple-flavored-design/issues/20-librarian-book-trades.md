# Librarian book trades

Type: grilling
Status: resolved

## Question

Which enchanted books do Librarians buy, at which levels and for how many emeralds, now that they no longer sell books? The answer must not make books or emeralds farmable in a way that skips exploration.

## Answer

Every Novice Librarian buys any enchanted book, and pays by how many finds the book holds. The trade is an outlet for surplus books, not an emerald economy. It never makes books, so it can't change book supply, and its rate is capped by vanilla's trade uses and restocks.

### The trade

- One buy trade takes any enchanted book, whatever its enchantment. If each Librarian wanted one random enchantment, the loot box would come back on the buying side, along with lectern rerolling.
- Every Novice Librarian offers it. The Novice set picks all 3 of its trades (paper, bookshelf and the book trade) instead of 2 of 3, so every Librarian is a buyer from the start.
- Levels 2 to 4 lose their book slot. Apprentice and Journeyman always offer both remaining trades, and Expert offers 2 of its 3. Master stays vanilla.
- No exceptions for farmable books. Fishing, outpost, raid, Wither and Dragon books all sell. An AFK fishing farm's book an hour is worth far less than a crop farm, and a raid farm already pays in emeralds.
- The rest of the Librarian stays vanilla. No villager sells an enchanted book.

### Payout

A level n book is 2^(n-1) finds, and each find pays 4 emeralds, 10 villager XP and 50 player XP points. Player XP comes on top of vanilla's trade orb.

| Book | Finds | Emeralds | Villager XP | Player XP points |
| --- | --- | --- | --- | --- |
| Level I | 1 | 4 | 10 | 50 |
| Level II | 2 | 8 | 20 | 100 |
| Level III | 4 | 16 | 40 | 200 |
| Level IV | 8 | 32 | 80 | 400 |
| Level V | 16 | 64 | 160 | 800 |

- Paying per find means combining never changes what a set of books is worth. Selling a combined book only adds the anvil's cost.
- The player XP matches the anvil's combining cost of 50 × the resulting level. A level I book is about 1.5 levels at level 30. Books are rare, so both kinds of XP are hefty: one spare level I book takes a Novice straight to Apprentice.
- A level V book pays a full stack, which is the most a trade's result can hold.

### Price and display

- 12 uses per restock. The price multiplier is 0, so demand, cure discounts and Hero of the Village never change either side: always 1 book in and its per-find payout out.
- The trade list shows the level I price, 4 emeralds. Once a book is placed, the result slot shows its real payout.

### Build notes

Checked against the 26.3 jar. Trades are data, in `villager_trade`, the level tags and `trade_set`. A wanted item of `{id: enchanted_book}` with no components matches any enchanted book. A trade's result is fixed when the offer is made, and an exact component match can't express "any level II book", so per-level trade slots would need code too. Instead, one mixin on `MerchantContainer.updateSellItem`, where the result slot is filled from `offer.assemble()` whenever a payment slot changes, sets the emeralds from the placed book. Taking the result never rebuilds it, and shift-clicking prices each book on its own. The same per-book scaling applies to the villager XP and the player XP orb when the trade is taken.

## Comments

Resolves the "Librarian book trades" design task in the Maple Flavored spec (ticket 04 in `.scratch/maple-flavored/issues/`).
