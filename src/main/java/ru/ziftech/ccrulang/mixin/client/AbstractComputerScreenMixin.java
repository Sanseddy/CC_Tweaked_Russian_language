package ru.ziftech.ccrulang.mixin.client;

import dan200.computercraft.client.gui.AbstractComputerScreen;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.ziftech.ccrulang.client.CyrillicRenderState;

/**
 * Brackets {@code AbstractComputerScreen.render} - the whole-screen render method computer/pocket-computer/
 * turtle terminal screens share, wrapping {@code super.render} (which draws the {@code TerminalWidget}) and
 * the tooltip pass - with {@link CyrillicRenderState#begin} / {@link CyrillicRenderState#flush}, so
 * {@link FixedWidthFontRendererMixin} has somewhere to record Cyrillic glyphs into and so they get drawn
 * once this screen has fully finished rendering (after every widget *and* the tooltip pass), rather than
 * appearing mid-draw while term_font.png's own pass for this screen is still in flight. See
 * {@link CyrillicRenderState} for the full reasoning, including why bracketing the whole screen (not just
 * {@code TerminalWidget.renderWidget}) matters here.
 */
@Mixin(AbstractComputerScreen.class)
public class AbstractComputerScreenMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void ccrulang$beginCyrillicPass(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        CyrillicRenderState.begin(graphics.bufferSource(), graphics.pose());
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void ccrulang$flushCyrillicPass(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        CyrillicRenderState.flush();
    }
}
