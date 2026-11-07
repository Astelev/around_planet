package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.util.LevelAccelerator;
import io.github.astelev.around_planet.compat.ShipPhysicsBlocks;
import io.github.astelev.around_planet.compat.ShipSeams;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.HashSet;
import java.util.Set;

/** Sable builds every collider through its public terrain-section API. No private native calls or synthetic packed data. */
@Pseudo
@Mixin(targets="dev.ryanhcode.sable.physics.impl.rapier.RapierPhysicsPipeline",remap=false)
public abstract class SeamShipTerrainMixin {
    @Shadow @Final private ServerLevel level;
    @Unique private ShipPhysicsBlocks around_planet$blocks;
    @Unique private boolean around_planet$imageUpload,around_planet$edgeUpload;
    @Unique private final Set<SectionPos> around_planet$images=new HashSet<>();
    @Unique private final Set<SectionPos> around_planet$dirty=new HashSet<>();
    @Unique private ShipPhysicsBlocks around_planet$blocks() {
        if(around_planet$blocks==null)around_planet$blocks=new ShipPhysicsBlocks(level);
        return around_planet$blocks;
    }
    @Unique private boolean around_planet$edge(BlockPos pos) {
        var topology=ShipSeams.topology(level);
        return topology!=null && !io.github.astelev.around_planet.compat.ShipyardSpace.reserved(level,pos.getX()>>4,pos.getZ()>>4)
                && (ShipSeams.image(level,pos.getX()>>4,pos.getZ()>>4)
                    || Math.abs(pos.getX()-topology.west())<17 || Math.abs(pos.getX()-topology.east())<17);
    }
    @WrapMethod(method="handleChunkSectionAddition")
    private void around_planet$realSourceSection(LevelChunkSection section,int x,int y,int z,boolean global,Operation<Void> original) {
        boolean image=ShipSeams.image(level,x,z);
        if(image) {
            // Sable's standard synchronous fallback is retained if asynchronous preloading has not finished.
            var source=level.getChunk(ShipSeams.source(level,x),z);
            section=source.getSection(level.getSectionIndexFromSectionY(y));
        }
        if(around_planet$blocks!=null)around_planet$blocks.clearCache();
        boolean oldImage=around_planet$imageUpload,oldEdge=around_planet$edgeUpload;
        around_planet$imageUpload=image;around_planet$edgeUpload=around_planet$edge(new BlockPos(x<<4,0,z<<4));
        try{original.call(section,x,y,z,global);}finally{around_planet$imageUpload=oldImage;around_planet$edgeUpload=oldEdge;}
        if(image)around_planet$images.add(SectionPos.of(x,y,z));
    }
    @WrapOperation(method="handleChunkSectionAddition",at=@At(value="INVOKE",target="Ldev/ryanhcode/sable/util/LevelAccelerator;getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;"))
    private LevelChunk around_planet$sourceChunk(LevelAccelerator accelerator,int x,int z,Operation<LevelChunk> original) {
        return around_planet$imageUpload?around_planet$blocks().getChunk(x,z):original.call(accelerator,x,z);
    }
    @WrapOperation(method="handleChunkSectionAddition",at=@At(value="INVOKE",target="Ldev/ryanhcode/sable/util/LevelAccelerator;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState around_planet$sourceState(LevelAccelerator accelerator,BlockPos pos,Operation<BlockState> original) {
        return around_planet$imageUpload?around_planet$blocks().getBlockState(pos):original.call(accelerator,pos);
    }
    @WrapOperation(method={"handleChunkSectionAddition","handleBlockChange"},at=@At(value="INVOKE",target="Ldev/ryanhcode/sable/physics/chunk/VoxelNeighborhoodState;getState(Ldev/ryanhcode/sable/util/LevelAccelerator;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/chunk/LevelChunk;)Ldev/ryanhcode/sable/physics/chunk/VoxelNeighborhoodState;"))
    private VoxelNeighborhoodState around_planet$neighbors(LevelAccelerator accelerator,BlockPos pos,LevelChunk chunk,Operation<VoxelNeighborhoodState> original) {
        return original.call(around_planet$edgeUpload || around_planet$edge(pos)?around_planet$blocks():accelerator,pos,chunk);
    }
    @WrapOperation(method="handleBlockChange",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState around_planet$neighborState(ServerLevel level,BlockPos pos,Operation<BlockState> original) {
        return ShipSeams.image(level,pos.getX()>>4,pos.getZ()>>4)?around_planet$blocks().getBlockState(pos):original.call(level,pos);
    }
    @Inject(method="handleBlockChange",at=@At("HEAD"),cancellable=true)
    private void around_planet$guardEdits(SectionPos section,LevelChunkSection chunk,int x,int y,int z,BlockState oldState,BlockState newState,CallbackInfo ci) {
        if(around_planet$blocks!=null)around_planet$blocks.clearCache();
        if(ShipSeams.image(level,section.x(),section.z()))ci.cancel();
    }
    @Inject(method="handleBlockChange",at=@At("RETURN"))
    private void around_planet$sourceEdit(SectionPos section,LevelChunkSection chunk,int x,int y,int z,BlockState oldState,BlockState newState,CallbackInfo ci) {
        if(around_planet$images.isEmpty() || ShipSeams.topology(level)==null)return;
        var topology=ShipSeams.topology(level);int half=topology.width()/32,band=io.github.astelev.around_planet.PlanetConfig.SHIP_BAND.getAsInt();
        if(section.x()< -half-1 || section.x()>half || section.x()> -half+band && section.x()<half-band-1)return;
        var pos=section.origin().offset(x,y,z);var affected=new HashSet<SectionPos>();affected.add(section);
        for(var d:Direction.values())affected.add(SectionPos.of(pos.relative(d)));
        for(var image:around_planet$images)if(affected.contains(SectionPos.of(ShipSeams.source(level,image.x()),image.y(),image.z())))
            around_planet$dirty.add(image);
        if(!around_planet$dirty.isEmpty()) {
            var system=SubLevelPhysicsSystem.get(level);int width=ShipSeams.topology(level).width();
            if(system!=null)system.wakeUpObjectsAt(pos.getX()+(pos.getX()<0?width:-width),pos.getY(),pos.getZ());
        }
    }
    @Inject(method="prePhysicsTicks",at=@At("HEAD"))
    private void around_planet$refresh(CallbackInfo ci) {
        var pending=Set.copyOf(around_planet$dirty);around_planet$dirty.clear();
        for(var image:pending)if(around_planet$images.contains(image) && ShipSeams.image(level,image.x(),image.z())) {
            var source=level.getChunkSource().getChunkNow(ShipSeams.source(level,image.x()),image.z());
            if(source!=null)((PhysicsPipeline)(Object)this).handleChunkSectionAddition(source.getSection(level.getSectionIndexFromSectionY(image.y())),image.x(),image.y(),image.z(),false);
        }
    }
    @Inject(method="handleChunkSectionRemoval",at=@At("HEAD"))
    private void around_planet$remove(int x,int y,int z,CallbackInfo ci) {
        var section=SectionPos.of(x,y,z);around_planet$images.remove(section);around_planet$dirty.remove(section);
    }
    @Inject(method="dispose",at=@At("HEAD"))
    private void around_planet$dispose(CallbackInfo ci) {around_planet$images.clear();around_planet$dirty.clear();around_planet$blocks=null;}
}
