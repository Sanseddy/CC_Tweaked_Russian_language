package ru.ziftech.ccrulang.client;

import net.minecraft.resources.ResourceLocation;
import ru.ziftech.ccrulang.CCRuLang;

import java.util.Arrays;

public final class CyrillicFont {
    public static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(CCRuLang.MOD_ID, "textures/gui/russian_font.png");

    public static final float TEXTURE_SIZE = 256.0f;

    private static final int YO_UPPER_BYTE = 0xA8;
    private static final int YO_LOWER_BYTE = 0xB8;
    private static final int YO_UPPER_STRIP = 6;
    private static final int YO_LOWER_STRIP = 39;

    private static final int[] STRIP_INDEX = new int[256];

    static {
        Arrays.fill(STRIP_INDEX, -1);
        STRIP_INDEX[YO_UPPER_BYTE] = YO_UPPER_STRIP;
        STRIP_INDEX[YO_LOWER_BYTE] = YO_LOWER_STRIP;

        var strip = 0;
        for (var b = 0xC0; b <= 0xDF; b++) {
            if (strip == YO_UPPER_STRIP) strip++;
            STRIP_INDEX[b] = strip++;
        }

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

    public static int stripIndex(int terminalByte) {
        return STRIP_INDEX[terminalByte];
    }
}
