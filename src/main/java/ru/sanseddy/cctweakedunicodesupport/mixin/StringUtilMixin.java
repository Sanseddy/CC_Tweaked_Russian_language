package ru.sanseddy.cctweakedunicodesupport.mixin;

import dan200.computercraft.core.util.StringUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.sanseddy.cctweakedunicodesupport.text.CraftOsCharset;
import ru.sanseddy.cctweakedunicodesupport.text.Utf8;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

@Mixin(StringUtil.class)
public class StringUtilMixin {
    @Inject(method = "getClipboardString", at = @At("HEAD"), cancellable = true)
    private static void cc_tweaked_unicode_support$unicodeClipboard(String clipboard, CallbackInfoReturnable<ByteBuffer> cir) {
        var out = new ByteArrayOutputStream(Math.min(StringUtil.MAX_PASTE_LENGTH, clipboard.length()));
        var codepoints = clipboard.codePoints().iterator();

        while (codepoints.hasNext()) {
            var codepoint = codepoints.next();

            if (codepoint >= Character.MIN_SURROGATE && codepoint <= Character.MAX_SURROGATE) continue;

            if (CraftOsCharset.isInternalMarker(codepoint)) continue;
            var legacy = CraftOsCharset.toLegacyByte(codepoint);

            if (legacy >= 0 && !StringUtil.isTypableChar(legacy)) break;

            var length = legacy >= 0 ? 1 : Utf8.encodedLength(codepoint);
            if (out.size() + length > StringUtil.MAX_PASTE_LENGTH) break;

            Utf8.encodePreferLegacy(codepoint, out);
        }

        cir.setReturnValue(ByteBuffer.wrap(out.toByteArray()).asReadOnlyBuffer());
    }
}
