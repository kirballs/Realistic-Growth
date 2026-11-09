# RealisticGrowth

A Forge 1.20.1 server-side/common mod that configures the initial growth age of baby mobs.

The generated `config/realisticgrowth-common.toml` contains a fallback age and optional per-entity entries:

```toml
[growth_times]
default_age = -24000
entries = ["minecraft:cow;-168000", "alexsmobs:elephant;-480000"]
```

A negative value is the baby age in ticks. `default_age` applies to every baby `AgeableMob` not listed in `entries`; it defaults to vanilla's `-24000` ticks and can be changed to adjust the default for all such mobs, including modded ageable mobs. An entry overrides the fallback for its entity type. Entries are applied once per entity and tracked using persistent entity data, so chunk reloads do not reset growth. Vanilla feeding and its growth acceleration remain unchanged.

## Build

Use Java 17 and run `gradlew build` (or `./gradlew build` after adding the standard Forge MDK Gradle wrapper).
