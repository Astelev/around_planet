package io.github.astelev.around_planet.compat;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
public final class VacuumSable {
    public static Vector3d worldEye(Entity entity){var p=entity.getEyePosition();var ship=Sable.HELPER.getContaining(entity.level(),net.minecraft.core.BlockPos.containing(p));var v=new Vector3d(p.x,p.y,p.z);return ship==null?v:ship.logicalPose().transformPosition(v);}
    private VacuumSable(){}
}
