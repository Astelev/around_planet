package io.github.astelev.around_planet.transfer;

import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.compat.ShipyardSpace;
import io.github.astelev.around_planet.seam.SeamGeometry;
import io.github.astelev.around_planet.topology.EastWestTopology;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.List;

/** Read-only, non-loading collision coordinates. Each half of a straddling hull uses its own canonical column. */
public final class SeamCollisions implements CollisionGetter {
    private final Level level;
    private final EastWestTopology topology;
    private final BlockGetter missing=new MissingColumn();
    private SeamCollisions(Level level, EastWestTopology topology) { this.level=level;this.topology=topology; }

    public static CollisionGetter view(CollisionGetter original, Entity entity, AABB box) {
        if (!(original instanceof Level level)) return original;
        var topology=PlanetBorders.get(level.getWorldBorder());
        if(topology==null || box.minX>=topology.west()+2 && box.maxX<=topology.east()-2
                || box.maxX<topology.west()-PlanetBorders.guardMargin()-4
                || box.minX>topology.east()+PlanetBorders.guardMargin()+4
                || entity!=null && ShipyardSpace.reserved(level,entity.chunkPosition().x,entity.chunkPosition().z)) return original;
        return new SeamCollisions(level,topology);
    }
    @Override public BlockGetter getChunkForCollisions(int x,int z) {
        if(SeamGeometry.canonical(topology,x) || ShipyardSpace.reserved(level,x,z))
            return level instanceof net.minecraft.server.level.ServerLevel server
                    ? server.getChunkSource().getChunkNow(x,z):level.getChunkForCollisions(x,z);
        BlockGetter source=level instanceof SeamCollisionChunks client ? client.around_planet$collisionImage(x,z)
                : level instanceof net.minecraft.server.level.ServerLevel server
                    ? server.getChunkSource().getChunkNow(SeamGeometry.sourceX(topology,x),z)
                    : level.getChunkForCollisions(SeamGeometry.sourceX(topology,x),z);
        // Missing previews/destination columns are solid until ready, never synchronously generated.
        return source==null?missing:source;
    }
    private BlockGetter chunk(BlockPos pos){return getChunkForCollisions(pos.getX()>>4,pos.getZ()>>4);}
    private BlockPos sourcePos(BlockPos pos) {
        return topology.outside(pos.getX()) && !(level instanceof SeamCollisionChunks)
                && !ShipyardSpace.reserved(level,pos.getX()>>4,pos.getZ()>>4)
                ? new BlockPos((int)topology.canonicalX(pos.getX()),pos.getY(),pos.getZ()):pos;
    }
    @Override public BlockState getBlockState(BlockPos pos) {
        var chunk=chunk(pos);return chunk==null?Blocks.AIR.defaultBlockState():chunk.getBlockState(sourcePos(pos));
    }
    @Override public FluidState getFluidState(BlockPos pos) {
        var chunk=chunk(pos);return chunk==null?Blocks.AIR.defaultBlockState().getFluidState():chunk.getFluidState(sourcePos(pos));
    }
    @Override public BlockEntity getBlockEntity(BlockPos pos) {
        var chunk=chunk(pos);return chunk==null?null:chunk.getBlockEntity(sourcePos(pos));
    }
    @Override public int getHeight(){return level.getHeight();}
    @Override public int getMinBuildHeight(){return level.getMinBuildHeight();}
    @Override public WorldBorder getWorldBorder(){return level.getWorldBorder();}
    @Override public List<VoxelShape> getEntityCollisions(Entity entity,AABB box){return level.getEntityCollisions(entity,box);}
    private final class MissingColumn implements BlockGetter {
        @Override public BlockState getBlockState(BlockPos pos){return Blocks.BARRIER.defaultBlockState();}
        @Override public FluidState getFluidState(BlockPos pos){return Blocks.AIR.defaultBlockState().getFluidState();}
        @Override public BlockEntity getBlockEntity(BlockPos pos){return null;}
        @Override public int getHeight(){return level.getHeight();}
        @Override public int getMinBuildHeight(){return level.getMinBuildHeight();}
    }
}
