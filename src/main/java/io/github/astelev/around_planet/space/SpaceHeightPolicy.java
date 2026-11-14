package io.github.astelev.around_planet.space;
/** Configured destinations are high; old saved addresses stay readable until safe relocation. */
public final class SpaceHeightPolicy {
    public static final int DEFAULT=6144,MIN=3072,MAX=30000,LEGACY_MIN=1400,LEGACY_MAX=1700,REVISION=2;
    public static int migrate(int height,int revision){return revision<REVISION && height>=LEGACY_MIN && height<=LEGACY_MAX?DEFAULT:height;}
    private SpaceHeightPolicy(){}
}
