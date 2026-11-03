package io.github.astelev.cruising.compat.dh;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.BitSet;

/** Reads existing render-source columns; no database, generation or height queries. */
public final class DhLodCoverageBridge {
    private static Access access;
    private static volatile boolean loggedFailure;
    private DhLodCoverageBridge() {}
    private static synchronized Access access() throws ReflectiveOperationException {
        if(access==null) access=new Access();
        return access;
    }
    public static DhLodCoverage.Source capture(Object source) {
        if(source==null) return null;
        try {
            var a=access();
            long pos=a.sourcePos.getLong(source);
            int minX=(Integer)a.minX.invoke(null,pos), minZ=(Integer)a.minZ.invoke(null,pos);
            int cell=(Integer)a.width.invoke(null,pos)/64;
            if(cell<=0) return null;
            var columns=new BitSet(4096);
            if((Boolean)a.empty.invoke(source)) return new DhLodCoverage.Source(minX,minZ,cell,columns);
            // Read the existing primitive array directly; no reflection/boxing
            // for each of the 4096 columns in this latency-sensitive build path.
            var data=(it.unimi.dsi.fastutil.longs.LongArrayList)a.data.get(source);
            int stride=a.stride.getInt(source);
            for(int column=0;column<4096;column++) {
                if(data.getLong(column*stride)!=0L) columns.set(column);
            }
            return new DhLodCoverage.Source(minX,minZ,cell,columns);
        } catch(ReflectiveOperationException | RuntimeException e) {
            if(!loggedFailure) {
                loggedFailure=true;
                io.github.astelev.cruising.CruisingSpeed.LOGGER.warn("DH column coverage unavailable; normal DH mesh generation continues",e);
            }
            return null;
        }
    }
    public static boolean[] coverage(Object params, int originX, int originZ, int size, int tileSize) throws ReflectiveOperationException {
        var a=access();
        var result=new boolean[size*size];
        var sections=new ArrayList<Object>();
        Object tree=a.tree.get(a.handler.get(params));
        a.enabled.invoke(tree,sections);
        for(Object section:sections) {
            Object buffer=a.buffer.get(section);
            if(buffer==null || !a.uploaded.getBoolean(buffer) || (Integer)a.vboCount.invoke(buffer)==0) continue;
            var source=DhLodCoverage.get(buffer);
            if(source!=null) source.mark(result,originX,originZ,size,tileSize);
        }
        return result;
    }
    private static final class Access {
        final Field sourcePos,handler,tree,buffer,uploaded,data,stride;
        final Method minX,minZ,width,enabled,empty,vboCount;
        Access() throws ReflectiveOperationException {
            var source=Class.forName("com.seibel.distanthorizons.core.dataObjects.render.ColumnRenderSource");
            sourcePos=source.getField("pos");
            empty=source.getMethod("isEmpty");
            data=source.getField("renderDataContainer");
            stride=source.getField("maxVerticalSliceCount");
            var pos=Class.forName("com.seibel.distanthorizons.core.pos.DhSectionPos");
            minX=pos.getMethod("getMinCornerBlockX",long.class);
            minZ=pos.getMethod("getMinCornerBlockZ",long.class);
            width=pos.getMethod("getBlockWidth",long.class);
            handler=Class.forName("com.seibel.distanthorizons.core.render.RenderParams").getField("renderBufferHandler");
            tree=Class.forName("com.seibel.distanthorizons.core.render.RenderBufferHandler").getField("lodQuadTree");
            enabled=tree.getType().getMethod("populateListWithEnabledRenderSections",ArrayList.class);
            buffer=Class.forName("com.seibel.distanthorizons.core.render.QuadTree.LodRenderSection").getField("renderBufferContainer");
            var container=Class.forName("com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding.LodBufferContainer");
            uploaded=container.getField("buffersUploaded");
            vboCount=container.getMethod("vboBufferCount");
        }
    }
}
