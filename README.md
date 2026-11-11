# RealisticGrowth

A Forge 1.20.1 server-side/common mod that configures the initial growth age of baby mobs.

## Configuration

The generated `config/realisticgrowth-common.toml` keeps only the fallback age:

```toml
[growth_times]
default_age = -24000
```

`default_age` is the baby age in ticks, defaults to vanilla's `-24000`, and applies to every baby
`AgeableMob` whose entity type has no entry of its own, including modded ageable mobs.

Per-entity overrides live in a separate, line-oriented file, `config/realisticgrowth-entries.txt`,
with one `<entity_resource_location>;<negative_tick_value>` entry per line:

```text
# RealisticGrowth per-entity baby growth times.
minecraft:chicken;-72000
minecraft:cow;-168000
minecraft:pig;-120000
```

An entry overrides `default_age` for its entity type. Negative values are required; blank lines and
lines starting with `#` are ignored. Entries are applied once per entity and tracked using persistent
entity data, so chunk reloads do not reset growth. Vanilla feeding and its growth acceleration remain
unchanged.

### Why a separate file

Forge's TOML writer rewrites `realisticgrowth-common.toml` on load and flattens a TOML list into a
single line, which loses a hand-formatted one-entry-per-line list. `realisticgrowth-entries.txt` is
therefore owned by you: RealisticGrowth reads it on startup and on config reload but never rewrites
it, so your line breaks and ordering survive. You can reorder, reformat, or annotate it freely.

### Migration from the old TOML list

Older versions stored the per-entity entries in the common TOML file as
`growth_times.entries = ["minecraft:cow;-168000", ...]`.

* On startup, before Forge loads and rewrites the TOML file, the mod reads the legacy
  `growth_times.entries` list from `realisticgrowth-common.toml`. If it contains entries, they are
  written, in their original order, to `config/realisticgrowth-entries.txt` (one entry per line).
* If there are no legacy entries (fresh install, or the list was already removed), the file is
  created with the built-in defaults instead.
* An existing `realisticgrowth-entries.txt` is never migrated over or rewritten — delete or edit it
  yourself if you want to re-run the defaults.
* After Forge reloads the TOML file, the dropped `entries` key is simply gone from the TOML config;
  the entries now come from the text file. Keep using `default_age` in the TOML file as before.

## Build

Use Java 17 and run `gradlew build` (or `./gradlew build` after adding the standard Forge MDK Gradle wrapper).
