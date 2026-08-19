package ru.ziftech.ccrulang;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CCRuLang.MOD_ID)
public class CCRuLang {
    public static final String MOD_ID = "ccrulang";
    private static final Logger LOGGER = LoggerFactory.getLogger(CCRuLang.class);

    public CCRuLang(IEventBus modEventBus) {
        LOGGER.info("CC: Tweaked Russian language addon loaded");
    }
}
