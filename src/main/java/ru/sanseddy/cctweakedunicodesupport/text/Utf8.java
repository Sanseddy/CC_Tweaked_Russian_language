package ru.sanseddy.cctweakedunicodesupport.text;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public final class Utf8 {

    private static final char REPLACEMENT = '?';

    private Utf8() {
    }

    public record Cells(char[] cells, int[] leadOffset, int length) {
        public String text() {
            return new String(cells, 0, length);
        }
    }

    public static String decode(String raw) {
        var cells = new char[raw.length()];
        var length = 0;
        var changed = false;
        var i = 0;
        while (i < raw.length()) {
            var consumed = sequenceLength(raw, i);
            var codepoint = codepoint(raw, i, consumed);

            if (isAstralText(codepoint)) {
                var pair = Character.toChars(codepoint);
                cells[length++] = pair[0];
                cells[length++] = pair[1];
                changed = true;
                i += consumed;
                continue;
            }

            var cell = cell(codepoint, raw.charAt(i) & 0xFF, consumed);

            if (consumed != 1 || cell != raw.charAt(i)) changed = true;
            cells[length++] = cell;
            i += consumed;
        }
        return changed ? new String(cells, 0, length) : raw;
    }

    public static String decodeWide(String raw) {
        var cells = new char[raw.length()];
        var i = 0;
        while (i < raw.length()) {
            var consumed = sequenceLength(raw, i);
            var codepoint = codepoint(raw, i, consumed);
            var occupied = 1;
            if (isAstralText(codepoint)) {
                var pair = Character.toChars(codepoint);
                cells[i] = pair[0];
                cells[i + 1] = pair[1];
                occupied = 2;
            } else {
                cells[i] = cell(codepoint, raw.charAt(i) & 0xFF, consumed);
            }
            for (var k = occupied; k < consumed; k++) cells[i + k] = CraftOsCharset.CONTINUATION;
            i += consumed;
        }
        return new String(cells);
    }

    public static Cells decode(byte[] bytes, int offset, int length) {
        var cells = new char[length];
        var leadOffset = new int[length];
        var count = 0;
        var i = 0;
        while (i < length) {
            var consumed = sequenceLength(bytes, offset + i, offset + length);
            var codepoint = codepoint(bytes, offset + i, consumed);
            leadOffset[count] = i;
            if (isAstralText(codepoint)) {
                var pair = Character.toChars(codepoint);
                cells[count++] = pair[0];
                leadOffset[count] = i;
                cells[count++] = pair[1];
            } else {
                cells[count++] = cell(codepoint, bytes[offset + i] & 0xFF, consumed);
            }
            i += consumed;
        }
        return new Cells(cells, leadOffset, count);
    }

    public static String asByteString(ByteBuffer buffer) {
        var out = new StringBuilder(buffer.remaining());
        for (var i = buffer.position(); i < buffer.limit(); i++) out.append((char) (buffer.get(i) & 0xFF));
        return out.toString();
    }

    public static Cells decode(ByteBuffer buffer) {
        var bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return decode(bytes, 0, bytes.length);
    }

    public static int characterCount(ByteBuffer buffer) {
        var bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);

        var count = 0;
        for (var i = 0; i < bytes.length; count++) i += sequenceLength(bytes, i, bytes.length);
        return count;
    }

    private static char cell(int codepoint, int lead, int consumed) {
        if (consumed == 1) return CraftOsCharset.toCell(lead);
        if (CraftOsCharset.isInternalMarker(codepoint)) return '\uFFFD';
        if (codepoint <= 0xFFFF) return (char) codepoint;

        return REPLACEMENT;
    }

    private static boolean isAstralText(int codepoint) {
        return codepoint > 0xFFFF;
    }

    public static int sequenceLength(byte[] bytes, int index, int limit) {
        var expected = expectedLength(bytes[index] & 0xFF);
        if (expected == 1 || index + expected > limit) return 1;
        for (var i = index + 1; i < index + expected; i++) {
            if ((bytes[i] & 0xC0) != 0x80) return 1;
        }
        return wellFormed(decodeUnchecked(bytes, index, expected), expected) ? expected : 1;
    }

    private static int sequenceLength(String raw, int index) {
        var expected = expectedLength(raw.charAt(index) & 0xFF);
        if (expected == 1 || index + expected > raw.length()) return 1;
        for (var i = index + 1; i < index + expected; i++) {
            if ((raw.charAt(i) & 0xC0) != 0x80) return 1;
        }
        return wellFormed(decodeUnchecked(raw, index, expected), expected) ? expected : 1;
    }

    private static int expectedLength(int lead) {
        if (lead < 0xC2) return 1;  
        if (lead < 0xE0) return 2;
        if (lead < 0xF0) return 3;
        if (lead < 0xF5) return 4;
        return 1;
    }

    private static boolean wellFormed(int codepoint, int consumed) {
        return switch (consumed) {
            case 3 -> codepoint >= 0x800 && (codepoint < 0xD800 || codepoint > 0xDFFF);
            case 4 -> codepoint >= 0x10000 && codepoint <= 0x10FFFF;
            default -> true;  
        };
    }

    private static int codepoint(byte[] bytes, int index, int consumed) {
        return consumed == 1 ? -1 : decodeUnchecked(bytes, index, consumed);
    }

    private static int codepoint(String raw, int index, int consumed) {
        return consumed == 1 ? -1 : decodeUnchecked(raw, index, consumed);
    }

    private static int decodeUnchecked(byte[] bytes, int index, int consumed) {
        var codepoint = bytes[index] & (0x7F >> consumed);
        for (var i = index + 1; i < index + consumed; i++) codepoint = codepoint << 6 | bytes[i] & 0x3F;
        return codepoint;
    }

    private static int decodeUnchecked(String raw, int index, int consumed) {
        var codepoint = raw.charAt(index) & (0x7F >> consumed);
        for (var i = index + 1; i < index + consumed; i++) codepoint = codepoint << 6 | raw.charAt(i) & 0x3F;
        return codepoint;
    }

    public static void encode(int codepoint, ByteArrayOutputStream out) {
        if (codepoint < 0x80) {
            out.write(codepoint);
        } else if (codepoint < 0x800) {
            out.write(0xC0 | codepoint >> 6);
            out.write(0x80 | codepoint & 0x3F);
        } else if (codepoint < 0x10000) {
            out.write(0xE0 | codepoint >> 12);
            out.write(0x80 | codepoint >> 6 & 0x3F);
            out.write(0x80 | codepoint & 0x3F);
        } else {
            out.write(0xF0 | codepoint >> 18);
            out.write(0x80 | codepoint >> 12 & 0x3F);
            out.write(0x80 | codepoint >> 6 & 0x3F);
            out.write(0x80 | codepoint & 0x3F);
        }
    }

    public static byte[] encode(int codepoint) {
        var out = new ByteArrayOutputStream(4);
        encode(codepoint, out);
        return out.toByteArray();
    }

    public static int encodedLength(int codepoint) {
        if (codepoint < 0x80) return 1;
        if (codepoint < 0x800) return 2;
        if (codepoint < 0x10000) return 3;
        return 4;
    }

    public static int readCells(byte[] bytes, int offset, int limit, char[] out, int count) {
        var i = offset;
        for (var cell = 0; cell < count; cell++) {
            if (i >= limit) {
                out[cell] = ' ';
                continue;
            }

            var lead = bytes[i] & 0xFF;
            var consumed = lead < 0x80 ? 1 : lead < 0xE0 ? 2 : 3;
            if (i + consumed > limit) {
                out[cell] = ' ';
                i = limit;
                continue;
            }

            var codepoint = lead;
            if (consumed > 1) {
                codepoint = lead & 0x7F >> consumed;
                for (var b = i + 1; b < i + consumed; b++) codepoint = codepoint << 6 | bytes[b] & 0x3F;
            }
            out[cell] = (char) codepoint;
            i += consumed;
        }
        return i;
    }

    public static void encodePreferLegacy(int codepoint, ByteArrayOutputStream out) {
        if (CraftOsCharset.isInternalMarker(codepoint)) {
            encode(0xFFFD, out);
            return;
        }
        var legacy = CraftOsCharset.toLegacyByte(codepoint);
        if (legacy >= 0) {
            out.write(legacy);
        } else {
            encode(codepoint, out);
        }
    }

    public static byte[] encodePreferLegacy(int codepoint) {
        var out = new ByteArrayOutputStream(4);
        encodePreferLegacy(codepoint, out);
        return out.toByteArray();
    }

    public static byte[] toUnicode(byte[] bytes, int offset, int length) {
        var out = new ByteArrayOutputStream(length + (length >> 2));
        var changed = false;
        var i = 0;
        while (i < length) {
            var consumed = sequenceLength(bytes, offset + i, offset + length);
            if (consumed > 1) {
                out.write(bytes, offset + i, consumed);
            } else {
                var lead = bytes[offset + i] & 0xFF;
                var codepoint = CraftOsCharset.toCodepoint(lead);

                if (codepoint < 0x80) {
                    out.write(lead);
                } else {
                    encode(codepoint, out);
                    changed = true;
                }
            }
            i += consumed;
        }
        return changed ? out.toByteArray() : null;
    }

    public static byte[] toLegacy(byte[] bytes, int offset, int length) {
        var out = new ByteArrayOutputStream(length);
        var changed = false;
        var i = 0;
        while (i < length) {
            var consumed = sequenceLength(bytes, offset + i, offset + length);
            if (consumed == 1) {
                out.write(bytes[offset + i]);
            } else {
                var legacy = CraftOsCharset.toLegacyByte(decodeUnchecked(bytes, offset + i, consumed));
                if (legacy >= 0) {
                    out.write(legacy);
                    changed = true;
                } else {
                    out.write(bytes, offset + i, consumed);
                }
            }
            i += consumed;
        }
        return changed ? out.toByteArray() : null;
    }
}
