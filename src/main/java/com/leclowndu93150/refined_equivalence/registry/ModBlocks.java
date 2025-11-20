package com.leclowndu93150.refined_equivalence.registry;

import com.leclowndu93150.refined_equivalence.RefinedEquivalence;
import com.leclowndu93150.refined_equivalence.block.EmcLinkBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RefinedEquivalence.MODID);

    public static final DeferredBlock<EmcLinkBlock> EMC_LINK = BLOCKS.register(
        "emc_link",
        () -> new EmcLinkBlock(BlockBehaviour.Properties.of().strength(4.0F).requiresCorrectToolForDrops())
    );

    private ModBlocks() {
    }

    public static void register(final IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
