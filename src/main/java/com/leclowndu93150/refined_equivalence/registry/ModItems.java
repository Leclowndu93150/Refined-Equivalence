package com.leclowndu93150.refined_equivalence.registry;

import com.leclowndu93150.refined_equivalence.RefinedEquivalence;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RefinedEquivalence.MODID);

    public static final DeferredItem<BlockItem> EMC_LINK = ITEMS.register(
        "emc_link",
        () -> new BlockItem(ModBlocks.EMC_LINK.get(), new Item.Properties())
    );

    private ModItems() {
    }

    public static void register(final IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
