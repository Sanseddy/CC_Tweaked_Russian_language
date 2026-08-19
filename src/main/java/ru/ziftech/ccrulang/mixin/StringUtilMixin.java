package ru.ziftech.ccrulang.mixin;

import dan200.computercraft.core.util.StringUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.ziftech.ccrulang.CyrillicCodec;

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
