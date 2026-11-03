package io.github.astelev.around_planet;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** Independent foundation for planet topology. World behavior is not changed yet. */
@Mod(AroundPlanet.MOD_ID)
public final class AroundPlanet {
    public static final String MOD_ID = "around_planet";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AroundPlanet() {
        LOGGER.info("around_planet loaded; planet topology is not enabled yet");
    }
}
