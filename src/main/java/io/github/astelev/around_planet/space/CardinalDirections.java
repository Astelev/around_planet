package io.github.astelev.around_planet.space;
/** Same geographic convention as Minecraft's planet chart: +Z is south, +X is east. */
public final class CardinalDirections {
    public static SpaceFrame at(SpaceVector position){
        if(position.length()<1e-9)return SpaceFrame.IDENTITY;
        var up=position.scale(1/position.length());var east=new SpaceVector(up.y(),-up.x(),0);
        east=east.length()<1e-9?new SpaceVector(1,0,0):east.scale(1/east.length());
        return new SpaceFrame(east,up,east.cross(up));
    }
    private CardinalDirections(){}
}
