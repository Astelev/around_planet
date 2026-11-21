package io.github.astelev.around_planet.space;
import io.github.astelev.around_planet.PlanetConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.*;
/** Every saved object participates, including dormant holding records; native bodies never move on an SOI change. */
@EventBusSubscriber(modid="around_planet")
public final class CelestialRuntime {
    private static final Map<ServerLevel,Map<UUID,Long>> LAST=new WeakHashMap<>();
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void tick(LevelTickEvent.Pre event){if(!(event.getLevel() instanceof ServerLevel level)||!level.dimension().equals(Level.OVERWORLD)||!PlanetConfig.SPEC.isLoaded())return;var data=SpaceOrbitData.get(level.getServer());double radius=new PlanetScale(PlanetConfig.PLANET_SIZE.get()).geometry().radius(),mu=PlanetConfig.SPACE_GRAVITY.get()*radius*radius,entry=Math.min(PlanetConfig.SPACE_ENTRY.get()-32,PlanetConfig.ATMOSPHERE_BOTTOM.get());if(mu<=0)return;var old=data.system();if(old==null||old.earthRadius()!=radius||old.earthMu()!=mu||old.earthEntry()!=entry)data.system(new CelestialSystem(radius,mu,old==null?0:old.clockTicks(),entry));advance(level,data);}
    public static void advance(ServerLevel level,SpaceOrbitData data){var system=data.system();if(system==null)return;long now=level.getGameTime();var times=LAST.computeIfAbsent(level,k->new HashMap<>());boolean changed=false;for(var id:data.ids()){var path=data.get(id).orElseThrow();long before=times.getOrDefault(id,now-1);times.put(id,now);if(!system.managed(path.primary())||data.reentry(id).isPresent()||data.solarImpacts().contains(id))continue;var flight=PatchedConics.trace(path,Math.min(now,Math.max(path.epochTick(),before)),now,system);if(flight.solarImpact()){var coast=flight.finalPath();data.put(id,new SpaceTrajectory(CelestialSystem.SUN,now,coast.at(flight.legs().getLast().until()),0));data.solarImpact(id);continue;}var result=flight.finalPath();if(!result.equals(path)){data.put(id,result);changed=true;if(net.neoforged.fml.ModList.get().isLoaded("sable"))io.github.astelev.around_planet.compat.SpaceReentry.entered(level,id,result.at(now).position());io.github.astelev.around_planet.AroundPlanet.LOGGER.info("SPACE_SOI object={} from={} to={} tick={}",id,path.primary(),result.primary(),result.epochTick());}}times.keySet().retainAll(data.ids());if(changed&&net.neoforged.fml.ModList.get().isLoaded("sable")){io.github.astelev.around_planet.compat.SpaceDormant.resetSamples(level);io.github.astelev.around_planet.compat.SpaceAtmosphere.clear(level);io.github.astelev.around_planet.network.SpaceMapPayload.refreshNow(level);}}
    private CelestialRuntime(){}
}
