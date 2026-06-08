package com.leclowndu93150.refined_equivalence.block.entity.storage;

import com.leclowndu93150.refined_equivalence.block.entity.EmcLinkBlockEntity;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceList;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceListImpl;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeAwareChild;
import com.refinedmods.refinedstorage.api.storage.composite.ParentComposite;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;

public final class EmcStorage implements Storage, CompositeAwareChild {
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    private final EmcLinkBlockEntity owner;
    private final Set<ParentComposite> parents = new HashSet<>();
    private final MutableResourceList cache = MutableResourceListImpl.create();
    private final Map<ItemInfo, ItemResource> itemResourceCache = new HashMap<>();

    public EmcStorage(final EmcLinkBlockEntity owner) {
        this.owner = owner;
    }

    public void invalidateCache() {
        detectChanges();
    }

    public void refreshCache() {
        detectChanges();
    }

    @Override
    public long insert(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        if (!(resource instanceof ItemResource itemResource) || amount <= 0) {
            return 0;
        }
        final ItemInfo info = toPersistentInfo(itemResource);
        if (info == null) {
            return 0;
        }
        final long sellValue = IEMCProxy.INSTANCE.getSellValue(info);
        if (sellValue <= 0) {
            return 0;
        }
        final Optional<IKnowledgeProvider> providerOptional = getMutableProvider();
        if (providerOptional.isEmpty()) {
            return 0;
        }
        if (action == Action.EXECUTE) {
            final IKnowledgeProvider provider = providerOptional.get();
            if (provider.addKnowledge(info)) {
                owner.syncKnowledgeChange(provider, info, true);
            }
            final BigInteger delta = BigInteger.valueOf(sellValue).multiply(BigInteger.valueOf(amount));
            provider.setEmc(provider.getEmc().add(delta));
            owner.onProviderChanged();
        }
        return amount;
    }

    @Override
    public long extract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        if (!(resource instanceof ItemResource itemResource) || amount <= 0) {
            return 0;
        }
        final ItemInfo info = toPersistentInfo(itemResource);
        if (info == null) {
            return 0;
        }
        final long value = IEMCProxy.INSTANCE.getValue(info);
        if (value <= 0) {
            return 0;
        }
        final Optional<IKnowledgeProvider> providerOptional = getMutableProvider();
        if (providerOptional.isEmpty()) {
            return 0;
        }
        final IKnowledgeProvider provider = providerOptional.get();
        if (!provider.hasFullKnowledge() && !provider.hasKnowledge(info)) {
            return 0;
        }
        final BigInteger available = provider.getEmc();
        if (available.signum() <= 0) {
            return 0;
        }
        final BigInteger costPerUnit = BigInteger.valueOf(value);
        BigInteger cost = costPerUnit.multiply(BigInteger.valueOf(amount));
        BigInteger resultAmount = BigInteger.valueOf(amount);
        if (available.compareTo(cost) < 0) {
            resultAmount = available.divide(costPerUnit);
            if (resultAmount.signum() <= 0) {
                return 0;
            }
            cost = costPerUnit.multiply(resultAmount);
        }
        if (action == Action.EXECUTE) {
            provider.setEmc(available.subtract(cost));
            owner.onProviderChanged();
        }
        return resultAmount.longValue();
    }

    @Override
    public Collection<ResourceAmount> getAll() {
        return cache.copyState();
    }

    @Override
    public long getStored() {
        final Optional<IKnowledgeProvider> providerOptional = owner.getKnowledgeProvider();
        if (providerOptional.isEmpty()) {
            return 0;
        }
        return providerOptional.get().getEmc().min(LONG_MAX).longValue();
    }

    @Override
    public void onAddedIntoComposite(final ParentComposite parentComposite) {
        parents.add(parentComposite);
    }

    @Override
    public void onRemovedFromComposite(final ParentComposite parentComposite) {
        parents.remove(parentComposite);
    }

    @Override
    public Amount compositeInsert(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        final long inserted = insert(resource, amount, action, actor);
        return new Amount(inserted, 0);
    }

    @Override
    public Amount compositeExtract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        final long extracted = extract(resource, amount, action, actor);
        return new Amount(extracted, 0);
    }

    public void detectChanges() {
        if (owner.getLevel() == null || owner.getLevel().isClientSide()) {
            return;
        }
        final MutableResourceList desired = computeDesiredState();
        removeMissing(desired);
        applyAdditionsAndDifferences(desired);
    }

    private MutableResourceList computeDesiredState() {
        final MutableResourceList desired = MutableResourceListImpl.create();
        final Optional<IKnowledgeProvider> providerOptional = owner.getKnowledgeProvider();
        if (providerOptional.isEmpty()) {
            return desired;
        }
        final IKnowledgeProvider provider = providerOptional.get();
        final BigInteger totalEmc = provider.getEmc();
        if (totalEmc.signum() <= 0) {
            return desired;
        }
        for (final ItemInfo info : provider.getKnowledge()) {
            final long value = IEMCProxy.INSTANCE.getValue(info);
            if (value <= 0) {
                continue;
            }
            final BigInteger max = totalEmc.divide(BigInteger.valueOf(value));
            if (max.signum() <= 0) {
                continue;
            }
            ItemResource resource = itemResourceCache.get(info);
            if (resource == null) {
                resource = ItemResource.ofItemStack(info.createStack());
                itemResourceCache.put(info, resource);
            }
            desired.add(resource, max.min(LONG_MAX).longValue());
        }
        return desired;
    }

    private void removeMissing(final MutableResourceList desired) {
        final Set<ResourceKey> gone = new HashSet<>();
        for (final ResourceKey key : cache.getAll()) {
            if (!desired.contains(key)) {
                gone.add(key);
            }
        }
        for (final ResourceKey key : gone) {
            removeFromCache(key, cache.get(key));
        }
    }

    private void applyAdditionsAndDifferences(final MutableResourceList desired) {
        for (final ResourceKey key : desired.getAll()) {
            final long want = desired.get(key);
            final long have = cache.get(key);
            if (have == 0) {
                addToCache(key, want);
            } else if (want > have) {
                addToCache(key, want - have);
            } else if (want < have) {
                removeFromCache(key, have - want);
            }
        }
    }

    private void addToCache(final ResourceKey resource, final long amount) {
        cache.add(resource, amount);
        parents.forEach(parent -> parent.addToCache(resource, amount));
    }

    private void removeFromCache(final ResourceKey resource, final long amount) {
        cache.remove(resource, amount);
        parents.forEach(parent -> parent.removeFromCache(resource, amount));
    }

    private Optional<IKnowledgeProvider> getMutableProvider() {
        if (!isOwnerOnline()) {
            return Optional.empty();
        }
        return owner.getKnowledgeProvider();
    }

    private boolean isOwnerOnline() {
        final UUID ownerId = owner.getOwnerId();
        if (ownerId == null || owner.getLevel() == null) {
            return false;
        }
        final MinecraftServer server = owner.getLevel().getServer();
        if (server == null) {
            return false;
        }
        return server.getPlayerList().getPlayer(ownerId) != null;
    }

    private ItemInfo toPersistentInfo(final ItemResource resource) {
        final ItemInfo info = ItemInfo.fromItem(
            BuiltInRegistries.ITEM.wrapAsHolder(resource.item()),
            resource.components()
        );
        return IEMCProxy.INSTANCE.getPersistentInfo(info);
    }
}
