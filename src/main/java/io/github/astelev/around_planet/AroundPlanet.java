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
        bus.addListener(io.github.astelev.around_planet.network.SpaceStatusPayload::register);
        bus.addListener(io.github.astelev.around_planet.network.VacuumPayload::register);
        bus.addListener(io.github.astelev.around_planet.network.PlasmaPayload::register);
        bus.addListener(io.github.astelev.around_planet.network.CelestialSkyPayload::register);
        bus.addListener(io.github.astelev.around_planet.network.SpaceMapPayload::register);
        bus.addListener(io.github.astelev.around_planet.network.PlanetAtlasPayload::register);
        io.github.astelev.around_planet.maneuver.ManeuverRegistry.register(bus);
        io.github.astelev.around_planet.loader.ShipLoaderRegistry.register(bus);
        bus.addListener(io.github.astelev.around_planet.network.ManeuverPayload::register);
        bus.addListener(io.github.astelev.around_planet.network.RendezvousPayload::register);
        io.github.astelev.around_planet.sensor.OrbitalSensorRegistry.register(bus);
        io.github.astelev.around_planet.propulsion.PropulsionRegistry.register(bus);
        io.github.astelev.around_planet.fuel.CryogenicFluids.register(bus);
        io.github.astelev.around_planet.solid.SolidRegistry.register(bus);
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(io.github.astelev.around_planet.solid.SolidMassBridge::registerDefaults));
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(io.github.astelev.around_planet.fuel.FuelMassBridge::registerDefaults));
        bus.addListener(io.github.astelev.around_planet.network.PropulsionPayload::register);
        bus.addListener(io.github.astelev.around_planet.network.OrbitalSensorPayload::register);
        if(net.neoforged.fml.ModList.get().isLoaded("sable")){io.github.astelev.around_planet.compat.SpaceRendezvous.init();io.github.astelev.around_planet.compat.ShipLoaderBridge.init();}
        if(net.neoforged.fml.ModList.get().isLoaded("sable")&&net.neoforged.fml.ModList.get().isLoaded("dimensional_sable"))io.github.astelev.around_planet.compat.DimensionalTransfers.init();
        container.registerConfig(ModConfig.Type.SERVER, new PlanetConfigMigration());
    }
}
