package ru.ziftech.ccrulang.client;

import net.minecraft.resources.ResourceLocation;
import ru.ziftech.ccrulang.CCRuLang;

import java.util.Arrays;

/**
 * Maps the ~66 terminal byte values this addon treats as Cyrillic (see {@code CyrillicCodec} for the
 * Unicode-to-byte half of this mapping) onto their position in {@code russian_font.png} - a separate,
 * independently-editable 256x256 texture laid out as a flat A-to-я strip (16 glyphs per row, same 8x11
 * pixel cell pitch / 6x9 glyph convention as CC: Tweaked's own {@code term_font.png}), rather than
 * baked into term_font.png itself.
 */
public final class CyrillicFont {
    public static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(CCRuLang.MOD_ID, "textures/gui/russian_font.png");

    /** Native resolution of russian_font.png - unlike term_font.png, it isn't shipped at a higher scale. */
    public static final float TEXTURE_SIZE = 256.0f;

    private static final int YO_UPPER_BYTE = 0xA8;
    private static final int YO_LOWER_BYTE = 0xB8;
    private static final int YO_UPPER_STRIP = 6;  // 7th glyph in the strip: А Б В Г Д Е [Ё]
    private static final int YO_LOWER_STRIP = 39; // ...а б в г д е [ё]

    private static final int[] STRIP_INDEX = new int[256];

    static {
        Arrays.fill(STRIP_INDEX, -1);
        STRIP_INDEX[YO_UPPER_BYTE] = YO_UPPER_STRIP;
        STRIP_INDEX[YO_LOWER_BYTE] = YO_LOWER_STRIP;

        // Uppercase А-Я (0xC0-0xDF, 32 bytes) -> strip positions 0-32 skipping the Ё slot.
        var strip = 0;
        for (var b = 0xC0; b <= 0xDF; b++) {
            if (strip == YO_UPPER_STRIP) strip++;
            STRIP_INDEX[b] = strip++;
        }

        // Lowercase а-я (0xE0-0xFF, 32 bytes) -> strip positions 33-65 skipping the ё slot.
        strip = 33;
        for (var b = 0xE0; b <= 0xFF; b++) {
            if (strip == YO_LOWER_STRIP) strip++;
            STRIP_INDEX[b] = strip++;
        }
    }

    private CyrillicFont() {
    }

    public static boolean isRedirected(int terminalByte) {
        return terminalByte >= 0 && terminalByte < 256 && STRIP_INDEX[terminalByte] != -1;
    }

    /**
     * @param terminalByte a byte for which {@link #isRedirected} is true
     * @return its position (0-65) in the russian_font.png glyph strip - use the same {@code % 16} / {@code / 16}
     *         column/row math as {@code term_font.png} to turn this into a UV rect
     */
    public static int stripIndex(int terminalByte) {
        return STRIP_INDEX[terminalByte];
    }
}
