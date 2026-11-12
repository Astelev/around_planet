package io.github.astelev.around_planet.space;
/** Bounded tessellation for large fallback quads; the regular 64-block biome mesh is untouched. */
public final class CurvatureGrid {
    @FunctionalInterface public interface Vertex {void put(float x,float y,float z,float u,float v);}
    public static void top(float x0,float z0,float x1,float z1,Vertex sink){
        if(!Float.isFinite(x0)||!Float.isFinite(x1)||!Float.isFinite(z0)||!Float.isFinite(z1)||x1<x0||z1<z0)throw new IllegalArgumentException("Grid bounds");
        if(x1==x0 || z1==z0)return;
        int nx=Math.clamp((int)Math.ceil((x1-(double)x0)/512),1,64),nz=Math.clamp((int)Math.ceil((z1-(double)z0)/512),1,64);
        for(int x=0;x<nx;x++)for(int z=0;z<nz;z++){
            float a=x0+(x1-x0)*x/nx,b=x0+(x1-x0)*(x+1)/nx,c=z0+(z1-z0)*z/nz,d=z0+(z1-z0)*(z+1)/nz;
            sink.put(a,0,c,a,c);sink.put(a,0,d,a,d);sink.put(b,0,d,b,d);sink.put(b,0,c,b,c);
        }
    }
    private CurvatureGrid(){}
}
