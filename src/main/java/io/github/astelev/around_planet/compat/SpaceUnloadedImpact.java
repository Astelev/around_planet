package io.github.astelev.around_planet.compat;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.space.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import java.util.*;

/** Retire genuinely unloaded returning objects without requesting terrain or creating live bodies. */
public final class SpaceUnloadedImpact {
    private record Sample(String primary,SpaceVector position){}
    private static final Map<ServerLevel,Map<UUID,Sample>> LAST=new WeakHashMap<>();
    public static void clear(ServerLevel level){LAST.remove(level);}
    public static void resetSamples(ServerLevel level){var samples=LAST.computeIfAbsent(level,k->new HashMap<>());samples.clear();var data=SpaceOrbitData.get(level.getServer());for(var id:data.ids())samples.put(id,new Sample(data.get(id).orElseThrow().primary(),data.sample(id,level.getGameTime()).position()));}
    public static void check(ServerLevel level,ServerSubLevelContainer container){
        var data=SpaceOrbitData.get(level.getServer());var cells=SpaceCells.get(level);
        var dormant=SpaceDormantData.get(level.getServer());var cargo=SpaceCargoData.get(level.getServer());
        var last=LAST.computeIfAbsent(level,k->new HashMap<>());last.keySet().retainAll(data.ids());
        for(var id:List.copyOf(data.ids())){
            var cell=cells.all().stream().filter(c->c.id().equals(id)).findFirst().orElse(null);
            var binding=dormant.get(id);if(cell==null&&binding==null)continue;
            var path=data.get(id).orElseThrow();if(CelestialSystem.SUN.equals(path.primary()))continue;var nav=data.navigation(id).orElse(null);if(nav==null)continue;
            var state=data.sample(id,level.getGameTime());var previous=last.put(id,new Sample(path.primary(),state.position()));
            boolean moon=CelestialSystem.MOON.equals(path.primary());
            var planet=moon&&data.system()!=null?MoonSurface.geometry(data.system()):nav.planet();
            double shell=moon?PlanetConfig.MOON_LANDING.get():SpaceAtmosphere.returnAltitude();
            boolean contact=data.reentry(id).isPresent()||MoonLandingData.get(level.getServer()).containsKey(id)
                    || planet.inwardCrossing(previous==null||!previous.primary().equals(path.primary())?path.at(Math.max(path.epochTick(),level.getGameTime()-20)).position():previous.position(),state.position(),shell).isPresent()
                    || state.position().length()<planet.radius()+shell&&state.position().dot(state.velocity())<0;
            if(!contact)continue;
            var ids=new HashSet<UUID>();if(cell!=null)ids.addAll(cell.ships());if(binding!=null)for(var m:binding.members())ids.add(m.ship());
            var addresses=new HashMap<UUID,ChunkPos>();for(var record:cargo.all())if(record.cell().equals(id)){ids.add(record.ship());addresses.put(record.ship(),new ChunkPos(record.chunk()));}
            // A single awake member, player, or pending force-load ticket protects the whole group.
            if(ids.stream().anyMatch(ship->container.getSubLevel(ship)!=null
                    || container.getAllTickets().containsKey(ship)&&!container.getAllTickets().get(ship).tickets().isEmpty()))continue;
            if(cell!=null&&container.getAllSubLevels().stream().anyMatch(s->SpaceLocalPhysics.inCell(s,cell)))continue;
            if(cell!=null&&java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false)
                    .anyMatch(e->cell.contains(e.getX(),e.getY(),e.getZ())))continue;
            var holding=(SpaceHoldingAccess)container.getHoldingChunkMap();boolean removed=true;
            for(var ship:ids)removed&=holding.around_planet$discard(ship,addresses.get(ship));
            if(!removed)continue;
            for(var ship:ids)cargo.remove(ship);
            dormant.remove(id);data.remove(id);MoonLandingData.get(level.getServer()).remove(id);
            if(cell!=null)cells.remove(level,cell);
            SpaceDormant.forget(level,id);SpaceAtmosphere.forget(level,id);last.remove(id);
            io.github.astelev.around_planet.AroundPlanet.LOGGER.info("SPACE_UNLOADED_IMPACT orbit={} primary={} bodies={}",id,path.primary(),ids.size());
        }
    }
    private SpaceUnloadedImpact(){}
}
