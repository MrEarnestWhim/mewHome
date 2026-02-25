package com.mew.mewhome;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(MewHome.MODID)
public class MewHome {
    public static final String MODID = "mewhome";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MewHome(IEventBus modEventBus, ModContainer modContainer) {
        ServerI18n.init();
        LOGGER.info("MewHome loaded!");
    }
}
