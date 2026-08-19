package ru.ziftech.ccrulang.mixin;

import dan200.computercraft.core.apis.handles.AbstractHandle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.ziftech.ccrulang.CyrillicCodec;

import java.util.Optional;

/**
 * Every text-mode file read in CC: Tweaked - {@code fs.open(path):readAll()}/{@code readLine()}, and
 * critically {@code loadfile}/{@code os.run}/{@code shell.run} (which read the whole program file this
 * same way before compiling it) - passes raw file bytes straight through to Lua with no charset decoding
 * at all; see {@link AbstractHandle#readAll()} and {@link AbstractHandle#readLine}. That's why a `.lua`
 * file has to be saved as Windows-1251 for its Cyrillic string literals to display correctly - CC:
 * Tweaked's terminal and Lua strings are single-byte, but a Cyrillic character saved the way virtually
 * every editor defaults to (UTF-8) is 2 bytes.
 *
 * <p>This rewrites those 2-byte UTF-8 sequences to the matching single CP1251 byte on the way out of both
 * methods, so `.lua` source (and any other text file) can just be saved as plain UTF-8 like normal text
 * and be "understood" - both when it's run as a program and when a script reads it as data - without
 * anyone needing to know CC: Tweaked's terminal is byte-oriented at all. See
 * {@link CyrillicCodec#transcodeUtf8ToTerminal} for why this is safe to apply unconditionally (it only
 * ever touches the exact byte pairs that decode to one of the ~66 mapped Cyrillic letters).
 *
 * <p>Binary-mode handles ({@code fs.open(path, "rb")}) are skipped - their contents aren't text, and
 * coincidentally matching a byte pair in binary data would corrupt it.
 */
@Mixin(AbstractHandle.class)
public class AbstractHandleMixin {
    @Shadow
    @Final
    protected boolean binary;

    @Inject(method = "readAll", at = @At("RETURN"), cancellable = true)
    private void ccrulang$transcodeReadAll(CallbackInfoReturnable<Object[]> cir) {
        ccrulang$transcode(cir);
    }

    @Inject(method = "readLine", at = @At("RETURN"), cancellable = true)
    private void ccrulang$transcodeReadLine(Optional<Boolean> withTrailingArg, CallbackInfoReturnable<Object[]> cir) {
        ccrulang$transcode(cir);
    }

    private void ccrulang$transcode(CallbackInfoReturnable<Object[]> cir) {
        if (this.binary) return;

        var result = cir.getReturnValue();
        if (result == null || result.length == 0 || !(result[0] instanceof byte[] bytes)) return;

        var transcoded = CyrillicCodec.transcodeUtf8ToTerminal(bytes);
        if (transcoded != bytes) {
            cir.setReturnValue(new Object[]{transcoded});
        }
    }
}
