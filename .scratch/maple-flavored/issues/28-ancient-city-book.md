# 28: Ancient City book

**What to build:** The Ancient City's Swift Sneak book floats above the altar in front of the portal. It's locked until three keeper shriekers in a ring near the city's edge are broken. The city keeps players Grounded until the book is taken, and taking it summons nothing.

**Blocked by:** 18 (Structure book framework, with Mineshafts), 22 (Nether Fortress book lock), 23 (Bastion books and keeper locks), 24 (Grounded)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder. Research: `docs/research/vanilla-ancient-city-sculk.md`. Plan: `../plans/28-ancient-city-book.md`.

Jackson's change on 2026-11-05: the Warden is a punishment for being loud, so taking the book no longer summons one. The lock and the city's Grounded replace it, so players can't fly in, grab the book and fly out.

- [x] The book sits in the centre piece above the altar in front of the portal, on the city's axis
- [x] Taking it summons no Warden: remove the angry-Warden summon, its room checks and message, and their GameTests
- [x] Keepers are chosen when the city is planned, from its sculk patch origins, by the spec's ring rule; fewer origins give fewer keepers, and none leaves the book unlocked
- [x] Each keeper's origin gets a summoning sculk shrieker after its patch generates, on top of the catalyst if there is one; the city's record holds the keepers and which are broken
- [x] The book shows the locked look until every keeper is broken by any means; a refused take says how many are broken and sends trails to the rest
- [x] Players inside the city's bounds are Grounded while the book is in place, and no longer once it's taken; a Warden Subspace's Grounded is unaffected
- [x] GameTests cover keeper choice and spread, a keeper placed with and without a catalyst, breaking keepers while the book is unloaded, the refused take, and Grounded before and after the take
- [x] Holding an Echo Shard makes every sculk shrieker within the Echo Shard shrieker range (a playtest game rule, default 15) pulse for the holder alone, faster the closer it is (Jackson's addition on 2026-11-05)
- [ ] Hand check: in an Ancient City, the locked book over the altar, the trails to the keepers, gliding ending at the city's edge, and Elytra working again once the book is taken
