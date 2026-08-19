package ru.ziftech.ccrulang;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Addon entry point. All the actual work happens elsewhere: {@link ru.ziftech.ccrulang.mixin.StringUtilMixin}
 * handles typed/pasted Cyrillic input, and {@link ru.ziftech.ccrulang.mixin.client.FixedWidthFontRendererMixin}
 * redirects Cyrillic glyphs to the separate {@code russian_font.png} texture at render time. There is nothing
 * to register here.
 */
@Mod(CCRuLang.MOD_ID)
public class CCRuLang {
    public static final String MOD_ID = "ccrulang";
    private static final Logger LOGGER = LoggerFactory.getLogger(CCRuLang.class);

    public CCRuLang(IEventBus modEventBus) {
        LOGGER.info("CC: Tweaked Russian language addon loaded");
    }
}
