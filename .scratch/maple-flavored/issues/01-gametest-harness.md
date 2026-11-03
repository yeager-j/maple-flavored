# 01: GameTest harness

**What to build:** Set up Fabric's server GameTest framework so every later ticket can test its rules through a headless game server. Read the spec's Testing Decisions first.

**Blocked by:** None (can start immediately)

**Status:** done

Spec: the Maple Flavored spec in this folder.

- [x] A GameTest source set and Gradle task exist, and a failing GameTest fails the build
- [x] One smoke test passes: it spawns a mock player in a test structure and checks a vanilla fact
- [x] The README says how to run the tests, including the JAVA_HOME note
