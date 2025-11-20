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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.core.registries.BuiltInRegistries;

public final class EmcStorage implements Storage, CompositeAwareChild {
    private final EmcLinkBlockEntity owner;
    private ParentComposite parentComposite;
    private final Map<ResourceKey, Long> cachedAmounts = new HashMap<>();

    public EmcStorage(final EmcLinkBlockEntity owner) {
        this.owner = owner;
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

    public void refreshCache() {
        if (parentComposite == null || owner.getLevel() == null || owner.getLevel().isClientSide()) {
            return;
        }
        final Map<ResourceKey, Long> latest = new HashMap<>();
        for (ResourceAmount amount : getAll()) {
            latest.put(amount.resource(), amount.amount());
        }
        for (Map.Entry<ResourceKey, Long> entry : latest.entrySet()) {
            final long previous = cachedAmounts.getOrDefault(entry.getKey(), 0L);
            final long delta = entry.getValue() - previous;
            if (delta > 0) {
                parentComposite.addToCache(entry.getKey(), delta);
            } else if (delta < 0) {
                parentComposite.removeFromCache(entry.getKey(), -delta);
            }
        }
        for (ResourceKey previousKey : new HashSet<>(cachedAmounts.keySet())) {
            if (!latest.containsKey(previousKey)) {
                final long previous = cachedAmounts.get(previousKey);
                if (previous > 0) {
                    parentComposite.removeFromCache(previousKey, previous);
                }
            }
        }
        cachedAmounts.clear();
        cachedAmounts.putAll(latest);
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
            return List.of();
        }
        final IKnowledgeProvider provider = providerOptional.get();
        final BigInteger totalEmc = provider.getEmc();
        if (totalEmc.signum() <= 0) {
            return List.of();
        }
        final List<ResourceAmount> amounts = new ArrayList<>();
        for (ItemInfo info : provider.getKnowledge()) {
            final long value = IEMCProxy.INSTANCE.getValue(info);
            if (value <= 0) {
                continue;
            }
            final BigInteger max = totalEmc.divide(BigInteger.valueOf(value));
            if (max.signum() <= 0) {
                continue;
            }
            final ItemResource resource = ItemResource.ofItemStack(info.createStack());
            amounts.add(new ResourceAmount(
                resource,
                max.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue()
            ));
        }
        return amounts;
    }
}
