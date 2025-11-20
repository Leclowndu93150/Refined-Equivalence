package com.leclowndu93150.refined_equivalence.client;

import com.leclowndu93150.refined_equivalence.RefinedEquivalence;
import com.leclowndu93150.refined_equivalence.client.screen.EmcLinkScreen;
import com.leclowndu93150.refined_equivalence.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = RefinedEquivalence.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientSetup {
    
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.EMC_LINK.get(), EmcLinkScreen::new);
    }
}
