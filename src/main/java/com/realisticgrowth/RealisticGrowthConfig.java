package com.realisticgrowth;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Defines the common config and turns its string entries into fast lookups. */
@Mod.EventBusSubscriber(modid = RealisticGrowth.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RealisticGrowthConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final List<String> DEFAULT_ENTRIES = List.of(
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

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTRY_LIST;
    private static volatile Map<ResourceLocation, Integer> growthTimes = Map.of();

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("growth_times");
        ENTRY_LIST = builder.defineList("entries", DEFAULT_ENTRIES,
                value -> value instanceof String);
        builder.pop();
        SPEC = builder.build();
    }

    private RealisticGrowthConfig() { }

    @SubscribeEvent
    public static void onConfigLoading(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            loadEntries();
        }
    }

    /** Returns an immutable snapshot suitable for use from the game thread. */
    public static Map<ResourceLocation, Integer> growthTimes() {
        return growthTimes;
    }

    private static void loadEntries() {
        Map<ResourceLocation, Integer> parsed = new HashMap<>();
        for (String entry : ENTRY_LIST.get()) {
            String[] parts = entry.split(";", 2);
            if (parts.length != 2) {
                LOGGER.warn("Ignoring malformed RealisticGrowth entry: {}", entry);
                continue;
            }

            ResourceLocation id = ResourceLocation.tryParse(parts[0].trim());
            Integer age;
            try {
                age = Integer.valueOf(parts[1].trim());
            } catch (NumberFormatException exception) {
                age = null;
            }
            if (id == null || age == null || age >= 0) {
                LOGGER.warn("Ignoring invalid RealisticGrowth entry (resource location and age must be valid, age negative): {}", entry);
                continue;
            }
            parsed.put(id, age);
        }
        growthTimes = Collections.unmodifiableMap(parsed);
        LOGGER.debug("Loaded {} RealisticGrowth growth-time entries", parsed.size());
    }
}
