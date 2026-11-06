package io.github.astelev.around_planet.client;

/** Native ray casts and game-mode prediction can read snapshots without inserting gameplay chunks. */
public final class SeamInteractionScope {
    private static final ThreadLocal<Integer> DEPTH=ThreadLocal.withInitial(()->0);
    public static void enter(){DEPTH.set(DEPTH.get()+1);}
    public static void exit(){int n=DEPTH.get()-1;if(n==0)DEPTH.remove();else DEPTH.set(n);}
    public static boolean active(){return DEPTH.get()>0;}
    private SeamInteractionScope(){}
}
