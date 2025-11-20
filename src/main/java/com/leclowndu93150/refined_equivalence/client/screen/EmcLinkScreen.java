package com.leclowndu93150.refined_equivalence.client.screen;

import com.leclowndu93150.refined_equivalence.RefinedEquivalence;
import com.leclowndu93150.refined_equivalence.menu.EmcLinkMenu;
import com.leclowndu93150.refined_equivalence.menu.EmcLinkPropertyTypes;
import com.refinedmods.refinedstorage.common.support.AbstractBaseScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class EmcLinkScreen extends AbstractBaseScreen<EmcLinkMenu> {
    
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        RefinedEquivalence.MODID,
        "textures/gui/emc_link.png"
    );
    
    private static final Component HELP_LINE1 = Component.literal("Use the side button");
    private static final Component HELP_LINE2 = Component.literal("to change priority");
    
    private final Inventory inventory;
    
    public EmcLinkScreen(EmcLinkMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.inventory = playerInventory;
        this.inventoryLabelY = 72;
        this.imageWidth = 176;
        this.imageHeight = 166;
    }
    
    @Override
    protected void init() {
        super.init();
        addSideButton(new EmcLinkPrioritySideButtonWidget(
            getMenu().getProperty(EmcLinkPropertyTypes.PRIORITY),
            inventory,
            this
        ));
    }
    
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        
        int x1 = (imageWidth - font.width(HELP_LINE1)) / 2;
        int x2 = (imageWidth - font.width(HELP_LINE2)) / 2;
        int y = 30;
        
        graphics.drawString(font, HELP_LINE1, x1, y, 0x404040, false);
        graphics.drawString(font, HELP_LINE2, x2, y + 10, 0x404040, false);
    }
    
    @Override
    protected ResourceLocation getTexture() {
        return TEXTURE;
    }
}
