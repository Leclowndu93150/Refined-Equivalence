package com.leclowndu93150.refined_equivalence.block.entity;

import com.leclowndu93150.refined_equivalence.block.EmcLinkBlock;
import com.leclowndu93150.refined_equivalence.block.entity.lifecycle.Rs2NodeLifecycle;
import com.leclowndu93150.refined_equivalence.block.entity.storage.EmcStorage;
import com.leclowndu93150.refined_equivalence.menu.EmcLinkData;
import com.leclowndu93150.refined_equivalence.menu.EmcLinkMenu;
import com.leclowndu93150.refined_equivalence.registry.ModBlockEntities;
import com.mojang.logging.LogUtils;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.energy.EnergyNetworkComponent;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.configurationcard.ConfigurationCardTarget;
import com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer;
import com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider;
import com.refinedmods.refinedstorage.common.support.containermenu.ExtendedMenuProvider;
import com.refinedmods.refinedstorage.common.support.network.ColoredConnectionStrategy;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.ITransmutationProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public class EmcLinkBlockEntity extends BlockEntity implements ConfigurationCardTarget, ExtendedMenuProvider<EmcLinkData> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG_OWNER_ID = "Owner";
    private static final String TAG_OWNER_NAME = "OwnerName";
    private static final String TAG_PRIORITY = "Priority";

    private static volatile boolean worldUnloading;

    // Track all active EMC Link blocks for knowledge change notifications
    private static final Map<BlockPos, EmcLinkBlockEntity> ACTIVE_BLOCKS = new ConcurrentHashMap<>();

    private final EmcLinkNetworkNode networkNode;
    private final NetworkNodeContainerProvider containerProvider;
    private final InWorldNetworkNodeContainer nodeContainer;
    private final Rs2NodeLifecycle nodeLifecycle;
    private final EmcStorage storage;

    private UUID ownerId;
    private String ownerName;
    private int priority = 0;

    public EmcLinkBlockEntity(final BlockPos pos, final BlockState state) {
        super(ModBlockEntities.EMC_LINK.get(), pos, state);
        this.networkNode = new EmcLinkNetworkNode(this, 16);
        this.storage = new EmcStorage(this);
        this.containerProvider = RefinedStorageApi.INSTANCE.createNetworkNodeContainerProvider();
        this.nodeContainer = RefinedStorageApi.INSTANCE.createNetworkNodeContainer(this, networkNode)
            .connectionStrategy(new ColoredConnectionStrategy(this::getBlockState, worldPosition))
            .build();
        this.containerProvider.addContainer(nodeContainer);
        this.nodeLifecycle = new Rs2NodeLifecycle(this, containerProvider, LOGGER);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final EmcLinkBlockEntity blockEntity) {
        if (worldUnloading) {
            return;
        }
        blockEntity.nodeLifecycle.tick();
        blockEntity.tickNetworkNode();
        if (level.getGameTime() % 20 == 0) {
            blockEntity.storage.refreshCache();
        }
    }

    private void tickNetworkNode() {
        final Network network = networkNode.getNetwork();
        if (network == null || worldUnloading) {
            if (networkNode.isActive()) {
                networkNode.setActive(false);
                updateConnectionState(false);
            }
            return;
        }
        final EnergyNetworkComponent energy = network.getComponent(EnergyNetworkComponent.class);
        final boolean hasEnergy = energy == null || energy.getStored() >= networkNode.getEnergyUsage();
        if (networkNode.isActive() != hasEnergy) {
            networkNode.setActive(hasEnergy);
        }
        if (hasEnergy) {
            networkNode.doWork();
        }
    }

    public void updateConnectionState(final boolean connected) {
        if (level == null || level.isClientSide()) {
            return;
        }
        final BlockState state = getBlockState();
        if (state.getBlock() instanceof EmcLinkBlock && state.hasProperty(EmcLinkBlock.CONNECTED)
            && state.getValue(EmcLinkBlock.CONNECTED) != connected) {
            level.setBlock(worldPosition, state.setValue(EmcLinkBlock.CONNECTED, connected), 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            ACTIVE_BLOCKS.put(worldPosition, this);
            nodeLifecycle.resetAfterDataLoad();
            nodeLifecycle.requestInitialization("load");
        }
    }

    @Override
    public void setRemoved() {
        ACTIVE_BLOCKS.remove(worldPosition);
        if (!nodeLifecycle.isRemoved()) {
            nodeLifecycle.shutdown("removed", false);
        }
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        ACTIVE_BLOCKS.remove(worldPosition);
        if (!nodeLifecycle.isRemoved()) {
            nodeLifecycle.shutdown("chunk_unloaded", true);
        }
        super.onChunkUnloaded();
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (ownerId != null) {
            tag.putUUID(TAG_OWNER_ID, ownerId);
        }
        if (ownerName != null) {
            tag.putString(TAG_OWNER_NAME, ownerName);
        }
        tag.putInt(TAG_PRIORITY, priority);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ownerId = tag.hasUUID(TAG_OWNER_ID) ? tag.getUUID(TAG_OWNER_ID) : null;
        ownerName = tag.contains(TAG_OWNER_NAME, Tag.TAG_STRING) ? tag.getString(TAG_OWNER_NAME) : null;
        if (tag.contains(TAG_PRIORITY)) {
            priority = tag.getInt(TAG_PRIORITY);
        }
        nodeLifecycle.resetAfterDataLoad();
    }

    public void onRsNodeInitialized() {
        updateConnectionState(networkNode.isActive());
        networkNode.setPriority(priority);
    }

    public void bindOwner(final Player player) {
        if (level == null || level.isClientSide()) {
            return;
        }
        this.ownerId = player.getUUID();
        this.ownerName = player.getName().getString();
        setChanged();
    }

    public void onNeighborChanged() {
        if (level == null || level.isClientSide()) {
            return;
        }
        nodeLifecycle.requestInitialization("neighbor");
    }

    public void requestInitialization(final String reason) {
        nodeLifecycle.requestInitialization(reason);
    }

    public Component describeOwner() {
        if (ownerName == null) {
            return Component.translatable("message.refined_equivalence.emc_link.unbound");
        }
        return Component.translatable("message.refined_equivalence.emc_link.owner", ownerName);
    }

    public Optional<IKnowledgeProvider> getKnowledgeProvider() {
        if (ownerId == null || level == null || level.isClientSide() || level.getServer() == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(ITransmutationProxy.INSTANCE.getKnowledgeProviderFor(ownerId));
        } catch (Exception e) {
            LOGGER.warn("Failed to resolve knowledge provider for {}: {}", ownerId, e.getMessage());
            return Optional.empty();
        }
    }

    public void onProviderChanged() {
        if (level != null && !level.isClientSide()) {
            getKnowledgeProvider().ifPresent(this::syncEmc);
            storage.refreshCache();
            setChanged();
        }
    }

    private void syncEmc(final IKnowledgeProvider provider) {
        if (!(level instanceof ServerLevel serverLevel) || ownerId == null) {
            return;
        }
        final ServerPlayer ownerPlayer = serverLevel.getServer().getPlayerList().getPlayer(ownerId);
        if (ownerPlayer != null) {
            provider.syncEmc(ownerPlayer);
        }
    }

    public EmcStorage getStorage() {
        return storage;
    }

    public NetworkNodeContainerProvider getContainerProvider() {
        return containerProvider;
    }

    public static void setWorldUnloading(final boolean unloading) {
        worldUnloading = unloading;
        if (unloading) {
            ACTIVE_BLOCKS.clear();
        }
    }

    public static boolean isWorldUnloading() {
        return worldUnloading;
    }

    public static void onKnowledgeChanged(final UUID playerUUID) {
        for (EmcLinkBlockEntity blockEntity : ACTIVE_BLOCKS.values()) {
            if (playerUUID.equals(blockEntity.ownerId)) {
                blockEntity.storage.invalidateCache();
            }
        }
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        if (this.priority != priority) {
            this.priority = priority;
            if (networkNode != null) {
                networkNode.setPriority(priority);
            }
            setChanged();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.refined_equivalence.emc_link");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new EmcLinkMenu(syncId, playerInventory, this);
    }

    @Override
    public EmcLinkData getMenuData() {
        return new EmcLinkData();
    }

    @Override
    public StreamEncoder<RegistryFriendlyByteBuf, EmcLinkData> getMenuCodec() {
        return EmcLinkData.STREAM_CODEC;
    }

    @Override
    public void writeConfiguration(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt(TAG_PRIORITY, priority);
    }

    @Override
    public void readConfiguration(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag.contains(TAG_PRIORITY)) {
            setPriority(tag.getInt(TAG_PRIORITY));
        }
    }
}
