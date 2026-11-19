package io.github.astelev.around_planet.compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import java.lang.reflect.Method;
import java.util.*;
/** AeroEngine 1.3 public helmet API, cached lazily; no hard dependency or alternate binding format. */
public final class AeroHelmetBridge {
    private static final Map<String,Method> METHODS=new java.util.concurrent.ConcurrentHashMap<>();
    private static Object call(String method,ItemStack stack,Object... args){try{
        var m=METHODS.computeIfAbsent(method,n->{try{Class<?>[] types=new Class<?>[args.length+1];types[0]=ItemStack.class;for(int i=0;i<args.length;i++)types[i+1]=args[i] instanceof BlockPos?BlockPos.class:String.class;return Class.forName("com.cxw.aeroengineering.content.item.EngineMonitorHelmetItem").getMethod(n,types);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}});
        Object[] values=new Object[args.length+1];values[0]=stack;System.arraycopy(args,0,values,1,args.length);return m.invoke(null,values);
    }catch(ReflectiveOperationException e){throw new IllegalStateException("Unsupported AeroEngine helmet API",e);}}
    @SuppressWarnings("unchecked") public static List<BlockPos> targets(ItemStack s){return (List<BlockPos>)call("getTargets",s);}
    public static UUID structure(ItemStack s){return (UUID)call("getStructureId",s);}
    public static boolean enabled(ItemStack s,BlockPos p){return (Boolean)call("isTargetEnabled",s,p) && (Boolean)call("isDynamicHudEnabled",s) && (Boolean)call("isHudElementVisible",s,"attitude");}
    public static float scale(ItemStack s){return ((Number)call("getFlightHudScale",s)).floatValue()*((Number)call("getHudElementScale",s,"attitude")).floatValue();}
    public static int offset(ItemStack s,boolean x){return ((Number)call(x?"getHudElementOffsetX":"getHudElementOffsetY",s,"attitude")).intValue();}
    private AeroHelmetBridge(){}
}
