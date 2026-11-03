# Maple Flavored

A Fabric mod for Minecraft 26.3: a slow-paced progression overhaul inspired by Matcha Flavoured and Raspberry Flavoured. See [CREDITS.md](CREDITS.md).

## Develop

The build uses the portable JDK 25 set in `gradle.properties`. There's no system-wide Java, so set `JAVA_HOME` to that JDK before running `gradlew`.

```bash
./gradlew build
```

```bash
./gradlew runClient
```

## Test

Tests are Fabric server GameTests in `src/gametest`. They run on a headless game server, and `build` runs them through `check`, so a failing test fails the build. As with any `gradlew` command, set `JAVA_HOME` to the JDK named by `org.gradle.java.home` in `gradle.properties` first, for example:

```bash
export JAVA_HOME="C:/Users/jackson/.jdks/jdk-25.0.4.1+1"
```

Run only the GameTests:

```bash
./gradlew runGameTest
```

Results are written as JUnit XML to `build/gametest/report.xml`. CI runs the same build on every pull request and push to `main`, and uploads that report.

## License

[CC BY-NC-SA 4.0](LICENSE), so that assets from Matcha Flavoured can be reused under its share-alike terms. Don't sell it.
