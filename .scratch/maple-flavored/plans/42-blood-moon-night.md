# Plan: ticket 42, Blood Moon night

Repo: C:\Users\jackson\Developer\maple-flavored (Fabric mod, Minecraft 26.3). Ticket: `.scratch/maple-flavored/issues/42-blood-moon-night.md`. Spec: `.scratch/maple-flavored/spec.md` ("Blood Moons" module, stories 194-201). Follow-up ticket 43 (Restoring) builds on this.

## End goal

Every 4th full moon (every 32 in-game days) the Overworld night is a Blood Moon: the sky and moon turn red on clients, every online player in every dimension gets one chat message as it rises, players in the Overworld earn an Adventure task advancement, sleeping works as normal and no mobs are added. Ticket 43 will hook its Restoring onto the same "risen" moment.

## Design

26.3 drives day, moon phase and sky colours from data-driven **timelines** on world **clocks** (`data/minecraft/timeline/moon.json`: full moon at ticks 0 of a 192 000-tick period; `#minecraft:in_overworld` timeline tag lists the Overworld's timelines). Timelines animate **environment attributes**, evaluated on both server and client from the synced clock.

1. **Schedule as data.** New timeline `data/maple_flavored/timeline/blood_moon.json`: clock `minecraft:overworld`, `period_ticks` 768 000 (32 days). Full moons fall on days 0, 8, 16, 24, so the 4th full moon is day 24: the night runs from tick 588 000 (day 24, 12 000 = sunset) to 600 000 (day 25 sunrise). Track for a new attribute `maple_flavored:gameplay/blood_moon` (float 0..1): 0 at 588 000, 1 at ~589 500, 1 at ~598 500, 0 at 600 000. A time marker `maple_flavored:blood_moon` at 588 000 with `show_in_commands: true`, so `/time set maple_flavored:blood_moon` jumps to the next rise for hand checks. Added to `#minecraft:in_overworld` via `data/minecraft/tags/timeline/in_overworld.json` (append, not replace).
   - First Blood Moon is day 24 (the 25th night), not day 0: "every 4th full moon" counts from the first.
2. **Attribute.** `BloodMoons.STRENGTH`: `EnvironmentAttribute<Float>`, `AttributeTypes.FLOAT`, default 0, `UNIT_FLOAT`, syncable, not positional, registered in `BuiltInRegistries.ENVIRONMENT_ATTRIBUTE` as `maple_flavored:gameplay/blood_moon`. Above 0 means a Blood Moon is up. Only Java knowledge of the schedule is "above 0"; the timing lives only in the timeline.
3. **Server: rising.** New package `mapleflavored.bloodmoons`, class `BloodMoons`:
   - `register()` from mod init: registers the attribute (static init) and an `END_SERVER_TICK` hook.
   - Each tick reads `server.overworld().environmentAttributes().getDimensionValue(STRENGTH)`. While it is above 0, the Blood Moon's identity is the `blood_moon` timeline's period count. A level `SavedData` (`BloodMoonState`, on the overworld, `last_risen`) remembers the last one announced, so a restart mid-night does not re-announce and a jump straight from one Blood Moon into the next still announces the next. A new one: record it, broadcast the message, trigger the advancement for Overworld players.
   - (Plan review: no public `isUp` query; ticket 43 adds its hook when it needs one.)
   - Message: `server.getPlayerList().broadcastSystemMessage(Component.translatable("chat.maple_flavored.blood_moon_rises").withStyle(DARK_RED), false)` — one per player, whatever dimension.
   - Missed nights (time jumped past the whole night) simply never rise; nothing stacks.
4. **Advancement.** `MapleTriggers.BLOOD_MOON_RISEN` (`PlayerTrigger`, `maple_flavored:blood_moon_risen`). `data/maple_flavored/advancement/adventure/blood_moon.json`, parent `minecraft:adventure/root`, task frame, no reward, title "Once in a Blood Moon", description "Be in the Overworld when a Blood Moon rises", icon red dye. Lang entries.
5. **Client: red sky and moon.** `client/bloodmoons/BloodMoonSky`: positional layers added in the existing `ClientLevelMixin.addEnvironmentAttributeLayers` hook, like `SculkSky`: lerp SKY_COLOR, FOG_COLOR, CLOUD_COLOR, SUNRISE_SUNSET_COLOR toward dark reds by the level's `STRENGTH`. Moon: `SkyRendererMixin` — `extractRenderState` TAIL stores the strength for the frame in a `@Unique` field; `@ModifyArg` on the `Vector4f` passed to `DynamicGpuData.writeTransform` inside `renderMoon` multiplies its RGB toward red. Colours are hand-check values.
6. **Sleeping / spawns.** Nothing changes; no code touches beds or spawning.

## Tests (GameTests, `BloodMoonGameTests`)

Each test has its own `test_environment` (`minecraft:clock_time` on `minecraft:overworld`) so it runs in its own batch and its clock is restored at teardown. Each test sets the overworld clock explicitly with `server.clockManager().setTotalTicks(...)` so it does not depend on whether time advances in the test server.

- Rising: before day 24's night no message; set clock into day 24's night (588 200); within a few ticks the Overworld player has exactly one Blood Moon message and the advancement; a player moved to the Nether also got exactly one message and no advancement. More ticks pass: still one message each.
- Ordinary full moon (day 16 night, 396 200): no message, no advancement.
- Back to back: one Blood Moon, then the clock set straight into the next, then the one after: each announces once, and a player who joined in between earns the advancement at the next.
- Restart: after a rising, save and reload the state as a restart would; no second message.
- The Overworld remembers the last Blood Moon across tests, so each test uses its own Blood Moons.

## Hand check

`/time set maple_flavored:blood_moon` then watch the sky redden and the moon turn red; message appears once; `/time set day` returns to normal.

## Exclusions

Restoring (43). No game rule: the period is fixed by spec and lives in data.

## Least confident

- That timeline tracks for a modded attribute in a modded timeline sync to and evaluate on the client (attribute must be syncable; registry sync of `ENVIRONMENT_ATTRIBUTE` by Fabric).
- Tag order of `in_overworld` (irrelevant now: colours are applied by client layers, not timeline tracks).
