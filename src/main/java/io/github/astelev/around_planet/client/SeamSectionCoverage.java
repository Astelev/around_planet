package io.github.astelev.around_planet.client;

import io.github.astelev.around_planet.seam.SeamGeometry;
import io.github.astelev.around_planet.mixin.SeamLevelRendererAccess;
import io.github.astelev.around_planet.mixin.SeamViewAreaAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class SeamSectionCoverage {
    public static LevelChunk data(ClientLevel level,int x,int z) {
        return SeamGeometry.clipped(level,x,z)?SeamImages.get(level,x,z):level.getChunkSource().getChunk(x,z,ChunkStatus.FULL,false);
    }
    public static boolean ready(ClientLevel level,int x,int y,int z,RenderType layer) {
        var chunk=data(level,x,z);if(chunk==null || y<level.getMinSection() || y>=level.getMaxSection())return false;
        if(chunk.getSection(level.getSectionIndexFromSectionY(y)).hasOnlyAir())return true;
        Boolean alternative=AlternativeRendererBridge.hasGeometry(x,y,z);
        if(alternative!=null)return alternative;
        var mc=Minecraft.getInstance();var origin=new BlockPos(x*16,y*16,z*16);
        if(!mc.levelRenderer.isSectionCompiled(origin))return false;
        var view=((SeamLevelRendererAccess)mc.levelRenderer).around_planet$viewArea();if(view==null)return false;
        var section=((SeamViewAreaAccess)view).around_planet$section(origin);if(section==null)return false;
        var compiled=section.getCompiled();
        return layer==null?!compiled.hasNoRenderableLayers():!compiled.isEmpty(layer);
    }
    private SeamSectionCoverage() {}
}
