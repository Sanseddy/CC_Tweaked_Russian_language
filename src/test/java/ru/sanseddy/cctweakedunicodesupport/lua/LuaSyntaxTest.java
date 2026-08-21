package ru.sanseddy.cctweakedunicodesupport.lua;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaThread;
import org.squiddev.cobalt.compiler.LuaC;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.lib.CoreLibraries;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LuaSyntaxTest {
    @ParameterizedTest
    @ValueSource(strings = {
        "/data/computercraft/lua/bios.lua",
        "/data/computercraft/lua/rom/apis/textutils.lua",
        "/data/computercraft/lua/rom/programs/edit.lua",
        "/data/computercraft/lua/rom/programs/help.lua",
        "/data/computercraft/lua/rom/programs/advanced/multishell.lua",
        "/data/computercraft/lua/rom/programs/rednet/chat.lua",
        "/data/computercraft/lua/rom/apis/window.lua",
        "/data/computercraft/lua/rom/modules/main/cc/image/nft.lua",
        "/data/computercraft/lua/rom/modules/main/cc/internal/error_printer.lua",
        "/data/computercraft/lua/rom/modules/main/cc/internal/menu.lua",
        "/data/computercraft/lua/rom/modules/main/cc/strings.lua",
        "/data/computercraft/lua/rom/modules/main/cc/pretty.lua"
    })
    void romOverrideCompiles(String path) throws Exception {
        try (var stream = LuaSyntaxTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, () -> "Missing test resource " + path);
            LuaC.compile(new LuaState(), stream, "@" + path);
        } catch (IOException e) {
            throw new AssertionError("Could not read " + path, e);
        }
    }

    @Test
    void unicodeHelpersHandleValidAndMalformedBoundaries() throws Exception {
        var path = "/data/computercraft/lua/bios.lua";
        String bios;
        try (var stream = LuaSyntaxTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, () -> "Missing test resource " + path);
            bios = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        var start = bios.indexOf("-- [cc_tweaked_unicode_support] Character-aware string handling.");
        var end = bios.indexOf("-- Inject a stub for the old bit library", start);
        if (start < 0 || end < 0) throw new AssertionError("Could not locate the Unicode helper block");

        var checks = """
            local ya = string.char(0xD0, 0xAF)
            local euro = string.char(0xE2, 0x82, 0xAC)
            local smile = string.char(0xF0, 0x9F, 0x99, 0x82)
            assert(unicode.len("A" .. ya .. smile) == 3)
            local full_row = string.rep(ya, 51)
            assert(unicode.len(full_row) == 51)
            assert(string.len(full_row) == 51)
            assert(unicode.byte_len(full_row) == 102)
            assert(unicode.sub(full_row, 1, 51) == full_row)
            assert(unicode.sub(full_row, 51, 51) == ya)
            assert(unicode.char(0x42F) == ya)
            assert(unicode.char(0x1F642) == smile)
            assert(unicode.charback(smile, 4) == 4)
            assert(unicode.charback("A" .. string.char(0x95), 2) == 1)
            assert(unicode.align(ya, 1) == 0)
            assert(unicode.align(ya, 1, true) == 2)
            assert(unicode.align(euro, 2) == 0)
            assert(unicode.align(euro, 2, true) == 3)
            for _, value in ipairs({ -1, 0xD800, 0xFDD0, 0xFDEF, 0x110000, 1.5 }) do
                assert(not pcall(unicode.char, value))
            end
            """;

        var script = bios.substring(start, end) + checks;
        var state = new LuaState();
        CoreLibraries.standardGlobals(state);
        var function = LoadState.load(
            state, new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)), "=unicode-test.lua", state.globals()
        );
        LuaThread.run(new LuaThread(state, function), Constants.NIL);
    }
}
