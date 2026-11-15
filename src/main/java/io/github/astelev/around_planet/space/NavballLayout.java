package io.github.astelev.around_planet.space;
/** A larger corner instrument with a separate text column to its left. */
public record NavballLayout(int x,int y,int radius,int textRight,int textTop) {
    public static NavballLayout of(int width,int height,float scale,int ox,int oy,int rows){
        int r=Math.clamp(Math.round(52*scale),34,Math.max(34,Math.min(width,height)/3));
        int right=width-r-8,left=Math.min(right,r+198);
        int x=Math.clamp(width-r-10+ox,left,right),y=Math.clamp(height-r-18+oy,r+8,Math.max(r+8,height-r-18));
        return new NavballLayout(x,y,r,x-r-10,Math.clamp(y-rows*11/2,4,Math.max(4,height-rows*11-12)));
    }
    public static boolean targetMarkers(boolean active,java.util.UUID target,double distance){return active&&target!=null&&!CelestialSystem.body(target)&&Double.isFinite(distance)&&distance>=0&&distance<40000;}
}
