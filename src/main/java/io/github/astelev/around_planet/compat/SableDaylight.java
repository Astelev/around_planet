package io.github.astelev.around_planet.compat;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;
/** Plot addresses are storage, never geographic longitudes. */
public final class SableDaylight {
    public static double worldX(Level level,BlockPos pos){var ship=Sable.HELPER.getContaining(level,pos);return ship==null?pos.getX():ship.logicalPose().transformPosition(new Vector3d(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)).x;}
    private SableDaylight(){}
}
