package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import io.github.astelev.around_planet.compat.ShipSeams;
import io.github.astelev.around_planet.compat.ShipTerrainPreload;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

/** Translate native ticket inputs only; Sable retains ownership of residency, uploading and expiry. */
@Pseudo
@Mixin(targets="dev.ryanhcode.sable.sublevel.system.ticket.PhysicsChunkTicketManager",remap=false)
public abstract class SeamShipPhysicsTicketsMixin {
    @Unique private final ShipTerrainPreload around_planet$preload=new ShipTerrainPreload();
    @Inject(method="update",at=@At("HEAD"))
    private void around_planet$prepare(ServerLevel level,ServerSubLevelContainer container,SubLevelPhysicsSystem system,PhysicsPipeline pipeline,double delta,CallbackInfo ci) {
        around_planet$preload.tick(level,container);
    }
    @WrapMethod(method="isChunkLoadedEnough")
    private static boolean around_planet$sourceReady(ServerLevel level,int x,int z,Operation<Boolean> original) {
        return original.call(level,ShipSeams.image(level,x,z)?ShipSeams.source(level,x):x,z);
    }
    @WrapMethod(method="inhabitChunk")
    private void around_planet$canonicalTicket(ServerLevel level,DistanceManager distance,UUID id,long time,long key,int x,int z,Operation<Void> original) {
        if(ShipSeams.image(level,x,z)){x=ShipSeams.source(level,x);key=ChunkPos.asLong(x,z);}
        original.call(level,distance,id,time,key,x,z);
    }
    @WrapOperation(method="addTicket",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;"))
    private LevelChunk around_planet$canonicalSection(Level level,int x,int z,Operation<LevelChunk> original) {
        return original.call(level,ShipSeams.image(level,x,z)?ShipSeams.source(level,x):x,z);
    }
}
