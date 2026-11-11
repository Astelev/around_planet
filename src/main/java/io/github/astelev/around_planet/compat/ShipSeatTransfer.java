package io.github.astelev.around_planet.compat;

import dev.ryanhcode.sable.Sable;
import io.github.astelev.around_planet.mixin.ChunkTrackingAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** A native seat remains in its plot; only world-space passengers follow the rebased pose. */
public final class ShipSeatTransfer {
    private record Rider(Entity entity, Vec3 position, Vec3 velocity) {}

    static Runnable prepare(Entity root,double dx) {return prepare(root,dx,0,0);}
    public static Runnable prepare(Entity root,double dx,double dy,double dz) {
        List<Rider> riders=root.getSelfAndPassengers()
                .filter(e->Sable.HELPER.getContaining(e)==null)
                .map(e->new Rider(e,e.position(),e.getDeltaMovement())).toList();
        return ()->{
            for(var rider:riders) {
                var entity=rider.entity();var target=rider.position().add(dx,dy,dz);
                // ServerPlayer.teleportTo invokes Sable's mounted-player conversion and sends
                // plot coordinates to its matching client hook. A vanilla vehicle packet would
                // instead put the player/seat into the reserved plot in world space.
                if(entity instanceof ServerPlayer player)
                    player.teleportTo(target.x,target.y,target.z);
                else entity.setPos(target);
                entity.setDeltaMovement(rider.velocity());
            }
            for(var rider:riders)if(rider.entity() instanceof ServerPlayer player) {
                ((ChunkTrackingAccessor)player.serverLevel().getChunkSource().chunkMap)
                        .around_planet$updateChunkTracking(player);
                player.serverLevel().getChunkSource().move(player);
            }
        };
    }
    private ShipSeatTransfer() {}
}
