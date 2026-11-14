package io.github.astelev.around_planet.space;
/** Instrument coordinates: right, up, forward; visible hemisphere has positive forward. */
public final class NavballMath {
    public static SpaceVector instrument(SpaceVector v,SpaceVector forward){return new SpaceVector(v.dot(forward.cross(new SpaceVector(0,1,0))),v.y(),v.dot(forward));}
    public static SpaceVector fromAngles(double yaw,double pitch){double y=Math.toRadians(yaw),p=Math.toRadians(pitch);return new SpaceVector(Math.sin(y)*Math.cos(p),Math.sin(p),Math.cos(y)*Math.cos(p));}
    public static int color(double x,double y,SpaceVector up){double d=1-x*x-y*y;if(d<0)return 0;double z=Math.sqrt(d),h=x*up.x()+y*up.y()+z*up.z();boolean sky=h>=0;double light=.6+.4*z;int r=sky?40:139,g=sky?115:77,b=sky?181:40;if(Math.abs(h)<.025){r=225;g=225;b=215;}else if(Math.abs(Math.sin(6*Math.asin(Math.clamp(h,-1,1))))<.075){r+=28;g+=28;b+=28;}return 0xFF000000|(int)(r*light)<<16|(int)(g*light)<<8|(int)(b*light);}
    private NavballMath(){}
}
