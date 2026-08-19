package ru.ziftech.ccrulang.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;

import java.util.ArrayList;
import java.util.List;

public final class CyrillicRenderState {
    public static final RenderType RENDER_TYPE = RenderType.text(CyrillicFont.TEXTURE);

    private record Glyph(float x, float y, int index, int colour, int light) {
    }

    private static MultiBufferSource bufferSource;
    private static PoseStack poseStack;
    private static final List<Glyph> pending = new ArrayList<>();

    private CyrillicRenderState() {
    }

    public static void begin(MultiBufferSource bufferSource, PoseStack poseStack) {
        CyrillicRenderState.bufferSource = bufferSource;
        CyrillicRenderState.poseStack = poseStack;
    }

    public static boolean isActive() {
        return bufferSource != null;
    }

    public static void record(float x, float y, int index, int colour, int light) {
        pending.add(new Glyph(x, y, index, colour, light));
    }

    public static void flush() {
        if (bufferSource != null && !pending.isEmpty()) {
            var emitter = FixedWidthFontRenderer.toVertexConsumer(poseStack, bufferSource.getBuffer(RENDER_TYPE));
            for (var glyph : pending) {
                emit(emitter, glyph);
            }
        }
        pending.clear();
        bufferSource = null;
        poseStack = null;
    }

    private static void emit(FixedWidthFontRenderer.QuadEmitter emitter, Glyph glyph) {
        var strip = CyrillicFont.stripIndex(glyph.index());
        var column = strip % 16;
        var row = strip / 16;
        var xStart = 1 + column * (FixedWidthFontRenderer.FONT_WIDTH + 2);
        var yStart = 1 + row * (FixedWidthFontRenderer.FONT_HEIGHT + 2);
        var u1 = xStart / CyrillicFont.TEXTURE_SIZE;
        var v1 = yStart / CyrillicFont.TEXTURE_SIZE;
        var u2 = (xStart + FixedWidthFontRenderer.FONT_WIDTH) / CyrillicFont.TEXTURE_SIZE;
        var v2 = (yStart + FixedWidthFontRenderer.FONT_HEIGHT) / CyrillicFont.TEXTURE_SIZE;

        var poseMatrix = emitter.poseMatrix();
        var consumer = emitter.consumer();
        var r = FastColor.ARGB32.red(glyph.colour());
        var g = FastColor.ARGB32.green(glyph.colour());
        var b = FastColor.ARGB32.blue(glyph.colour());
        var a = FastColor.ARGB32.alpha(glyph.colour());
        var x = glyph.x();
        var y = glyph.y();
        var x2 = x + FixedWidthFontRenderer.FONT_WIDTH;
        var y2 = y + FixedWidthFontRenderer.FONT_HEIGHT;
        var light = glyph.light();

        consumer.addVertex(poseMatrix, x, y, 0).setColor(r, g, b, a).setUv(u1, v1).setLight(light);
        consumer.addVertex(poseMatrix, x, y2, 0).setColor(r, g, b, a).setUv(u1, v2).setLight(light);
        consumer.addVertex(poseMatrix, x2, y2, 0).setColor(r, g, b, a).setUv(u2, v2).setLight(light);
        consumer.addVertex(poseMatrix, x2, y, 0).setColor(r, g, b, a).setUv(u2, v1).setLight(light);
    }
}
