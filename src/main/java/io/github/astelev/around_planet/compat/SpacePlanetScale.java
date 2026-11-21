package io.github.astelev.around_planet.compat;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.PlanetConfig;
import net.minecraft.server.level.ServerLevel;
import java.util.*;
/** Rebase saved planet-bound records once on load/size change, keeping altitude, inertial direction and surface gravity. */
public final class SpacePlanetScale {
    private record Settings(int size,double period,double gravity,double atmosphere){}
    private static final Map<ServerLevel,Settings> APPLIED=new WeakHashMap<>();
    public static void check(ServerLevel level){int size=PlanetConfig.PLANET_SIZE.get();double period=PlanetDaylight.periodSeconds();var settings=new Settings(size,period,PlanetConfig.SPACE_GRAVITY.get(),SpaceAtmosphere.model().top());if(Objects.equals(APPLIED.get(level),settings))return;APPLIED.put(level,settings);
        var data=SpaceOrbitData.get(level.getServer());var planet=new PlanetScale(size).geometry();
        double gravity=PlanetConfig.SPACE_GRAVITY.get(),spinRatio=PlanetSpin.circularToSpinRatio(planet.radius(),SpaceAtmosphere.model().top(),gravity,period);
        io.github.astelev.around_planet.AroundPlanet.LOGGER.info("PLANET_SPIN periodSeconds={} radius={} equatorSpeed={} circularSpeed={} upperAtmosphereRatio={}",period,planet.radius(),2*Math.PI/period*planet.radius(),Math.sqrt(gravity*planet.radius()),spinRatio);
        if(gravity>0 && spinRatio<3)io.github.astelev.around_planet.AroundPlanet.LOGGER.warn("Custom planet size/gravity gives circular-to-spin speed ratio below 3 at the atmosphere top: {}. Rotation period and configured gravity were retained.",spinRatio);
        for(var id:data.ids()){var old=data.navigation(id);if(old.isEmpty() || old.get().planet().equals(planet))continue;
            var nav=old.get();var state=data.sample(id,level.getGameTime());boolean moon=CelestialSystem.MOON.equals(data.get(id).orElseThrow().primary());double scale=CelestialSystem.SUN.equals(data.get(id).orElseThrow().primary())?12:moon?.2724:1;double oldRadius=nav.planet().radius()*scale,newRadius=planet.radius()*scale;double distance=state.position().length(),altitude=distance-oldRadius;
            if(distance<1e-9 || newRadius+altitude<=0)continue;
            var p=state.position().scale((newRadius+altitude)/distance);var orbit=data.get(id).orElseThrow();boolean waiting=data.reentry(id).isPresent();
            double ratio=planet.radius()/nav.planet().radius();data.put(id,new SpaceTrajectory(orbit.primary(),level.getGameTime(),new SpaceState(p,state.velocity()),orbit.mu()*ratio*ratio));
            data.navigate(id,new SpaceNavigation(planet,nav.frame()));if(waiting)data.reenter(id,p,level.getGameTime());
        }
    }
    public static void clear(ServerLevel level){APPLIED.remove(level);}
    private SpacePlanetScale(){}
}
