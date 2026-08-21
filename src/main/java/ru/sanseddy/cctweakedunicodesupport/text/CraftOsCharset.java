package ru.sanseddy.cctweakedunicodesupport.text;

import dan200.computercraft.core.util.StringUtil;

import java.util.Arrays;

public final class CraftOsCharset {

    public static final int SIZE = 256;

    public static final char LEGACY_ALIAS_BASE = '\uFDD0';

    public static final char LEGACY_ALIAS_END = '\uFDEE';

    public static final char CONTINUATION = '\uFDEF';

    private static final int[] TO_CODEPOINT = new int[SIZE];

    private static final char[] TO_CELL = new char[SIZE];

    private static final int[] ALIAS_TO_BYTE = new int[LEGACY_ALIAS_END - LEGACY_ALIAS_BASE + 1];

    static {
        Arrays.fill(TO_CODEPOINT, -1);

        for (var codepoint = 0; codepoint <= 0xFFFF; codepoint++) recordCodepoint(codepoint);
        for (var codepoint = 0x1FB00; codepoint <= 0x1FBFF; codepoint++) recordCodepoint(codepoint);

        var alias = 0;
        for (var b = 0; b < SIZE; b++) {
            var codepoint = TO_CODEPOINT[b];
            if (codepoint < 0) {
                TO_CELL[b] = (char) b;
            } else if (codepoint <= 0xFFFF) {
                TO_CELL[b] = (char) codepoint;
            } else {
                if (alias >= ALIAS_TO_BYTE.length) throw new IllegalStateException("Too many terminal aliases");
                TO_CELL[b] = (char) (LEGACY_ALIAS_BASE + alias);
                ALIAS_TO_BYTE[alias++] = b;
            }
        }
        if (alias != ALIAS_TO_BYTE.length) throw new IllegalStateException("Unexpected terminal alias count");
    }

    private CraftOsCharset() {
    }

    private static void recordCodepoint(int codepoint) {
        var b = StringUtil.unicodeToTerminal(codepoint);
        if (b >= 0 && b < SIZE && TO_CODEPOINT[b] < 0) TO_CODEPOINT[b] = codepoint;
    }

    public static int toCodepoint(int b) {
        return TO_CODEPOINT[b & 0xFF];
    }

    public static char toCell(int b) {
        return TO_CELL[b & 0xFF];
    }

    public static int toLegacyByte(int codepoint) {
        if (isInternalMarker(codepoint)) return -1;
        if (codepoint >= 0 && codepoint < SIZE && TO_CODEPOINT[codepoint] < 0
            && TO_CELL[codepoint] == codepoint) {
            return codepoint;
        }
        var b = StringUtil.unicodeToTerminal(codepoint);
        return b >= 0 && b < SIZE ? b : -1;
    }

    public static boolean isLegacy(int codepoint) {
        return toLegacyByte(codepoint) >= 0;
    }

    public static boolean isInternalMarker(int codepoint) {
        return codepoint >= LEGACY_ALIAS_BASE && codepoint <= CONTINUATION;
    }

    public static int terminalOnlyGlyph(int codepoint) {
        if (codepoint >= LEGACY_ALIAS_BASE && codepoint <= LEGACY_ALIAS_END) {
            return ALIAS_TO_BYTE[codepoint - LEGACY_ALIAS_BASE];
        }
        if (codepoint == '\t' || codepoint == '\n' || codepoint == '\r') return codepoint;
        if (codepoint >= 0 && codepoint < SIZE && TO_CODEPOINT[codepoint] < 0
            && TO_CELL[codepoint] == codepoint) {
            return codepoint;
        }
        if (codepoint > 0xFFFF) {
            var legacy = toLegacyByte(codepoint);
            if (legacy >= 0 && TO_CODEPOINT[legacy] == codepoint) return legacy;
        }
        return -1;
    }
}
