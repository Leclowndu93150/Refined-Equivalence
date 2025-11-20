package com.leclowndu93150.refined_equivalence.registry;

import com.leclowndu93150.refined_equivalence.RefinedEquivalence;
import com.leclowndu93150.refined_equivalence.block.entity.EmcLinkBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RefinedEquivalence.MODID);

    public static final Supplier<BlockEntityType<EmcLinkBlockEntity>> EMC_LINK = BLOCK_ENTITIES.register(
        "emc_link",
        () -> BlockEntityType.Builder.of(EmcLinkBlockEntity::new, ModBlocks.EMC_LINK.get()).build(null)
    );

    private ModBlockEntities() {
    }

    public static void register(final IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
