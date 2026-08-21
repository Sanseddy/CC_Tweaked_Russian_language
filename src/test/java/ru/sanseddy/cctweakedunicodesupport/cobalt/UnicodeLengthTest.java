package ru.sanseddy.cctweakedunicodesupport.cobalt;

import org.junit.jupiter.api.Test;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.UnicodeLength;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnicodeLengthTest {
    @Test
    void countsUnicodeScalars() {
        assertEquals(8, UnicodeLength.length(utf8("\u041A\u041D\u041E\u041F\u041A\u0410 1")));
        assertEquals(3, UnicodeLength.length(utf8("A\u042F\uD83D\uDE00")));
    }

    @Test
    void preservesByteLengthForMalformedUtf8() {
        assertEquals(3, UnicodeLength.length(LuaString.valueOf(new byte[]{(byte) 0xE2, 0x28, (byte) 0xA1})));
        assertEquals(2, UnicodeLength.length(LuaString.valueOf(new byte[]{(byte) 0xC0, (byte) 0xAF})));
    }

    private static LuaString utf8(String value) {
        return LuaString.valueOf(value.getBytes(StandardCharsets.UTF_8));
    }
}
