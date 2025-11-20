package com.leclowndu93150.refined_equivalence.menu;

import com.leclowndu93150.refined_equivalence.block.entity.EmcLinkBlockEntity;
import com.leclowndu93150.refined_equivalence.registry.ModMenus;
import com.refinedmods.refinedstorage.common.support.AbstractBaseContainerMenu;
import com.refinedmods.refinedstorage.common.support.containermenu.ClientProperty;
import com.refinedmods.refinedstorage.common.support.containermenu.ServerProperty;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import javax.annotation.Nullable;

public class EmcLinkMenu extends AbstractBaseContainerMenu {
    
    @Nullable
    private final EmcLinkBlockEntity blockEntity;
    
    public EmcLinkMenu(int syncId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(ModMenus.EMC_LINK.get(), syncId);
        this.blockEntity = null;
        
        registerProperty(new ClientProperty<>(EmcLinkPropertyTypes.PRIORITY, 0));
        
        addPlayerInventory(playerInventory, 8, 84);
    }
    
    public EmcLinkMenu(int syncId, Inventory playerInventory, EmcLinkBlockEntity blockEntity) {
        super(ModMenus.EMC_LINK.get(), syncId);
        this.blockEntity = blockEntity;
        
        registerProperty(new ServerProperty<>(
            EmcLinkPropertyTypes.PRIORITY,
            blockEntity::getPriority,
            blockEntity::setPriority
        ));
        
        addPlayerInventory(playerInventory, 8, 84);
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) {
            return true;
        }
        return Container.stillValidBlockEntity(blockEntity, player);
    }
}
