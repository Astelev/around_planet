package io.github.astelev.around_planet.space;
/** Planet-fixed latitude/longitude checker, lit in cosmic axes. ARGB grayscale. */
public final class SphereGrid {
    public static int color(SpaceVector normal){return color(normal.x(),normal.y(),normal.z());}
    public static int color(double nx,double ny,double nz){
        double lon=Math.atan2(nx,ny),lat=Math.asin(Math.max(-1,Math.min(1,nz)));
        int x=(int)Math.floor((lon+Math.PI)*16/Math.PI),y=Math.min(15,(int)Math.floor((lat+Math.PI/2)*16/Math.PI));
        double light=Math.max(0,nx*.3030457634+ny*.8081220356+nz*.5050762723);
        int value=(int)Math.round((((x+y)&1)==0?250:145)*(.55+.45*light));return 0xFF000000|value<<16|value<<8|value;
    }
    private SphereGrid(){}
}
