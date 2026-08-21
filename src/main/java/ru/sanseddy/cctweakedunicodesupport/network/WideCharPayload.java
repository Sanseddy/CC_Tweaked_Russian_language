package ru.sanseddy.cctweakedunicodesupport.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import ru.sanseddy.cctweakedunicodesupport.CCTweakedUnicodeSupport;
import ru.sanseddy.cctweakedunicodesupport.text.CraftOsCharset;

public record WideCharPayload(int containerId, int codepoint) implements CustomPacketPayload {
    public static final Type<WideCharPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CCTweakedUnicodeSupport.MOD_ID, "wide_char"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WideCharPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, WideCharPayload::containerId,
        ByteBufCodecs.VAR_INT, WideCharPayload::codepoint,
        WideCharPayload::new
    );

    public boolean isValid() {
        return codepoint > 0 && codepoint <= Character.MAX_CODE_POINT
            && (codepoint < Character.MIN_SURROGATE || codepoint > Character.MAX_SURROGATE)
            && !CraftOsCharset.isInternalMarker(codepoint);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
