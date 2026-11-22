package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.propulsion.NozzleLining;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
/** Outlet, plume width and hot lining follow the same sloped OBJ as the visible solid nozzle. */
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class SolidNozzleMesh {
    private static volatile NozzleLining mesh=NozzleLining.EMPTY;
    public static NozzleLining current(){return mesh;}
    @SubscribeEvent public static void register(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event){
        event.registerReloadListener(new SimplePreparableReloadListener<NozzleLining>() {
            @Override protected NozzleLining prepare(ResourceManager resources,ProfilerFiller profiler){try(var reader=resources.openAsReader(ResourceLocation.fromNamespaceAndPath("around_planet","models/block/solid_rocket_nozzle.obj"))){return NozzleLining.read(reader);}catch(java.io.IOException e){throw new IllegalStateException("Cannot load solid nozzle lining",e);}}
            @Override protected void apply(NozzleLining lining,ResourceManager resources,ProfilerFiller profiler){mesh=lining;}
        });
    }
    private SolidNozzleMesh(){}
}
