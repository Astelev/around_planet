package io.github.astelev.around_planet.space;
import net.minecraft.resources.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
/** Nonrotating lunar surface chart. No atmosphere or velocity clamp. */
public final class MoonSurface {
    public static ResourceKey<Level> dimension(){return Keys.DIMENSION;}
    private static final class Keys {static final ResourceKey<Level> DIMENSION=ResourceKey.create(Registries.DIMENSION,ResourceLocation.fromNamespaceAndPath("around_planet","moon"));}
    public static PlanetGeometry geometry(CelestialSystem system){return MoonScale.fromEarthRadius(system.earthRadius()).geometry();}
    public static SpaceVector contact(PlanetGeometry planet,SpaceVector previous,SpaceState state,double altitude){if(state.position().dot(state.velocity())<0&&state.position().length()<planet.radius()+altitude)return state.position().scale((planet.radius()+altitude)/state.position().length());return planet.inwardCrossing(previous,state.position(),altitude).orElse(null);}
    private MoonSurface(){}
}
