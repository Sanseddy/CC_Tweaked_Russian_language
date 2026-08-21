package org.squiddev.cobalt;

public final class UnicodeLength {
    private UnicodeLength() {
    }

    public static int length(LuaString value) {
        var byteLength = value.length();
        var characters = 0;

        for (var index = 0; index < byteLength; characters++) {
            var first = value.byteAt(index) & 0xFF;
            if (first < 0x80) {
                index++;
                continue;
            }

            final int sequenceLength;
            if (first >= 0xC2 && first <= 0xDF) {
                sequenceLength = 2;
            } else if (first >= 0xE0 && first <= 0xEF) {
                sequenceLength = 3;
            } else if (first >= 0xF0 && first <= 0xF4) {
                sequenceLength = 4;
            } else {
                return byteLength;
            }

            if (index + sequenceLength > byteLength) return byteLength;

            var second = value.byteAt(index + 1) & 0xFF;
            if (second < 0x80 || second > 0xBF) return byteLength;
            if (first == 0xE0 && second < 0xA0) return byteLength; 
            if (first == 0xED && second > 0x9F) return byteLength; 
            if (first == 0xF0 && second < 0x90) return byteLength; 
            if (first == 0xF4 && second > 0x8F) return byteLength; 

            for (var offset = 2; offset < sequenceLength; offset++) {
                var continuation = value.byteAt(index + offset) & 0xFF;
                if (continuation < 0x80 || continuation > 0xBF) return byteLength;
            }

            index += sequenceLength;
        }

        return characters;
    }
}
