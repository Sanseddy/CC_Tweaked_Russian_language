package ru.sanseddy.cctweakedunicodesupport.mixin;

import dan200.computercraft.api.lua.Coerced;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.peripheral.printer.PrinterPeripheral;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.sanseddy.cctweakedunicodesupport.text.Utf8;

@Mixin(PrinterPeripheral.class)
public class PrinterPeripheralMixin {
    @Shadow
    private Terminal getCurrentPage() throws LuaException {
        throw new AssertionError("mixin did not apply");
    }

    @Inject(method = "write", at = @At("HEAD"), cancellable = true)
    private void cc_tweaked_unicode_support$writeDecoded(Coerced<String> textArg, CallbackInfo ci) throws LuaException {
        var text = Utf8.decode(textArg.value());
        var page = getCurrentPage();
        page.write(text);
        page.setCursorPos(page.getCursorX() + text.length(), page.getCursorY());
        ci.cancel();
    }
}
