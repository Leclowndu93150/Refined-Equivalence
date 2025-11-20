package com.leclowndu93150.refined_equivalence.menu;

import com.leclowndu93150.refined_equivalence.RefinedEquivalence;
import com.refinedmods.refinedstorage.common.support.containermenu.PropertyType;
import com.refinedmods.refinedstorage.common.support.containermenu.PropertyTypes;
import net.minecraft.resources.ResourceLocation;

public final class EmcLinkPropertyTypes {
    
    public static final PropertyType<Integer> PRIORITY = PropertyTypes.createIntegerProperty(
        ResourceLocation.fromNamespaceAndPath(RefinedEquivalence.MODID, "emc_link_priority")
    );
    
    private EmcLinkPropertyTypes() {
    }
}
