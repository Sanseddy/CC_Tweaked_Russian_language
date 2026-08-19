package ru.ziftech.ccrulang;

public final class CyrillicCodec {
    private static final int CYRILLIC_BLOCK_START = 0x0410;
    private static final int CYRILLIC_BLOCK_END = 0x044F;
    private static final int CP1251_BLOCK_START = 0xC0;

    private static final int YO_UPPER = 0x0401;
    private static final int YO_LOWER = 0x0451;
    private static final int YO_UPPER_CP1251 = 0xA8;
    private static final int YO_LOWER_CP1251 = 0xB8;

    private CyrillicCodec() {
    }

    public static int toTerminalByte(int codepoint) {
        if (codepoint == YO_UPPER) return YO_UPPER_CP1251;
        if (codepoint == YO_LOWER) return YO_LOWER_CP1251;
        if (codepoint >= CYRILLIC_BLOCK_START && codepoint <= CYRILLIC_BLOCK_END) {
            return codepoint - CYRILLIC_BLOCK_START + CP1251_BLOCK_START;
        }
        return -1;
    }

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
