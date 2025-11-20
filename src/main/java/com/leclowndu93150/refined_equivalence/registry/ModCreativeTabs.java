package com.leclowndu93150.refined_equivalence.registry;

import com.leclowndu93150.refined_equivalence.RefinedEquivalence;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RefinedEquivalence.MODID);

    public static final Supplier<CreativeModeTab> MAIN = TABS.register(
        "main",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + RefinedEquivalence.MODID))
            .icon(() -> ModItems.EMC_LINK.get().getDefaultInstance())
            .displayItems((params, output) -> output.accept(ModItems.EMC_LINK.get()))
            .build()
    );

    private ModCreativeTabs() {
    }

    public static void register(final IEventBus eventBus) {
        TABS.register(eventBus);
    }
}
