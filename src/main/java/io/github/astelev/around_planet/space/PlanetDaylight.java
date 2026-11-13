package io.github.astelev.around_planet.space;
import io.github.astelev.around_planet.PlanetConfig;
import net.minecraft.world.level.*;
/** Position-aware daylight without rewriting chunk skylight arrays. */
public final class PlanetDaylight {
    public static boolean enabled(LevelReader level){return level instanceof Level l && l.dimension().equals(Level.OVERWORLD) && PlanetConfig.SPEC.isLoaded() && PlanetConfig.ENABLED.getAsBoolean();}
    public static PlanetSpin spin(Level level){return spin(level,0);}
    public static PlanetSpin spin(Level level,float partial){boolean moving=level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);return PlanetSpin.at(Math.floorMod(level.getDayTime(),24000)+(moving?partial:0),moving);}
    public static double time(Level level,double x,float partial){return PlanetSpin.localTime(Math.floorMod(level.getDayTime(),24000)+(level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)?partial:0),x,PlanetConfig.PLANET_SIZE.get());}
    public static double worldX(Level level,net.minecraft.core.BlockPos pos){return net.neoforged.fml.ModList.get().isLoaded("sable")?io.github.astelev.around_planet.compat.SableDaylight.worldX(level,pos):pos.getX();}
    public static int darken(Level level,double x){return PlanetSpin.skyDarken(time(level,x,0),level.getRainLevel(1),level.getThunderLevel(1));}
    private record Position(Level level,double x){}
    private static final ThreadLocal<Position> POSITION=new ThreadLocal<>();
    public static Double scopedX(Level level){var p=POSITION.get();return p!=null && p.level==level?p.x:null;}
    public static Scope scope(Level level,double x){var old=POSITION.get();if(enabled(level))POSITION.set(new Position(level,x));return new Scope(old);}
    public static final class Scope implements AutoCloseable {private final Position old;private Scope(Position old){this.old=old;}public void close(){if(old==null)POSITION.remove();else POSITION.set(old);}}
    private PlanetDaylight(){}
}
