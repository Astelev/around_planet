package io.github.astelev.around_planet.space;
/** Planet-centered inertial axes; independent of the owner's local entry basis. */
public enum OrbitAxes {
    X(new SpaceVector(1,0,0),0xFFFF7478),Y(new SpaceVector(0,1,0),0xFF91E69B),Z(new SpaceVector(0,0,1),0xFF78B7FF);
    public final SpaceVector vector;public final int color;
    OrbitAxes(SpaceVector vector,int color){this.vector=vector;this.color=color;}
    public int dim(){return 0xFF000000|((color&0xFEFEFE)>>>1);}
}
