package io.github.astelev.around_planet;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/** East/west world topology; independent of terrain-rendering mods. */
@Mod(AroundPlanet.MOD_ID)
public final class AroundPlanet {
    public static final String MOD_ID = "around_planet";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AroundPlanet(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, PlanetConfig.SPEC);
    }
}
