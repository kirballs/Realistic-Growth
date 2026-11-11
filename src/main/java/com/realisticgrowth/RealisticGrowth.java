package com.realisticgrowth;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

/** Main entry point for the server/common portion of RealisticGrowth. */
@Mod(RealisticGrowth.MOD_ID)
public final class RealisticGrowth {
    public static final String MOD_ID = "realisticgrowth";

    public RealisticGrowth() {
        // Runs during CONSTRUCT, i.e. before Forge loads (and may rewrite) the common TOML config, so
        // any legacy growth_times.entries list can still be migrated to the line-preserving file.
        RealisticGrowthConfig.bootstrapEntries();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RealisticGrowthConfig.SPEC);
        // GrowthEventHandler and config reload callbacks are registered by their Forge
        // event-bus subscriber annotations; no client-side initialization is needed.
    }
}
