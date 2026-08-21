package ru.sanseddy.cctweakedunicodesupport.text;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftOsCharsetTest {
    @Test
    void noncharacterAliasesAreTerminalStateButNeverTextBytes() {
        for (var alias = CraftOsCharset.LEGACY_ALIAS_BASE; alias <= CraftOsCharset.LEGACY_ALIAS_END; alias++) {
            assertEquals(-1, CraftOsCharset.toLegacyByte(alias));
            assertTrue(CraftOsCharset.isInternalMarker(alias));
            var legacy = CraftOsCharset.terminalOnlyGlyph(alias);
            assertTrue(legacy >= 0);
            assertEquals(alias, CraftOsCharset.toCell(legacy));
        }

        assertEquals(0x14, CraftOsCharset.toCell(0x14));
        assertEquals(0x14, CraftOsCharset.terminalOnlyGlyph(0x14));
        assertEquals(0x81, CraftOsCharset.toLegacyByte(0x1FB00));
        assertEquals(0x81, CraftOsCharset.terminalOnlyGlyph(0x1FB00));
        assertEquals(9, CraftOsCharset.terminalOnlyGlyph('\t'));
        assertEquals(10, CraftOsCharset.terminalOnlyGlyph('\n'));
        assertEquals(13, CraftOsCharset.terminalOnlyGlyph('\r'));

        var terminalOnly = 0;
        for (var legacy = 0; legacy < CraftOsCharset.SIZE; legacy++) {
            if (CraftOsCharset.terminalOnlyGlyph(CraftOsCharset.toCell(legacy)) >= 0) terminalOnly++;
        }
        assertEquals(37, terminalOnly);
    }

    @Test
    void sentinelsDoNotCollideWithPrivateUseText() {
        assertEquals('\uFDEF', CraftOsCharset.CONTINUATION);
        assertNotEquals(CraftOsCharset.LEGACY_ALIAS_END, CraftOsCharset.CONTINUATION);
        assertTrue(CraftOsCharset.isInternalMarker(CraftOsCharset.CONTINUATION));
        assertEquals(-1, CraftOsCharset.toLegacyByte(CraftOsCharset.CONTINUATION));
        assertEquals(-1, CraftOsCharset.terminalOnlyGlyph(CraftOsCharset.CONTINUATION));
        assertEquals(-1, CraftOsCharset.toLegacyByte('\uE014'));
        assertEquals(-1, CraftOsCharset.terminalOnlyGlyph('\uE014'));

        var internal = new String(new char[]{
            CraftOsCharset.LEGACY_ALIAS_BASE, CraftOsCharset.LEGACY_ALIAS_END, CraftOsCharset.CONTINUATION
        });
        assertEquals(internal, new String(internal.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
    }

    @Test
    void legacyHalfBlockKeepsItsUnicodeIdentity() {
        assertEquals(0x258C, CraftOsCharset.toCodepoint(0x95));
        assertEquals('\u258C', CraftOsCharset.toCell(0x95));
        assertEquals(0x95, CraftOsCharset.toLegacyByte(0x258C));
    }
}
