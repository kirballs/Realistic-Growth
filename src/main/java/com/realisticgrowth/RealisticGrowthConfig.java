package com.realisticgrowth;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.util.Map;

/**
 * Defines the common TOML config. Only the fallback {@code growth_times.default_age} lives here;
 * per-entity entries are kept in {@code config/realisticgrowth-entries.txt} so that Forge's TOML
 * writer can never flatten the user's formatting. See {@link GrowthEntries}.
 */
@Mod.EventBusSubscriber(modid = RealisticGrowth.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RealisticGrowthConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.IntValue DEFAULT_AGE;
    private static volatile int defaultAge = -24_000;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("growth_times");
        DEFAULT_AGE = builder
                .comment("Fallback baby age in ticks for every baby mob without an entry in",
                        GrowthEntries.FILE_NAME + ". Vanilla uses -24000. Must be negative.")
                .defineInRange("default_age", -24_000, Integer.MIN_VALUE, -1);
        builder.pop();
        SPEC = builder.build();
    }

    private RealisticGrowthConfig() { }

    /**
     * Runs from the mod constructor, before Forge loads the common config. This is the last moment
     * the legacy {@code growth_times.entries} list can still be read from disk and migrated before
     * Forge rewrites or discards it.
     */
    public static void bootstrapEntries() {
        GrowthEntries.initialize(FMLPaths.CONFIGDIR.get());
    }

    @SubscribeEvent
    public static void onConfigLoading(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            defaultAge = DEFAULT_AGE.get();
            // Reload in case the entries file was created or edited while the TOML config loaded.
            GrowthEntries.reload();
        }
    }

    /** Returns the configured fallback age for babies whose entity type has no entry. */
    public static int defaultAge() {
        return defaultAge;
    }

    /** Returns the age for one entity type, falling back to {@link #defaultAge()}. */
    public static int ageFor(ResourceLocation entityId) {
        return GrowthEntries.ageFor(entityId, defaultAge);
    }

    /** Returns an immutable, file-ordered snapshot of the per-entity entries. */
    public static Map<ResourceLocation, Integer> growthTimes() {
        return GrowthEntries.growthTimes();
    }
}
