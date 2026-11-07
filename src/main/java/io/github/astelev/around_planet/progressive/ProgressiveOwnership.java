package io.github.astelev.around_planet.progressive;
/** New Cruising owns scheduling even when its feature is disabled by the user. */
public final class ProgressiveOwnership {
    private static final boolean CRUISING=present();
    private static boolean present(){try{Class.forName("io.github.astelev.cruising.compat.dh.progressive.ProgressiveConfig",false,ProgressiveOwnership.class.getClassLoader());return true;}catch(ClassNotFoundException e){return false;}}
    public static boolean cruisingOwnsGeneration(){return CRUISING;}
    public static String diagnostic(Object provider){try {
        return "cruising:"+Class.forName("io.github.astelev.cruising.compat.dh.progressive.ProgressiveProviderAccess")
                .getMethod("cruising$progressiveDiagnostic").invoke(provider);
    }catch(ReflectiveOperationException e){return "cruising:unavailable";}}
    private ProgressiveOwnership(){}
}
