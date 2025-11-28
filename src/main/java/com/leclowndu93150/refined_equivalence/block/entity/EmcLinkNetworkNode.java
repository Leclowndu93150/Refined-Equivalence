package com.leclowndu93150.refined_equivalence.block.entity;

import com.refinedmods.refinedstorage.api.network.impl.node.AbstractNetworkNode;
import com.refinedmods.refinedstorage.api.network.storage.StorageProvider;
import com.refinedmods.refinedstorage.api.storage.Storage;

public final class EmcLinkNetworkNode extends AbstractNetworkNode implements StorageProvider {
    private final EmcLinkBlockEntity blockEntity;
    private final long energyUsage;
    private int priority = 0;

    public EmcLinkNetworkNode(final EmcLinkBlockEntity blockEntity, final long energyUsage) {
        this.blockEntity = blockEntity;
        this.energyUsage = energyUsage;
    }

    @Override
    protected void onActiveChanged(final boolean newActive) {
        super.onActiveChanged(newActive);
        blockEntity.updateConnectionState(newActive);
    }

    @Override
    public long getEnergyUsage() {
        return energyUsage;
    }

    @Override
    public Storage getStorage() {
        return blockEntity.getStorage();
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
