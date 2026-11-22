package io.github.astelev.around_planet.client;
/** Public optional AeroWorks fog API. Its completed-world queue preserves near effects. */
public final class AeroFogBridge {
    private static boolean searched;private static java.lang.reflect.Method draw,strength;
    private static void find(){if(searched)return;searched=true;if(!net.neoforged.fml.ModList.get().isLoaded("aste_aeroworks"))return;try{strength=Class.forName("net.aste.aeroworks.client.CloudFogEvents").getMethod("strength");}catch(ReflectiveOperationException|LinkageError ignored){}try{draw=Class.forName("net.aste.aeroworks.client.CloudFogPostProcess").getMethod("drawEffect",Runnable.class);}catch(ReflectiveOperationException|LinkageError ignored){}}
    public static void submit(Runnable effect){find();if(draw!=null)try{draw.invoke(null,effect);return;}catch(ReflectiveOperationException e){throw new IllegalStateException("AeroWorks deferred exhaust failed",e);}effect.run();}
    /** Legacy fog-only versions have no completed-world pass to fog a custom sky. */
    public static float legacySkyStrength(){find();if(draw!=null||strength==null)return 0;try{return Math.clamp(((Number)strength.invoke(null)).floatValue(),0,1);}catch(ReflectiveOperationException e){return 0;}}
    public static boolean hasWorldFog(){find();return draw!=null;}
    private AeroFogBridge(){}
}
