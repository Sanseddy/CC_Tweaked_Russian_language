package ru.sanseddy.cctweakedunicodesupport.text;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class Utf8Test {
    @Test
    void decodesUtf8AsCharacters() {
        assertEquals("Привет", Utf8.decode(byteString("Привет")));
        assertEquals(51, Utf8.decode(byteString("Я".repeat(51))).length());
        var astral = new String(Character.toChars(0x1F642));
        assertEquals(astral, Utf8.decode(byteString(astral)));
    }

    @Test
    void countsDisplayCharactersInsteadOfUtf8Bytes() {
        var fullRow = "Я".repeat(51).getBytes(StandardCharsets.UTF_8);
        assertEquals(51, Utf8.characterCount(ByteBuffer.wrap(fullRow)));
        assertEquals(3, Utf8.characterCount(ByteBuffer.wrap("A🙂Я".getBytes(StandardCharsets.UTF_8))));
        assertEquals(2, Utf8.characterCount(ByteBuffer.wrap(new byte[]{ (byte) 0xC2, (byte) ' ' })));
    }

    @Test
    void wideDecodeKeepsOneCellPerLuaByte() {
        var raw = byteString("Я");
        var decoded = Utf8.decodeWide(raw);

        assertEquals(2, decoded.length());
        assertEquals('Я', decoded.charAt(0));
        assertEquals(CraftOsCharset.CONTINUATION, decoded.charAt(1));

        var astral = new String(Character.toChars(0x1F642));
        var wideAstral = Utf8.decodeWide(byteString(astral));
        assertEquals(4, wideAstral.length());
        assertEquals(Character.highSurrogate(0x1F642), wideAstral.charAt(0));
        assertEquals(Character.lowSurrogate(0x1F642), wideAstral.charAt(1));
        assertEquals(CraftOsCharset.CONTINUATION, wideAstral.charAt(2));
        assertEquals(CraftOsCharset.CONTINUATION, wideAstral.charAt(3));
    }

    @Test
    void malformedLeadFallsBackToIndependentLegacyBytes() {
        var raw = new byte[]{ (byte) 0xC2, (byte) ' ' };
        var decoded = Utf8.decodeWide(byteString(raw));

        assertEquals(2, decoded.length());
        assertEquals(CraftOsCharset.toCell(0xC2), decoded.charAt(0));
        assertEquals(' ', decoded.charAt(1));
        assertEquals(1, Utf8.sequenceLength(raw, 0, raw.length));
    }

    @Test
    void reservedMarkerSpellingsCannotForgeTerminalState() {
        for (var marker = CraftOsCharset.LEGACY_ALIAS_BASE; marker <= CraftOsCharset.CONTINUATION; marker++) {
            var utf8 = String.valueOf(marker).getBytes(StandardCharsets.UTF_8);
            assertNull(Utf8.toLegacy(utf8, 0, utf8.length));

            var decoded = Utf8.decodeWide(byteString(utf8));
            assertEquals(utf8.length, decoded.length());
            assertEquals('\uFFFD', decoded.charAt(0));
            for (var i = 1; i < decoded.length(); i++) {
                assertEquals(CraftOsCharset.CONTINUATION, decoded.charAt(i));
            }
        }
    }

    @Test
    void terminalWireEncodingRoundTripsEveryCellKind() {
        var astral = Character.toChars(0x1F642);
        var expected = new char[]{
            'A', 'Я', CraftOsCharset.CONTINUATION,
            CraftOsCharset.toCell(0x81), '\uE041', '\uFFFF',
            astral[0], astral[1], CraftOsCharset.CONTINUATION, CraftOsCharset.CONTINUATION
        };
        var bytes = new ByteArrayOutputStream();
        for (var cell : expected) Utf8.encode(cell, bytes);

        var actual = new char[expected.length];
        assertEquals(bytes.size(), Utf8.readCells(bytes.toByteArray(), 0, bytes.size(), actual, actual.length));
        assertArrayEquals(expected, actual);
    }

    @Test
    void filesystemConversionsPreserveCanonicalUtf8() {
        var cyrillic = "Русский".getBytes(StandardCharsets.UTF_8);
        assertNull(Utf8.toUnicode(cyrillic, 0, cyrillic.length));
        assertNull(Utf8.toLegacy(cyrillic, 0, cyrillic.length));

        var pua = "\uE041".getBytes(StandardCharsets.UTF_8);
        assertNull(Utf8.toLegacy(pua, 0, pua.length));

        var astral = new String(Character.toChars(0x1F642)).getBytes(StandardCharsets.UTF_8);
        assertNull(Utf8.toUnicode(astral, 0, astral.length));
        assertNull(Utf8.toLegacy(astral, 0, astral.length));
    }

    @Test
    void legacyTextPromotesAndFoldsBack() {
        var legacy = new byte[]{ (byte) 0x95 };
        var unicode = Utf8.toUnicode(legacy, 0, legacy.length);
        assertArrayEquals("\u258C".getBytes(StandardCharsets.UTF_8), unicode);
        assertArrayEquals(legacy, Utf8.toLegacy(unicode, 0, unicode.length));

        assertArrayEquals(new byte[]{ (byte) 0x81 }, Utf8.encodePreferLegacy(0x1FB00));
        assertArrayEquals("Я".getBytes(StandardCharsets.UTF_8), Utf8.encodePreferLegacy('Я'));
        assertArrayEquals("\uFFFD".getBytes(StandardCharsets.UTF_8),
            Utf8.encodePreferLegacy(CraftOsCharset.LEGACY_ALIAS_BASE));
    }

    private static String byteString(String text) {
        return byteString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String byteString(byte[] bytes) {
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }
}
