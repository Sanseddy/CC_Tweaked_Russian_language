package ru.sanseddy.cctweakedunicodesupport.mixin.client;

import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FixedWidthFontRenderer.class)
public interface FixedWidthFontRendererInvoker {
    @Invoker("drawChar")
    static void cc_tweaked_unicode_support$drawChar(
        FixedWidthFontRenderer.QuadEmitter emitter, float x, float y, int index, int colour, int light
    ) {
        throw new AssertionError("mixin did not apply");
    }
}
