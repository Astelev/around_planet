package io.github.astelev.around_planet.space;
/** Fixed inertial sky. Sunlight and the terminator stay inertial while surface geography rotates. */
public final class SpaceEnvironment {
    public static final SpaceVector SUN_DIRECTION=new SpaceVector(0,1,0);
    public static double illumination(SpaceVector normal){return .045+.955*Math.max(0,normal.dot(SUN_DIRECTION));}
    public static int planetColor(SpaceVector n){return planetColor(n,illumination(n));}
    public static int planetColor(SpaceVector n,double light){double land=Math.sin(n.x()*12+Math.sin(n.z()*7))*Math.cos(n.y()*9)+.4*Math.sin(n.z()*19+n.x()*6);int r=land>.3?68:26,g=land>.3?124:91,b=land>.3?70:185;if(Math.abs(n.z())>.93){r=205;g=220;b=230;}return 0xFF000000|(int)(r*light)<<16|(int)(g*light)<<8|(int)(b*light);}
    public static SpaceVector observer(SpaceState orbit,SpaceFrame basis,SpaceVector physicalCamera,SpaceVector anchor){return orbit.position().add(basis.toCosmic(physicalCamera.subtract(anchor)));}
    public static double angularRadius(double radius,double distance){if(!(radius>0) || !Double.isFinite(distance) || distance<=radius)throw new IllegalArgumentException("Observer must be outside planet");return Math.asin(radius/distance);}
    private SpaceEnvironment(){}
}
