package io.github.astelev.aerospace.compat.dh;

import java.util.BitSet;
import java.util.WeakHashMap;

/** CPU coverage belongs to the exact uploaded buffer, not a position's next rebuild. */
public final class DhLodCoverage {
    private static final WeakHashMap<Object,Source> BUFFERS = new WeakHashMap<>();
    private DhLodCoverage() {}
    public static synchronized void attach(Object buffer, Source source) { if (buffer != null && source != null) BUFFERS.put(buffer,source); }
    public static synchronized int trackedCount() { return BUFFERS.size(); }
    public static synchronized Source get(Object buffer) { return BUFFERS.get(buffer); }

    public record Source(int minX, int minZ, int cellSize, BitSet columns) {
        public Source { columns = (BitSet) columns.clone(); }
        public void mark(boolean[] covered, int originX, int originZ, int width) {
            mark(covered,originX,originZ,width,FallbackBiomeColors.CELL_SIZE);
        }
        public void mark(boolean[] covered, int originX, int originZ, int width, int tile) {
            int startX = Math.max(0, Math.floorDiv(minX,tile)-originX);
            int startZ = Math.max(0, Math.floorDiv(minZ,tile)-originZ);
            int endX = Math.min(width, Math.floorDiv(minX+64*cellSize-1,tile)-originX+1);
            int endZ = Math.min(width, Math.floorDiv(minZ+64*cellSize-1,tile)-originZ+1);
            for(int x=startX;x<endX;x++) for(int z=startZ;z<endZ;z++) {
                int bx=(originX+x)*tile-minX, bz=(originZ+z)*tile-minZ;
                if(bx<0 || bz<0 || bx+tile>64*cellSize || bz+tile>64*cellSize) continue;
                int fromX=bx/cellSize, fromZ=bz/cellSize;
                int toX=(bx+tile-1)/cellSize, toZ=(bz+tile-1)/cellSize;
                boolean complete=true;
                for(int cx=fromX;cx<=toX && complete;cx++) for(int cz=fromZ;cz<=toZ;cz++) {
                    if(!columns.get(cx*64+cz)) { complete=false; break; }
                }
                if(complete) covered[x*width+z]=true;
            }
        }
    }
}
