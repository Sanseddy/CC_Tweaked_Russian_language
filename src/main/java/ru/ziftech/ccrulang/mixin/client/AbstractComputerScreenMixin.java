package ru.ziftech.ccrulang.mixin.client;

import dan200.computercraft.client.gui.AbstractComputerScreen;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.ziftech.ccrulang.client.CyrillicRenderState;

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
