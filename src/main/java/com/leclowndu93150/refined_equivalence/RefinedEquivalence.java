package com.leclowndu93150.refined_equivalence;

import com.leclowndu93150.refined_equivalence.block.entity.EmcLinkBlockEntity;
import com.leclowndu93150.refined_equivalence.registry.ModBlockEntities;
import com.leclowndu93150.refined_equivalence.registry.ModBlocks;
import com.leclowndu93150.refined_equivalence.registry.ModCapabilities;
import com.leclowndu93150.refined_equivalence.registry.ModCreativeTabs;
import com.leclowndu93150.refined_equivalence.registry.ModItems;
import com.leclowndu93150.refined_equivalence.registry.ModMenus;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import org.slf4j.Logger;

@Mod(RefinedEquivalence.MODID)
public final class RefinedEquivalence {
    public static final String MODID = "refined_equivalence";
    private static final Logger LOGGER = LogUtils.getLogger();

    public RefinedEquivalence(final IEventBus modEventBus, final ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenus.register(modEventBus);

        modEventBus.addListener(this::registerCapabilities);

        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
    }

    private void registerCapabilities(final RegisterCapabilitiesEvent event) {
        ModCapabilities.register(event);
    }

    private void onServerStarting(final ServerStartingEvent event) {
        EmcLinkBlockEntity.setWorldUnloading(false);
    }

    private void onServerStopping(final ServerStoppingEvent event) {
        EmcLinkBlockEntity.setWorldUnloading(true);
    }
}
