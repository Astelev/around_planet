package io.github.astelev.around_planet.client;

import io.github.astelev.around_planet.seam.SeamGeometry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LevelLightEngine;

/** Render-only light lookup: snapshots never enqueue propagation or clear the real world's lighting. */
final class SeamImageLight extends LevelLightEngine {
    final ClientLevel level;
    private final LayerLightEventListener sky,block;
    SeamImageLight(ClientLevel level) {
        super(level.getChunkSource(),false,false);this.level=level;
        sky=new Layer(LightLayer.SKY);block=new Layer(LightLayer.BLOCK);
    }
    @Override public LayerLightEventListener getLayerListener(LightLayer type) {return type==LightLayer.SKY?sky:block;}
    @Override public int getRawBrightness(BlockPos pos,int darkness) {
        return Math.max(block.getLightValue(pos),sky.getLightValue(pos)-darkness);
    }
    private final class Layer implements LayerLightEventListener {
        private final LightLayer type;
        Layer(LightLayer type){this.type=type;}
        @Override public DataLayer getDataLayerData(SectionPos section) {
            return SeamGeometry.clipped(level,section.x(),section.z())?SeamImages.lightData(level,type,section)
                    :level.getLightEngine().getLayerListener(type).getDataLayerData(section);
        }
        @Override public int getLightValue(BlockPos pos) {
            int x=pos.getX()>>4,z=pos.getZ()>>4;
            if(!SeamGeometry.clipped(level,x,z))return level.getLightEngine().getLayerListener(type).getLightValue(pos);
            if(SeamImages.get(level,x,z)==null)return 0;
            int y=pos.getY()>>4;var data=SeamImages.lightData(level,type,SectionPos.of(x,y,z));
            if(data!=null)return data.get(pos.getX()&15,pos.getY()&15,pos.getZ()&15);
            if(type==LightLayer.BLOCK || !level.dimensionType().hasSkyLight())return 0;
            // Omitted sky sections inherit the bottom of the next stored layer; above all layers is open sky.
            while(++y<=level.getMaxSection()) {
                data=SeamImages.lightData(level,type,SectionPos.of(x,y,z));
                if(data!=null)return data.get(pos.getX()&15,0,pos.getZ()&15);
            }
            return 15;
        }
        @Override public void checkBlock(BlockPos pos) {}
        @Override public boolean hasLightWork(){return false;}
        @Override public int runLightUpdates(){return 0;}
        @Override public void updateSectionStatus(SectionPos pos,boolean empty) {}
        @Override public void setLightEnabled(ChunkPos pos,boolean enabled) {}
        @Override public void propagateLightSources(ChunkPos pos) {}
    }
}
