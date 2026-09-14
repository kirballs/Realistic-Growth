# RealisticGrowth

A Forge 1.20.1 server-side/common mod that configures the initial growth age of baby mobs.

The generated `config/realisticgrowth-common.toml` contains entries in the form:

```toml
[growth_times]
entries = ["minecraft:cow;-168000", "alexsmobs:elephant;-480000"]
```

A negative value is the baby age in ticks. Unlisted entity types retain vanilla behavior. Entries are applied once per entity and tracked using persistent entity data, so chunk reloads do not reset growth.

## Build

Use Java 17 and run `gradlew build` (or `./gradlew build` after adding the standard Forge MDK Gradle wrapper).
