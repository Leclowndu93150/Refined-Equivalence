package com.leclowndu93150.refined_equivalence.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record EmcLinkData() {
    public static final StreamCodec<RegistryFriendlyByteBuf, EmcLinkData> STREAM_CODEC = 
        StreamCodec.unit(new EmcLinkData());
}
