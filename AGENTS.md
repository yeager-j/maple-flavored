# Maple Flavored

A Fabric mod for Minecraft 26.3, inspired by Matcha Flavoured and Raspberry Flavoured. Reused assets must be listed in `CREDITS.md`, and the mod must stay under CC BY-NC-SA 4.0.

There's no system-wide Java. Set `JAVA_HOME` to the JDK named in `gradle.properties` before running `gradlew`.

## Design

The design is being worked out on the map at `.scratch/maple-flavored-design/map.md`. Jackson's opinions behind it are in `docs/manifestos/`, and research findings are in `docs/research/`.

When Jackson asks to tweak a tuning value (a radius, rate, charge or count), offer to make it a playtest game rule in `MapleGameRules` instead, so it can be tuned in game without a rebuild.

## Agent skills

### Issue tracker

Issues live as local markdown files under `.scratch/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Default labels: needs-triage, needs-info, ready-for-agent, ready-for-human, wontfix. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.
