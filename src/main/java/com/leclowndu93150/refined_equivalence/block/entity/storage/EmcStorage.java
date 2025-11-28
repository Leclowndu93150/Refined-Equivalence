package com.leclowndu93150.refined_equivalence.block.entity.storage;

import com.leclowndu93150.refined_equivalence.block.entity.EmcLinkBlockEntity;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeAwareChild;
import com.refinedmods.refinedstorage.api.storage.composite.ParentComposite;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.core.registries.BuiltInRegistries;

public final class EmcStorage implements Storage, CompositeAwareChild {
    private final EmcLinkBlockEntity owner;
    private ParentComposite parentComposite;
    private final Map<ResourceKey, Long> cachedAmounts = new HashMap<>();

    // Cached resource data - invalidated when knowledge/EMC changes
    private List<ResourceAmount> cachedResourceAmounts;
    private Map<ItemInfo, ItemResource> itemResourceCache = new HashMap<>();
    private BigInteger lastKnownEmc = BigInteger.ZERO;
    private int lastKnowledgeSize = 0;
    private boolean cacheValid = false;

    public EmcStorage(final EmcLinkBlockEntity owner) {
        this.owner = owner;
    }

    public void invalidateCache() {
        cacheValid = false;
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
        final Optional<IKnowledgeProvider> providerOptional = owner.getKnowledgeProvider();
        if (providerOptional.isEmpty()) {
            return 0;
        }
        if (action == Action.EXECUTE) {
            final IKnowledgeProvider provider = providerOptional.get();
            provider.addKnowledge(info);
            final BigInteger delta = BigInteger.valueOf(sellValue).multiply(BigInteger.valueOf(amount));
            provider.setEmc(provider.getEmc().add(delta));
            cacheValid = false; // Invalidate cache on EMC change
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
        final Optional<IKnowledgeProvider> providerOptional = owner.getKnowledgeProvider();
        if (providerOptional.isEmpty()) {
            return 0;
        }
        final IKnowledgeProvider provider = providerOptional.get();
        if (!provider.hasFullKnowledge() && !provider.hasKnowledge(info)) {
            return 0;
        }
        BigInteger available = provider.getEmc();
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
            cacheValid = false; // Invalidate cache on EMC change
            owner.onProviderChanged();
        }
        return resultAmount.longValue();
    }

    @Override
    public Collection<ResourceAmount> getAll() {
        return buildResourceAmounts();
    }

    @Override
    public long getStored() {
        final Optional<IKnowledgeProvider> providerOptional = owner.getKnowledgeProvider();
        if (providerOptional.isEmpty()) {
            return 0;
        }
        final IKnowledgeProvider provider = providerOptional.get();
        final BigInteger totalEmc = provider.getEmc();
        return totalEmc.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }

    @Override
    public void onAddedIntoComposite(final ParentComposite parentComposite) {
        this.parentComposite = parentComposite;
        cachedAmounts.clear();
        for (ResourceAmount amount : getAll()) {
            cachedAmounts.put(amount.resource(), amount.amount());
        }
    }

    @Override
    public void onRemovedFromComposite(final ParentComposite parentComposite) {
        this.parentComposite = null;
        cachedAmounts.clear();
    }

    @Override
    public Amount compositeInsert(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        final long inserted = insert(resource, amount, action, actor);
        if (inserted == 0) {
            return Amount.ZERO;
        }
        return new Amount(inserted, 0);
    }

    @Override
    public Amount compositeExtract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        final long extracted = extract(resource, amount, action, actor);
        if (extracted == 0) {
            return Amount.ZERO;
        }
        return new Amount(extracted, 0);
    }

    // Reusable map for refreshCache to avoid allocations
    private final Map<ResourceKey, Long> latestAmounts = new HashMap<>();

    public void refreshCache() {
        if (parentComposite == null || owner.getLevel() == null || owner.getLevel().isClientSide()) {
            return;
        }

        // Reuse the map instead of creating a new one each time
        latestAmounts.clear();
        for (ResourceAmount amount : getAll()) {
            latestAmounts.put(amount.resource(), amount.amount());
        }

        // Process additions and changes
        for (Map.Entry<ResourceKey, Long> entry : latestAmounts.entrySet()) {
            final ResourceKey key = entry.getKey();
            final long current = entry.getValue();
            final long previous = cachedAmounts.getOrDefault(key, 0L);
            final long delta = current - previous;
            if (delta > 0) {
                parentComposite.addToCache(key, delta);
            } else if (delta < 0) {
                parentComposite.removeFromCache(key, -delta);
            }
        }

        // Process removals - iterate over cached keys and check if they're gone
        final var iterator = cachedAmounts.entrySet().iterator();
        while (iterator.hasNext()) {
            final var entry = iterator.next();
            if (!latestAmounts.containsKey(entry.getKey())) {
                final long previous = entry.getValue();
                if (previous > 0) {
                    parentComposite.removeFromCache(entry.getKey(), previous);
                }
            }
        }

        cachedAmounts.clear();
        cachedAmounts.putAll(latestAmounts);
    }

    private ItemInfo toPersistentInfo(final ItemResource resource) {
        final ItemInfo info = ItemInfo.fromItem(
            BuiltInRegistries.ITEM.wrapAsHolder(resource.item()),
            resource.components()
        );
        return IEMCProxy.INSTANCE.getPersistentInfo(info);
    }

    private List<ResourceAmount> buildResourceAmounts() {
        final Optional<IKnowledgeProvider> providerOptional = owner.getKnowledgeProvider();
        if (providerOptional.isEmpty()) {
            cachedResourceAmounts = null;
            cacheValid = false;
            return List.of();
        }
        final IKnowledgeProvider provider = providerOptional.get();
        final BigInteger totalEmc = provider.getEmc();
        if (totalEmc.signum() <= 0) {
            cachedResourceAmounts = null;
            cacheValid = false;
            return List.of();
        }

        final Set<ItemInfo> knowledge = provider.getKnowledge();
        final int currentKnowledgeSize = knowledge.size();

        // Check if cache is still valid
        if (cacheValid && cachedResourceAmounts != null
                && totalEmc.equals(lastKnownEmc)
                && currentKnowledgeSize == lastKnowledgeSize) {
            return cachedResourceAmounts;
        }

        // Rebuild cache
        final List<ResourceAmount> amounts = new ArrayList<>(currentKnowledgeSize);
        for (ItemInfo info : knowledge) {
            final long value = IEMCProxy.INSTANCE.getValue(info);
            if (value <= 0) {
                continue;
            }
            final BigInteger max = totalEmc.divide(BigInteger.valueOf(value));
            if (max.signum() <= 0) {
                continue;
            }
            // Cache ItemResource to avoid repeated createStack() calls
            ItemResource resource = itemResourceCache.get(info);
            if (resource == null) {
                resource = ItemResource.ofItemStack(info.createStack());
                itemResourceCache.put(info, resource);
            }
            amounts.add(new ResourceAmount(
                resource,
                max.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue()
            ));
        }

        // Update cache state
        cachedResourceAmounts = amounts;
        lastKnownEmc = totalEmc;
        lastKnowledgeSize = currentKnowledgeSize;
        cacheValid = true;

        return amounts;
    }
}
