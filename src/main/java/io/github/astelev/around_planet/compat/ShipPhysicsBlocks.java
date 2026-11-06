package io.github.astelev.around_planet.compat;

import dev.ryanhcode.sable.util.LevelAccelerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import java.util.HashMap;
import java.util.Map;

/** Read-only block source for Sable's existing collider builder, never a new physics backend. */
public final class ShipPhysicsBlocks extends LevelAccelerator {
    private final ServerLevel level;
    private final Map<Long,LevelChunk> chunks=new HashMap<>();
    public ShipPhysicsBlocks(ServerLevel level){super(level);this.level=level;}
    @Override public void clearCache(){super.clearCache();if(chunks!=null)chunks.clear();}
    @Override public LevelChunk getChunk(int x,int z) {
        long key=net.minecraft.world.level.ChunkPos.asLong(x,z);
        if(chunks.containsKey(key))return chunks.get(key);
        var topology=ShipSeams.topology(level);
        int source=topology==null || ShipyardSpace.reserved(level,x,z)?x:
                io.github.astelev.around_planet.seam.SeamGeometry.sourceX(topology,x);
        chunks.put(key,level.getChunkSource().getChunkNow(source,z));
        return chunks.get(key);
    }
    @Override public LevelChunk getChunk(BlockPos pos){return getChunk(pos.getX()>>4,pos.getZ()>>4);}
    @Override public BlockState getBlockState(BlockPos pos) {
        if(level.isOutsideBuildHeight(pos))return Blocks.AIR.defaultBlockState();
        var chunk=getChunk(pos);return chunk==null?Blocks.AIR.defaultBlockState():chunk.getBlockState(pos);
    }
    @Override public BlockState getBlockState(LevelChunk chunk,BlockPos pos){return getBlockState(pos);}
    @Override public FluidState getFluidState(BlockPos pos){return getBlockState(pos).getFluidState();}
    @Override public BlockEntity getBlockEntity(BlockPos pos){return null;}
}
