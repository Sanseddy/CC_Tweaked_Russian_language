package ru.ziftech.ccrulang.mixin.client;

import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.ziftech.ccrulang.client.CyrillicFont;
import ru.ziftech.ccrulang.client.CyrillicRenderState;

@Mixin(FixedWidthFontRenderer.class)
public class FixedWidthFontRendererMixin {
    @Inject(method = "drawChar", at = @At("HEAD"), cancellable = true)
    private static void ccrulang$redirectCyrillic(
        FixedWidthFontRenderer.QuadEmitter emitter, float x, float y, int index, int colour, int light, CallbackInfo ci
    ) {
        if (!CyrillicFont.isRedirected(index)) return;
        if (!CyrillicRenderState.isActive()) return;

        ci.cancel();
        CyrillicRenderState.record(x, y, index, colour, light);
    }
}
