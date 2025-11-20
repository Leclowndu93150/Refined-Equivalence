package com.leclowndu93150.refined_equivalence.item;

import com.refinedmods.refinedstorage.common.api.support.HelpTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

public class EmcLinkBlockItem extends BlockItem {
    
    private static final Component HELP_TEXT = Component.translatable("block.refined_equivalence.emc_link.tooltip");
    
    public EmcLinkBlockItem(Block block, Properties properties) {
        super(block, properties);
    }
    
    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.of(new HelpTooltipComponent(HELP_TEXT));
    }
}
