package io.github.astelev.around_planet.portal;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.topology.EastWestTopology;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import java.util.List;

/** Manages only our tagged EW pair. Native Aeronautics owns rendering, tracking and teleportation. */
@EventBusSubscriber(modid=AroundPlanet.MOD_ID)
public final class PlanetPortals {
    public static final String EAST="around_planet:ew/east",WEST="around_planet:ew/west";
    public static boolean owned(Portal portal){return EAST.equals(portal.portalTag)||WEST.equals(portal.portalTag);}
    public static PortalLayout configuredLayout() {
        return new PortalLayout(EastWestTopology.atBoundary(PlanetConfig.BOUNDARY.getAsInt()),
                PlanetConfig.MIN_Y.getAsInt(),PlanetConfig.MAX_Y.getAsInt(),PlanetConfig.HALF_SPAN_Z.getAsInt());
    }
    @SubscribeEvent public static void load(LevelEvent.Load event) {
        if(event.getLevel() instanceof Level level && level.dimension()==Level.OVERWORLD
                && PlanetConfig.SPEC.isLoaded() && PlanetConfig.ENABLED.getAsBoolean()) {
            try {PlanetBorders.bind(level.getWorldBorder(),configuredLayout().topology());}
            catch(IllegalArgumentException e){AroundPlanet.LOGGER.error("Invalid around_planet portal settings",e);}
        }
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if(event.getLevel() instanceof Level level)PlanetBorders.unbind(level.getWorldBorder());
    }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        var level=event.getServer().overworld();
        try {reconcile(level,PlanetConfig.ENABLED.getAsBoolean()?configuredLayout():null);}
        catch(IllegalArgumentException | IllegalStateException e) {
            reconcile(level,null);
            AroundPlanet.LOGGER.error("around_planet EW portals disabled: {}",e.getMessage());
        }
    }
    /** Idempotent startup/config migration. A null layout removes only our managed pair. */
    public static void reconcile(ServerLevel level,PortalLayout layout) {
        var storage=GlobalPortalStorage.get(level);
        var existing=storage.data.stream().filter(PlanetPortals::owned).toList();
        if(layout==null) {
            existing.forEach(storage::removePortal);PlanetBorders.unbind(level.getWorldBorder());return;
        }
        if(existing.size()==2 && existing.stream().anyMatch(p->matches(p,layout,true))
                && existing.stream().anyMatch(p->matches(p,layout,false))) {
            PlanetBorders.bind(level.getWorldBorder(),layout.topology());return;
        }
        // Validate both before replacing anything; foreign portals are never touched.
        var east=create(level,layout,true);var west=create(level,layout,false);
        if(!east.isPortalValid() || !west.isPortalValid())throw new IllegalStateException("Portal destination must be inside the vanilla world border, with a valid height");
        existing.forEach(storage::removePortal);
        storage.addPortal(east);storage.addPortal(west);
        PlanetBorders.bind(level.getWorldBorder(),layout.topology());
        AroundPlanet.LOGGER.info("Aeronautics EW portals at X=±{}, Y=[{},{}], Z=±{}",layout.topology().east(),layout.minY(),layout.maxY(),layout.halfSpanZ());
    }
    private static Portal create(ServerLevel level,PortalLayout layout,boolean east) {
        var portal=Portal.ENTITY_TYPE.create(level);
        if(portal==null)throw new IllegalStateException("Aeronautics portal entity type unavailable");
        portal.portalTag=east?EAST:WEST;portal.isGlobalPortal=true;
        portal.setOriginPos(layout.origin(east));portal.setDestinationDimension(level.dimension());
        portal.setDestination(layout.destination(east));
        portal.setOrientationAndSize(layout.axisW(east),new Vec3(0,1,0),layout.width(),layout.height());
        portal.setTeleportChangesScale(false);portal.setTeleportChangesGravity(false);
        portal.setTeleportable(true);portal.setInteractable(true);portal.setIsVisible(true);
        return portal;
    }
    private static boolean matches(Portal p,PortalLayout layout,boolean east) {
        return (east?EAST:WEST).equals(p.portalTag) && !p.isRemoved() && p.getIsGlobal()
                && p.getDestDim()==Level.OVERWORLD && p.getOriginPos().equals(layout.origin(east))
                && p.getDestPos().equals(layout.destination(east)) && p.getAxisW().equals(layout.axisW(east))
                && p.getAxisH().equals(new Vec3(0,1,0)) && p.getWidth()==layout.width() && p.getHeight()==layout.height()
                && p.getScale()==1 && p.getRotation()==null && p.isVisible() && p.getTeleportable() && p.isInteractable();
    }
    public static List<Portal> managed(ServerLevel level){return GlobalPortalStorage.get(level).data.stream().filter(PlanetPortals::owned).toList();}
    private PlanetPortals() {}
}
