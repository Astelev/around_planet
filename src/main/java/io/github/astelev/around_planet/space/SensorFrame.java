package io.github.astelev.around_planet.space;

/** The model's +Y (top) is forward, +X is right, and its red +Z side is instrument up. */
public record SensorFrame(SpaceVector forward,SpaceVector right,SpaceVector up) {
    public static SensorFrame of(SpaceVector forward){
        var right=Math.abs(forward.y())>.5?new SpaceVector(1,0,0):forward.cross(new SpaceVector(0,1,0));
        return new SensorFrame(forward,right,right.cross(forward));
    }
    public SpaceVector instrument(SpaceVector v){return new SpaceVector(v.dot(right),v.dot(up),v.dot(forward));}
    public SpaceVector model(SpaceVector v){return right.scale(v.x()).add(forward.scale(v.y())).add(up.scale(v.z()));}
    /** Output is on the side the nose deviates toward, opposite the target's lateral component. */
    public int power(SpaceVector target,SpaceVector side,int scale){
        if(Math.abs(side.dot(forward))>.5)return 0;
        var error=OrbitalAttitude.error(target,forward,right);
        double angle=side.dot(right)*-error.yaw()+side.dot(up)*-error.pitch();
        return angle>0?OrbitalAttitude.power(angle,scale):0;
    }
}
