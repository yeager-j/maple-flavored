# 42: Blood Moon night

**What to build:** Every 32 in-game days a Blood Moon rises: a red night announced in chat.

**Blocked by:** 01 (GameTest harness)

**Status:** ready-for-agent

Spec: the Maple Flavored spec in this folder.

- [x] Fixed schedule on every 4th full moon
- [x] One chat message in every dimension; red moon and sky on the client; sleeping allowed; no extra spawns
- [x] Advancement: Be in the Overworld when a Blood Moon rises (Adventure)
- [x] GameTests set the day count and check the message and advancement
- [ ] Hand check: red sky and moon (`/time set maple_flavored:blood_moon`; `gradlew runClientGameTest` also screenshots it)
