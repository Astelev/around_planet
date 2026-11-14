package io.github.astelev.around_planet.atlas;
/** Planet-fixed equirectangular RGB atlas. Its chart is the same as PlanetGeometry. */
public final class PlanetAtlas {
    public static final int WIDTH=512,HEIGHT=256,SIZE=WIDTH*HEIGHT;
    public static double worldX(int column,int circumference){return ((column+.5)/WIDTH-.5)*circumference;}
    public static double worldZ(int row,int circumference){return ((row+.5)/HEIGHT-.5)*circumference;}
    public static int sample(int[] pixels,double nx,double ny,double nz){
        double u=Math.atan2(nx,ny)/(2*Math.PI)+.5,v=Math.asin(Math.clamp(nz,-1,1))/(Math.PI-2e-6)+.5;
        int x=Math.floorMod((int)Math.floor(u*WIDTH),WIDTH),y=Math.clamp((int)Math.floor(v*HEIGHT),0,HEIGHT-1);
        return pixels[y*WIDTH+x];
    }
    public static int abgr(int rgb){return 0xff000000|((rgb&255)<<16)|(rgb&0xff00)|((rgb>>>16)&255);}
    /** Server-safe grass approximation: vanilla's grass colormap is client-only. */
    public static int landColor(float temperature,float downfall){double t=Math.clamp(temperature,0,1),wet=Math.clamp(downfall,0,1)*t;int r=(int)(140-75*wet-10*t),g=(int)(155+15*wet+15*t),b=(int)(75-20*t+20*wet);return (r<<16)|(g<<8)|b;}
    private PlanetAtlas(){}
}
