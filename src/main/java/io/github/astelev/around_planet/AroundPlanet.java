package io.github.astelev.around_planet;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/** Configures the Aeronautics portal engine; owns no alternate renderer or teleport loop. */
@Mod(AroundPlanet.MOD_ID)
public final class AroundPlanet {
    public static final String MOD_ID = "around_planet";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AroundPlanet(net.neoforged.bus.api.IEventBus bus, ModContainer container) {
        if(getClass().getClassLoader().getResource("qouteall/imm_ptl/core/IPShipPortalTraversal.class")==null)
            throw new IllegalStateException("around_planet requires Immersive Aeronautics 1.1.4, not the base Immersive Portals 6.0.7 JAR");
        container.registerConfig(ModConfig.Type.SERVER, PlanetConfig.SPEC);
    }
}
