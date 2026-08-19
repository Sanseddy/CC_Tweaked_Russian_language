package ru.ziftech.ccrulang.mixin;

import dan200.computercraft.core.apis.handles.AbstractHandle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.ziftech.ccrulang.CyrillicCodec;

import java.util.Optional;

@Mixin(AbstractHandle.class)
public class AbstractHandleMixin {
    @Shadow
    @Final
    protected boolean binary;

    @Inject(method = "readAll", at = @At("RETURN"), cancellable = true)
    private void ccrulang$transcodeReadAll(CallbackInfoReturnable<Object[]> cir) {
        ccrulang$transcode(cir);
    }

    @Inject(method = "readLine", at = @At("RETURN"), cancellable = true)
    private void ccrulang$transcodeReadLine(Optional<Boolean> withTrailingArg, CallbackInfoReturnable<Object[]> cir) {
        ccrulang$transcode(cir);
    }

    private void ccrulang$transcode(CallbackInfoReturnable<Object[]> cir) {
        if (this.binary) return;

        var result = cir.getReturnValue();
        if (result == null || result.length == 0 || !(result[0] instanceof byte[] bytes)) return;

        var transcoded = CyrillicCodec.transcodeUtf8ToTerminal(bytes);
        if (transcoded != bytes) {
            cir.setReturnValue(new Object[]{transcoded});
        }
    }
}
