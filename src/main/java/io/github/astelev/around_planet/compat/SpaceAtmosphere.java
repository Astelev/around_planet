package io.github.astelev.around_planet.compat;

import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.space.*;
import net.minecraft.server.level.ServerLevel;
import java.util.*;

/** Perturb virtual velocity only during atmospheric descent. Outside it the saved Kepler coast is unchanged. */
public final class SpaceAtmosphere {
    private static final Map<ServerLevel,Map<UUID,Long>> LAST=new WeakHashMap<>();
    public static void clear(ServerLevel level){LAST.remove(level);}
    public static void forget(ServerLevel level,UUID id){var m=LAST.get(level);if(m!=null)m.remove(id);}
    public static double returnAltitude(){return Math.min(PlanetConfig.SPACE_ENTRY.getAsInt()-32,PlanetConfig.ATMOSPHERE_BOTTOM.getAsInt());}
    public static Atmosphere model(){double bottom=returnAltitude();return new Atmosphere(Math.max(bottom+512,PlanetConfig.ATMOSPHERE_TOP.getAsInt()),bottom,PlanetConfig.ATMOSPHERE_DRAG.get(),PlanetConfig.REENTRY_SPEED.get());}
    public static void advance(ServerLevel level,UUID id,SpaceCells.Cell cell){
        var data=SpaceOrbitData.get(level.getServer());var trajectory=data.get(id);var nav=data.navigation(id);if(trajectory.isEmpty() || nav.isEmpty())return;
        long now=level.getGameTime();var times=LAST.computeIfAbsent(level,k->new HashMap<>());Long previous=times.put(id,now);
        if(data.reentry(id).isPresent())return;
        double dt=Math.clamp((now-(previous==null?Math.max(trajectory.get().epochTick(),now-1):previous))/20.0,0,1);var state=data.sample(id,now);var next=PlanetDaylight.spin(level).damp(model(),state,nav.get().planet().radius(),dt);var delta=next.subtract(state.velocity());
        if(delta.length()>1e-10){data.impulse(id,now,delta);if(cell!=null)SpaceLocalPhysics.externalImpulse(level,cell,nav.get().frame().toLocal(delta));}
        if(times.size()>256 && now%100==0)times.keySet().removeIf(key->data.get(key).isEmpty());
    }
    public static void handoff(ServerLevel level,UUID id,SpaceCells.Cell cell){var data=SpaceOrbitData.get(level.getServer());var state=data.sample(id,level.getGameTime());var next=PlanetDaylight.spin(level).handoff(model(),state);var delta=next.subtract(state.velocity());
        if(delta.length()>1e-10){data.impulse(id,level.getGameTime(),delta);if(cell!=null)SpaceLocalPhysics.externalImpulse(level,cell,data.navigation(id).orElseThrow().frame().toLocal(delta));}
    }
    private SpaceAtmosphere(){}
}
