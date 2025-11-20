package com.leclowndu93150.refined_equivalence.registry;

import com.leclowndu93150.refined_equivalence.block.entity.EmcLinkBlockEntity;
import com.refinedmods.refinedstorage.neoforge.api.RefinedStorageNeoForgeApi;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ModCapabilities {
    private ModCapabilities() {
    }

    public static void register(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability(),
            ModBlockEntities.EMC_LINK.get(),
            (EmcLinkBlockEntity blockEntity, net.minecraft.core.Direction direction) -> blockEntity.getContainerProvider()
        );
    }
}
