package com.leclowndu93150.refined_equivalence.block.entity;

import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.impl.node.AbstractNetworkNode;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.network.storage.StorageProvider;
import com.refinedmods.refinedstorage.api.storage.Storage;

import javax.annotation.Nullable;

public final class EmcLinkNetworkNode extends AbstractNetworkNode implements StorageProvider {
    private final EmcLinkBlockEntity blockEntity;
    private final long energyUsage;
    private int priority = 0;

    public EmcLinkNetworkNode(final EmcLinkBlockEntity blockEntity, final long energyUsage) {
        this.blockEntity = blockEntity;
        this.energyUsage = energyUsage;
    }

    @Override
    public void setNetwork(@Nullable final Network newNetwork) {
        final Network previous = this.network;
        if (previous != null && previous != newNetwork) {
            final StorageNetworkComponent storage = previous.getComponent(StorageNetworkComponent.class);
            if (storage != null) {
                storage.removeSource(getStorage());
            }
        }
        super.setNetwork(newNetwork);
        if (newNetwork != null && isActive()) {
            final StorageNetworkComponent storage = newNetwork.getComponent(StorageNetworkComponent.class);
            if (storage != null) {
                storage.addSource(getStorage());
            }
        }
    }

    @Override
    protected void onActiveChanged(final boolean newActive) {
        super.onActiveChanged(newActive);
        if (network != null) {
            final StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
            if (storage != null) {
                if (newActive) {
                    storage.addSource(getStorage());
                } else {
                    storage.removeSource(getStorage());
                }
            }
        }
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

    public void refreshStorage() {
        if (network == null || !isActive()) {
            return;
        }
        final StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
        if (storage != null) {
            storage.removeSource(getStorage());
            storage.addSource(getStorage());
        }
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
