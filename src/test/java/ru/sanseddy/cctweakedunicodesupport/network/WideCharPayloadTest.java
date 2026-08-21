package ru.sanseddy.cctweakedunicodesupport.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WideCharPayloadTest {
    @Test
    void validatesUnicodeScalarValues() {
        assertTrue(new WideCharPayload(7, 'Я').isValid());
        assertTrue(new WideCharPayload(7, Character.MAX_CODE_POINT).isValid());

        assertFalse(new WideCharPayload(7, 0).isValid());
        assertFalse(new WideCharPayload(7, Character.MIN_SURROGATE).isValid());
        assertFalse(new WideCharPayload(7, 0xFDD0).isValid());
        assertFalse(new WideCharPayload(7, 0xFDEF).isValid());
        assertFalse(new WideCharPayload(7, Character.MAX_CODE_POINT + 1).isValid());
    }
}
