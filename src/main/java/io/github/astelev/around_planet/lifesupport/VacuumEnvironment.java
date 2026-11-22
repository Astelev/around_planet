package io.github.astelev.around_planet.lifesupport;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.space.*;
/** Physical cell addresses are service coordinates, not atmospheric altitude. */
public final class VacuumEnvironment {
    public static boolean outside(Entity entity){
        if(!PlanetConfig.VACUUM.get())return false;
        if(entity.level().dimension().equals(MoonSurface.dimension()))return true;
        if(!entity.level().dimension().equals(Level.OVERWORLD))return false;
        if(SpaceCells.contains(entity))return true;
        double eye=entity.getEyeY();
        if(net.neoforged.fml.ModList.get().isLoaded("sable"))eye=io.github.astelev.around_planet.compat.VacuumSable.worldEye(entity).y;
        return eye>=PlanetConfig.SPACE_ENTRY.get();
    }
    private VacuumEnvironment(){}
}
