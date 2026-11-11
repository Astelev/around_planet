package io.github.astelev.around_planet.compat;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.PlanetConfig;
import net.minecraft.server.level.ServerLevel;
import java.util.*;
/** Rebase saved planet-bound records once on load/size change, keeping altitude, inertial direction and surface gravity. */
public final class SpacePlanetScale {
    private static final Map<ServerLevel,Integer> APPLIED=new WeakHashMap<>();
    public static void check(ServerLevel level){int size=PlanetConfig.PLANET_SIZE.get();if(Objects.equals(APPLIED.get(level),size))return;APPLIED.put(level,size);
        var data=SpaceOrbitData.get(level.getServer());var planet=new PlanetScale(size).geometry();
        for(var id:data.ids()){var old=data.navigation(id);if(old.isEmpty() || old.get().planet().equals(planet))continue;
            var nav=old.get();var state=data.sample(id,level.getGameTime());double distance=state.position().length(),altitude=distance-nav.planet().radius();
            if(distance<1e-9 || planet.radius()+altitude<=0)continue;
            var p=state.position().scale((planet.radius()+altitude)/distance);var orbit=data.get(id).orElseThrow();boolean waiting=data.reentry(id).isPresent();
            double ratio=planet.radius()/nav.planet().radius();data.put(id,new SpaceTrajectory(orbit.primary(),level.getGameTime(),new SpaceState(p,state.velocity()),orbit.mu()*ratio*ratio));
            data.navigate(id,new SpaceNavigation(planet,nav.frame()));if(waiting)data.reenter(id,p,level.getGameTime());
        }
    }
    public static void clear(ServerLevel level){APPLIED.remove(level);}
    private SpacePlanetScale(){}
}
