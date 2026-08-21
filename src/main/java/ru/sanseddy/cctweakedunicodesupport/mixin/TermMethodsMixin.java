package ru.sanseddy.cctweakedunicodesupport.mixin;

import dan200.computercraft.api.lua.Coerced;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.TermMethods;
import dan200.computercraft.core.terminal.Terminal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.sanseddy.cctweakedunicodesupport.text.Utf8;

import java.nio.ByteBuffer;

@Mixin(TermMethods.class)
public abstract class TermMethodsMixin {
    @Shadow
    public abstract Terminal getTerminal() throws LuaException;

    @Inject(method = "write", at = @At("HEAD"), cancellable = true)
    private void cc_tweaked_unicode_support$writeDecoded(Coerced<String> textArg, CallbackInfo ci) throws LuaException {
        var text = Utf8.decode(textArg.value());
        var terminal = getTerminal();
        synchronized (terminal) {
            terminal.write(text);
            terminal.setCursorPos(terminal.getCursorX() + text.length(), terminal.getCursorY());
        }
        ci.cancel();
    }

    @Inject(method = "blit", at = @At("HEAD"), cancellable = true)
    private void cc_tweaked_unicode_support$blitDecoded(
        ByteBuffer text, ByteBuffer textColour, ByteBuffer backgroundColour, CallbackInfo ci
    ) throws LuaException {
        var cells = Utf8.decode(Utf8.asByteString(text));
        var characterCount = cells.codePointCount(0, cells.length());
        if (textColour.remaining() != characterCount || backgroundColour.remaining() != characterCount) {
            throw new LuaException("Arguments must be the same length");
        }

        var terminal = getTerminal();
        synchronized (terminal) {
            var x = terminal.getCursorX();
            var y = terminal.getCursorY();
            if (y >= 0 && y < terminal.getHeight()) {
                var line = terminal.getLine(y);
                var foreground = terminal.getTextColourLine(y);
                var background = terminal.getBackgroundColourLine(y);
                var colourIndex = 0;
                for (var i = 0; i < cells.length(); i++) {
                    var trailingSurrogate = i > 0 && Character.isHighSurrogate(cells.charAt(i - 1))
                        && Character.isLowSurrogate(cells.charAt(i));
                    if (trailingSurrogate) colourIndex--;
                    line.setChar(x + i, cells.charAt(i));
                    foreground.setChar(x + i, (char) (textColour.get(textColour.position() + colourIndex) & 0xFF));
                    background.setChar(x + i, (char) (backgroundColour.get(backgroundColour.position() + colourIndex) & 0xFF));
                    colourIndex++;
                }
                terminal.setChanged();
            }

            terminal.setCursorPos(x + cells.length(), y);
        }
        ci.cancel();
    }
}
