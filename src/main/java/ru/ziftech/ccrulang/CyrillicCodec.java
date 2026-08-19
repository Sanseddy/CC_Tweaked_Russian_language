package ru.ziftech.ccrulang;

/**
 * Maps Cyrillic Unicode codepoints to the single-byte terminal codes CC: Tweaked's terminal buffer
 * stores, using the byte values the Windows-1251 (CP1251) codepage assigns to those letters:
 * 0xC0-0xDF = А-Я, 0xE0-0xFF = а-я, 0xA8 = Ё, 0xB8 = ё. {@link ru.ziftech.ccrulang.mixin.StringUtilMixin}
 * uses this for typed/pasted input; {@link ru.ziftech.ccrulang.client.CyrillicFont} uses the same byte
 * values to redirect those cells to russian_font.png at render time.
 *
 * <p>CC: Tweaked terminals and Lua strings are single-byte (0-255), so this only covers the
 * Russian alphabet itself - not the wider CP1251 punctuation/extension-letter set, to avoid
 * clobbering the vanilla Latin-1 glyphs CC: Tweaked's own term_font.png draws at those other byte
 * positions (which stays completely untouched by this addon).
 */
public final class CyrillicCodec {
    private static final int CYRILLIC_BLOCK_START = 0x0410; // А
    private static final int CYRILLIC_BLOCK_END = 0x044F;   // я (inclusive)
    private static final int CP1251_BLOCK_START = 0xC0;

    private static final int YO_UPPER = 0x0401; // Ё
    private static final int YO_LOWER = 0x0451; // ё
    private static final int YO_UPPER_CP1251 = 0xA8;
    private static final int YO_LOWER_CP1251 = 0xB8;

    private CyrillicCodec() {
    }

    /**
     * @param codepoint a Unicode codepoint (e.g. from a keyboard char event or clipboard text)
     * @return the CP1251 terminal byte for {@code codepoint}, or -1 if it isn't one of the
     *         Cyrillic letters this addon maps.
     */
    public static int toTerminalByte(int codepoint) {
        if (codepoint == YO_UPPER) return YO_UPPER_CP1251;
        if (codepoint == YO_LOWER) return YO_LOWER_CP1251;
        if (codepoint >= CYRILLIC_BLOCK_START && codepoint <= CYRILLIC_BLOCK_END) {
            return codepoint - CYRILLIC_BLOCK_START + CP1251_BLOCK_START;
        }
        return -1;
    }

    /**
     * Rewrites the UTF-8 byte sequence for each of the ~66 Cyrillic letters {@link #toTerminalByte} maps
     * into its single CP1251 terminal byte, leaving every other byte untouched. Used by
     * {@link ru.ziftech.ccrulang.mixin.AbstractHandleMixin} so {@code .lua} source (and any other text
     * file) can be saved as plain UTF-8 - the default nearly every editor uses - instead of requiring the
     * single-byte encoding CC: Tweaked's terminal and Lua strings actually store.
     *
     * <p>All 66 mapped codepoints (U+0401, U+0410-U+044F, U+0451) fall in the U+0400-U+047F range, which
     * UTF-8 always encodes as exactly 2 bytes with lead byte 0xD0 or 0xD1 - so this only ever needs to
     * look at byte pairs, never track state across a longer sequence. Anything outside that exact pattern
     * (plain ASCII, other non-Cyrillic UTF-8 text, already-CP1251 raw bytes, binary data) round-trips
     * byte-for-byte, which is what makes this safe to run unconditionally over arbitrary file content
     * rather than only on files verified to be UTF-8.
     *
     * @return {@code input} itself (no copy) if nothing matched, otherwise a new array with matches
     *         replaced
     */
    public static byte[] transcodeUtf8ToTerminal(byte[] input) {
        var out = new byte[input.length];
        var outLen = 0;
        var changed = false;
        var i = 0;
        while (i < input.length) {
            var lead = input[i] & 0xFF;
            if ((lead == 0xD0 || lead == 0xD1) && i + 1 < input.length) {
                var continuation = input[i + 1] & 0xFF;
                if ((continuation & 0xC0) == 0x80) {
                    var codepoint = (lead & 0x1F) << 6 | continuation & 0x3F;
                    var terminalByte = toTerminalByte(codepoint);
                    if (terminalByte != -1) {
                        out[outLen++] = (byte) terminalByte;
                        i += 2;
                        changed = true;
                        continue;
                    }
                }
            }
            out[outLen++] = input[i];
            i++;
        }
        return changed ? java.util.Arrays.copyOf(out, outLen) : input;
    }
}
