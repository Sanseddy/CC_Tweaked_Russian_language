package ru.ziftech.ccrulang.mixin;

import dan200.computercraft.core.util.StringUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.ziftech.ccrulang.CyrillicCodec;

/**
 * {@link StringUtil#unicodeToTerminal(int)} whitelists which typed/pasted Unicode codepoints are
 * allowed to reach the terminal (see its ASCII/Latin-1/teletext-mosaic checks); everything else -
 * Cyrillic included - falls through to a fallback table and is ultimately dropped. This injects a
 * Cyrillic-to-CP1251 mapping before that whitelist runs, so typing or pasting Russian text into an
 * open CC: Tweaked terminal actually produces characters instead of being silently discarded.
 */
@Mixin(StringUtil.class)
public class StringUtilMixin {
    @Inject(method = "unicodeToTerminal", at = @At("HEAD"), cancellable = true)
    private static void ccrulang$mapCyrillic(int chr, CallbackInfoReturnable<Integer> cir) {
        int mapped = CyrillicCodec.toTerminalByte(chr);
        if (mapped != -1) {
            cir.setReturnValue(mapped);
        }
    }
}
