package io.github.astelev.around_planet.client;

/** Restrict periodic render poses to actual rendering; client ticks/collisions never see translated physical poses. */
public final class ShipRenderScope {
    private static final ThreadLocal<Integer> DEPTH=ThreadLocal.withInitial(()->0);
    public static void enter(){DEPTH.set(DEPTH.get()+1);}
    public static void exit(){int depth=DEPTH.get()-1;if(depth==0)DEPTH.remove();else DEPTH.set(depth);}
    public static boolean active(){return DEPTH.get()>0;}
    private ShipRenderScope(){}
}
