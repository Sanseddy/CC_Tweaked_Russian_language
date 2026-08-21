package ru.sanseddy.cctweakedunicodesupport.mixin;

import dan200.computercraft.shared.computer.terminal.TerminalState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TerminalState.class)
public interface TerminalStateAccessor {
    @Invoker("<init>")
    static TerminalState cc_tweaked_unicode_support$create(
        boolean colour, int width, int height, int cursorX, int cursorY,
        boolean cursorBlink, int cursorFgColour, int cursorBgColour, byte[] contents
    ) {
        throw new AssertionError("mixin did not apply");
    }

    @Accessor("width")
    int cc_tweaked_unicode_support$width();

    @Accessor("height")
    int cc_tweaked_unicode_support$height();

    @Accessor("cursorX")
    int cc_tweaked_unicode_support$cursorX();

    @Accessor("cursorY")
    int cc_tweaked_unicode_support$cursorY();

    @Accessor("cursorBlink")
    boolean cc_tweaked_unicode_support$cursorBlink();

    @Accessor("cursorFgColour")
    int cc_tweaked_unicode_support$cursorFgColour();

    @Accessor("cursorBgColour")
    int cc_tweaked_unicode_support$cursorBgColour();

    @Accessor("contents")
    byte[] cc_tweaked_unicode_support$contents();
}
