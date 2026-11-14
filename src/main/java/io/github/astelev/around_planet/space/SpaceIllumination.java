package io.github.astelev.around_planet.space;
/** Minimum linear-lightmap RGB in space; preserves brighter block lights and face shading. */
public final class SpaceIllumination {
    public static int ambient(int abgr){int r=Math.max(105,abgr&255),g=Math.max(111,(abgr>>>8)&255),b=Math.max(122,(abgr>>>16)&255);return 0xff000000|(b<<16)|(g<<8)|r;}
    private SpaceIllumination(){}
}
