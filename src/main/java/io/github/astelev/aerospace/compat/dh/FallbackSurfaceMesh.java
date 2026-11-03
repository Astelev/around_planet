package io.github.astelev.aerospace.compat.dh;

/** Shared-corner ramps and downward skirts along ownership boundaries. */
public final class FallbackSurfaceMesh {
    private FallbackSurfaceMesh() {}
    @FunctionalInterface public interface QuadSink {
        void quad(float x0, float y0, float z0, float x1, float y1, float z1,
                  float x2, float y2, float z2, float x3, float y3, float z3);
    }
    @FunctionalInterface public interface VertexSink {
        void vertex(float x,float y,float z,float sampleX,float sampleZ);
    }
    public static void build(int[][] heights,int cellSize,float extent,QuadSink out) {
        var vertices=new float[12];
        int[] index={0};
        build(heights,null,cellSize,extent,null,heights.length,-256,(x,y,z,u,v) -> {
            vertices[index[0]++]=x; vertices[index[0]++]=y; vertices[index[0]++]=z;
            if(index[0]==12) {
                out.quad(vertices[0],vertices[1],vertices[2],vertices[3],vertices[4],vertices[5],
                        vertices[6],vertices[7],vertices[8],vertices[9],vertices[10],vertices[11]);
                index[0]=0;
            }
        });
    }
    public static float[][] corners(int[][] heights,boolean[][] water) {
        int n=heights.length;
        var result=new float[n+1][n+1];
        for(int x=0;x<=n;x++) for(int z=0;z<=n;z++) {
            float sum=0; int wet=0; float sea=0;
            for(int dx=-1;dx<=0;dx++) for(int dz=-1;dz<=0;dz++) {
                int cx=Math.max(0,Math.min(n-1,x+dx)), cz=Math.max(0,Math.min(n-1,z+dz));
                sum+=heights[cx][cz];
                if(water!=null && water[cx][cz]) { sea+=heights[cx][cz]; wet++; }
            }
            result[x][z]=wet==0 ? sum/4 : sea/wet;
        }
        return result;
    }
    public static void build(int[][] heights,boolean[][] water,int cellSize,float extent,
                             boolean[] covered,int coverageWidth,float bottom,VertexSink out) {
        int n=heights.length;
        float span=n*cellSize;
        var nodes=corners(heights,water);
        for(int x=0;x<n;x++) for(int z=0;z<n;z++) {
            float x0=x*cellSize,z0=z*cellSize,x1=x0+cellSize,z1=z0+cellSize;
            float mx=(x0+x1)/2,mz=(z0+z1)/2,center=heights[x][z];
            // Four triangles retain the original biome height at the center.
            vertex(out,x0,nodes[x][z],z0); vertex(out,x0,nodes[x][z+1],z1);
            vertex(out,mx,center,mz); vertex(out,x1,nodes[x+1][z],z0);
            vertex(out,x1,nodes[x+1][z+1],z1); vertex(out,x1,nodes[x+1][z],z0);
            vertex(out,mx,center,mz); vertex(out,x0,nodes[x][z+1],z1);
        }
        float step=span/coverageWidth;
        for(int x=0;x<coverageWidth;x++) for(int z=0;z<coverageWidth;z++) {
            if(covered!=null && covered[x*coverageWidth+z]) continue;
            float x0=x*step,z0=z*step,x1=x0+step,z1=z0+step;
            // Sample ownership/color at the unknown column's center, not at
            // the exact border which can round onto the already-ready side.
            float u=(x0+x1)/2,v=(z0+z1)/2;
            if(x==0 || covered!=null && covered[(x-1)*coverageWidth+z])
                skirtX(out,nodes,heights,cellSize,x0,z0,z1,bottom,u,v,true);
            if(x==coverageWidth-1 || covered!=null && covered[(x+1)*coverageWidth+z])
                skirtX(out,nodes,heights,cellSize,x1,z0,z1,bottom,u,v,false);
            if(z==0 || covered!=null && covered[x*coverageWidth+z-1])
                skirtZ(out,nodes,heights,cellSize,z0,x0,x1,bottom,u,v,false);
            if(z==coverageWidth-1 || covered!=null && covered[x*coverageWidth+z+1])
                skirtZ(out,nodes,heights,cellSize,z1,x0,x1,bottom,u,v,true);
        }
        float low=-extent,high=span+extent;
        top(out,low,low,high,0); top(out,low,span,high,high);
        top(out,low,0,0,span); top(out,span,0,high,span);
    }
    private static void vertex(VertexSink out,float x,float y,float z) { out.vertex(x,y,z,x,z); }
    private static void top(VertexSink out,float x0,float z0,float x1,float z1) {
        vertex(out,x0,0,z0); vertex(out,x0,0,z1); vertex(out,x1,0,z1); vertex(out,x1,0,z0);
    }
    static float sampleHeight(float[][] nodes,int[][] heights,int cell,float x,float z) {
        int n=nodes.length-1;
        int cx=Math.max(0,Math.min(n-1,(int)(x/cell))),cz=Math.max(0,Math.min(n-1,(int)(z/cell)));
        float tx=Math.max(0,Math.min(1,x/cell-cx)),tz=Math.max(0,Math.min(1,z/cell-cz));
        float a=nodes[cx][cz], b=nodes[cx][cz+1], c=nodes[cx+1][cz+1], d=nodes[cx+1][cz];
        // Match the four GPU triangles exactly, including the center vertex.
        float center=heights[cx][cz];
        if(tx+tz<=1) return tz>=tx ? a+(b-a)*tz+(2*center-a-b)*tx : a+(d-a)*tx+(2*center-a-d)*tz;
        float rx=1-tx,rz=1-tz;
        return tx>=tz ? c+(d-c)*rz+(2*center-c-d)*rx : c+(b-c)*rx+(2*center-c-b)*rz;
    }
    private static void skirtX(VertexSink out,float[][] h,int[][] centers,int cell,float x,float z0,float z1,float floor,float u,float v,boolean west) {
        float a=sampleHeight(h,centers,cell,x,z0), b=sampleHeight(h,centers,cell,x,z1);
        if(west) {
            out.vertex(x,floor,z1,u,v); out.vertex(x,b,z1,u,v); out.vertex(x,a,z0,u,v); out.vertex(x,floor,z0,u,v);
        } else {
            out.vertex(x,floor,z0,u,v); out.vertex(x,a,z0,u,v); out.vertex(x,b,z1,u,v); out.vertex(x,floor,z1,u,v);
        }
    }
    private static void skirtZ(VertexSink out,float[][] h,int[][] centers,int cell,float z,float x0,float x1,float floor,float u,float v,boolean south) {
        float a=sampleHeight(h,centers,cell,x0,z), b=sampleHeight(h,centers,cell,x1,z);
        if(south) {
            out.vertex(x1,floor,z,u,v); out.vertex(x1,b,z,u,v); out.vertex(x0,a,z,u,v); out.vertex(x0,floor,z,u,v);
        } else {
            out.vertex(x0,floor,z,u,v); out.vertex(x0,a,z,u,v); out.vertex(x1,b,z,u,v); out.vertex(x1,floor,z,u,v);
        }
    }
}
