# 22: Nether Fortress book lock

**What to build:** The Nether Fortress's Fire Protection book stays locked until every Blaze spawner that generated there is Cleared.

**Blocked by:** 18 (Structure book framework, with Mineshafts), 21 (Spawner Clearing and more dungeons)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [ ] The Fortress gets its Fire Protection book
- [ ] A persistent per-fortress record, made at generation, holds its Blaze spawner positions and which are Cleared; the Cleared event updates it whether or not the book is loaded; a broken Cleared spawner stays counted
- [ ] The book shows a locked state until all are Cleared; a fortress with none starts unlocked
- [ ] GameTests clear spawners while the book is unloaded, break one, reload and take the book
- [ ] Hand check: locked look
