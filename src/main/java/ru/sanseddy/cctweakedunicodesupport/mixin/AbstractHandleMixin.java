package ru.sanseddy.cctweakedunicodesupport.mixin;

import dan200.computercraft.api.lua.Coerced;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.handles.AbstractHandle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.sanseddy.cctweakedunicodesupport.text.Utf8;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

@Mixin(AbstractHandle.class)
public class AbstractHandleMixin {
    @Shadow
    @Final
    protected boolean binary;

    @Shadow
    @Final
    private SeekableByteChannel channel;

    @Shadow
    protected void checkOpen() throws LuaException {
        throw new AssertionError("mixin did not apply");
    }

    @Inject(method = "readAll", at = @At("RETURN"), cancellable = true)
    private void cc_tweaked_unicode_support$readAllAsUnicode(CallbackInfoReturnable<Object[]> cir) {
        cc_tweaked_unicode_support$promoteResult(cir);
    }

    @Inject(method = "readLine", at = @At("RETURN"), cancellable = true)
    private void cc_tweaked_unicode_support$readLineAsUnicode(Optional<Boolean> withTrailing, CallbackInfoReturnable<Object[]> cir) {
        cc_tweaked_unicode_support$promoteResult(cir);
    }

    private void cc_tweaked_unicode_support$promoteResult(CallbackInfoReturnable<Object[]> cir) {
        if (binary) return;

        var result = cir.getReturnValue();
        if (result == null || result.length == 0 || !(result[0] instanceof byte[] bytes)) return;

        var promoted = Utf8.toUnicode(bytes, 0, bytes.length);
        if (promoted != null) cir.setReturnValue(new Object[]{promoted});
    }

    @Inject(method = "write", at = @At("HEAD"), cancellable = true)
    private void cc_tweaked_unicode_support$writeAsLegacy(IArguments arguments, CallbackInfo ci) throws LuaException {
        if (binary) return;
        checkOpen();
        cc_tweaked_unicode_support$writeFolded(arguments.getBytesCoerced(0));
        ci.cancel();
    }

    @Inject(method = "writeLine", at = @At("HEAD"), cancellable = true)
    private void cc_tweaked_unicode_support$writeLineAsLegacy(Coerced<ByteBuffer> text, CallbackInfo ci) throws LuaException {
        if (binary) return;
        checkOpen();
        cc_tweaked_unicode_support$writeFolded(text.value());
        cc_tweaked_unicode_support$writeFolded(ByteBuffer.wrap(new byte[]{'\n'}));
        ci.cancel();
    }

    private void cc_tweaked_unicode_support$writeFolded(ByteBuffer buffer) throws LuaException {
        var bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);

        var folded = Utf8.toLegacy(bytes, 0, bytes.length);
        try {
            channel.write(ByteBuffer.wrap(folded == null ? bytes : folded));
        } catch (IOException e) {
            throw new LuaException(e.getMessage());
        }
    }
}
