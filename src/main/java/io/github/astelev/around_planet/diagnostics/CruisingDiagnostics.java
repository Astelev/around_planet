package io.github.astelev.around_planet.diagnostics;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.lang.reflect.*;

/** Optional, read-only probe. Never publishes flight state or changes Cruising configuration. */
public final class CruisingDiagnostics {
    private static boolean initialized;private static Field speed;private static Class<?> state,plane,coverage,config;
    private static String failure="absent";
    private static void init() {
        if(initialized)return;initialized=true;if(!ModList.get().isLoaded("cruising"))return;
        try {
            speed=Class.forName("io.github.astelev.cruising.client.DhFlightController").getDeclaredField("speed");speed.setAccessible(true);
            state=Class.forName("io.github.astelev.cruising.compat.dh.DhFlightState");
            plane=Class.forName("io.github.astelev.cruising.client.FallbackPlaneRenderer");
            coverage=Class.forName("io.github.astelev.cruising.compat.dh.DhLodCoverage");
            config=Class.forName("io.github.astelev.cruising.compat.dh.DhFlightConfig");failure=null;
        }catch(ReflectiveOperationException | RuntimeException | LinkageError e){failure="probe_unavailable:"+e.getClass().getSimpleName();}
    }
    public static double cameraSpeed(){init();if(failure!=null)return Double.NaN;try{return speed.getDouble(null);}catch(IllegalAccessException | RuntimeException | LinkageError e){return Double.NaN;}}
    private static Object call(Class<?> owner,String method) throws ReflectiveOperationException {return owner.getMethod(method).invoke(null);}
    public static String snapshot() {
        init();if(failure!=null)return failure;
        try {
            var text=new StringBuilder("cameraSpeed=").append(cameraSpeed()).append(" detailFloor=").append(call(state,"minimumDetail"))
                    .append(" gate=").append(call(state,"recentGate")).append(" active=").append(call(state,"bypassMotionPause"))
                    .append(" fallbackFrames=").append(call(plane,"drawnFrames")).append(" fallbackSkip=").append(call(plane,"lastSkip"))
                    .append(" coverageBuffers=").append(call(coverage,"trackedCount"));
            for(String key:new String[]{"ENABLED","MODIFY_DH","ADAPT_ABOVE_Y","COARSE_DETAIL","FAST_SPEED","QUIET_SECONDS","REFINE_SECONDS","FALLBACK_PLANE","FALLBACK_HEIGHTS","SMOOTH_TERRAIN"})
                try {var value=config.getField(key).get(null);if(value instanceof ModConfigSpec.ConfigValue<?> v)text.append(' ').append(key).append('=').append(v.get());}
                catch(NoSuchFieldException ignored){text.append(' ').append(key).append("=unavailable");}
            return text.toString();
        }catch(ReflectiveOperationException | RuntimeException | LinkageError e){return "probe_error:"+e.getClass().getSimpleName();}
    }
    private CruisingDiagnostics(){}
}
