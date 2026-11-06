package io.github.astelev.around_planet.compat;

import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.topology.EastWestTopology;
import net.minecraft.world.level.Level;

/** DH 3.3.3 section encoding; isolated from all DH class dependencies. */
public final class DhSeamBounds {
    public static long minX(long pos) { int x=(int)((pos>>8)&0xfffffff); x=x<<4>>4; return (long)x << (int)(pos&127); }
    public static long maxX(long pos) { return minX(pos)+(1L<<(int)(pos&127)); }
    public static boolean outside(EastWestTopology topology,long pos) {
        return minX(pos)>=topology.east() || maxX(pos)<=topology.west();
    }
    public static boolean inside(EastWestTopology topology,long pos) {
        return minX(pos)>=topology.west() && maxX(pos)<=topology.east();
    }
    private static final ClassValue<java.lang.reflect.Field> LEVEL_FIELD=new ClassValue<>() {
        @Override protected java.lang.reflect.Field computeValue(Class<?> type) {
            try {var f=type.getDeclaredField("level");f.setAccessible(true);return f;}
            catch(ReflectiveOperationException e) {throw new IllegalStateException(e);}
        }
    };
    public static boolean generationOutside(Object queue,long pos) {
        try {
            Object dhLevel=LEVEL_FIELD.get(queue.getClass()).get(queue);
            Object wrapper=Class.forName("com.seibel.distanthorizons.core.level.IDhServerLevel")
                    .getMethod("getServerLevelWrapper").invoke(dhLevel);
            Object nativeLevel=Class.forName("com.seibel.distanthorizons.api.interfaces.IDhApiUnsafeWrapper")
                    .getMethod("getWrappedMcObject").invoke(wrapper);
            if(nativeLevel instanceof Level level) {
                var topology=PlanetBorders.get(level.getWorldBorder());
                return topology!=null && outside(topology,pos);
            }
        } catch(ReflectiveOperationException | IllegalStateException ignored) {
            // Unknown DH implementations are not given a global/world-independent boundary.
        }
        return false;
    }
    private DhSeamBounds() {}
}
