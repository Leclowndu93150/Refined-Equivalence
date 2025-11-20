package com.leclowndu93150.refined_equivalence.registry;

import com.leclowndu93150.refined_equivalence.RefinedEquivalence;
import com.leclowndu93150.refined_equivalence.menu.EmcLinkMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(
        Registries.MENU,
        RefinedEquivalence.MODID
    );

    public static final DeferredHolder<MenuType<?>, MenuType<EmcLinkMenu>> EMC_LINK = MENUS.register(
        "emc_link",
        () -> IMenuTypeExtension.create(EmcLinkMenu::new)
    );
    
    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
