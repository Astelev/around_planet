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

    public AroundPlanet(net.neoforged.bus.api.IEventBus bus, ModContainer container) {
        bus.addListener(io.github.astelev.around_planet.network.SeamChunkPayload::register);
        bus.addListener(io.github.astelev.around_planet.network.SeamVehiclePayload::register);
        bus.addListener(io.github.astelev.around_planet.network.SeamEntitiesPayload::register);
        bus.addListener(io.github.astelev.around_planet.network.SeamShipTransferPayload::register);
        bus.addListener(io.github.astelev.around_planet.network.SpaceTransferPayload::register);
        container.registerConfig(ModConfig.Type.SERVER, PlanetConfig.SPEC);
    }
}
