![Maple Flavored logo](maple_flavored.png)

# Maple Flavored

A Fabric mod for Minecraft 26.3: a slow-paced progression overhaul inspired by Matcha Flavoured and Raspberry Flavoured. See [CREDITS.md](CREDITS.md).

## Develop

The build needs a JDK of the `java_version` in `gradle.properties`, which is where every version is set. Gradle runs on the JDK `JAVA_HOME` names, so set it before running `gradlew`, for example:

```bash
export JAVA_HOME="$HOME/.jdks/jdk-25.0.4.1+1"
```

```bash
./gradlew build
```

```bash
./gradlew runClient
```

## Test

Tests are Fabric server GameTests in `src/gametest`. They run on a headless game server, and `build` runs them through `check`, so a failing test fails the build.

Run only the GameTests:

```bash
./gradlew runGameTest
```

Results are written as JUnit XML to `build/gametest/report.xml`. CI runs the same build on every pull request and push to `main`, and uploads that report.

`check` also runs `checkWiring`, which fails when a mixin or GameTest class is missing from its list, since a missing one silently never runs. CI then regenerates `src/main/generated` and fails if it differs from what is committed, so run datagen and commit the result after changing a generator:

```bash
./gradlew runDatagen
```

The client GameTests in `src/gametest/java/mapleflavored/gametest/client` start a real client, and CI doesn't run them. Run them yourself:

```bash
./gradlew runClientGameTest
```

## Compatibility

Maple Flavored doesn't support vanilla's Villager Trade Rebalance experiment. That data pack's trades name Curse of Binding, which Maple removes, so a world with the experiment turned on fails to load. Leave it off when creating a world.

## License

[CC BY-NC-SA 4.0](LICENSE), so that assets from Matcha Flavoured can be reused under its share-alike terms. Don't sell it.
