package io.github.astelev.around_planet.propulsion;
import net.minecraft.world.level.Level;
import java.util.*;
/** Only loaded block entities are visited; no world/chunk scans or background threads. */
@net.neoforged.fml.common.EventBusSubscriber(modid="around_planet")
public final class PropulsionRuntime {
    private static final Map<Level,Set<PropulsionBlockEntity>> LOADED=new WeakHashMap<>();
    public static synchronized void add(PropulsionBlockEntity be){LOADED.computeIfAbsent(be.getLevel(),k->Collections.newSetFromMap(new IdentityHashMap<>())).add(be);}
    public static synchronized void remove(PropulsionBlockEntity be){var s=LOADED.get(be.getLevel());if(s!=null){s.remove(be);if(s.isEmpty())LOADED.remove(be.getLevel());}}
    public static synchronized java.util.List<PropulsionBlockEntity> loaded(Level level){var s=LOADED.get(level);return s==null?List.of():List.copyOf(s);}
    @net.neoforged.bus.api.SubscribeEvent public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload event){if(event.getLevel() instanceof Level level)for(var be:loaded(level)){be.stop();remove(be);}}
    private PropulsionRuntime(){}
}
