package com.realisticgrowth;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Per-entity growth times, stored in a line-oriented text file next to the TOML config.
 *
 * <p>Each line holds one {@code <entity_resource_location>;<negative_tick_value>} entry. The file
 * belongs to the user: it is only ever written once, when it does not exist yet (either migrated from
 * the legacy {@code growth_times.entries} TOML list or seeded with the built-in defaults). Reads never
 * rewrite it, so the user's line breaks and ordering are preserved exactly.</p>
 */
public final class GrowthEntries {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** File inside the Forge {@code config} directory that holds the per-entity entries. */
    public static final String FILE_NAME = "realisticgrowth-entries.txt";
    /** Common TOML config file that may still carry a legacy {@code growth_times.entries} list. */
    public static final String LEGACY_CONFIG_FILE_NAME = "realisticgrowth-common.toml";

    /** Entries written when no file and no legacy entries exist yet. */
    public static final List<String> DEFAULT_ENTRIES = List.of(
            "minecraft:chicken;-72000",
            "minecraft:pig;-120000",
            "minecraft:sheep;-120000",
            "minecraft:cow;-168000",
            "minecraft:horse;-240000",
            "minecraft:donkey;-240000",
            "minecraft:mule;-240000",
            "minecraft:wolf;-120000",
            "minecraft:cat;-96000",
            "minecraft:rabbit;-72000",
            "minecraft:goat;-168000",
            "minecraft:fox;-96000",
            "minecraft:turtle;-168000",
            "minecraft:villager;-480000",
            "minecraft:mooshroom;-168000",
            "minecraft:llama;-240000",
            "minecraft:bee;-48000",
            "minecraft:hoglin;-120000",
            "minecraft:strider;-120000",
            "minecraft:frog;-72000",
            "minecraft:axolotl;-96000",
            "minecraft:camel;-240000",
            "minecraft:sniffer;-168000"
    );

    private static final List<String> HEADER = List.of(
            "# RealisticGrowth per-entity baby growth times.",
            "#",
            "# One entry per line, in the form:",
            "#   <entity_resource_location>;<negative_tick_value>",
            "#",
            "# For example:",
            "#   minecraft:cow;-168000",
            "#",
            "# Blank lines and lines starting with '#' are ignored. The order of the lines here is kept as",
            "# written, and this file is not rewritten by the mod: RealisticGrowth only creates it when it",
            "# is missing, so your formatting survives. An entry overrides growth_times.default_age from",
            "# " + LEGACY_CONFIG_FILE_NAME + " for its entity type; every other baby mob uses default_age."
    );

    private static volatile Path entriesFile;
    private static volatile Map<ResourceLocation, Integer> growthTimes = Map.of();

    private GrowthEntries() { }

    /**
     * Prepares the entries file for this launch: migrates the legacy TOML list, seeds the built-in
     * defaults when there is nothing to migrate, or simply reads an existing file. Never throws; on
     * I/O failure the in-memory entries fall back to the built-in defaults.
     *
     * @param configDir the Forge {@code config} directory
     */
    public static synchronized void initialize(Path configDir) {
        Path target = configDir.resolve(FILE_NAME).toAbsolutePath();
        entriesFile = target;

        try {
            if (Files.isRegularFile(target)) {
                readFile(target);
                return;
            }

            List<String> migrated = migrateLegacyEntries(configDir.resolve(LEGACY_CONFIG_FILE_NAME));
            List<String> lines = migrated.isEmpty() ? normalize(DEFAULT_ENTRIES) : migrated;
            boolean created = createFile(target, lines);

            if (created && migrated.isEmpty()) {
                LOGGER.info("Created {} with {} built-in growth-time entries", target.getFileName(), lines.size());
            } else if (created) {
                LOGGER.info("Migrated {} growth-time entries from {} into {}",
                        migrated.size(), LEGACY_CONFIG_FILE_NAME, target.getFileName());
            }
            readFile(target);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Could not prepare {}; using built-in growth-time entries until it can be read",
                    target, exception);
            growthTimes = immutable(normalize(DEFAULT_ENTRIES), message -> { });
        }
    }

    /** Re-reads the entries file without ever writing to it; used when the common config reloads. */
    public static synchronized void reload() {
        Path file = entriesFile;
        if (file == null) {
            return;
        }
        try {
            readFile(file);
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not read {}; keeping the {} growth-time entries already loaded",
                    file, growthTimes.size(), exception);
        }
    }

    /** Returns the configured age for one entity type, or {@code fallback} when it has no entry. */
    public static int ageFor(ResourceLocation entityId, int fallback) {
        if (entityId == null) {
            return fallback;
        }
        Integer configured = growthTimes.get(entityId);
        return configured == null ? fallback : configured;
    }

    /** Returns an immutable, file-ordered snapshot of the per-entity entries. */
    public static Map<ResourceLocation, Integer> growthTimes() {
        return growthTimes;
    }

    private static void readFile(Path file) throws IOException {
        List<String> lines = stripByteOrderMark(Files.readAllLines(file, StandardCharsets.UTF_8));
        growthTimes = immutable(lines, LOGGER::warn);
        LOGGER.debug("Loaded {} growth-time entries from {}", growthTimes.size(), file);
    }

    /**
     * Builds the initial file content. Only called when the file is absent, so a file created
     * concurrently (or by hand) is left untouched.
     *
     * @return whether this call created the file
     */
    private static boolean createFile(Path file, List<String> entryLines) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        StringBuilder content = new StringBuilder();
        for (String line : HEADER) {
            content.append(line).append('\n');
        }
        for (String line : entryLines) {
            content.append(line).append('\n');
        }

        try {
            Files.write(file, content.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return true;
        } catch (FileAlreadyExistsException exception) {
            LOGGER.debug("{} already exists; leaving it untouched", file);
            return false;
        }
    }

    /**
     * Reads the legacy {@code growth_times.entries} list straight out of the TOML file. This has to
     * happen before Forge loads (and rewrites, or drops) the old list, so it runs from the mod
     * constructor and parses the raw text instead of going through the config system.
     *
     * @return normalized {@code id;age} lines in their original order, or an empty list when the old
     *         config has no usable entries
     */
    private static List<String> migrateLegacyEntries(Path legacyConfig) {
        String text;
        try {
            if (!Files.isRegularFile(legacyConfig)) {
                return List.of();
            }
            text = new String(Files.readAllBytes(legacyConfig), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOGGER.warn("Could not read {} while looking for legacy growth-time entries", legacyConfig, exception);
            return List.of();
        }

        List<String> values = legacyEntryValues(text);
        if (values.isEmpty()) {
            return List.of();
        }
        return normalize(values);
    }

    // ---------------------------------------------------------------------------------------------
    // Parsing helpers (also exercised by the standalone verification harness)
    // ---------------------------------------------------------------------------------------------

    /**
     * Parses {@code id;age} lines. Blank lines and {@code #} comments are ignored; ordering is
     * preserved; malformed lines are reported through {@code warning} and skipped; a repeated entity
     * keeps its first position and its last value, mirroring the old list behaviour.
     */
    public static Map<ResourceLocation, Integer> parseEntries(List<String> lines, Consumer<String> warning) {
        Map<ResourceLocation, Integer> parsed = new LinkedHashMap<>();
        int lineNumber = 0;
        for (String raw : lines) {
            lineNumber++;
            String line = stripComment(raw).strip();
            if (line.isEmpty()) {
                continue;
            }
            parseSingle(line, "line " + lineNumber, parsed, warning);
        }
        return parsed;
    }

    /** Parses {@code id;age} strings into the ordered {@code id;age} lines written to the new file. */
    public static List<String> normalize(List<String> values) {
        Map<ResourceLocation, Integer> parsed = new LinkedHashMap<>();
        for (String value : values) {
            parseSingle(value.strip(), "entry \"" + value + "\"", parsed, LOGGER::warn);
        }

        List<String> normalized = new ArrayList<>(parsed.size());
        parsed.forEach((id, age) -> normalized.add(id + ";" + age));
        return List.copyOf(normalized);
    }

    private static void parseSingle(String text, String origin, Map<ResourceLocation, Integer> into,
                                    Consumer<String> warning) {
        String line = stripComment(text).strip();
        int separator = line.indexOf(';');
        if (separator < 0) {
            warning.accept("Ignoring malformed RealisticGrowth entry (" + origin
                    + "); expected <entity_resource_location>;<negative_tick_value>: " + line);
            return;
        }

        String idPart = line.substring(0, separator).strip();
        String agePart = line.substring(separator + 1).strip();
        ResourceLocation id = ResourceLocation.tryParse(idPart);
        Integer age = parseAge(agePart);
        if (id == null || age == null) {
            warning.accept("Ignoring invalid RealisticGrowth entry (" + origin
                    + "); the resource location must be valid and the age negative: " + line);
            return;
        }

        if (into.put(id, age) != null) {
            warning.accept("Duplicate RealisticGrowth entry for " + id + " (" + origin
                    + "); the last value wins");
        }
    }

    private static Integer parseAge(String text) {
        try {
            int age = Integer.parseInt(text);
            return age < 0 ? age : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static List<String> stripByteOrderMark(List<String> lines) {
        if (lines.isEmpty() || lines.get(0).isEmpty() || lines.get(0).charAt(0) != '\uFEFF') {
            return lines;
        }
        List<String> stripped = new ArrayList<>(lines);
        stripped.set(0, stripped.get(0).substring(1));
        return stripped;
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        return hash < 0 ? line : line.substring(0, hash);
    }

    /**
     * Extracts the string values of the legacy {@code growth_times.entries} list from raw TOML text.
     * Supports single- and multi-line arrays as well as basic ("double quoted") and literal
     * ('single quoted') strings.
     */
    public static List<String> legacyEntryValues(String tomlText) {
        List<String> values = new ArrayList<>();
        if (tomlText == null || tomlText.isEmpty()) {
            return values;
        }
        if (tomlText.charAt(0) == '\uFEFF') {
            tomlText = tomlText.substring(1);
        }

        String[] lines = tomlText.split("\\r\\n|\\n|\\r", -1);
        String table = "";
        for (int index = 0; index < lines.length; index++) {
            String line = stripComment(lines[index]).strip();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                table = line.substring(1, line.length() - 1).strip();
                continue;
            }

            int equals = line.indexOf('=');
            if (equals < 0) {
                continue;
            }
            String key = line.substring(0, equals).strip();
            boolean matches = ("growth_times".equals(table) && "entries".equals(key))
                    || "growth_times.entries".equals(key);
            if (!matches) {
                continue;
            }

            StringBuilder array = new StringBuilder(line.substring(equals + 1));
            while (!bracketsBalanced(array) && index + 1 < lines.length) {
                index++;
                array.append('\n').append(stripComment(lines[index]));
            }
            values.addAll(quotedValues(array.toString()));
        }
        return values;
    }

    private static boolean bracketsBalanced(CharSequence text) {
        int depth = 0;
        char quote = 0;
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (quote != 0) {
                if (current == '\\' && quote == '"') {
                    index++;
                } else if (current == quote) {
                    quote = 0;
                }
            } else if (current == '"' || current == '\'') {
                quote = current;
            } else if (current == '[') {
                depth++;
            } else if (current == ']') {
                depth--;
            }
        }
        return depth <= 0;
    }

    private static List<String> quotedValues(CharSequence text) {
        List<String> values = new ArrayList<>();
        int index = 0;
        while (index < text.length()) {
            char current = text.charAt(index);
            if (current != '"' && current != '\'') {
                index++;
                continue;
            }

            char quote = current;
            StringBuilder value = new StringBuilder();
            index++;
            while (index < text.length()) {
                char character = text.charAt(index);
                if (character == '\\' && quote == '"' && index + 1 < text.length()) {
                    char escaped = text.charAt(index + 1);
                    value.append(switch (escaped) {
                        case 'n' -> '\n';
                        case 't' -> '\t';
                        case 'r' -> '\r';
                        default -> escaped;
                    });
                    index += 2;
                    continue;
                }
                if (character == quote) {
                    index++;
                    break;
                }
                value.append(character);
                index++;
            }
            values.add(value.toString());
        }
        return values;
    }

    private static Map<ResourceLocation, Integer> immutable(List<String> lines, Consumer<String> warning) {
        return Collections.unmodifiableMap(parseEntries(lines, warning));
    }
}
