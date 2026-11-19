package io.github.astelev.around_planet.compat;
import io.github.astelev.around_planet.space.CurvatureGrid;
import java.lang.invoke.*;
/** Optional bridge called only by the loaded Cruising mesh mixin. No compile/runtime dependency is added. */
public final class CruisingCurvatureMesh {
    private static final MethodHandle VERTEX=vertex();
    private static MethodHandle vertex(){try{
        var type=Class.forName("io.github.astelev.cruising.compat.dh.FallbackSurfaceMesh$VertexSink");
        return MethodHandles.publicLookup().findVirtual(type,"vertex",MethodType.methodType(void.class,float.class,float.class,float.class,float.class,float.class)).asType(MethodType.methodType(void.class,Object.class,float.class,float.class,float.class,float.class,float.class));
    }catch(ReflectiveOperationException e){throw new IllegalStateException("Unsupported Cruising fallback mesh API",e);}}
    public static void top(Object sink,float x0,float z0,float x1,float z1){CurvatureGrid.top(x0,z0,x1,z1,(x,y,z,u,v)->{
        try{VERTEX.invokeExact(sink,x,y,z,u,v);}catch(Throwable e){throw new IllegalStateException("Cruising fallback vertex write",e);}
    });}
    private CruisingCurvatureMesh(){}
}
