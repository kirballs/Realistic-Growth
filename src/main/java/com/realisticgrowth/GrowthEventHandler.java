package com.realisticgrowth;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AgeableMob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** Applies configured ages exactly once to newly encountered baby entities. */
@Mod.EventBusSubscriber(modid = RealisticGrowth.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GrowthEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String APPLIED_TAG = "realisticgrowth_applied";

    private GrowthEventHandler() { }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AgeableMob baby)) {
            return;
        }
        if (!baby.isBaby() || baby.getPersistentData().getBoolean(APPLIED_TAG)) {
            return;
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(baby.getType());
        int configuredAge = RealisticGrowthConfig.growthTimes()
                .getOrDefault(entityId, RealisticGrowthConfig.defaultAge());

        // A just-created AgeableMob starts with forcedAge and forcedAgeTimer at zero.
        // setAge changes only its normal age, so feeding continues to use the new age.
        baby.setAge(configuredAge);
        baby.getPersistentData().putBoolean(APPLIED_TAG, true);
        LOGGER.debug("[RealisticGrowth] Set {} baby age to {}", entityId, configuredAge);
    }
}
