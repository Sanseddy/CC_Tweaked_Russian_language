package ru.ziftech.ccrulang.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Render pipeline for russian_font.png, plus the side-channel {@link FixedWidthFontRendererMixin} uses to
 * reach it.
 *
 * <p>{@code FixedWidthFontRenderer}'s internals ({@code drawChar}, {@code drawString}, ...) only ever see the
 * one {@code QuadEmitter} their caller already bound to {@code term_font.png} - there's no
 * {@code MultiBufferSource} threaded through to ask for a second texture from deeper in the call stack. The
 * call-site mixin ({@link AbstractComputerScreenMixin}) brackets the *whole screen's* render with {@link
 * #begin} / {@link #flush}; {@link FixedWidthFontRendererMixin} redirects the ~66 Cyrillic byte values into
 * {@link #record} instead of drawing them from term_font.png.
 *
 * <p><b>Why this is a record-and-replay, not a direct draw:</b> {@code GuiGraphics}'s buffer source keeps
 * only one shared {@code BufferBuilder} for any {@code RenderType.text(...)} - term_font.png and
 * russian_font.png both go through it, since only block/entity render types get their own dedicated buffer
 * (see {@code RenderBuffers}). {@code TerminalWidget.renderWidget} fetches the term_font.png
 * {@code VertexConsumer} once and threads that same object through the entire background+foreground+cursor
 * draw, so asking {@code graphics.bufferSource()} for a *different* {@code RenderType.text(...)} at any
 * point before that whole draw finishes forces it to end (build and draw) whichever builder was active -
 * including term_font.png's, out from under the vanilla code still using it. So every Cyrillic glyph is
 * recorded (not drawn) while term_font.png's draw is in flight, and only actually emitted once
 * {@code AbstractComputerScreen.render} - not just the {@code TerminalWidget} - has fully returned, so this
 * ends up asking for term_font.png's builder at essentially the same point the screen's own rendering would
 * naturally have ended it anyway.
 *
 * <p>A private, self-owned {@code MultiBufferSource} was tried here at one point instead, specifically to
 * avoid ending term_font.png's batch at all - it turned out not to render the Cyrillic quads visibly (most
 * likely a Z/depth or GL-state mismatch from drawing through a completely separate immediate buffer outside
 * {@code GuiGraphics}'s own bookkeeping), so this went back to sharing {@code graphics.bufferSource()},
 * which is the version that's actually been confirmed to render Cyrillic correctly in-game.
 *
 * <p>What actually caused the terminal background to render semi-transparent turned out to be unrelated to
 * any of this: the addon's bundled {@code term_font.png} had silently drifted from CC: Tweaked's own copy
 * (1024x1024 with alpha 0 at the pixel {@code drawQuad}'s solid-fill swatch samples, instead of the upstream
 * 256x256 copy's opaque white there) - background quads for every terminal, Cyrillic or not, were sampling a
 * fully transparent pixel. Fixed by replacing the bundled file with byte-identical upstream, as the code
 * comments here already claimed it was.
 *
 * <p>Only {@code TerminalWidget} (computer/pocket-computer/turtle screens, via {@code AbstractComputerScreen})
 * is covered - printed pages ({@code PrintoutRenderer}) and monitors ({@code DirectFixedWidthFontRenderer})
 * still draw Cyrillic bytes as whatever term_font.png has at those positions, i.e. not redirected. Extending
 * to those would mean adding a parallel mixin per call site, following the same pattern as
 * {@link AbstractComputerScreenMixin}.
 *
 * <p>Minecraft's render thread is single-threaded, so plain static fields are sufficient - no ThreadLocal
 * needed.
 */
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

    /** Emits every glyph recorded since {@link #begin}, then resets for the next screen render. */
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

    /** A deliberate copy of {@code FixedWidthFontRenderer}'s private {@code quad}/{@code drawChar} logic -
     * those are private methods on a class this mod doesn't own, so duplicating six lines of vertex
     * emission is simpler and less fragile than shadowing them. */
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
